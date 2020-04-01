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
package saker.build.ide.intellij.extension.script.proposal;

import saker.build.ide.intellij.extension.script.information.IScriptInformationEntry;

import javax.swing.Icon;
import java.util.List;
import java.util.Map;

public interface IScriptProposalEntry {
    public String getDisplayString();

    public String getDisplayType();

    public String getDisplayRelation();

    public void setDisplayString(String display);

    public void setDisplayType(String type);

    public void setDisplayRelation(String relation);

    public void setProposalIcon(Icon icon);

    public List<? extends IScriptInformationEntry> getInformationEntries();

    public String getSchemaIdentifier();

    public Map<String, String> getSchemaMetaData();
}
