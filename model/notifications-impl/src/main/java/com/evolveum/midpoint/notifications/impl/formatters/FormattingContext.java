/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;

/**
 * Context used when formatting notification content.
 */
public record FormattingContext(@NotNull Locale locale) {
}
