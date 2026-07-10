# Kaylas Launcher

![Java 17](https://img.shields.io/badge/Java-17-blue)
![Launcher](https://img.shields.io/badge/Launcher-2.0.0--Anderson-orange)
![KaylasUI Engine](https://img.shields.io/badge/KaylasUI-2.1.0--AURELIA-purple)

Kaylas Launcher uses the KaylasUI Engine 2.1 Component Constructor Runtime for reusable linked controls.

## Launcher component library

The launcher registers its component library after `GuiBuilder` initialization and before loading application screens:

```java
buildGui(new InitialValue(this));
LauncherComponentLibrary.register(getGuiBuilder());
loadMainPanel(fileProperties.getMainFrame());
```

Registered launcher-owned types:

| Kind | Type | Purpose |
| --- | --- | --- |
| Basic | `launcherSliderTrack` | Textured settings slider |
| Basic | `launcherSettingSpinner` | Numeric settings editor |
| Basic | `launcherDirectoryButton` | Directory chooser button |
| Composite | `launcherVolumeControl` | Label, volume slider and spinner |
| Composite | `launcherRamControl` | Label, dynamic RAM slider and spinner |
| Composite | `launcherDirectoryControl` | Read-only path field and browse button |

The settings XML screens only instantiate the composite roots. Internal nodes, bounds, scripts and styles are defined in reusable templates under:

```text
src/main/resources/assets/components/settings/
```

Lua signal policies are located under:

```text
src/main/resources/assets/scripts/launcher/
├── component-links.lua
└── composite-controls.lua
```

Detailed architecture: [`docs/component-constructor.md`](docs/component-constructor.md).

## Verification

```powershell
.\gradlew.bat test launcherComponentsCheck
```

The verification parses all component templates, checks that legacy `compositeSlider` and `fileSelector` declarations were removed from settings screens, compiles the Lua policies, and validates every launcher-owned style resource.
