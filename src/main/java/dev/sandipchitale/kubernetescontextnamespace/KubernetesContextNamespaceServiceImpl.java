package dev.sandipchitale.kubernetescontextnamespace;

import com.google.gson.*;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesContextNamespaceServiceImpl implements KubernetesContextNamespaceService {

    private final GeneralCommandLine kubectlConfigList = new GeneralCommandLine("kubectl", "config", "view", "-o=json", "-v", "9");
    private final Pattern KUBECONFIG_PATTERN = Pattern.compile(".+Config loaded from file:\\s+(.+)\n");
    private final Gson gson = new Gson();

    private KubernetesContextNamespaceService.KubernetesConfig kubernetesConfig;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public KubernetesContextNamespaceServiceImpl() {
        java.util.Timer timer = new java.util.Timer("kubectl config list");
        long delay = 10000; // 10-second delay
        TimerTask kubectlConfigListTask = new TimerTask() {
            @Override
            public void run() {
                String KUBECONFIG = UNKNOWN;
                String context = UNKNOWN;
                String cluster = UNKNOWN;
                String namespace = UNKNOWN;

                KubernetesConfig newKubernetesConfig = new KubernetesConfig(false,
                        KUBECONFIG,
                        context,
                        EMPTY,
                        namespace,
                        EMPTY,
                        cluster);
                try {
                    OSProcessHandler handler = new OSProcessHandler(kubectlConfigList);
                    // StringBuilder to capture stdout
                    StringBuilder stdout = new StringBuilder();
                    // StringBuilder to capture stderr
                    StringBuilder stderr = new StringBuilder();

                    // Add a process listener to capture output
                    handler.addProcessListener(new ProcessAdapter() {
                        @Override
                        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                            if (outputType == ProcessOutputTypes.STDOUT) {
                                stdout.append(event.getText());
                            } else if (outputType == ProcessOutputTypes.STDERR) {
                                stderr.append(event.getText());
                            }
                        }
                    });

                    // Start the process
                    handler.startNotify();
                    // Wait for the process to finish
                    handler.waitFor();
                    // Get the exit code
                    @SuppressWarnings("DataFlowIssue") int exitCode = handler.getExitCode();

                    if (exitCode == 0) {
                        Matcher matcher = KUBECONFIG_PATTERN.matcher(stderr.toString());
                        if (matcher.matches()) {
                            KUBECONFIG = matcher.group(1);
                        }
                        String stdoutString = stdout.toString();
                        JsonObject config = gson.fromJson(stdoutString, JsonObject.class);
                        JsonArray contexts = config.getAsJsonArray("contexts");
                        JsonPrimitive contextNamePrimitive = config.getAsJsonPrimitive("current-context");
                        String currentContext = contextNamePrimitive.getAsString();
                        JsonObject currentContextObject = null;
                        java.util.List<String> contextsList = new LinkedList<>();
                        for (int i = 0; i < contexts.size(); i++) {
                            JsonElement jsonElement = contexts.get(i);
                            if (jsonElement.isJsonObject()) {
                                JsonObject contextObject = (JsonObject) jsonElement;
                                String contextName = contextObject.getAsJsonPrimitive("name").getAsString();
                                contextsList.add(contextName);
                                if (currentContext.equals(contextName)) {
                                    currentContextObject = contextObject.getAsJsonObject("context");
                                    break;
                                }
                            }
                        }
                        if (currentContextObject != null) {
                            context = currentContext;
                            cluster = currentContextObject.getAsJsonPrimitive("cluster").getAsString();
                            namespace = currentContextObject.getAsJsonPrimitive("namespace").getAsString();
                        }

                        newKubernetesConfig = new KubernetesConfig(true,
                                KUBECONFIG,
                                context,
                                contextsList.toArray(new String[0]),
                                namespace,
                                EMPTY,
                                cluster);
                    }
                } catch (Exception ignore) {
                }
                if (!newKubernetesConfig.equals(kubernetesConfig)) {
                    KubernetesConfig oldKubernetesConfig = kubernetesConfig;
                    kubernetesConfig = newKubernetesConfig;
                    propertyChangeSupport.firePropertyChange(KUBERNETES_CONFIG_PROPERTY_NAME, oldKubernetesConfig, kubernetesConfig);
                }
            }
        };
        timer.schedule(kubectlConfigListTask, 0, delay);
    }

    @Override
    public KubernetesConfig getKubernetesConfig() {
        return kubernetesConfig;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }
}
