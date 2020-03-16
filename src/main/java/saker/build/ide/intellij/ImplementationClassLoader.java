/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.build.ide.intellij;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ImplementationClassLoader extends ClassLoader {
    private List<JarFile> jars;

    public ImplementationClassLoader(List<JarFile> jars) {
        super(ImplementationClassLoader.class.getClassLoader());
        this.jars = jars;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String cfilepath = name.replace('.', '/') + ".class";
        try (InputStream resin = getParent().getResourceAsStream("impl/" + cfilepath)) {
            if (resin != null) {
                byte[] classbytes = readStreamFully(resin);
                return defineClass(name, classbytes, 0, classbytes.length);
            }
        } catch (IOException ignored) {
        }
        for (JarFile jf : jars) {
            ZipEntry jfentry = jf.getEntry(cfilepath);
            if (jfentry != null) {
                byte[] classbytes;
                try (InputStream is = jf.getInputStream(jfentry)) {
                    classbytes = readStreamFully(is);
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
                return defineClass(name, classbytes, 0, classbytes.length);
            }
        }
        return super.findClass(name);
    }

    @Override
    protected URL findResource(String name) {
        for (JarFile jf : jars) {
            ZipEntry jfentry = jf.getEntry(name);
            if (jfentry != null) {
                try {
                    return jarFileEntryToURL(name, jf, jfentry);
                } catch (MalformedURLException e) {
                    //shouldn't happen
                    e.printStackTrace();
                }
            }
        }
        return super.findResource(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        Collection<URL> res = new ArrayList<>();
        for (JarFile jf : jars) {
            ZipEntry jfentry = jf.getEntry(name);
            if (jfentry != null) {
                try {
                    res.add(jarFileEntryToURL(name, jf, jfentry));
                } catch (MalformedURLException e) {
                    // shouldn't happen
                    e.printStackTrace();
                }
            }
        }
        return Collections.enumeration(res);
    }

    private static byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int r; (r = is.read(buf)) > 0; ) {
            baos.write(buf, 0, r);
        }
        byte[] classbytes = baos.toByteArray();
        return classbytes;
    }

    private static URL jarFileEntryToURL(String name, JarFile jf, ZipEntry jfentry) throws MalformedURLException {
        return new URL("sakerimplfile", null, 0, name, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return new URLConnection(u) {
                    @Override
                    public void connect() throws IOException {
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return jf.getInputStream(jfentry);
                    }
                };
            }
        });
    }

}
