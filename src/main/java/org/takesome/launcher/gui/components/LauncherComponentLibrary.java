package org.takesome.launcher.gui.components;

import org.takesome.kaylasEngine.Engine;
import org.takesome.kaylasEngine.gui.GuiBuilder;
import org.takesome.kaylasEngine.gui.adapters.xml.XmlFrameAttributesLoader;
import org.takesome.kaylasEngine.gui.components.Attributes;
import org.takesome.kaylasEngine.gui.components.ComponentAttributes;
import org.takesome.kaylasEngine.gui.components.ComponentCatalog;
import org.takesome.kaylasEngine.gui.components.ComponentCreationContext;
import org.takesome.kaylasEngine.gui.components.ComponentDefinition;
import org.takesome.kaylasEngine.gui.components.CompositeComponent;
import org.takesome.kaylasEngine.gui.components.button.Button;
import org.takesome.kaylasEngine.gui.components.button.ButtonStyle;
import org.takesome.kaylasEngine.gui.components.constructor.ComponentConnection;
import org.takesome.kaylasEngine.gui.components.constructor.ComponentConstructor;
import org.takesome.kaylasEngine.gui.components.constructor.CompositeComponentDefinition;
import org.takesome.kaylasEngine.gui.components.slider.Slider;
import org.takesome.kaylasEngine.gui.components.slider.TexturedSliderUI;
import org.takesome.kaylasEngine.gui.components.spinner.Spinner;
import org.takesome.kaylasEngine.gui.styles.StyleAttributes;
import org.takesome.kaylasEngine.utils.RamRangeCalculator;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.takesome.kaylasEngine.utils.FontUtils.hexToColor;

/**
 * Launcher-owned component library built on KaylasUIEngine Component Constructor 2.1.
 *
 * <p>The launcher defines its reusable settings controls here instead of relying on hard-coded
 * engine composites. Child synchronization is declared through Lua signal routes, while Java keeps
 * responsibility for launcher-domain operations such as changing sound volume or opening a native
 * directory chooser.</p>
 */
public final class LauncherComponentLibrary {
    public static final String SLIDER_TRACK = "launcherSliderTrack";
    public static final String SETTING_SPINNER = "launcherSettingSpinner";
    public static final String DIRECTORY_BUTTON = "launcherDirectoryButton";

    public static final String VOLUME_CONTROL = "launcherVolumeControl";
    public static final String RAM_CONTROL = "launcherRamControl";
    public static final String DIRECTORY_CONTROL = "launcherDirectoryControl";

    public static final String NODE_LABEL = "label";
    public static final String NODE_SLIDER = "slider";
    public static final String NODE_SPINNER = "spinner";
    public static final String NODE_PATH = "path";
    public static final String NODE_BROWSE = "browse";

    public static final String EVENT_VALUE_CHANGED = "valueChanged";
    public static final String EVENT_BROWSE_REQUESTED = "browseRequested";
    public static final String EVENT_SYNC_VALUE = "syncValue";

    public static final String LUA_VALUE_CHANGED_EVENT = "launcher.settings.control.changed";
    public static final String LUA_DIRECTORY_REQUESTED_EVENT = "launcher.settings.directory.requested";
    public static final String SETTING_KEY_PROPERTY = "launcher.setting.key";
    public static final String RAM_RANGE_PROPERTY = "launcher.slider.ram";
    public static final String TICK_LABEL_STYLE = "tickLabel";

    private static final String VOLUME_TEMPLATE =
            "assets/components/settings/volume-control.xml";
    private static final String RAM_TEMPLATE =
            "assets/components/settings/ram-control.xml";
    private static final String DIRECTORY_TEMPLATE =
            "assets/components/settings/directory-control.xml";

    private LauncherComponentLibrary() {
    }

