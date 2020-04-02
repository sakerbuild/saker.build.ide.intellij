package saker.build.ide.intellij.designer;

import com.intellij.openapi.project.DumbAware;
import saker.build.ide.intellij.extension.script.information.IScriptInformationDesigner;
import saker.build.ide.intellij.extension.script.information.IScriptInformationEntry;
import saker.build.ide.intellij.extension.script.information.IScriptInformationRoot;

import java.util.List;

public class SakerScriptInformationDesigner implements IScriptInformationDesigner, DumbAware {
    //TODO handle dark icons
    private static final String ICON_SOURCE_TASK = createIconSource("icons/task@2x.png");
    private static final String ICON_SOURCE_VARIABLE = createIconSource("icons/var@2x.png");
    private static final String ICON_SOURCE_TARGET_INPUT_PARAMETER = createIconSource("icons/inparam@2x.png");
    private static final String ICON_SOURCE_TARGET_OUTPUT_PARAMETER = createIconSource("icons/outparam@2x.png");
    private static final String ICON_SOURCE_BUILD_TARGET = createIconSource("icons/target@2x.png");

    public static final String INFORMATION_SCHEMA_IDENTIFIER = "saker.script";
    private static final String INFORMATION_SCHEMA_TASK = INFORMATION_SCHEMA_IDENTIFIER + ".task";
    private static final String INFORMATION_SCHEMA_TASK_PARAMETER = INFORMATION_SCHEMA_IDENTIFIER + ".task_parameter";
    private static final String INFORMATION_SCHEMA_ENUM = INFORMATION_SCHEMA_IDENTIFIER + ".enum";
    private static final String INFORMATION_SCHEMA_VARIABLE = INFORMATION_SCHEMA_IDENTIFIER + ".var";
    private static final String INFORMATION_SCHEMA_TARGET_INPUT_PARAMETER = INFORMATION_SCHEMA_IDENTIFIER + ".target.input_parameter";
    private static final String INFORMATION_SCHEMA_TARGET_OUTPUT_PARAMETER = INFORMATION_SCHEMA_IDENTIFIER + ".target.output_parameter";
    private static final String INFORMATION_SCHEMA_BUILD_TARGET = INFORMATION_SCHEMA_IDENTIFIER + ".target";

    @Override
    public void process(IScriptInformationRoot informationroot) {
        System.out.println("SakerScriptInformationDesigner.process");
        processEntries(informationroot.getEntries());
    }

    public static void processEntries(List<? extends IScriptInformationEntry> entries) {
        if (entries == null) {
            return;
        }
        for (IScriptInformationEntry entry : entries) {
            processEntry(entry);
        }
    }

    public static void processEntry(IScriptInformationEntry entry) {
        if (entry == null) {
            return;
        }
        String entryschema = entry.getSchemaIdentifier();
        if (entryschema == null) {
            return;
        }
        switch (entryschema) {
            case INFORMATION_SCHEMA_TASK: {
//                entry.setIconSource(ICON_SOURCE_TASK);
                break;
            }
            case INFORMATION_SCHEMA_VARIABLE: {
//                entry.setIconSource(ICON_SOURCE_VARIABLE);
                break;
            }
            case INFORMATION_SCHEMA_TARGET_INPUT_PARAMETER: {
//                entry.setIconSource(ICON_SOURCE_TARGET_INPUT_PARAMETER);
                break;
            }
            case INFORMATION_SCHEMA_TARGET_OUTPUT_PARAMETER: {
//                entry.setIconSource(ICON_SOURCE_TARGET_OUTPUT_PARAMETER);
                break;
            }
            case INFORMATION_SCHEMA_BUILD_TARGET: {
//                entry.setIconSource(ICON_SOURCE_BUILD_TARGET);
                break;
            }
            default: {
                break;
            }
        }
    }

    private static String createIconSource(String path) {
        //unused for now
        return null;
//        Path file = SakerBuildPlugin.exportEmbeddedFile(path);
//        if (file == null) {
//            return null;
//        }
//        //based on ImageDocumentationProvider
//        String srcpath = file.toString();
//        if (SystemInfo.isWindows) {
//            srcpath = "/" + srcpath;
//        }
//        try {
//            return new URI("file", null, srcpath, null).toString();
//        } catch (Exception e) {
//            Logger.getInstance(SakerScriptInformationDesigner.class).warn(e);
//            return null;
//        }
    }
}
