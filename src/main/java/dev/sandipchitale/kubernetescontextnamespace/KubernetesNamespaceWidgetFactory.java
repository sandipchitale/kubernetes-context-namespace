package dev.sandipchitale.kubernetescontextnamespace;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;

public class KubernetesNamespaceWidgetFactory implements StatusBarWidgetFactory {

    private static final String ID = "KubernetesNamesapce";

    @Override
    public @NotNull @NonNls String getId() {
        return ID;
    }

    @Override
    public @NotNull @NlsContexts.ConfigurableName String getDisplayName() {
        return "Kubernetes Namespace";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new KubernetesNamespaceWidgetFactory.KubernetesNamespaceWidget(project);
    }

    private static final class KubernetesNamespaceWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

        private final KubernetesContextNamespaceService kubernetesContextNamespaceService;
        private final PropertyChangeListener propertyChangeListener;
        private KubernetesContextNamespaceService.KubernetesConfig kubernetesConfig = null;

        KubernetesNamespaceWidget(Project project) {
            kubernetesContextNamespaceService = ApplicationManager.getApplication().getService(KubernetesContextNamespaceService.class);
            kubernetesConfig = kubernetesContextNamespaceService.getKubernetesConfig();
            propertyChangeListener = (PropertyChangeEvent propertyChangeEvent) -> {
                kubernetesConfig = (KubernetesContextNamespaceService.KubernetesConfig) propertyChangeEvent.getNewValue();
                WindowManager.getInstance().getStatusBar(project).updateWidget(ID);
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
        public @NotNull Consumer<MouseEvent> getClickConsumer() {
            return (MouseEvent mouseEvent) -> {
                if (kubernetesConfig == null) {
                    return;
                }
                String[] namespaces = kubernetesConfig.namespaces();
                if (namespaces.length == 0) {
                    return;
                }
                java.util.List<AnAction> actions = new LinkedList<>();
                for (String namespace : namespaces) {
                    if (namespace.equals(kubernetesConfig.namespace())) {
                        // Skip current
                        continue;
                    }
                    // Create popup actions
                    actions.add(new AnAction(namespace) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            new Thread(() -> {
                                try {
                                    // Switch namespace
                                    ScriptRunnerUtil.getProcessOutput(new GeneralCommandLine("kubectl", "config", "set-context", "--current", "--namespace=" + namespace));
                                    kubernetesContextNamespaceService.refreshNow();
                                } catch (ExecutionException ignore) {
                                }
                            }).start();
                        }
                    });
                }

                if (actions.isEmpty()) {
                    return;
                }

                // Create the popup
                ListPopup popup = JBPopupFactory.getInstance()
                        .createActionGroupPopup("Kubernetes Namespaces",
                                new DefaultActionGroup(actions),
                                DataManager.getInstance().getDataContext(mouseEvent.getComponent()),
                                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                true);

                // Show the popup
                popup.showInBestPositionFor(DataManager.getInstance().getDataContext(mouseEvent.getComponent()));
            };
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
            if (kubernetesConfig == null) {
                return KubernetesContextNamespaceService.UNKNOWN;
            }
            return kubernetesConfig.namespace();
        }

        @Override
        public @NotNull String getTooltipText() {
            return String.format("<html>Kubernetes namespace: %s<br/>Kubernetes cluster: %s<br/>KUBECONFIG: %s",
                    getText(),
                    kubernetesConfig == null ? KubernetesContextNamespaceService.UNKNOWN : kubernetesConfig.cluster(),
                    kubernetesConfig == null ? KubernetesContextNamespaceService.UNKNOWN : kubernetesConfig.KUBECONFIG());
        }
    }
}
