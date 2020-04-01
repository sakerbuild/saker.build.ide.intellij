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
package saker.build.ide.intellij.impl.editor;

import com.intellij.navigation.ItemPresentation;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineEntry;
import saker.build.ide.support.ui.BaseScriptOutlineEntry;
import saker.build.scripting.model.StructureOutlineEntry;

import javax.swing.Icon;

public class IntellijScriptOutlineEntry extends BaseScriptOutlineEntry<IntellijScriptOutlineEntry> implements IScriptOutlineEntry {
    private ItemPresentation widgetLabel;
    private StructureOutlineEntry entry;
    private Icon widgetIcon;

    IntellijScriptOutlineEntry(IntellijScriptOutlineRoot root, StructureOutlineEntry entry) {
        super(root, entry);

        this.entry = entry;
    }

    public StructureOutlineEntry getEntry() {
        return entry;
    }

    @Override
    public void setWidgetIcon(Icon icon) {
        this.widgetIcon = icon;
    }

    @Override
    public void setWidgetLabel(ItemPresentation label) {
        this.widgetLabel = label;
    }

    @Override
    public Icon getWidgetIcon() {
        return widgetIcon;
    }

    @Override
    public ItemPresentation getWidgetLabel() {
        return widgetLabel;
    }
}
