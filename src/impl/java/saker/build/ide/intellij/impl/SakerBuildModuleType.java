package saker.build.ide.intellij.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.ModuleTypeManager;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SakerBuildModuleType extends ModuleType<SakerBuildModuleBuilder> {
    public static final String ID = "SAKER_BUILD_MODULE_TYPE";

    public SakerBuildModuleType() {
        super(ID);
    }

    public static SakerBuildModuleType getInstance() {
        return (SakerBuildModuleType) ModuleTypeManager.getInstance().findByID(ID);
    }

    @NotNull
    @Override
    public SakerBuildModuleBuilder createModuleBuilder() {
        return new SakerBuildModuleBuilder();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getName() {
        return "Saker.build";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getDescription() {
        return "Saker.build system";
    }

    @NotNull
    @Override
    public Icon getNodeIcon(boolean isOpened) {
        //TODO real icon
        return AllIcons.General.Information;
    }
}
