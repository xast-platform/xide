package org.xast.xide.core.config;

import org.xast.xide.core.event.ConfigChangedEvent;
import org.xast.xide.core.event.EventBus;

import lombok.Getter;

public class ConfigField {
    @Getter
    private final String description;
    @Getter
    private ConfigValue value;
    private EventBus eventBus;

    public ConfigField(EventBus eventBus, String description, ConfigValue value) {
        this.description = description;
        this.value = value;
        this.eventBus = eventBus;
    }

    public void setValue(ConfigValue value) throws SetConfigFieldException {
        if (this.value.getClass() != value.getClass()) {
            throw new SetConfigFieldException(this.value.getClass(), value.getClass());
        }

        this.value = value;
        this.eventBus.publish(new ConfigChangedEvent());
    }
}
