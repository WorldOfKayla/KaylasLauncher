package org.takesome.launcher.gui.components;

import org.apache.logging.log4j.LogManager;
import org.luaj.vm2.Globals;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.descriptor.XmlUiDescriptorLoader;
import org.takesome.kaylasEngine.gui.components.Attributes;
import org.takesome.kaylasEngine.gui.components.ComponentAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final List<String> SCREEN_DESCRIPTORS = List.of(
            "assets/frames/frame.xml",
            "assets/frames/mainFrame.xml",
            "assets/frames/forms/mainPanel/authForm.xml",
            "assets/frames/forms/mainPanel/download.xml",
            "assets/frames/forms/mainPanel/loggedForm.xml",
            "assets/frames/forms/mainPanel/newsForm.xml",
            "assets/frames/forms/mainPanel/serverInfo.xml",
            "assets/frames/forms/mainPanel/settings.xml",
            "assets/frames/forms/settings/generalSettings.xml",
            "assets/frames/forms/settings/settingsInfo.xml",
            "assets/frames/forms/user/paneContents.xml",
            "assets/frames/forms/user/serverSelector.xml",
            "assets/frames/forms/user/userBalance.xml",
            "assets/frames/forms/user/userPane.xml",
            "assets/frames/forms/utils/loadPanel.xml"
    );

    private static final Pattern COMPONENT_ID = Pattern.compile(
            "<component\\b[^>]*\\bid=\"([^\"]+)\"",
            Pattern.DOTALL
    );

    private LauncherComponentLibraryVerification() {
    }

    public static void main(String[] args) {
        System.setProperty("log.dir", System.getProperty("user.dir", "."));
        System.setProperty("log.level", "INFO");
        Engine.LOGGER = LogManager.getLogger(LauncherComponentLibraryVerification.class);

        verifyTemplates();
        verifyLauncherScreens();
        verifyUniqueScreenComponentIds();
        verifyLuaSyntax();
        verifyStyleResources();
        verifyEngineManifest();

        System.out.println("Launcher Component Constructor 2.1 verification passed.");
    }

    private static void verifyTemplates() {
        XmlUiDescriptorLoader loader = new XmlUiDescriptorLoader();
        TEMPLATES.forEach((resource, expectedNodes) -> {
            Attributes attributes = loader.load(resource);
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
        String generalSettings = resourceText("assets/frames/forms/settings/generalSettings.xml");
        require(generalSettings.contains("type=\"tabs\""),
                "Settings screen does not use the generic tabs component");
        require(generalSettings.contains("id=\"settingsTabs\""),
                "Settings screen does not expose settingsTabs");
        require(generalSettings.contains("launcher.access.group"),
                "Administration settings tab does not declare a group policy");
        require(generalSettings.contains("id=\"showTaskManager\""),
                "Administrator Task Manager setting is missing");

        Attributes settingsDescriptor = new XmlUiDescriptorLoader()
                .load("assets/frames/forms/settings/generalSettings.xml");
        var settingsPanel = settingsDescriptor.getGroups().get("generalSettings");
        require(settingsPanel != null, "generalSettings panel is missing");

        Map<String, ComponentAttributes> rootComponents = index(settingsPanel.getChildComponents());
        ComponentAttributes tabs = rootComponents.get("settingsTabs");
        require(tabs != null, "settingsTabs descriptor is missing");
        require("tabs".equals(tabs.getComponentType()),
                "settingsTabs is not backed by the engine tabs component");
        require("general".equals(String.valueOf(tabs.getProperties().get("tabs.selected"))),
                "settings tabs do not default to the general page");

        Map<String, ComponentAttributes> pages = index(tabs.getChildComponents());
        for (String pageId : List.of(
                "settingsGeneralTab",
                "settingsRuntimeTab",
                "settingsAdministrationTab"
        )) {
            require(pages.containsKey(pageId), "Missing settings tab page: " + pageId);
        }

        ComponentAttributes administration = pages.get("settingsAdministrationTab");
        require("false".equals(String.valueOf(administration.getProperties().get("tab.visible"))),
                "Administration tab must be hidden until group authorization");
        require("admin".equals(String.valueOf(
                        administration.getProperties().get("launcher.access.group")
                )),
                "Administration tab does not require the admin group");

        Map<String, ComponentAttributes> controls = indexRecursively(tabs.getChildComponents());
        require(LauncherComponentLibrary.VOLUME_CONTROL.equals(
                        controls.get("volume").getComponentType()
                ),
                "Runtime settings tab does not use launcherVolumeControl");
        require(LauncherComponentLibrary.RAM_CONTROL.equals(
                        controls.get("ramAmount").getComponentType()
                ),
                "Runtime settings tab does not use launcherRamControl");
        require(LauncherComponentLibrary.DIRECTORY_CONTROL.equals(
                        controls.get("homeDir").getComponentType()
                ),
                "Runtime settings tab does not use launcherDirectoryControl");
        require("checkBox".equals(controls.get("showTaskManager").getComponentType()),
                "Task Manager setting is not a checkbox");
        require("admin".equals(String.valueOf(
                        controls.get("showTaskManager")
                                .getProperties()
                                .get("launcher.access.group")
                )),
                "Task Manager setting does not require the admin group");
    }

    private static void verifyUniqueScreenComponentIds() {
        Map<String, String> owners = new LinkedHashMap<>();
        for (String resource : SCREEN_DESCRIPTORS) {
            Matcher matcher = COMPONENT_ID.matcher(resourceText(resource));
            while (matcher.find()) {
                String componentId = matcher.group(1);
                String previousOwner = owners.putIfAbsent(componentId, resource);
                require(
                        previousOwner == null,
                        "Duplicate launcher component id '" + componentId
                                + "' in " + previousOwner + " and " + resource
                );
            }
        }
        require(!owners.isEmpty(), "Launcher descriptors did not expose any component ids");
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

    private static Map<String, ComponentAttributes> indexRecursively(
            List<ComponentAttributes> components
    ) {
        Map<String, ComponentAttributes> result = new LinkedHashMap<>();
        indexRecursively(components, result);
        return result;
    }

    private static void indexRecursively(
            List<ComponentAttributes> components,
            Map<String, ComponentAttributes> result
    ) {
        if (components == null) {
            return;
        }
        for (ComponentAttributes component : components) {
            if (component == null) {
                continue;
            }
            String id = component.getComponentId();
            if (id != null && !id.isBlank()) {
                require(result.putIfAbsent(id, component) == null,
                        "Duplicate nested settings component id: " + id);
            }
            indexRecursively(component.getChildComponents(), result);
        }
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
