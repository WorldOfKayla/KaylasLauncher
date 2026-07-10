# Launcher Component Constructor Integration

## Registration lifecycle

`LauncherComponentLibrary.register(...)` must run after `Engine.buildGui(...)` creates the `GuiBuilder` and before any screen referencing launcher-owned types is loaded.

```java
buildGui(new InitialValue(this));
LauncherComponentLibrary.register(getGuiBuilder());
loadMainPanel(fileProperties.getMainFrame());
```

This guarantees that:

- built-in engine types already exist in `ComponentCatalog`;
- launcher BASIC definitions can reference the active `ComponentFactory`;
- launcher COMPOSITE definitions pass child-type validation;
- screen XML can resolve launcher-owned canonical types and aliases.

## Registered types

### Basic definitions

```text
launcherSliderTrack
launcherSettingSpinner
launcherDirectoryButton
```

These are launcher-specific implementations used as nodes in graph definitions. They are not directly exposed by settings screen XML.

### Composite definitions

```text
launcherVolumeControl
launcherRamControl
launcherDirectoryControl
```

Settings screens instantiate only these root types.

## Prototype resources

```text
assets/components/settings/
├── volume-control.xml
├── ram-control.xml
└── directory-control.xml
```

Each prototype XML is parsed by `XmlUiDescriptorLoader` during library registration. Child descriptors are deep-copied by the engine before each instance is created.

### Volume graph

```text
launcherVolumeControl
├── label   : label
├── slider  : launcherSliderTrack
└── spinner : launcherSettingSpinner
```

Connections:

```text
slider.change  -> spinner.syncValue
spinner.change -> slider.syncValue
slider.change  -> $root.valueChanged
spinner.change -> $root.valueChanged
```

### RAM graph

The RAM graph has the same node topology. Both numeric nodes carry:

```text
launcher.slider.ram = true
```

The basic creators use `RamRangeCalculator` to resolve minimum, maximum, initial value, tick values and safe step size.

### Directory graph

```text
launcherDirectoryControl
├── path   : textField
└── browse : launcherDirectoryButton
```

Connections:

```text
path.textChanged -> $root.valueChanged
browse.action    -> $root.browseRequested
```

## Changing slider text styles

The slider exposes two different text layers:

```text
label             main caption above the slider
slider.tickLabel  numeric tick labels below the track
```

They are configured independently in the screen XML:

```xml
<component
    type="launcherVolumeControl"
    id="volume"
    style="default">
    <bounds x="35" y="160" width="430" height="65" />
    <styles>
        <style target="label" name="titleBold" />
        <style target="slider.tickLabel" name="promptLabel" />
    </styles>
</component>
```

To change only the main caption:

```xml
<style target="label" name="titleBold" />
```

To change the font, color, font style and size of the numeric scale values:

```xml
<style target="slider.tickLabel" name="promptLabel" />
```

The selector is hierarchical:

```text
slider      local child node of launcherVolumeControl
 tickLabel  internal style slot exposed by launcherSliderTrack
```

The current launcher settings declare both slots explicitly:

```xml
<styles>
    <style target="label" name="title" />
    <style target="slider.tickLabel" name="title" />
</styles>
```

Available label styles are defined in:

```text
assets/styles/label.json
```

For example, to make the numeric values bold:

```xml
<style target="slider.tickLabel" name="titleBold" />
```

To change their exact size, add or edit `fontSize` in that label style:

```json
"tickNumbers": {
  "opaque": false,
  "font": "mcfontBold",
  "fontStyle": "bold",
  "fontSize": 10,
  "color": "#252424"
}
```

Then select it:

```xml
<style target="slider.tickLabel" name="tickNumbers" />
```

The reusable prototype defines the fallback slot under the internal slider node:

```xml
<styles>
    <style target="tickLabel" name="title" />
</styles>
```

in `assets/components/settings/volume-control.xml` and `ram-control.xml`.

## Scoped runtime ids

For the XML declaration:

```xml
<component type="launcherVolumeControl" id="volume" />
```

runtime ids become:

```text
volume
volume.label
volume.slider
volume.spinner
```

For the directory control:

```text
homeDir
homeDir.path
homeDir.browse
```

The root id remains compatible with `ComponentsAccessor` and configuration keys. Child ids stay isolated inside the constructor scope.

## Lua synchronization

### Internal node synchronization

`component-links.lua` registers a targeted listener on slider and spinner nodes:

```lua
component:on("syncValue", function(signal, target)
    local incoming = signal.payload
    if target:getValue() ~= incoming then
        target:setValue(incoming)
    end
end)
```

The equality guard prevents cyclic change propagation.

### Root policy

`composite-controls.lua` registers root listeners:

```lua
component:on("valueChanged", function(signal, root)
    root:emit("launcher.settings.control.changed", {
        key = root:getProperty("launcher.setting.key"),
        value = signal.payload,
        scope = root:getScopeId()
    })
end)
```

Duplicate slider/spinner deliveries are suppressed using the root client property:

```text
launcher.control.lastValue
```

Directory intent is forwarded through:

```text
launcher.settings.directory.requested
```

## Java bridge

`LauncherLuaUiBridge` owns launcher-domain effects:

- updating `Config` values;
- applying live sound volume;
- updating the composite root value;
- opening `JFileChooser` for directory selection;
- writing the selected path to the scoped `path` node.

Lua remains responsible for UI intent and component routing. Java remains responsible for operating-system dialogs and application state.

## Form collection

`Settings` collects `ConstructedCompositeComponent` roots rather than their internal nodes:

```java
List.of(
    TextArea.class,
    Checkbox.class,
    Combobox.class,
    ConstructedCompositeComponent.class
)
```

Because `ConstructedCompositeComponent` extends `CompositeComponent`, the existing `ComponentsAccessor` value extractor reads the root value. The resulting configuration map keeps the original keys:

```text
volume
ramAmount
homeDir
```

No child id such as `volume.slider` or `homeDir.path` leaks into persisted launcher settings.

## Styles

Launcher-owned definition styles:

```text
launcherSliderTrack.json
launcherSettingSpinner.json
launcherDirectoryButton.json
launcherVolumeControl.json
launcherRamControl.json
launcherDirectoryControl.json
```

Specialized basic creators resolve the visual style of the underlying engine type:

```text
launcherSliderTrack  -> slider/slidingOut
launcherDirectoryButton -> button/altButton
```

This allows catalog registration and root style validation without duplicating engine textures.

## Verification

```powershell
.\gradlew.bat test launcherComponentsCheck
```

`LauncherComponentLibraryVerification` checks:

- prototype XML structure and node types;
- non-zero node bounds;
- replacement of legacy `compositeSlider` screen declarations;
- replacement of legacy `fileSelector` screen declarations;
- Lua syntax for both constructor policies;
- presence of every launcher-owned style resource.
