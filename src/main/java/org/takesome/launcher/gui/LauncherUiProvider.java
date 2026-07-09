package org.takesome.launcher.gui;

import com.google.gson.Gson;
import org.takesome.kaylasEngine.Engine;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
            LauncherUiManifest manifest = loadManifest(manifestPath);
            return new LauncherUiProvider(manifest);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to load launcher UI manifest: " + manifestPath, error);
        }
    }

    private static LauncherUiManifest loadManifest(String manifestPath) throws Exception {
        Path filePath = Path.of(manifestPath);
        if (Files.isRegularFile(filePath)) {
            try (InputStream input = Files.newInputStream(filePath);
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, LauncherUiManifest.class);
            }
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = LauncherUiProvider.class.getClassLoader();
        }
        try (InputStream input = classLoader.getResourceAsStream(normalizeResourcePath(manifestPath))) {
            if (input == null) {
                throw new IllegalStateException("Launcher UI manifest was not found as resource or file: " + manifestPath);
            }
            try (InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, LauncherUiManifest.class);
            }
        }
    }

    private static String normalizeResourcePath(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
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
            forms().settingsFields();
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
