-- Launcher-facing event policy for Component Constructor 2.1 controls.
-- Child-to-child synchronization is declared by Java ComponentConnection routes.
-- This script forwards root-level intent to the Java launcher bridge.

local EVENTS = {
    VALUE_CHANGED = "launcher.settings.control.changed",
    DIRECTORY_REQUESTED = "launcher.settings.directory.requested"
}

local function setting_key(root)
    local key = root:getProperty("launcher.setting.key")
    if key == nil then
        return ""
    end
    return tostring(key)
end

if event.name == "init"
        and component:getProperty("launcher.control.bound") ~= true then
    component:putProperty("launcher.control.bound", true)

    component:on("valueChanged", function(signal, root)
        local incoming = signal.payload
        local previous = root:getProperty("launcher.control.lastValue")
        if previous ~= nil and previous == incoming then
            return
        end

        root:putProperty("launcher.control.lastValue", incoming)
        root:emit(EVENTS.VALUE_CHANGED, {
            key = setting_key(root),
            value = incoming,
            scope = root:getScopeId()
        })
    end)

    component:on("browseRequested", function(signal, root)
        root:emit(EVENTS.DIRECTORY_REQUESTED, {
            key = setting_key(root),
            scope = root:getScopeId()
        })
    end)
end
