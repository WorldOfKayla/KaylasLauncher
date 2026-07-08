# User.lua UI Policy

`assets/scripts/launcher/User.lua` is the launcher-owned UI policy file for the authenticated user panel.

Java code keeps runtime orchestration: authentication state, backend calls, Swing component updates, loaders, and task execution. Lua keeps tunable UI policy values: panel IDs, panel visibility specs, timing values, resource paths, localization keys, component dimensions, and notification layout.

## Runtime flow

1. `User` constructs `LauncherUserUiConfig`.
2. `LauncherUserUiConfig` reads the script path from `assets/ui/launcher-ui-provider.json`:

```json
"userUi": "assets/scripts/launcher/User.lua"
```

3. `LuaConfigScript` executes `User.lua` and converts the returned Lua table into Java maps.
4. `LauncherUserUiConfig` exposes typed records to `User.java`.
5. `User.java` applies those values while keeping domain logic in Java.

## Ownership rules

Keep these values in `User.lua`:

- panel IDs and panel visibility specs;
- authorization polling interval;
- balance keys and fallback amount;
- server box event names;
- Discord locale keys and icon key;
- greeting locale key;
- loader request method;
- head icon size and corner radius;
- badge layout, icon size, and error locale keys;
- skin hover duration and frame count;
- login notification bounds and duration;
- task manager visibility group, icon path, and resizable flag;
- deprecated news item visual policy;
- task name prefixes.

Keep these in Java:

- actual authentication state transitions;
- loader calls and backend calls;
- Swing component mutation;
- validation and fallback behavior;
- logging and exception handling;
- typed config parsing.

## Localization rule

Do not put localized human text directly in `User.lua` or Java. Use locale keys in `User.lua` and put translated strings in language files.

Russian text belongs only in:

```text
assets/lang/ru_RU.json
```

English text belongs in:

```text
assets/lang/en_US.json
```

Polish text belongs in:

```text
assets/lang/pl_PL.json
```

Example:

```lua
badges = {
    singleBadgeFailureKey = "user.badges.singleFailure",
    allBadgesFailureKey = "user.badges.allFailure"
}
```

Corresponding locale values:

```json
"user": {
  "badges": {
    "singleFailure": "Failed to load badge {badge}: {error}",
    "allFailure": "Failed to load badges for {login}. Reason: {error}"
  }
}
```

## Adding new user-panel policy

1. Add the value to `User.lua`.
2. Add a typed field to `LauncherUserUiConfig`.
3. Add a safe fallback in `LauncherUserUiConfig.FALLBACK`.
4. Use the typed accessor from `User.java`.
5. If the value is human-readable text, store a locale key in `User.lua` and put the text in the language files.
6. Run:

```bash
./gradlew processResources compileJava
```