    /** Registers every launcher-owned basic and composite component type exactly once. */
    public static void register(GuiBuilder guiBuilder) {
        Objects.requireNonNull(guiBuilder, "guiBuilder");
        ComponentCatalog catalog = guiBuilder.getComponentCatalog();
        ComponentConstructor constructor = guiBuilder.getComponentConstructor();

        registerBasicTypes(constructor, catalog);
        registerSettingControl(
                constructor,
                catalog,
                VOLUME_CONTROL,
                "launcher-volume-control",
                VOLUME_TEMPLATE
        );
        registerSettingControl(
                constructor,
                catalog,
                RAM_CONTROL,
                "launcher-ram-control",
                RAM_TEMPLATE
        );
        registerDirectoryControl(constructor, catalog);

        Engine.LOGGER.info(
                "Launcher component library ready: basic={}, composite={}",
                catalog.types(org.takesome.kaylasEngine.gui.components.ComponentKind.BASIC).size(),
                catalog.types(org.takesome.kaylasEngine.gui.components.ComponentKind.COMPOSITE).size()
        );
    }

    private static void registerBasicTypes(ComponentConstructor constructor,
                                           ComponentCatalog catalog) {
        if (!catalog.contains(SLIDER_TRACK)) {
            ComponentDefinition<Slider> sliderDefinition = constructor
                    .<Slider>basic(SLIDER_TRACK)
                    .applyBaseStyle(false)
                    .creator(LauncherComponentLibrary::createSlider)
                    .build();
            constructor.register(sliderDefinition);
        }

        if (!catalog.contains(SETTING_SPINNER)) {
            ComponentDefinition<Spinner> spinnerDefinition = constructor
                    .<Spinner>basic(SETTING_SPINNER)
                    .applyBaseStyle(false)
                    .creator(LauncherComponentLibrary::createSpinner)
                    .build();
            constructor.register(spinnerDefinition);
        }

        if (!catalog.contains(DIRECTORY_BUTTON)) {
            ComponentDefinition<Button> buttonDefinition = constructor
                    .<Button>basic(DIRECTORY_BUTTON)
                    .applyBaseStyle(false)
                    .creator(LauncherComponentLibrary::createDirectoryButton)
                    .build();
            constructor.register(buttonDefinition);
        }
    }

    private static void registerSettingControl(ComponentConstructor constructor,
                                               ComponentCatalog catalog,
                                               String type,
                                               String alias,
                                               String templatePath) {
        if (catalog.contains(type)) {
            return;
        }

        Map<String, ComponentAttributes> nodes = loadNodes(templatePath);
        requireNodes(type, nodes, NODE_LABEL, NODE_SLIDER, NODE_SPINNER);

        CompositeComponentDefinition definition = constructor
                .composite(type)
                .alias(alias)
                .layout(CompositeComponent.LayoutMode.ABSOLUTE)
                .child(NODE_LABEL, nodes.get(NODE_LABEL))
                .child(NODE_SLIDER, nodes.get(NODE_SLIDER))
                .child(NODE_SPINNER, nodes.get(NODE_SPINNER))
                .connect(NODE_SLIDER, "change", NODE_SPINNER, EVENT_SYNC_VALUE)
                .connect(NODE_SPINNER, "change", NODE_SLIDER, EVENT_SYNC_VALUE)
                .connect(NODE_SLIDER, "change", ComponentConnection.ROOT, EVENT_VALUE_CHANGED)
                .connect(NODE_SPINNER, "change", ComponentConnection.ROOT, EVENT_VALUE_CHANGED)
                .build();
        constructor.register(definition);
    }

    private static void registerDirectoryControl(ComponentConstructor constructor,
                                                 ComponentCatalog catalog) {
        if (catalog.contains(DIRECTORY_CONTROL)) {
            return;
        }

        Map<String, ComponentAttributes> nodes = loadNodes(DIRECTORY_TEMPLATE);
        requireNodes(DIRECTORY_CONTROL, nodes, NODE_PATH, NODE_BROWSE);

        CompositeComponentDefinition definition = constructor
                .composite(DIRECTORY_CONTROL)
                .alias("launcher-directory-control")
                .layout(CompositeComponent.LayoutMode.ABSOLUTE)
                .child(NODE_PATH, nodes.get(NODE_PATH))
                .child(NODE_BROWSE, nodes.get(NODE_BROWSE))
                .connect(NODE_PATH, "textChanged", ComponentConnection.ROOT, EVENT_VALUE_CHANGED)
                .connect(NODE_BROWSE, "action", ComponentConnection.ROOT, EVENT_BROWSE_REQUESTED)
                .build();
        constructor.register(definition);
    }

