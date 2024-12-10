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
        private KubernetesContextNamespaceService.KubernetesConfig kubernetesConfig = null;

        KubernetesContextWidget(Project project) {
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
            return mouseEvent -> {
                String[] contexts = kubernetesConfig.contexts();
                java.util.List<AnAction> actions = new LinkedList<>();
                for (String context : contexts) {
                    // Create popup actions
                    actions.add(new AnAction(context) {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e) {
                            new Thread(() -> {
                                try {
                                    // Switch the context
                                    ScriptRunnerUtil.getProcessOutput(new GeneralCommandLine("kubectl", "config", "use-context", context));
                                } catch (ExecutionException ignore) {
                                }
                            }).start();
                        }
                    });
                }

                // Create the popup
                ListPopup popup = JBPopupFactory.getInstance()
                        .createActionGroupPopup("Kubernetes Contexts",
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
            return 0;
        }

        @Override
        public @NotNull String getText() {
            return kubernetesConfig.currentContext();
        }

        @Override
        public @NotNull String getTooltipText() {
            return String.format("Current context %s", getText());
        }
    }
}
