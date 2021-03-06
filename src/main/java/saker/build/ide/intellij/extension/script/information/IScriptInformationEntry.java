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
package saker.build.ide.intellij.extension.script.information;

import java.util.Map;

public interface IScriptInformationEntry {
	public String getTitle();

	public String getSubTitle();

	public void setTitle(String title);

	public void setSubTitle(String subtitle);

	//TODO unimplemented as icon display doesn't work properly. will be readded as it is solved
	//doc: for <img src tag>, must not contain quotes
	//     probably something like "data:image/png;base64, "
//	public void setIconSource(String sourceurl) throws IllegalArgumentException;

	public String getSchemaIdentifier();

	public Map<String, String> getSchemaMetaData();
}
