-- Lua-driven launcher notification policy.
-- Java supplies semantic payload only; the engine owns rendering through engine.notifications.

local payload = event ~= nil and event.payload or {}
local api = engine ~= nil and engine.notifications or nil

if api == nil or api.show == nil then
    if ui ~= nil and ui.warn ~= nil then
        ui.warn("[notifications] engine.notifications API is unavailable")
    end
    return
end

local message = payload.message or ""
if payload.localeKey ~= nil and payload.localeKey ~= "" then
    if payload.replacements ~= nil and engine.langWith ~= nil then
        message = engine.langWith(payload.localeKey, payload.replacements)
    elseif engine.lang ~= nil then
        message = engine.lang(payload.localeKey)
    end
end

api.show({
    type = payload.type or "INFO",
    location = payload.location or "BOTTOM_RIGHT",
    durationMs = payload.durationMs or 3000,
    message = message,
    bounds = payload.bounds
})
