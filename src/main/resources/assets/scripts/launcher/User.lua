-- KaylasLauncher user panel UI policy.
--
-- Java owns runtime orchestration. This Lua file owns user-panel ids, timings, paths,
-- layout numbers and other tweakable UI policy values.

return {
    panels = {
        userPane = "userPane",
        loggedForm = "loggedForm",
        newsForm = "newsForm",
        userBadges = "userBadges",
        authorisedSpec = "loggedForm->true|newsForm->true|authForm->false",
        pendingSpec = "loggedForm->false|newsForm->true|authForm->true",
        unauthorisedSpec = "loggedForm->false|newsForm->true|authForm->true"
    },
    auth = {
        waitIntervalMs = 200
    },
    balance = {
        crystalsKey = "crystals",
        unitsKey = "units",
        fallbackAmount = "0"
    },
    serverBox = {
        valuesChangedEvent = "valuesChanged",
        valuesChangedReason = "serversLoaded"
    },
    discord = {
        launcherLocaleKey = "general.launcher",
        loginLocaleKey = "game.login",
        loginPlaceholder = "login",
        iconKey = "launcher"
    },
    greet = {
        localeKey = "logged.greet",
        loginPlaceholder = "login"
    },
    loaders = {
        headRequestMethod = "GET"
    },
    headIcon = {
        size = 64,
        radius = 32
    },
    badges = {
        hgap = 5,
        vgap = 4,
        iconWidth = 25,
        iconHeight = 25,
        singleBadgeFailureKey = "user.badges.singleFailure",
        allBadgesFailureKey = "user.badges.allFailure"
    },
    skinHover = {
        durationMs = 120,
        steps = 15
    },
    loginNotification = {
        localeKey = "auth.loggedIn",
        loginPlaceholder = "login",
        x = 10,
        yOffset = 40,
        width = 340,
        height = 45,
        durationMs = 3000
    },
    taskManager = {
        adminGroup = "admin",
        iconPath = "assets/ui/icons/threadBolt.png",
        resizable = false
    },
    newsItem = {
        insetTop = 10,
        insetLeft = 10,
        insetBottom = 10,
        insetRight = 10,
        keyColor = "#000000",
        valueColor = "#808080",
        keyFont = "mcfontBold",
        valueFont = "mcfont",
        fontSize = 11
    },
    tasks = {
        updateServerPrefix = "updateServer"
    }
}
