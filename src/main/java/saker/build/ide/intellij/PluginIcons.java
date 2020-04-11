package saker.build.ide.intellij;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.util.Version;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class PluginIcons {
    private static final Version INTELLIJ_FULL_VERSION;
    private static final boolean SVG_ICON_SUPPORTED;

    static {
        ApplicationInfo version = ApplicationInfo.getInstance();
        //e.g.
        //major: 2019
        //minor: 3.3
        Version theversion = Version.parseVersion(version.getMajorVersion() + "." + version.getMinorVersion());
        if (theversion == null) {
            theversion = new Version(-1, -1, -1);
        }
        INTELLIJ_FULL_VERSION = theversion;
        //svgs are only supported after 2018.2+
        //https://www.jetbrains.org/intellij/sdk/docs/reference_guide/work_with_icons_and_images.html
        SVG_ICON_SUPPORTED = INTELLIJ_FULL_VERSION.isOrGreaterThan(2018, 2);
    }

    public static final Icon SCRIPT_FILE = loadPng("/icons/icon_file");
    public static final Icon ICON_BOOL_FALSE = loadPng("/icons/bool_false");
    public static final Icon ICON_BOOL_TRUE = loadPng("/icons/bool_true");
    public static final Icon ICON_STRING = loadPng("/icons/string");
    public static final Icon ICON_STRINGLITERAL = loadPng("/icons/stringliteral");
    public static final Icon ICON_NUM = loadPng("/icons/num");
    public static final Icon ICON_MAP = loadPng("/icons/map");
    public static final Icon ICON_LIST = loadPng("/icons/list");
    public static final Icon ICON_TASK = loadPng("/icons/task");
    public static final Icon ICON_TARGET = loadPng("/icons/target");
    public static final Icon ICON_INPARAM = loadPng("/icons/inparam");
    public static final Icon ICON_OUTPARAM = loadPng("/icons/outparam");
    public static final Icon ICON_FOR = loadPng("/icons/foreach");
    public static final Icon ICON_VAR = loadPng("/icons/var");
    public static final Icon ICON_STOP_INTERRUPT = loadSvgOrPng("/icons/stop_interrupt");

    private static final Icon loadSvgOrPng(String path) {
        if (SVG_ICON_SUPPORTED) {
            Icon svgicon = IconLoader.findIcon(path + ".svg", PluginIcons.class, false, false);
            if (svgicon != null) {
                return svgicon;
            }
        }
        return loadPng(path);
    }

    @NotNull
    private static Icon loadPng(String path) {
        return IconLoader.getIcon(path + ".png");
    }
}
