package org.takesome.launcher.gui.components;

import org.apache.logging.log4j.LogManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.adapters.xml.XmlFrameAttributesLoader;
import org.takesome.kaylasEngine.gui.components.Attributes;
import org.takesome.kaylasEngine.gui.components.ComponentAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executable verification for launcher-owned Component Constructor 2.1 resources. */
public final class LauncherComponentLibraryVerification {
    private static final Map<String, Map<String, String>> TEMPLATES = Map.of(
            "assets/components/settings/volume-control.xml",
            Map.of(
                    LauncherComponentLibrary.NODE_LABEL, "label",
                    LauncherComponentLibrary.NODE_SLIDER, LauncherComponentLibrary.SLIDER_TRACK,
                    LauncherComponentLibrary.NODE_SPINNER, LauncherComponentLibrary.SETTING_SPINNER
            ),
            "assets/components/settings/ram-control.xml",
            Map.of(
                    LauncherComponentLibrary.NODE_LABEL, "label",
                    LauncherComponentLibrary.NODE_SLIDER, LauncherComponentLibrary.SLIDER_TRACK,
                    LauncherComponentLibrary.NODE_SPINNER, LauncherComponentLibrary.SETTING_SPINNER
            ),
            "assets/components/settings/directory-control.xml",
            Map.of(
                    LauncherComponentLibrary.NODE_PATH, "textField",
                    LauncherComponentLibrary.NODE_BROWSE, LauncherComponentLibrary.DIRECTORY_BUTTON
            )
    );

    private static final List<String> LUA_SCRIPTS = List.of(
            "assets/scripts/launcher/component-links.lua",
            "assets/scripts/launcher/composite-controls.lua"
    );

    private static final List<String> STYLE_RESOURCES = List.of(
            "assets/styles/launcherSliderTrack.json",
            "assets/styles/launcherSettingSpinner.json",
            "assets/styles/launcherDirectoryButton.json",
            "assets/styles/launcherVolumeControl.json",
            "assets/styles/launcherRamControl.json",
            "assets/styles/launcherDirectoryControl.json"
    );

    private LauncherComponentLibraryVerification() {
    }

    public static void main(String[] args) {
        System.setProperty("log.dir", System.getProperty("user.dir", "."));
        System.setProperty("log.level", "INFO");
        Engine.LOGGER = LogManager.getLogger(LauncherComponentLibraryVerification.class);

        verifyTemplates();
        verifyLauncherScreens();
        verifyLuaSyntax();
        verifyStyleResources();
        verifyEngineManifest();

        System.out.println("Launcher Component Constructor 2.1 verification passed.");
    }

    private static void verifyTemplates() {
        XmlFrameAttributesLoader loader = new XmlFrameAttributesLoader();
        TEMPLATES.forEach((resource, expectedNodes) -> {
            Attributes attributes = loader.getAttributes(resource);
            Map<String, ComponentAttributes> nodes = index(attributes.getChildComponents());
            require(nodes.size() == expectedNodes.size(),
                    "Unexpected node count in " + resource + ": " + nodes.keySet());

            expectedNodes.forEach((nodeId, expectedType) -> {
                ComponentAttributes node = nodes.get(nodeId);
                require(node != null, "Missing node '" + nodeId + "' in " + resource);
                require(expectedType.equals(node.getComponentType()),
                        "Node '" + nodeId + "' in " + resource
                                + " has type '" + node.getComponentType()
                                + "', expected '" + expectedType + "'");
                require(node.getBounds().width > 0 && node.getBounds().height > 0,
                        "Node '" + nodeId + "' in " + resource + " has invalid bounds");
                if (LauncherComponentLibrary.NODE_SLIDER.equals(nodeId)
                        || LauncherComponentLibrary.NODE_SPINNER.equals(nodeId)) {
                    require(
                            "assets/scripts/launcher/component-links.lua".equals(
                                    node.getScripts().get("init")
                            ),
                            "Linked numeric node '" + nodeId + "' in " + resource
                                    + " does not bind component-links.lua"
                    );
                }
                if (LauncherComponentLibrary.NODE_SLIDER.equals(nodeId)) {
                    require(
                            "title".equals(
                                    LauncherComponentLibrary.tickLabelStyleName(node)
                            ),
                            "Slider node in " + resource
                                    + " does not expose the tickLabel style slot"
                    );
                }
            });
        });
    }

