package org.takesome.launcher.gui;

import com.google.gson.Gson;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.resources.ResourceLoader;

import java.util.Objects;

/**
 * Provider facade for launcher UI configuration.
 *
 * Java code depends on this typed provider, not on hardcoded UI values. The actual values are loaded
 * from {@code assets/ui/launcher-ui-provider.json} or from {@code -Dkaylas.launcher.ui.manifest=...}.
 */
public final class LauncherUiProvider {
    private static final Gson GSON = new Gson();
    private static final String MANIFEST_PROPERTY = "kaylas.launcher.ui.manifest";
    private static final String DEFAULT_MANIFEST_RESOURCE = "assets/ui/launcher-ui-provider.json";

    private final LauncherUiManifest manifest;

    private LauncherUiProvider(LauncherUiManifest manifest) {
        this.manifest = Objects.requireNonNull(manifest, "manifest");
    }

    public static LauncherUiProvider load() {
        String manifestPath = System.getProperty(MANIFEST_PROPERTY, DEFAULT_MANIFEST_RESOURCE);
        try {
            LauncherUiManifest manifest = ResourceLoader.loadJson(
                    manifestPath,
                    LauncherUiManifest.class,
                    GSON,
                    LauncherUiProvider.class.getClassLoader()
            );
            return new LauncherUiProvider(manifest == null ? new LauncherUiManifest() : manifest);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to load launcher UI manifest: " + manifestPath, error);
        }
    }

    LauncherUiManifest.Scopes scopes() { return manifest.scopes(); }
    LauncherUiManifest.Forms forms() { return manifest.forms(); }
    LauncherUiManifest.LocaleKeys localeKeys() { return manifest.localeKeys(); }
    LauncherUiManifest.Components components() { return manifest.components(); }
    LauncherUiManifest.Panels panels() { return manifest.panels(); }
    LauncherUiManifest.PanelSpecs panelSpecs() { return manifest.panelSpecs(); }
    LauncherUiManifest.Events events() { return manifest.events(); }
    LauncherUiManifest.Listeners listeners() { return manifest.listeners(); }
    LauncherUiManifest.Scripts scripts() { return manifest.scripts(); }
    LauncherUiManifest.Icons icons() { return manifest.icons(); }
    LauncherUiManifest.Sounds sounds() { return manifest.sounds(); }
    LauncherUiManifest.Dialogs dialogs() { return manifest.dialogs(); }
    LauncherUiManifest.ConfigKeys configKeys() { return manifest.configKeys(); }
    LauncherUiManifest.Tasks tasks() { return manifest.tasks(); }

    public String loadingUiScriptPath() {
        return scripts().loadingUi();
    }

    public String authFormPanelId() {
        return forms().authForm();
    }

    void validate() {
        try {
            scopes().mainFrame();
            forms().authForm();
            forms().settingsTabs();
            localeKeys().authLoggedOut();
            components().userPane();
            components().authSubmit();
            components().toGame();
            panelSpecs().hideAuth();
            events().userPaneToggle();
            listeners().userPaneToggle();
            icons().userPaneMenu();
            dialogs().errorTitle();
            tasks().auth();
        } catch (RuntimeException error) {
            Engine.getLOGGER().error("Launcher UI provider validation failed.", error);
            throw error;
        }
    }
}
