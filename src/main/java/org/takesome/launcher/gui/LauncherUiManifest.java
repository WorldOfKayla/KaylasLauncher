package org.takesome.launcher.gui;

/**
 * Data-only launcher UI manifest loaded from resources.
 *
 * This class intentionally contains no launcher UI values. Values live in
 * {@code assets/ui/launcher-ui-provider.json}; Java only provides typed accessors.
 */
final class LauncherUiManifest {
    private Scopes scopes = new Scopes();
    private Forms forms = new Forms();
    private LocaleKeys localeKeys = new LocaleKeys();
    private Components components = new Components();
    private Panels panels = new Panels();
    private PanelSpecs panelSpecs = new PanelSpecs();
    private Events events = new Events();
    private Listeners listeners = new Listeners();
    private Scripts scripts = new Scripts();
    private Icons icons = new Icons();
    private Sounds sounds = new Sounds();
    private Dialogs dialogs = new Dialogs();
    private ConfigKeys configKeys = new ConfigKeys();
    private Tasks tasks = new Tasks();

    Scopes scopes() { return scopes == null ? new Scopes() : scopes; }
    Forms forms() { return forms == null ? new Forms() : forms; }
    LocaleKeys localeKeys() { return localeKeys == null ? new LocaleKeys() : localeKeys; }
    Components components() { return components == null ? new Components() : components; }
    Panels panels() { return panels == null ? new Panels() : panels; }
    PanelSpecs panelSpecs() { return panelSpecs == null ? new PanelSpecs() : panelSpecs; }
    Events events() { return events == null ? new Events() : events; }
    Listeners listeners() { return listeners == null ? new Listeners() : listeners; }
    Scripts scripts() { return scripts == null ? new Scripts() : scripts; }
    Icons icons() { return icons == null ? new Icons() : icons; }
    Sounds sounds() { return sounds == null ? new Sounds() : sounds; }
    Dialogs dialogs() { return dialogs == null ? new Dialogs() : dialogs; }
    ConfigKeys configKeys() { return configKeys == null ? new ConfigKeys() : configKeys; }
    Tasks tasks() { return tasks == null ? new Tasks() : tasks; }

