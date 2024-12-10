package dev.sandipchitale.kubernetescontextnamespace;

import java.beans.PropertyChangeListener;

public interface KubernetesContextNamespaceService {
    String UNKNOWN = "UNKNOWN";
    String[] EMPTY = new String[0];

    String KUBERNETES_CONFIG_PROPERTY_NAME = "KubernetesConfig";

    public static record KubernetesConfig(boolean connected,
                                          String KUBECONFIG,
                                          String currentContext,
                                          String[] contexts,
                                          String namespace,
                                          String[] namespaces,
                                          String cluster) {
    }

    void refreshNow();

    KubernetesConfig getKubernetesConfig();

    void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

    void removePropertyChangeListener(PropertyChangeListener propertyChangeListener);
}
