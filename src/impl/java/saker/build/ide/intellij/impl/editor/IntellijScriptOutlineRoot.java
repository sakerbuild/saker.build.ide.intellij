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

import saker.build.ide.intellij.extension.script.outline.IScriptOutlineRoot;
import saker.build.ide.support.ui.BaseScriptOutlineRoot;
import saker.build.scripting.model.ScriptStructureOutline;
import saker.build.scripting.model.StructureOutlineEntry;

public class IntellijScriptOutlineRoot extends BaseScriptOutlineRoot<IntellijScriptOutlineEntry>
		implements IScriptOutlineRoot {

	private IntellijScriptOutlineRoot() {
	}

	public static IntellijScriptOutlineRoot create(ScriptStructureOutline outline) {
		IntellijScriptOutlineRoot result = new IntellijScriptOutlineRoot();
		result.init(outline);
		return result;
	}

	@Override
	protected IntellijScriptOutlineEntry createOutlineEntry(StructureOutlineEntry entry) {
		return new IntellijScriptOutlineEntry(this, entry);
	}

}
