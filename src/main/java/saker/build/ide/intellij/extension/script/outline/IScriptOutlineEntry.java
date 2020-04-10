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
package saker.build.ide.intellij.extension.script.outline;

import com.intellij.navigation.ItemPresentation;

import java.util.List;
import java.util.Map;

public interface IScriptOutlineEntry {
    public List<? extends IScriptOutlineEntry> getChildren();

    public String getLabel();

    public String getType();

    public ItemPresentation getEntryPresentation();

    public void setEntryPresentation(ItemPresentation presentation);

    public void setLabel(String label);

    public void setType(String type);

    public String getSchemaIdentifier();

    public Map<String, String> getSchemaMetaData();
}
