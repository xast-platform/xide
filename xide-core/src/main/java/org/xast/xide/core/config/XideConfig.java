package org.xast.xide.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.xast.xide.core.config.ConfigValue.BooleanValue;
import org.xast.xide.core.config.ConfigValue.FloatValue;
import org.xast.xide.core.config.ConfigValue.IntValue;
import org.xast.xide.core.config.ConfigValue.StringArrayValue;
import org.xast.xide.core.config.ConfigValue.StringValue;
import org.xast.xide.core.event.EventBus;
import org.xast.xide.core.utils.Debug;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import lombok.Data;
import lombok.Getter;
import lombok.val;

public class XideConfig {
    public static final Toml EMPTY_CONFIG = new Toml();
    public static final String CONFIG_NAME = "xide.toml";
    public static final String CONFIG_FOLDER = "Xide";

    @Getter
    private Map<String, Map<String, ConfigField>> fields;
    private EventBus eventBus;

    private XideConfig(
        EventBus eventBus, 
        Map<String, Map<String, ConfigField>> fields
    ) {
        this.eventBus = eventBus;
        this.fields = fields;
    }

    public static XideConfig load(EventBus eventBus) {
        // Path path = getConfigPath();
        // File configFile = path.toFile();
        // XideConfig currentConfig;

        // if (configFile.exists() && configFile.isFile()) {
        //     try {
        //         return new XideConfig(new Toml().read(configFile));
        //     } catch (IllegalStateException e) {
        //         Debug.error("Current config is not a valid TOML: " + e.getMessage());
        //         currentConfig = new XideConfig(new Toml());
        //     }
        // } else {
        //     Debug.warn("Current config doesn't exist. Creating empty one...");
        //     currentConfig = new XideConfig(new Toml());
        // }

        // try {
        //     Files.createDirectories(path.getParent());
        //     Files.createFile(path);
        // } catch (IOException e) {
        //     Debug.warn("Failed to create config file: " + e.getMessage());
        // }

        // try {
        //     TomlWriter writer = new TomlWriter();
        //     writer.write(currentConfig.table, configFile);
        // } catch (IOException e) {
        //     Debug.error("Failed to save config file: " + e.getMessage());
        // }

        // return currentConfig;
        return new XideConfig(eventBus, new HashMap<>());
    }

    public boolean getBoolean(String sectionName, String recordName, boolean defaultVal) {
        if (!fields.containsKey(sectionName)) {
            Debug.warn("No config section `"+sectionName+"`");
            return defaultVal;
        }

        var section = fields.get(sectionName);

        if (!section.containsKey(recordName)) {
            Debug.warn("No config record `"+recordName+"` in section `"+sectionName+"`");
            return defaultVal;
        }

        var record = section.get(recordName);

        if (record.getValue() instanceof BooleanValue bool) {
            return bool.value();
        } else {
            Debug.warn("Config record `"+recordName+"` in section `"+sectionName+"` is not a boolean");
            return defaultVal;
        }
    }

    public String getString(String sectionName, String recordName, String defaultVal) {
        if (!fields.containsKey(sectionName)) {
            Debug.warn("No config section `"+sectionName+"`");
            return defaultVal;
        }

        var section = fields.get(sectionName);

        if (!section.containsKey(recordName)) {
            Debug.warn("No config record `"+recordName+"` in section `"+sectionName+"`");
            return defaultVal;
        }

        var record = section.get(recordName);

        if (record.getValue() instanceof StringValue string) {
            return string.value();
        } else {
            Debug.warn("Config record `"+recordName+"` in section `"+sectionName+"` is not a string");
            return defaultVal;
        }
    }

    public int getInt(String sectionName, String recordName, int defaultVal) {
        if (!fields.containsKey(sectionName)) {
            Debug.warn("No config section `"+sectionName+"`");
            return defaultVal;
        }

        var section = fields.get(sectionName);

        if (!section.containsKey(recordName)) {
            Debug.warn("No config record `"+recordName+"` in section `"+sectionName+"`");
            return defaultVal;
        }

        var record = section.get(recordName);

        if (record.getValue() instanceof IntValue integer) {
            return integer.value();
        } else {
            Debug.warn("Config record `"+recordName+"` in section `"+sectionName+"` is not an int");
            return defaultVal;
        }
    }

