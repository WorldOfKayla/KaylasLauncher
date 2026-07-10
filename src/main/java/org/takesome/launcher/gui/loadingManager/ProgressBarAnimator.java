package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.gui.loadingManager.ScriptedLoadingUi;

import javax.swing.JLabel;
import org.takesome.kaylasEngine.gui.components.progressBar.ProgressBar;
import java.util.List;
import java.util.Objects;

/** Launcher adapter for the engine-owned progress animation controller. */
public final class ProgressBarAnimator extends org.takesome.kaylasEngine.gui.animation.ProgressBarAnimator {
    private final Launcher launcher;
    private final ScriptedLoadingUi.Progress progressConfig;

    public ProgressBarAnimator(Launcher launcher,
                               ProgressBar progressBar,
                               JLabel progressText,
                               ScriptedLoadingUi.Progress progressConfig) {
        super(
                progressBar,
                progressText,
                Objects.requireNonNull(progressConfig, "progressConfig").messagesResource(),
                progressConfig.animationConfigResource(),
                "[LOAD-UI]",
                progressConfig.toEngineOptions()
        );
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.progressConfig = progressConfig;
    }

    @Override
    protected List<String> resolveMessages() {
        List<String> sectionMessages = launcher.getLANG().getSectionValues(progressConfig.messagesSection());
        if (!sectionMessages.isEmpty()) {
            return sectionMessages;
        }

        List<String> messageKeys = progressConfig.messageKeys();
        if (messageKeys != null && !messageKeys.isEmpty()) {
            List<String> localized = messageKeys.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(key -> !key.isEmpty())
                    .map(this::localizedMessage)
                    .filter(message -> message != null && !message.isBlank())
                    .toList();
            if (!localized.isEmpty()) {
                return localized;
            }
        }
        return super.resolveMessages();
    }

    private String localizedMessage(String key) {
        String message = launcher.getLANG().getString(key);
        if (message == null || message.isBlank() || message.equals(key)) {
            return key;
        }
        return message;
    }
}
