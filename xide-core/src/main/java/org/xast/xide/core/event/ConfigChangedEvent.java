package org.xast.xide.core.event;

import org.xast.xide.core.config.XideConfig;

public record ConfigChangedEvent(XideConfig config) {}