    public float getFloat(String sectionName, String recordName, float defaultVal) {
        if (!fields.containsKey(sectionName)) {
            Debug.warn("No config section `"+sectionName+"`");
            return defaultVal;
        }

        var section = fields.get(sectionName);

        if (!section.containsKey(recordName)) {
            Debug.warn("No config record `"+recordName+"` in section `"+sectionName+"`");
            return defaultVal;
        }

        var record = section.get(recordName);

        if (record.getValue() instanceof FloatValue f) {
            return f.value();
        } else {
            Debug.warn("Config record `"+recordName+"` in section `"+sectionName+"` is not a float");
            return defaultVal;
        }
    }

    public String[] getStringArray(String sectionName, String recordName, String[] defaultVal) {
        if (!fields.containsKey(sectionName)) {
            Debug.warn("No config section `"+sectionName+"`");
            return defaultVal;
        }

        var section = fields.get(sectionName);

        if (!section.containsKey(recordName)) {
            Debug.warn("No config record `"+recordName+"` in section `"+sectionName+"`");
            return defaultVal;
        }

        var record = section.get(recordName);

        if (record.getValue() instanceof StringArrayValue strings) {
            return strings.value();
        } else {
            Debug.warn("Config record `"+recordName+"` in section `"+sectionName+"` is not a string array");
            return defaultVal;
        }
    }

    // public void save() {
    //     File configFile = getConfigPath().toFile();
    //     try {
    //         TomlWriter writer = new TomlWriter();
    //         writer.write(this.table, configFile);
    //     } catch (IOException e) {
    //         Debug.error("Failed to save config file: " + e.getMessage());
    //     }
    // }

    // private XideConfig(Toml table) {
    //     this.table = table;
    // }

    private static Path getConfigPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, CONFIG_FOLDER, CONFIG_NAME);
        }

        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", CONFIG_FOLDER, CONFIG_NAME);
        }

        // Linux / BSD / others → XDG
        String xdg = System.getenv("XDG_CONFIG_HOME");
        if (xdg != null && !xdg.isBlank()) {
            return Path.of(xdg, CONFIG_FOLDER, CONFIG_NAME);
        }

        return Path.of(home, ".config", CONFIG_FOLDER, CONFIG_NAME);
    }

    @Data
    private static class XideConfigDto {
        private Map<String, Map<String, ConfigFieldDto>> config; 

        public XideConfig toSolid(EventBus eventBus) {
            var fields = config
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    sectionEntry -> sectionEntry.getValue()
                        .entrySet()
                        .stream()
                        .map(fieldEntry ->
                            fieldEntry.getValue()
                                .toSolid()
                                .map(solidField -> Map.entry(fieldEntry.getKey(), solidField))
                        )
                        .flatMap(Optional::stream)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                ));

            return new XideConfig(eventBus, fields);
        }
    }

    @Data
    private static class ConfigFieldDto {
        private String description;
        private Object value;

        public Optional<ConfigField> toSolid() {
            if (this.value == null) {
                Debug.warn("Config field value is null");
                return Optional.empty();
            }

            Optional<ConfigValue> value = switch (this.value) {
                case String s -> Optional.of(new StringValue(s));
                case Boolean b -> Optional.of(new BooleanValue(b));
                case Long l -> Optional.of(new IntValue(l.intValue()));
                case Double d -> Optional.of(new FloatValue(d.floatValue()));
                case List<?> list -> {
                    if (list.stream().allMatch(e -> e instanceof String)) {
                        yield Optional.of(new StringArrayValue(list.toArray(new String[0])));
                    }
                    Debug.warn("Only string arrays are supported");
                    yield Optional.empty();
                }
                default -> {
                    Debug.warn("Unsupported type: " + this.value.getClass().getSimpleName());
                    yield Optional.empty();
                }
            };

            if (value.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ConfigField(
                this.description,
                value.get()
            ));
        }
    }
}
