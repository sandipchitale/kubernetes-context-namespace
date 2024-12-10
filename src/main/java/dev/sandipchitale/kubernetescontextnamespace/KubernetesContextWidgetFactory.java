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

public class KubernetesContextWidgetFactory implements StatusBarWidgetFactory {

    private static final String ID = "KubernetesContext";

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
        return new KubernetesContextWidget(project);
    }

    private static final class KubernetesContextWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

        private final KubernetesContextNamespaceService kubernetesContextNamespaceService;
        private final PropertyChangeListener propertyChangeListener;
        private String context = KubernetesContextNamespaceService.UNKNOWN;

        KubernetesContextWidget(Project project) {
            kubernetesContextNamespaceService = ApplicationManager.getApplication().getService(KubernetesContextNamespaceService.class);
            propertyChangeListener = (PropertyChangeEvent propertyChangeEvent) -> {
                KubernetesContextNamespaceService.KubernetesConfig kubernetesConfig = (KubernetesContextNamespaceService.KubernetesConfig) propertyChangeEvent.getNewValue();
                context = kubernetesConfig.currentContext();
                StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
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
            return 0;
        }

        @Override
        public @NotNull String getText() {
            return context;
        }

        @Override
        public @NotNull String getTooltipText() {
            return String.format("Current context %s", context);
        }
    }
}
