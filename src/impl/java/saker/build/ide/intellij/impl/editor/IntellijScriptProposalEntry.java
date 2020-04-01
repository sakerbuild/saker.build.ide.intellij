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


import saker.build.ide.intellij.extension.script.proposal.IScriptProposalEntry;
import saker.build.ide.support.ui.BaseScriptProposalEntry;
import saker.build.scripting.model.ScriptCompletionProposal;
import saker.build.scripting.model.TextPartition;

import javax.swing.Icon;

public class IntellijScriptProposalEntry extends BaseScriptProposalEntry<IntellijScriptInformationEntry>
		implements IScriptProposalEntry {
	private Icon proposalIcon;

	public IntellijScriptProposalEntry(ScriptCompletionProposal proposal) {
		super(proposal);
	}

	@Override
	protected IntellijScriptInformationEntry createInformationEntry(TextPartition partition) {
		return new IntellijScriptInformationEntry(partition);
	}

	@Override
	public void setProposalIcon(Icon proposalIcon) {
		this.proposalIcon = proposalIcon;
	}

	public Icon getProposalIcon() {
		return proposalIcon;
	}
}