    private static Slider createSlider(ComponentCreationContext context) {
        ComponentAttributes attributes = context.attributes();
        SliderRange range = resolveRange(attributes);
        Slider slider = new Slider(context.factory());

        slider.setMinimum(range.minimum());
        slider.setMaximum(range.maximum());
        slider.setValue(range.initial());
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(resolveMajorSpacing(attributes, range));
        slider.setMinorTickSpacing(resolveMinorSpacing(attributes, range));
        slider.setOpaque(false);

        StyleAttributes sliderStyle = context.engine().getStyleProvider().resolveStyle(
                "slider",
                attributes.getStyleChain(),
                attributes.getStyleOverrides()
        );
        slider.setUI(new TexturedSliderUI(context.factory(), slider, sliderStyle));
        StyleAttributes tickLabelStyle = resolveTickLabelStyle(
                context,
                attributes,
                sliderStyle
        );
        slider.setLabelTable(createSliderLabels(context, range, tickLabelStyle));
        return slider;
    }

    private static Spinner createSpinner(ComponentCreationContext context) {
        ComponentAttributes attributes = context.attributes();
        SliderRange range = resolveRange(attributes);
        int step = attributes.getStepSize() > 0
                ? attributes.getStepSize()
                : Math.max(1, resolveMinorSpacing(attributes, range));
        Spinner spinner = new Spinner(
                range.initial(),
                range.minimum(),
                range.maximum(),
                step
        );
        spinner.setOpaque(false);
        return spinner;
    }

    private static Button createDirectoryButton(ComponentCreationContext context) {
        ComponentAttributes attributes = context.attributes();
        StyleAttributes buttonStyle = context.engine().getStyleProvider().resolveStyle(
                "button",
                attributes.getStyleChain(),
                attributes.getStyleOverrides()
        );

        return context.factory().withStyle(buttonStyle, () -> {
            Button button = new Button(
                    context.factory(),
                    context.factory().getIconUtils().getIcon(attributes),
                    ""
            );
            new ButtonStyle(context.factory()).apply(button);
            button.setOpaque(false);
            return button;
        });
    }

    private static Hashtable<Integer, JLabel> createSliderLabels(ComponentCreationContext context,
                                                                 SliderRange range,
                                                                 StyleAttributes tickLabelStyle) {
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        Color foreground = hexToColor(tickLabelStyle.getColor());
        int fontSize = Math.max(8, tickLabelStyle.getFontSize());

        for (int value : range.values()) {
            JLabel label = new JLabel(String.valueOf(value));
            label.setOpaque(false);
            label.setForeground(foreground);
            label.setFont(context.engine().getFONTUTILS().getFont(
                    tickLabelStyle.getFont(),
                    fontSize,
                    tickLabelStyle.getFontStyle()
            ));
            Dimension preferred = label.getPreferredSize();
            Dimension compact = new Dimension(
                    Math.max(1, preferred.width + 2),
                    Math.max(1, preferred.height)
            );
            label.setPreferredSize(compact);
            label.setMinimumSize(compact);
            label.setMaximumSize(compact);
            label.setSize(compact);
            labels.put(value, label);
        }
        return labels;
    }

    static StyleAttributes resolveTickLabelStyle(ComponentCreationContext context,
                                                   ComponentAttributes attributes,
                                                   StyleAttributes fallback) {
        String styleName = tickLabelStyleName(attributes);
        if (styleName == null) {
            return fallback;
        }
        if (!context.engine().getStyleProvider().hasStyle("label", styleName)) {
            Engine.LOGGER.warn(
                    "Slider tick label style '{}' was not found; using slider style fallback.",
                    styleName
            );
            return fallback;
        }
        return context.engine().getStyleProvider().getStyle("label", styleName);
    }

