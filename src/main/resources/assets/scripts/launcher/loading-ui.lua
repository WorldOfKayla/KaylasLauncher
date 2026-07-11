-- Kayla's Launcher loading UI policy.
--
-- This script is the single source of truth for the loader appearance and animation.
-- KaylasUIEngine only validates these tables and executes the requested transitions.

local function position(reference, reference_x, reference_y, window_x, window_y, offset_x, offset_y)
    return {
        reference = reference,
        referenceX = reference_x,
        referenceY = reference_y,
        windowX = window_x,
        windowY = window_y,
        offsetX = offset_x or 0,
        offsetY = offset_y or 0
    }
end

local function cubic_bezier(x1, y1, x2, y2)
    return {
        type = "cubicBezier",
        x1 = x1,
        y1 = y1,
        x2 = x2,
        y2 = y2
    }
end

local loading = {
    window = {
        width = 500,
        height = 150,
        alwaysOnTop = true,
        cornerRadius = 15
    },

    -- The launcher setting loaderIndex selects one of these profiles (0-based).
    -- Every sprite/background detail lives here rather than in engine.json.
    loader = {
        profiles = {
            {
                enabled = true,
                sprite = {
                    path = "assets/ui/sprites/loaderFox.png",
                    rows = 3,
                    columns = 5,
                    frameDelayMs = 45,
                    bounds = {
                        x = 30,
                        y = 40,
                        width = 64,
                        height = 64
                    }
                },
                background = {
                    image = "assets/ui/img/bg/season/summer.png",
                    color = "#b3a8998a"
                },
                titleColor = "#252424",
                messageColor = "#2c382f"
            },
            {
                enabled = true,
                sprite = {
                    path = "assets/ui/sprites/exp.png",
                    rows = 7,
                    columns = 6,
                    frameDelayMs = 50,
                    bounds = {
                        x = 30,
                        y = 40,
                        width = 64,
                        height = 64
                    }
                },
                background = {
                    image = "assets/ui/img/bg/season/summer.png",
                    color = "#b3a8998a"
                },
                titleColor = "#ffffff",
                messageColor = "#2c382f"
            }
        }
    },

    -- Floating loader-window animation.
    -- Motion and opacity are independent channels and may use different speeds, delays and easing.
    transition = {
        enabled = true,

        entry = {
            motion = {
                enabled = true,
                delayMs = 120,

                -- Controls the speed at which the loader drops into place.
                durationMs = 820,
                frameDelayMs = 360,
                easing = cubic_bezier(0.16, 1.0, 0.30, 1.0),

                -- referenceX/referenceY choose a point on the launcher frame.
                -- windowX/windowY choose the point on the loader window attached to it.
                -- This starts above the frame and ends in its center.
                from = position("frame", 0.5, 0.0, 0.5, 1.0, 0, -12),
                to = position("frame", 0.5, 0.5, 0.5, 0.5, 0, 0)
            },

            opacity = {
                enabled = true,
                delayMs = 35,
                durationMs = 550,
                frameDelayMs = 16,
                easing = "easeOutCubic",
                from = 0.0,
                to = 1.0
            }
        },

        exit = {
            motion = {
                enabled = true,
                delayMs = 0,
                durationMs = 300,
                frameDelayMs = 16,
                easing = "easeInCubic",

                -- current keeps the exact current position as the start point.
                from = {
                    reference = "current",
                    offsetX = 0,
                    offsetY = 0
                },

                -- Move beyond the right edge of the launcher frame.
                to = position("frame", 1.0, 0.5, 0.0, 0.5, 24, 0)
            },

            opacity = {
                enabled = true,
                delayMs = 40,
                durationMs = 210,
                frameDelayMs = 16,
                easing = "easeInQuad",
                from = 1.0,
                to = 0.0
            }
        }
    },

    -- Main-frame darkening behind the floating loader window.
    overlay = {
        enabled = true,
        name = "loadingOverlay",
        color = "#000000",

        -- 0.0 .. 1.0; alpha = 0 .. 255 is also accepted.
        opacity = 0.70,

        -- Independent darkening and clearing speeds.
        fadeIn = {
            durationMs = 220,
            frameDelayMs = 16,
            easing = "easeOutQuad"
        },
        fadeOut = {
            durationMs = 260,
            frameDelayMs = 16,
            easing = "easeInOutSine"
        },

        -- Exact darkened region relative to the launcher's layered pane.
        -- -1 follows the current launcher width or height.
        bounds = {
            x = 0,
            y = 0,
            width = -1,
            height = -1
        }
    },

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
        style = "progressMini",

        font = "FSElliotPro",
        fontSize = 13,
        fontStyle = "plain",
        textColor = "#2c382f",

        updateMs = 85,
        step = 4,
        maxValue = -1,
        initialDelayMs = 0,
        cycleDelayMs = 16,
        loop = true,

        timelineDurationMs = 256,
        timelineFrameDelayMs = 32,
        animateEntrance = true,
        animateExit = true,

        showText = true,
        showPercent = false,
        randomMessages = true,
        resetOnStop = true,
        hideOnStop = true,

        messagesSection = "progressMessages",
        messagesResource = "assets/messages.json",
        animationConfigResource = "assets/animation_config.json",
        messageKeys = {}
    }
}

return loading
