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
        frameDelayMs = 32,

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
        cornerRadius = 15
    },

    -- Typography of the loading-window labels.
    -- Empty font/color values inherit them from the selected label style.
    typography = {
        title = {
            style = "titleBold",
            font = "mcfontBold",
            fontSize = 16,
            fontStyle = "plain", -- plain | bold | italic | boldItalic
            color = ""
        },

        message = {
            style = "title",
            font = "mcfont",
            fontSize = 11,
            fontStyle = "plain",
            color = ""
        }
    },

    progress = {
        enabled = true,

        -- Named profile from assets/styles/progressBar.json.
        style = "progressMini",

        -- Text font override. Empty/0 values inherit from the named style.
        font = "FSElliotPro",
        fontSize = 13,
        fontStyle = "plain", -- plain | bold | italic | boldItalic
        textColor = "#9552f8",

        -- Progress speed policy. Smaller updateMs and larger step = faster fill.
        updateMs = 85,
        step = 4,
        maxValue = -1,
        initialDelayMs = 0,
        cycleDelayMs = 16,
        loop = true,

        -- Entrance/exit animation policy.
        timelineDurationMs = 256,
        timelineFrameDelayMs = 32,
        animateEntrance = true,
        animateExit = true,

        -- Text policy. Messages are read from progressMessages in the active assets/lang/* locale.
        showText = true,
        showPercent = false,
        randomMessages = true,
        resetOnStop = true,
        hideOnStop = true,

        -- Primary localized source: the complete section in assets/lang/<locale>.json.
        messagesSection = "progressMessages",

        -- Legacy fallback used only when the localized section is absent.
        messagesResource = "assets/messages.json",
        animationConfigResource = "assets/animation_config.json",

        messageKeys = {}
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
