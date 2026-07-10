package org.takesome.launcher.gui.loadingManager;

import org.takesome.Launcher;

import javax.swing.JLabel;
import javax.swing.JProgressBar;
import java.util.Objects;

/** Launcher adapter for the engine-owned progress animation controller. */
public final class ProgressBarAnimator extends org.takesome.kaylasEngine.gui.animation.ProgressBarAnimator {
    private static final String MESSAGES_RESOURCE = "assets/messages.json";
    private static final String ANIMATION_CONFIG_RESOURCE = "assets/animation_config.json";

    public ProgressBarAnimator(Launcher launcher, JProgressBar progressBar, JLabel progressText) {
        super(progressBar, progressText, MESSAGES_RESOURCE, ANIMATION_CONFIG_RESOURCE, "[LOAD-UI]");
        Objects.requireNonNull(launcher, "launcher");
    }
}
