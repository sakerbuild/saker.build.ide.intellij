package saker.build.ide.intellij.designer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.openapi.project.DumbAware;
import saker.build.ide.intellij.PluginIcons;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineDesigner;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineEntry;
import saker.build.ide.intellij.extension.script.outline.IScriptOutlineRoot;

import javax.swing.Icon;
import java.util.List;
import java.util.Map;

public class SakerScriptOutlineDesigner implements IScriptOutlineDesigner, DumbAware {
    @Override
    public void process(IScriptOutlineRoot outlineroot) {
        System.out.println("SakerScriptOutlineDesigner.process");
        List<? extends IScriptOutlineEntry> roots = outlineroot.getRootEntries();
        processEntries(roots);
    }

    private void processEntries(List<? extends IScriptOutlineEntry> children) {
        if (children == null) {
            return;
        }
        for (IScriptOutlineEntry child : children) {
            processEntry(child);
        }
    }

    private void setIcon(IScriptOutlineEntry entry, Icon icon) {
        entry.setEntryPresentation(new PresentationData(entry.getLabel(), entry.getType(), icon, null));
    }

    private void processEntry(IScriptOutlineEntry entry) {
        if (entry == null) {
            return;
        }
        String entryschema = entry.getSchemaIdentifier();
        if (entryschema != null) {
            switch (entryschema) {
                case "saker.script.literal.null": {
                    //TODO null script outline image
                    break;
                }
                case "saker.script.literal.boolean": {
                    if (Boolean.parseBoolean(entry.getLabel())) {
                        setIcon(entry, PluginIcons.ICON_BOOL_TRUE);
                    } else {
                        setIcon(entry, PluginIcons.ICON_BOOL_FALSE);
                    }
                    break;
                }
                case "saker.script.task": {
                    setIcon(entry, PluginIcons.ICON_TASK);
                    break;
                }
                case "saker.script.list": {
                    setIcon(entry, PluginIcons.ICON_LIST);
                    break;
                }
                case "saker.script.map": {
                    setIcon(entry, PluginIcons.ICON_MAP);
                    break;
                }
                case "saker.script.literal.number": {
                    setIcon(entry, PluginIcons.ICON_NUM);
                    break;
                }
                case "saker.script.literal.string": {
                    setIcon(entry, PluginIcons.ICON_STRINGLITERAL);
                    break;
                }
                case "saker.script.literal.compound-string": {
                    setIcon(entry, PluginIcons.ICON_STRING);
                    break;
                }
                case "saker.script.literal.map.entry": {
                    //TODO map entry script outline image
                    break;
                }
                case "saker.script.target": {
                    setIcon(entry, PluginIcons.ICON_TARGET);
                    break;
                }
                case "saker.script.target.parameter.in": {
                    setIcon(entry, PluginIcons.ICON_INPARAM);
                    break;
                }
                case "saker.script.target.parameter.out": {
                    setIcon(entry, PluginIcons.ICON_OUTPARAM);
                    break;
                }
                case "saker.script.foreach": {
                    setIcon(entry, PluginIcons.ICON_FOR);
                    break;
                }
                case "saker.script.var": {
                    setIcon(entry, PluginIcons.ICON_VAR);
                    break;
                }
                default: {
                    Map<String, String> schemameta = entry.getSchemaMetaData();
                    String coaltype = schemameta == null ? null : schemameta.get("saker.script.coalesced-type");
                    if (coaltype != null) {
                        switch (coaltype) {
                            case "map": {
                                setIcon(entry, PluginIcons.ICON_MAP);
                                break;
                            }
                            case "list": {
                                setIcon(entry, PluginIcons.ICON_LIST);
                                break;
                            }
                            case "string": {
                                setIcon(entry, PluginIcons.ICON_STRING);
                                break;
                            }
                            case "literal.null": {
                                //TODO null script outline image
                                break;
                            }
                            case "literal.boolean.true": {
                                setIcon(entry, PluginIcons.ICON_BOOL_TRUE);
                                break;
                            }
                            case "literal.boolean.false": {
                                setIcon(entry, PluginIcons.ICON_BOOL_FALSE);
                                break;
                            }
                            case "literal.number": {
                                setIcon(entry, PluginIcons.ICON_NUM);
                                break;
                            }
                            case "literal.string": {
                                setIcon(entry, PluginIcons.ICON_STRINGLITERAL);
                                break;
                            }
                            case "var": {
                                setIcon(entry, PluginIcons.ICON_VAR);
                                break;
                            }
                            default: {
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        processEntries(entry.getChildren());
    }

}
