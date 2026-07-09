-- Launcher-owned loading UI configuration and renderer.
--
-- KaylasUIEngine provides the window/timer/Lua execution/draw primitives.
-- KaylasLauncher owns visual policy and render logic here.

local loading = {
    overlay = {
        name = "loadingOverlay",
        color = "#000000",

        -- 0.0 .. 1.0. Converted to Swing alpha 0..255 by the launcher.
        opacity = 0.70,

        -- Fade timing in milliseconds.
        fadeInMs = 220,
        fadeOutMs = 240,
        frameDelayMs = 16,

        -- Overlay geometry is launcher policy.
        -- x/y are relative to the launcher frame layered pane.
        x = 0,
        y = 0,

        -- Use -1 to follow the current frame size.
        width = -1,
        height = -1
    },

    window = {
        alwaysOnTop = true,
        cornerRadius = 20
    },

    progress = {
        enabled = true
    }
}

local function resolve_size(value, fallback)
    if value == nil or value < 0 then
        return fallback
    end
    return value
end

function loading.overlay.render(ctx, draw)
    local overlay = loading.overlay
    local state = ctx.state or {}
    local alpha = state.alpha or 0

    local x = overlay.x or 0
    local y = overlay.y or 0
    local width = resolve_size(overlay.width, ctx.width)
    local height = resolve_size(overlay.height, ctx.height)

    draw.fillRect(x, y, width, height, overlay.color, alpha)
end

return loading
