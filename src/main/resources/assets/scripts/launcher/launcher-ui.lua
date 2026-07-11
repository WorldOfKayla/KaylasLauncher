-- KaylasLauncher UI script layer.
--
-- This file is the declarative UI behavior map for lightweight launcher actions.
-- Java remains responsible for runtime/domain operations: auth, backend, game launch,
-- filesystem access and heavy services. Lua owns component-level UI intent dispatch.
--
-- Text is resolved through the active Engine LanguageProvider using engine.lang(...).

local UI = {
    VERSION = "1.1.0",

    SETTINGS = {
        debug = false,
        ignoreDisabledComponents = true,
        markLastActionOnComponent = true,
        actionPropertyKey = "launcher.ui.lastAction",
        eventPropertyKey = "launcher.ui.lastEvent",
        unknownActionEvent = "launcher.ui.unknownAction"
    },

    COMPONENTS = {
        USER_PANE = "userPane",
        AUTH_SETTINGS = "authSettings",
        SETTINGS_SMALL = "settings-small",
        BACK = "back",
        OPTIONAL_MODS = "optionalMods",
        SERVER_BOX = "serverBox",
        SMALL_BUTTON = "smallButton",
        GAME_DIR_SMALL = "gameDir-small",
        APPLY_SETTINGS = "applySettings"
    },

    EVENTS = {
        USER_PANE_TOGGLE = "launcher.userPane.toggle",
        OPEN_SETTINGS = "launcher.ui.open.settings",
        BACK = "launcher.ui.back",
        OPTIONAL_MODS = "launcher.ui.optionalMods",
        LOADING_TOGGLE = "launcher.ui.loading.toggle",
        OPEN_GAME_DIR = "launcher.ui.gameDir.open",
        APPLY_SETTINGS = "launcher.ui.settings.apply",
        SERVER_CORE_ICONS_APPLY = "launcher.serverBox.coreIcons.apply"
    },

    LOCALE_KEYS = {
        NO_EMITTER = "launcherUi.noEmitter",
        NO_COMPONENT_ID = "launcherUi.noComponentId",
        NO_ACTION = "launcherUi.noAction",
        IGNORED_DISABLED = "launcherUi.ignoredDisabled",
        DISPATCHING = "launcherUi.dispatching"
    },

    ACTION_LOCALE_KEYS = {
        TOGGLE_USER_PANE = "launcherUi.action.toggleUserPane",
        OPEN_SETTINGS = "launcherUi.action.openSettings",
        BACK_FROM_SETTINGS = "launcherUi.action.backFromSettings",
        OPTIONAL_MODS = "launcherUi.action.optionalMods",
        TOGGLE_LOADING = "launcherUi.action.toggleLoadingOverlay",
        OPEN_GAME_DIR = "launcherUi.action.openGameDirectory",
        APPLY_SETTINGS = "launcherUi.action.applySettings"
    },

    SERVER_CORE_ICONS = {
        componentId = "serverBox",
        iconRoot = "assets/ui/icons/srvIcons/",
        iconExt = ".png",
        fallback = "Vanilla",
        icons = {
            vanilla = "Vanilla",
            forge = "Forge",
            fabric = "Fabric",
            neoforge = "NeoForge",
            quilt = "Quilt"
        }
    }
}

UI.ACTIONS = {
    [UI.COMPONENTS.USER_PANE] = {
        name = "toggleUserPane",
        event = UI.EVENTS.USER_PANE_TOGGLE,
        localeKey = UI.ACTION_LOCALE_KEYS.TOGGLE_USER_PANE
    },
    [UI.COMPONENTS.AUTH_SETTINGS] = {
        name = "openSettings",
        event = UI.EVENTS.OPEN_SETTINGS,
        localeKey = UI.ACTION_LOCALE_KEYS.OPEN_SETTINGS
    },
    [UI.COMPONENTS.SETTINGS_SMALL] = {
        name = "openSettings",
        event = UI.EVENTS.OPEN_SETTINGS,
        localeKey = UI.ACTION_LOCALE_KEYS.OPEN_SETTINGS
    },
    [UI.COMPONENTS.BACK] = {
        name = "backFromSettings",
        event = UI.EVENTS.BACK,
        localeKey = UI.ACTION_LOCALE_KEYS.BACK_FROM_SETTINGS
    },
    [UI.COMPONENTS.OPTIONAL_MODS] = {
        name = "optionalMods",
        event = UI.EVENTS.OPTIONAL_MODS,
        localeKey = UI.ACTION_LOCALE_KEYS.OPTIONAL_MODS
    },
    [UI.COMPONENTS.SMALL_BUTTON] = {
        name = "toggleLoadingOverlay",
        event = UI.EVENTS.LOADING_TOGGLE,
        localeKey = UI.ACTION_LOCALE_KEYS.TOGGLE_LOADING
    },
    [UI.COMPONENTS.GAME_DIR_SMALL] = {
        name = "openGameDirectory",
        event = UI.EVENTS.OPEN_GAME_DIR,
        localeKey = UI.ACTION_LOCALE_KEYS.OPEN_GAME_DIR
    },
    [UI.COMPONENTS.APPLY_SETTINGS] = {
        name = "applySettings",
        event = UI.EVENTS.APPLY_SETTINGS,
        localeKey = UI.ACTION_LOCALE_KEYS.APPLY_SETTINGS
    }
}