    static String tickLabelStyleName(ComponentAttributes attributes) {
        if (attributes == null) {
            return null;
        }
        return firstNonBlank(
                attributes.getStyles().get(TICK_LABEL_STYLE),
                attributes.getStyles().get("ticks"),
                attributes.getStyles().get("columns")
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static SliderRange resolveRange(ComponentAttributes attributes) {
        if (Boolean.TRUE.equals(attributes.getProperties().get(RAM_RANGE_PROPERTY))) {
            RamRangeCalculator.SliderRange ramRange = new RamRangeCalculator()
                    .calculateSliderRange(Math.max(2, attributes.getStepSize()));
            int initial = clamp(
                    intValue(attributes.getInitialValue(), ramRange.initialValue()),
                    ramRange.minValue(),
                    ramRange.maxValue()
            );
            return new SliderRange(
                    ramRange.minValue(),
                    ramRange.maxValue(),
                    initial,
                    List.copyOf(ramRange.values())
            );
        }

        int minimum = attributes.getMinValue();
        int maximum = attributes.getMaxValue() > minimum
                ? attributes.getMaxValue()
                : minimum + 100;
        int initial = clamp(
                intValue(attributes.getInitialValue(), minimum),
                minimum,
                maximum
        );
        int spacing = attributes.getMajorSpacing() > 0
                ? attributes.getMajorSpacing()
                : Math.max(1, (maximum - minimum) / 5);
        return new SliderRange(
                minimum,
                maximum,
                initial,
                values(minimum, maximum, spacing)
        );
    }

    private static int resolveMajorSpacing(ComponentAttributes attributes, SliderRange range) {
        if (attributes.getMajorSpacing() > 0) {
            return attributes.getMajorSpacing();
        }
        return Math.max(1, (range.maximum() - range.minimum()) / 5);
    }

    private static int resolveMinorSpacing(ComponentAttributes attributes, SliderRange range) {
        if (attributes.getMinorSpacing() > 0) {
            return attributes.getMinorSpacing();
        }
        return Math.max(1, (range.maximum() - range.minimum()) / 10);
    }

    private static List<Integer> values(int minimum, int maximum, int spacing) {
        List<Integer> values = new ArrayList<>();
        int safeSpacing = Math.max(1, spacing);
        for (int value = minimum; value <= maximum; value += safeSpacing) {
            values.add(value);
        }
        if (values.isEmpty() || values.get(values.size() - 1) != maximum) {
            values.add(maximum);
        }
        return List.copyOf(values);
    }

    private static Map<String, ComponentAttributes> loadNodes(String resourcePath) {
        Attributes attributes = new XmlFrameAttributesLoader().getAttributes(resourcePath);
        Map<String, ComponentAttributes> result = new LinkedHashMap<>();
        for (ComponentAttributes component : attributes.getChildComponents()) {
            String id = component.getComponentId();
            if (id == null || id.isBlank()) {
                throw new IllegalStateException(
                        "Launcher component template contains a child without id: " + resourcePath
                );
            }
            if (result.putIfAbsent(id, component) != null) {
                throw new IllegalStateException(
                        "Duplicate launcher component node '" + id + "' in " + resourcePath
                );
            }
        }
        return Map.copyOf(result);
    }

    private static void requireNodes(String componentType,
                                     Map<String, ComponentAttributes> nodes,
                                     String... requiredNodes) {
        for (String requiredNode : requiredNodes) {
            if (!nodes.containsKey(requiredNode)) {
                throw new IllegalStateException(
                        "Composite '" + componentType + "' is missing node '" + requiredNode + "'"
                );
            }
        }
    }

    private static int intValue(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return (int) Math.round(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException error) {
            Engine.LOGGER.warn(
                    "Invalid launcher component numeric value '{}'; using {}.",
                    value,
                    fallback
            );
            return fallback;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record SliderRange(
            int minimum,
            int maximum,
            int initial,
            List<Integer> values
    ) {
    }
}
