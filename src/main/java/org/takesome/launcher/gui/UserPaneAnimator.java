package org.takesome.launcher.gui;

import org.takesome.Launcher;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.animation.SnapshotDrawerAnimator;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.util.Objects;
import java.util.function.Supplier;

/** Launcher-specific adapter around the engine-owned snapshot drawer animator. */
final class UserPaneAnimator {
    private static final int ANIMATION_DURATION_MS = 360;

    private final Launcher launcher;
    private final LauncherUiProvider ui;
    private final SnapshotDrawerAnimator animator;

    private volatile ImageIcon menuIcon;
    private volatile ImageIcon backIcon;

    UserPaneAnimator(Launcher launcher, LauncherUiProvider ui, Supplier<JComponent> buttonSupplier) {
        this.launcher = Objects.requireNonNull(launcher, "launcher");
        this.ui = Objects.requireNonNull(ui, "ui");
        Objects.requireNonNull(buttonSupplier, "buttonSupplier");
        this.animator = SnapshotDrawerAnimator.builder()
                .panelSupplier(() -> launcher.getUser() == null ? null : launcher.getUser().getPanel())
                .controlSupplier(buttonSupplier)
                .edge(SnapshotDrawerAnimator.Edge.LEFT)
                .durationMs(ANIMATION_DURATION_MS)
                .repaintPaddingPx(2)
                .onTargetStateChanged(state -> swapButtonIcon(state.control(), state.open()))
                .build();
    }

    void toggle() {
        animator.toggle();
    }

    private void swapButtonIcon(JComponent component, boolean opening) {
        if (!(component instanceof AbstractButton button)) {
            Engine.getLOGGER().warn("userPane control is not an AbstractButton: {}", component == null ? null : component.getClass().getName());
            return;
        }
        button.setIcon(opening ? backIcon(button) : menuIcon(button));
        button.repaint();
    }

    private ImageIcon menuIcon(AbstractButton button) {
        ImageIcon icon = menuIcon;
        if (icon == null) {
            icon = vectorIcon(button, ui.icons().userPaneMenu());
            menuIcon = icon;
        }
        return icon;
    }

    private ImageIcon backIcon(AbstractButton button) {
        ImageIcon icon = backIcon;
        if (icon == null) {
            icon = vectorIcon(button, ui.icons().userPaneBack());
            backIcon = icon;
        }
        return icon;
    }

    private ImageIcon vectorIcon(AbstractButton button, String path) {
        int width = button.getIcon() == null ? Math.max(1, button.getWidth()) : Math.max(1, button.getIcon().getIconWidth());
        int height = button.getIcon() == null ? Math.max(1, button.getHeight()) : Math.max(1, button.getIcon().getIconHeight());
        return launcher.getIconUtils().getVectorIcon(path, width, height);
    }
}
