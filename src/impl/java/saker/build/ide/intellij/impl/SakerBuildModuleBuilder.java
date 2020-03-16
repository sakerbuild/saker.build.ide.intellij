package saker.build.ide.intellij.impl;

import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.module.ModuleType;

public class SakerBuildModuleBuilder extends ModuleBuilder {
    @Override
    public ModuleType<?> getModuleType() {
        return SakerBuildModuleType.getInstance();
    }

}
