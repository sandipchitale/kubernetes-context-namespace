package dev.sandipchitale.kubernetescontextnamespace;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class KubernetesNamespaceWidgetFactory implements StatusBarWidgetFactory {

    private static final String ID = "KubernetesNamesapce";

    @Override
    public @NotNull @NonNls String getId() {
        return ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return "Kubernetes Context";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new KubernetesNamespaceWidgetFactory.KubernetesNamespaceWidget(project);
    }

    private static final class KubernetesNamespaceWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

        private final KubernetesContextNamespaceService kubernetesContextNamespaceService;
        private final PropertyChangeListener propertyChangeListener;
        private String namespace = KubernetesContextNamespaceService.UNKNOWN;

        KubernetesNamespaceWidget(Project project) {
            kubernetesContextNamespaceService = ApplicationManager.getApplication().getService(KubernetesContextNamespaceService.class);
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            propertyChangeListener = (PropertyChangeEvent propertyChangeEvent) -> {
                KubernetesContextNamespaceService.KubernetesConfig kubernetesConfig = (KubernetesContextNamespaceService.KubernetesConfig) propertyChangeEvent.getNewValue();
                namespace = kubernetesConfig.namespace();
                statusBar.updateWidget(ID);
            };
            kubernetesContextNamespaceService.addPropertyChangeListener(propertyChangeListener);
        }

        @Override
        public @NotNull String ID() {
            return ID;
        }

        @Override
        public @NotNull WidgetPresentation getPresentation() {
            return this;
        }

        @Override
        public void dispose() {
            kubernetesContextNamespaceService.removePropertyChangeListener(propertyChangeListener);
            StatusBarWidget.super.dispose();
        }

        @Override
        public float getAlignment() {
            return 1;
        }

        @Override
        public @NotNull String getText() {
            return namespace;
        }

        @Override
        public @NotNull String getTooltipText() {
            return String.format("Current namespace %s", namespace);
        }
    }
}
