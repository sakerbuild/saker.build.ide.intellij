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

import com.intellij.openapi.extensions.PluginId;

import java.util.Objects;

public final class ExtensionDisablement {
    private PluginId pluginId;
    private String extensionId;

    public ExtensionDisablement(PluginId pluginId, String extensionId) {
        this.pluginId = Objects.requireNonNull(pluginId, "plugin id");
        this.extensionId = Objects.requireNonNull(extensionId, "extension id");
    }

    public ExtensionDisablement(ContributedExtension ext) {
        this(ext.getPluginId(), ext.getId());
    }

    public PluginId getPluginId() {
        return pluginId;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public boolean isDisabled(PluginId pluginId, String extensionId) {
        if (extensionId == null) {
            //generally disable all extensions which doesn't have an unique identifier
            return true;
        }
        if (this.pluginId.equals(pluginId) && this.extensionId.equals(extensionId)) {
            return true;
        }
        return false;
    }

    public static boolean isDisabled(Iterable<? extends ExtensionDisablement> disablements, PluginId pluginId,
            String extensionId) {
        if (extensionId == null) {
            //generally disable all extensions which doesn't have an unique identifier
            return true;
        }
        for (ExtensionDisablement disablement : disablements) {
            if (disablement.isDisabled(pluginId, extensionId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExtensionDisablement that = (ExtensionDisablement) o;
        return pluginId.equals(that.pluginId) && extensionId.equals(that.extensionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginId, extensionId);
    }
}
