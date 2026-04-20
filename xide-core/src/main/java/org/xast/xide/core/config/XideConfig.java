package org.xast.xide.core.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.xast.xide.core.utils.Debug;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import lombok.Getter;

public class XideConfig {
    public static final Toml EMPTY_CONFIG = new Toml();
    public static final String CONFIG_NAME = "xide.toml";
    public static final String CONFIG_FOLDER = "Xide";

    public static XideConfig load() {
        Path path = getConfigPath();
        File configFile = path.toFile();
        XideConfig currentConfig;

        if (configFile.exists() && configFile.isFile()) {
            try {
                return new XideConfig(new Toml().read(configFile));
            } catch (IllegalStateException e) {
                Debug.error("Current config is not a valid TOML: " + e.getMessage());
                currentConfig = new XideConfig(new Toml());
            }
        } else {
            Debug.warn("Current config doesn't exist. Creating empty one...");
            currentConfig = new XideConfig(new Toml());
        }

        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (IOException e) {
            Debug.warn("Failed to create config file: " + e.getMessage());
        }

        try {
            TomlWriter writer = new TomlWriter();
            writer.write(currentConfig.table, configFile);
        } catch (IOException e) {
            Debug.error("Failed to save config file: " + e.getMessage());
        }

        return currentConfig;
    }

    public void save() {
        File configFile = getConfigPath().toFile();
        try {
            TomlWriter writer = new TomlWriter();
            writer.write(this.table, configFile);
        } catch (IOException e) {
            Debug.error("Failed to save config file: " + e.getMessage());
        }
    }

    private XideConfig(Toml table) {
        this.table = table;
    }

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
}