local function lang(key)
    if engine ~= nil and engine.lang ~= nil then
        return engine.lang(key)
    end
    return key
end

local function lang_with(key, data)
    if engine ~= nil and engine.langWith ~= nil then
        return engine.langWith(key, data)
    end
    return lang(key)
end

local function component_id()
    if component == nil then
        return nil
    end
    if component.getId ~= nil then
        return component:getId()
    end
    return component.id
end

local function component_enabled()
    if component == nil or component.isEnabled == nil then
        return true
    end
    return component:isEnabled()
end

local function remember_action(action)
    if not UI.SETTINGS.markLastActionOnComponent or component == nil or component.putProperty == nil then
        return
    end
    component:putProperty(UI.SETTINGS.actionPropertyKey, action.name)
    component:putProperty(UI.SETTINGS.eventPropertyKey, action.event)
end

local function log_debug_key(key, data)
    if UI.SETTINGS.debug and ui ~= nil and ui.log ~= nil then
        ui.log("[launcher-ui] " .. lang_with(key, data or {}))
    end
end

local function warn_key(key, data)
    if ui ~= nil and ui.warn ~= nil then
        ui.warn("[launcher-ui] " .. lang_with(key, data or {}))
    end
end

local function emit_action(action)
    if component ~= nil and component.emit ~= nil then
        component:emit(action.event)
    elseif ui ~= nil and ui.emit ~= nil then
        ui.emit(action.event)
    else
        warn_key(UI.LOCALE_KEYS.NO_EMITTER, { action = action.name })
    end
end

local function apply_server_core_icons()
    if component ~= nil and component.putProperty ~= nil then
        component:putProperty("launcher.serverBox.coreIcons.source", "lua")
        component:putProperty("launcher.serverBox.coreIcons.iconRoot", UI.SERVER_CORE_ICONS.iconRoot)
        component:putProperty("launcher.serverBox.coreIcons.iconExt", UI.SERVER_CORE_ICONS.iconExt)
        component:putProperty("launcher.serverBox.coreIcons.fallback", UI.SERVER_CORE_ICONS.fallback)
        for core, icon in pairs(UI.SERVER_CORE_ICONS.icons) do
            component:putProperty("launcher.serverBox.coreIcons.map." .. core, icon)
        end
    end
    if component ~= nil and component.emit ~= nil then
        component:emit(UI.EVENTS.SERVER_CORE_ICONS_APPLY)
    elseif ui ~= nil and ui.emit ~= nil then
        ui.emit(UI.EVENTS.SERVER_CORE_ICONS_APPLY)
    else
        warn_key(UI.LOCALE_KEYS.NO_EMITTER, { action = "applyServerCoreIcons" })
    end
end

local function dispatch()
    local id = component_id()
    if id == nil or id == "" then
        warn_key(UI.LOCALE_KEYS.NO_COMPONENT_ID)
        return
    end

    if id == UI.COMPONENTS.SERVER_BOX and (event.name == "init" or event.name == "valuesChanged") then
        apply_server_core_icons()
        return
    end

    local action = UI.ACTIONS[id]
    if action == nil then
        warn_key(UI.LOCALE_KEYS.NO_ACTION, { component = id })
        if component ~= nil and component.emit ~= nil then
            component:emit(UI.SETTINGS.unknownActionEvent)
        end
        return
    end

    if UI.SETTINGS.ignoreDisabledComponents and not component_enabled() then
        log_debug_key(UI.LOCALE_KEYS.IGNORED_DISABLED, { action = action.name, component = id })
        return
    end

    remember_action(action)
    log_debug_key(UI.LOCALE_KEYS.DISPATCHING, { action = action.name, event = action.event })
    emit_action(action)
end

if component ~= nil and event ~= nil then
    dispatch()
end

return UI
