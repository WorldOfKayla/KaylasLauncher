-- Internal value synchronization for launcher constructor components.
-- Each component keeps a targeted listener scoped to its fully-qualified runtime id.

if event.name == "init"
        and component:getProperty("launcher.links.bound") ~= true then
    component:putProperty("launcher.links.bound", true)

    component:on("syncValue", function(signal, target)
        local incoming = signal.payload
        if incoming == nil then
            return
        end

        local current = target:getValue()
        if current ~= incoming then
            target:setValue(incoming)
        end
    end)
end
