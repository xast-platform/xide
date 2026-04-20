package org.xast.xide.core.config;

public class SetConfigFieldException extends Exception {
    public SetConfigFieldException(
        Class<? extends ConfigValue> expected,
        Class<? extends ConfigValue> found
    ) {
        super("Expected config value of type " + expected.getSimpleName() + ", but found " + found.getSimpleName());
    }
}