    private static void verifyLauncherScreens() {
        String sliders = resourceText("assets/frames/forms/settings/sliders.xml");
        require(sliders.contains("type=\"" + LauncherComponentLibrary.VOLUME_CONTROL + "\""),
                "Settings sliders screen does not use launcherVolumeControl");
        require(sliders.contains("type=\"" + LauncherComponentLibrary.RAM_CONTROL + "\""),
                "Settings sliders screen does not use launcherRamControl");
        require(!sliders.contains("type=\"compositeSlider\""),
                "Legacy compositeSlider remains in settings sliders screen");

        Attributes sliderScreen = new XmlFrameAttributesLoader()
                .getAttributes("assets/frames/forms/settings/sliders.xml");
        Map<String, ComponentAttributes> sliderControls = index(sliderScreen.getChildComponents());
        for (String controlId : List.of("volume", "ramAmount")) {
            ComponentAttributes control = sliderControls.get(controlId);
            require(control != null, "Missing settings control: " + controlId);
            require("title".equals(control.getStyles().get("label")),
                    "Settings control '" + controlId
                            + "' does not expose an instance-level label style override");
            require("title".equals(control.getStyles().get("slider.tickLabel")),
                    "Settings control '" + controlId
                            + "' does not expose an instance-level tick label style override");
        }

        String generalSettings = resourceText("assets/frames/forms/settings/generalSettings.xml");
        require(generalSettings.contains(
                        "type=\"" + LauncherComponentLibrary.DIRECTORY_CONTROL + "\""),
                "General settings screen does not use launcherDirectoryControl");
        require(!generalSettings.contains("type=\"fileSelector\""),
                "Legacy fileSelector remains in general settings screen");
    }

    private static void verifyLuaSyntax() {
        Globals globals = JsePlatform.standardGlobals();
        for (String script : LUA_SCRIPTS) {
            String source = resourceText(script);
            require(!source.isBlank(), "Lua policy is empty: " + script);
            globals.load(source, script);
        }
    }

    private static void verifyStyleResources() {
        for (String style : STYLE_RESOURCES) {
            require(resourceExists(style), "Missing launcher component style: " + style);
        }
    }

    private static void verifyEngineManifest() {
        String engineManifest = resourceText("engine.json");
        for (String componentType : List.of(
                LauncherComponentLibrary.SLIDER_TRACK,
                LauncherComponentLibrary.SETTING_SPINNER,
                LauncherComponentLibrary.DIRECTORY_BUTTON,
                LauncherComponentLibrary.VOLUME_CONTROL,
                LauncherComponentLibrary.RAM_CONTROL,
                LauncherComponentLibrary.DIRECTORY_CONTROL
        )) {
            require(
                    engineManifest.contains("\"" + componentType + "\""),
                    "engine.json does not declare launcher component style: " + componentType
            );
        }
    }

    private static Map<String, ComponentAttributes> index(List<ComponentAttributes> components) {
        Map<String, ComponentAttributes> result = new LinkedHashMap<>();
        for (ComponentAttributes component : components) {
            String id = component.getComponentId();
            require(id != null && !id.isBlank(), "Component template contains a node without id");
            require(result.putIfAbsent(id, component) == null,
                    "Duplicate node id in component template: " + id);
        }
        return result;
    }

    private static String resourceText(String resource) {
        try (InputStream stream = resourceStream(resource)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read resource: " + resource, error);
        }
    }

    private static boolean resourceExists(String resource) {
        try (InputStream ignored = LauncherComponentLibraryVerification.class
                .getClassLoader()
                .getResourceAsStream(resource)) {
            return ignored != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static InputStream resourceStream(String resource) {
        InputStream stream = LauncherComponentLibraryVerification.class
                .getClassLoader()
                .getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Resource not found: " + resource);
        }
        return stream;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
