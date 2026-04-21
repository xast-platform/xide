package org.xast.xide.core.config;

import lombok.Getter;

public class ConfigField {
    @Getter
    private final String description;
    @Getter
    private ConfigValue value;

    public ConfigField(String description, ConfigValue value) {
        this.description = description;
        this.value = value;
    }

    public <T extends ConfigValue> T get() {
        return null;
    }

    public void setValue(ConfigValue value) throws SetConfigFieldException {
        if (this.value.getClass() != value.getClass()) {
            throw new SetConfigFieldException(this.value.getClass(), value.getClass());
        }

        this.value = value;
    }
}