    static String required(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Launcher UI manifest value is missing: " + key);
        }
        return value;
    }

    static final class Scopes {
        private String mainFrame;
        String mainFrame() { return required(mainFrame, "scopes.mainFrame"); }
    }

    static final class Forms {
        private String authForm;
        private String settingsFields;
        String authForm() { return required(authForm, "forms.authForm"); }
        String settingsFields() { return required(settingsFields, "forms.settingsFields"); }
    }

    static final class LocaleKeys {
        private String authLoggedOut;
        String authLoggedOut() { return required(authLoggedOut, "localeKeys.authLoggedOut"); }
    }

    static final class Components {
        private String userPane;
        private String userControls;
        private String userActions;
        private String authSubmit;
        private String authSettings;
        private String userinfoSubmit;
        private String smallButton;
        private String gameDirSmall;
        private String cancelDownloadSmall;
        private String applySettings;
        private String logOut;
        private String infoSmall;
        private String settingsSmall;
        private String loadCancelSmall;
        private String back;
        private String toGame;
        private String optionalMods;
        private String closeButton;
        private String hideButton;

        String userPane() { return required(userPane, "components.userPane"); }
        String userControls() { return required(userControls, "components.userControls"); }
        String userActions() { return required(userActions, "components.userActions"); }
        String authSubmit() { return required(authSubmit, "components.authSubmit"); }
        String authSettings() { return required(authSettings, "components.authSettings"); }
        String userinfoSubmit() { return required(userinfoSubmit, "components.userinfoSubmit"); }
        String smallButton() { return required(smallButton, "components.smallButton"); }
        String gameDirSmall() { return required(gameDirSmall, "components.gameDirSmall"); }
        String cancelDownloadSmall() { return required(cancelDownloadSmall, "components.cancelDownloadSmall"); }
        String applySettings() { return required(applySettings, "components.applySettings"); }
        String logOut() { return required(logOut, "components.logOut"); }
        String infoSmall() { return required(infoSmall, "components.infoSmall"); }
        String settingsSmall() { return required(settingsSmall, "components.settingsSmall"); }
        String loadCancelSmall() { return required(loadCancelSmall, "components.loadCancelSmall"); }
        String back() { return required(back, "components.back"); }
        String toGame() { return required(toGame, "components.toGame"); }
        String optionalMods() { return required(optionalMods, "components.optionalMods"); }
        String closeButton() { return required(closeButton, "components.closeButton"); }
        String hideButton() { return required(hideButton, "components.hideButton"); }
    }

    static final class Panels {
        private String authForm;
        private String newsForm;
        private String loggedForm;
        private String settings;
        private String test;

        String authForm() { return required(authForm, "panels.authForm"); }
        String newsForm() { return required(newsForm, "panels.newsForm"); }
        String loggedForm() { return required(loggedForm, "panels.loggedForm"); }
        String settings() { return required(settings, "panels.settings"); }
        String test() { return required(test, "panels.test"); }
    }

    static final class PanelSpecs {
        private String authToTest;
        private String loggedToTest;
        private String authToSettings;
        private String loggedToSettings;
        private String backToAuth;
        private String backToLogged;
        private String hideAuth;

        String authToTest() { return required(authToTest, "panelSpecs.authToTest"); }
        String loggedToTest() { return required(loggedToTest, "panelSpecs.loggedToTest"); }
        String authToSettings() { return required(authToSettings, "panelSpecs.authToSettings"); }
        String loggedToSettings() { return required(loggedToSettings, "panelSpecs.loggedToSettings"); }
        String backToAuth() { return required(backToAuth, "panelSpecs.backToAuth"); }
        String backToLogged() { return required(backToLogged, "panelSpecs.backToLogged"); }
        String hideAuth() { return required(hideAuth, "panelSpecs.hideAuth"); }
    }

    static final class Events {
        private String userPaneToggle;
        private String openInfo;
        private String openSettings;
        private String back;
        private String optionalMods;
        private String loadingToggle;
        private String openGameDir;
        private String applySettings;

        String userPaneToggle() { return required(userPaneToggle, "events.userPaneToggle"); }
        String openInfo() { return required(openInfo, "events.openInfo"); }
        String openSettings() { return required(openSettings, "events.openSettings"); }
        String back() { return required(back, "events.back"); }
        String optionalMods() { return required(optionalMods, "events.optionalMods"); }
        String loadingToggle() { return required(loadingToggle, "events.loadingToggle"); }
        String openGameDir() { return required(openGameDir, "events.openGameDir"); }
        String applySettings() { return required(applySettings, "events.applySettings"); }
    }

    static final class Listeners {
        private String userPaneToggle;
        private String openInfo;
        private String openSettings;
        private String back;
        private String optionalMods;
        private String loadingToggle;
        private String openGameDir;
        private String applySettings;

        String userPaneToggle() { return required(userPaneToggle, "listeners.userPaneToggle"); }
        String openInfo() { return required(openInfo, "listeners.openInfo"); }
        String openSettings() { return required(openSettings, "listeners.openSettings"); }
        String back() { return required(back, "listeners.back"); }
        String optionalMods() { return required(optionalMods, "listeners.optionalMods"); }
        String loadingToggle() { return required(loadingToggle, "listeners.loadingToggle"); }
        String openGameDir() { return required(openGameDir, "listeners.openGameDir"); }
        String applySettings() { return required(applySettings, "listeners.applySettings"); }
    }

    static final class Scripts {
        private String launcherUi;
        private String loadingUi;
        private String userUi;
        String launcherUi() { return required(launcherUi, "scripts.launcherUi"); }
        String loadingUi() { return required(loadingUi, "scripts.loadingUi"); }
        String userUi() { return required(userUi, "scripts.userUi"); }
    }

    static final class Icons {
        private String userPaneMenu;
        private String userPaneBack;
        String userPaneMenu() { return required(userPaneMenu, "icons.userPaneMenu"); }
        String userPaneBack() { return required(userPaneBack, "icons.userPaneBack"); }
    }

    static final class Sounds {
        private String other;
        private String start;
        private String loggedOut;
        String other() { return required(other, "sounds.other"); }
        String start() { return required(start, "sounds.start"); }
        String loggedOut() { return required(loggedOut, "sounds.loggedOut"); }
    }

    static final class Dialogs {
        private String errorTitle;
        private String optionalModsTitle;
        private String optionalModsMessage;
        private String noServersMessage;
        private String failedToStartGame;

        String errorTitle() { return required(errorTitle, "dialogs.errorTitle"); }
        String optionalModsTitle() { return required(optionalModsTitle, "dialogs.optionalModsTitle"); }
        String optionalModsMessage() { return required(optionalModsMessage, "dialogs.optionalModsMessage"); }
        String noServersMessage() { return required(noServersMessage, "dialogs.noServersMessage"); }
        String failedToStartGame() { return required(failedToStartGame, "dialogs.failedToStartGame"); }
    }

    static final class ConfigKeys {
        private String selectedServer;
        String selectedServer() { return required(selectedServer, "configKeys.selectedServer"); }
    }

    static final class Tasks {
        private String auth;
        private String createCore;
        String auth() { return required(auth, "tasks.auth"); }
        String createCore() { return required(createCore, "tasks.createCore"); }
    }
}
