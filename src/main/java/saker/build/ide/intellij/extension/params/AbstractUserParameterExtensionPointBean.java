package saker.build.ide.intellij.extension.params;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nls;
import saker.build.ide.intellij.ContributedExtension;

public class AbstractUserParameterExtensionPointBean extends AbstractExtensionPointBean implements ContributedExtension {
    @Attribute("id")
    public String id;

    @Attribute("implementationClass")
    public String implementationClass;

    @Attribute("displayName")
    @Nls(capitalization = Nls.Capitalization.Title)
    public String displayName;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getImplementationClass() {
        return implementationClass;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (myPluginDescriptor != null) {
            sb.append(myPluginDescriptor.getPluginId());
        }
        if (id != null) {
            if (sb.length() > 0) {
                sb.append('.');
            }
            sb.append(id);
        }
        if (implementationClass != null) {
            if (sb.length() > 0) {
                sb.append(": ");
            }
            sb.append(implementationClass);
        }
        if (displayName != null && displayName.length() > 0) {
            if (sb.length() > 0) {
                sb.append(" (");
                sb.append(displayName);
                sb.append(')');
            } else {
                sb.append(displayName);
            }
        }
        if (sb.length() == 0) {
            return "[unrecognized extension]";
        }
        return sb.toString();
    }
}
