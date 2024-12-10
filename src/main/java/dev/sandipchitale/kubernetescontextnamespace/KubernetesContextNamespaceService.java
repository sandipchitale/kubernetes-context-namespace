package dev.sandipchitale.kubernetescontextnamespace;

import java.beans.PropertyChangeListener;

public interface KubernetesContextNamespaceService {
    String UNKNOWN = "UNKNOWN";

    String KUBERNETES_CONFIG_PROPERTY_NAME = "KubernetesConfig";

    public static record KubernetesConfig(boolean connected,
                                          String KUBECONFIG,
                                          String currentContext,
                                          String namespace,
                                          String cluster) {
    }

    KubernetesConfig getKubernetesConfig();

    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
}
