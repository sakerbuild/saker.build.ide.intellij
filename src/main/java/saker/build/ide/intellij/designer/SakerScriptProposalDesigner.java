package saker.build.ide.intellij.designer;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.DumbAware;
import saker.build.ide.intellij.PluginIcons;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalDesigner;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalEntry;
import saker.build.ide.intellij.extension.script.proposal.IScriptProposalsRoot;

import java.util.Map;

public class SakerScriptProposalDesigner implements IScriptProposalDesigner, DumbAware {
    public static final String PROPOSAL_SCHEMA = "saker.script";
    private static final String PROPOSAL_META_DATA_TYPE = "type";
    private static final String PROPOSAL_META_DATA_TYPE_FILE = "file";
    private static final String PROPOSAL_META_DATA_TYPE_ENUM = "enum";
    private static final String PROPOSAL_META_DATA_TYPE_EXTERNAL_LITERAL = "external_literal";
    private static final String PROPOSAL_META_DATA_TYPE_FIELD = "field";
    private static final String PROPOSAL_META_DATA_TYPE_TASK_PARAMETER = "task_parameter";
    private static final String PROPOSAL_META_DATA_TYPE_TASK = "task";
    private static final String PROPOSAL_META_DATA_TYPE_USER_PARAMETER = "user_parameter";
    private static final String PROPOSAL_META_DATA_TYPE_ENVIRONMENT_PARAMETER = "environment_parameter";
    private static final String PROPOSAL_META_DATA_TYPE_VARIABLE = "variable";
    private static final String PROPOSAL_META_DATA_TYPE_FOREACH_VARIABLE = "foreach_variable";
    private static final String PROPOSAL_META_DATA_TYPE_STATIC_VARIABLE = "static_variable";
    private static final String PROPOSAL_META_DATA_TYPE_GLOBAL_VARIABLE = "global_variable";
    private static final String PROPOSAL_META_DATA_TYPE_TASK_QUALIFIER = "task_qualifier";
    private static final String PROPOSAL_META_DATA_TYPE_LITERAL = "literal";
    private static final String PROPOSAL_META_DATA_TYPE_BUILD_TARGET = "build_target";

    private static final String PROPOSAL_META_DATA_FILE_TYPE = "file_type";
    private static final String PROPOSAL_META_DATA_FILE_TYPE_FILE = "file";
    private static final String PROPOSAL_META_DATA_FILE_TYPE_BUILD_SCRIPT = "build_script";
    private static final String PROPOSAL_META_DATA_FILE_TYPE_DIRECTORY = "dir";

    @Override
    public void process(IScriptProposalsRoot proposalsroot) {
        for (IScriptProposalEntry proposal : proposalsroot.getProposals()) {
            if (proposal == null) {
                continue;
            }
            if (!PROPOSAL_SCHEMA.equals(proposal.getSchemaIdentifier())) {
                continue;
            }
            processProposal(proposal);
        }
    }

    private static void processProposal(IScriptProposalEntry proposal) {
        Map<String, String> schememeta = proposal.getSchemaMetaData();
        if (schememeta != null) {
            String type = schememeta.get(PROPOSAL_META_DATA_TYPE);
            if (type != null) {
                switch (type) {
                    case PROPOSAL_META_DATA_TYPE_BUILD_TARGET: {
                        proposal.setProposalIcon(PluginIcons.ICON_TARGET);
                        break;
                    }
                    case PROPOSAL_META_DATA_TYPE_LITERAL: {
                        proposal.setProposalIcon(PluginIcons.ICON_STRINGLITERAL);
                        break;
                    }
                    case PROPOSAL_META_DATA_TYPE_VARIABLE:
                    case PROPOSAL_META_DATA_TYPE_STATIC_VARIABLE:
                    case PROPOSAL_META_DATA_TYPE_GLOBAL_VARIABLE: {
                        proposal.setProposalIcon(PluginIcons.ICON_VAR);
                        break;
                    }
                    case PROPOSAL_META_DATA_TYPE_TASK: {
                        proposal.setProposalIcon(PluginIcons.ICON_TASK);
                        break;
                    }
                    case PROPOSAL_META_DATA_TYPE_FILE: {
                        switch (schememeta
                                .getOrDefault(PROPOSAL_META_DATA_FILE_TYPE, PROPOSAL_META_DATA_FILE_TYPE_FILE)) {
                            case PROPOSAL_META_DATA_FILE_TYPE_DIRECTORY: {
                                proposal.setProposalIcon(AllIcons.Nodes.Folder);
                                break;
                            }
                            case PROPOSAL_META_DATA_FILE_TYPE_BUILD_SCRIPT: {
                                proposal.setProposalIcon(PluginIcons.SCRIPT_FILE);
                                break;
                            }
                            case PROPOSAL_META_DATA_FILE_TYPE_FILE:
                            default: {
                                proposal.setProposalIcon(AllIcons.FileTypes.Any_type);
                                break;
                            }
                        }
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }
        }

        SakerScriptInformationDesigner.processEntries(proposal.getInformationEntries());
    }
}
