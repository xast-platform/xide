package org.xast.xide.core.config;

import static org.xast.xide.core.config.ConfigValue.*;

import org.xast.xide.core.config.ConfigValue.BooleanValue;
import org.xast.xide.core.config.ConfigValue.IntValue;
import org.xast.xide.core.config.ConfigValue.StringArrayValue;
import org.xast.xide.core.config.ConfigValue.StringValue;

public sealed interface ConfigValue
    permits
        StringValue,
        BooleanValue,
        IntValue,
        FloatValue,
        StringArrayValue
{
    public static final record StringValue(String value) implements ConfigValue {}
    public static final record BooleanValue(boolean value) implements ConfigValue {}
    public static final record IntValue(int value) implements ConfigValue {}
    public static final record FloatValue(float value) implements ConfigValue {}
    public static final record StringArrayValue(String[] value) implements ConfigValue {}
}
