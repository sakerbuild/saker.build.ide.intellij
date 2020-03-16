package saker.build.ide.intellij;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Date;
import java.util.List;

class ForwardingImplementationPluginDescriptor implements IdeaPluginDescriptor {
    private IdeaPluginDescriptor descriptor;
    private ClassLoader implementationClassLoader;

    public ForwardingImplementationPluginDescriptor(IdeaPluginDescriptor descriptor,
            ClassLoader implementationClassLoader) {
        this.descriptor = descriptor;
        this.implementationClassLoader = implementationClassLoader;
    }

    @Override
    public File getPath() {
        return descriptor.getPath();
    }

    @Nullable
    @Override
    public String getDescription() {
        return descriptor.getDescription();
    }

    @Override
    public String getChangeNotes() {
        return descriptor.getChangeNotes();
    }

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Nullable
    @Override
    public String getProductCode() {
        return descriptor.getProductCode();
    }

    @Nullable
    @Override
    public Date getReleaseDate() {
        return descriptor.getReleaseDate();
    }

    @Override
    public int getReleaseVersion() {
        return descriptor.getReleaseVersion();
    }

    @NotNull
    @Override
    public PluginId[] getDependentPluginIds() {
        return descriptor.getDependentPluginIds();
    }

    @NotNull
    @Override
    public PluginId[] getOptionalDependentPluginIds() {
        return descriptor.getOptionalDependentPluginIds();
    }

    @Override
    public String getVendor() {
        return descriptor.getVendor();
    }

    @Override
    public String getVersion() {
        return descriptor.getVersion();
    }

    @Override
    public String getResourceBundleBaseName() {
        return descriptor.getResourceBundleBaseName();
    }

    @Override
    public String getCategory() {
        return descriptor.getCategory();
    }

    @Nullable
    @Override
    public List<Element> getActionDescriptionElements() {
        return descriptor.getActionDescriptionElements();
    }

    @Override
    public String getVendorEmail() {
        return descriptor.getVendorEmail();
    }

    @Override
    public String getVendorUrl() {
        return descriptor.getVendorUrl();
    }

    @Override
    public String getUrl() {
        return descriptor.getUrl();
    }

    @Override
    public String getVendorLogoPath() {
        return descriptor.getVendorLogoPath();
    }

    @Override
    public boolean getUseIdeaClassLoader() {
        return descriptor.getUseIdeaClassLoader();
    }

    @Override
    public String getDownloads() {
        return descriptor.getDownloads();
    }

    @Override
    public String getSinceBuild() {
        return descriptor.getSinceBuild();
    }

    @Override
    public String getUntilBuild() {
        return descriptor.getUntilBuild();
    }

    @Override
    public boolean allowBundledUpdate() {
        return descriptor.allowBundledUpdate();
    }

    @Override
    public boolean isImplementationDetail() {
        return descriptor.isImplementationDetail();
    }

    @Override
    public boolean isEnabled() {
        return descriptor.isEnabled();
    }

    @Override
    public void setEnabled(boolean b) {
        descriptor.setEnabled(b);
    }

    @Override
    public PluginId getPluginId() {
        return descriptor.getPluginId();
    }

    @Override
    public ClassLoader getPluginClassLoader() {
        return implementationClassLoader;
    }

    @Override
    public boolean isBundled() {
        return descriptor.isBundled();
    }
}
