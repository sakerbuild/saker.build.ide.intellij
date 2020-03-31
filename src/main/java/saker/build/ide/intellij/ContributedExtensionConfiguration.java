package saker.build.ide.intellij;

public final class ContributedExtensionConfiguration<T> {
    private final T contributor;
    private final UserParameterContributorExtension contributedExtension;
    private final boolean enabled;

    public ContributedExtensionConfiguration(T contributor, UserParameterContributorExtension configelem, boolean enabled) {
        this.contributor = contributor;
        this.contributedExtension = configelem;
        this.enabled = enabled;
    }

    public T getContributor() {
        return contributor;
    }

    public UserParameterContributorExtension getContributedExtension() {
        return contributedExtension;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ContributedExtensionConfiguration<T> setEnabled(boolean enabled) {
        if (enabled == this.enabled) {
            return this;
        }
        return new ContributedExtensionConfiguration<>(contributor, contributedExtension, enabled);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((contributedExtension == null) ? 0 : contributedExtension.hashCode());
        result = prime * result + ((contributor == null) ? 0 : contributor.hashCode());
        result = prime * result + (enabled ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContributedExtensionConfiguration<?> other = (ContributedExtensionConfiguration<?>) obj;
        if (contributedExtension == null) {
            if (other.contributedExtension != null) {
                return false;
            }
        } else if (!contributedExtension.equals(other.contributedExtension)) {
            return false;
        }
        if (contributor == null) {
            if (other.contributor != null) {
                return false;
            }
        } else if (!contributor.equals(other.contributor)) {
            return false;
        }
        if (enabled != other.enabled) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getClass()
                .getSimpleName() + "[contributor=" + contributor + ", configurationElement=" + contributedExtension + ", enabled=" + enabled + "]";
    }

}
