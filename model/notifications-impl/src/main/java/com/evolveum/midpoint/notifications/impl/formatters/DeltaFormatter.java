/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import com.evolveum.midpoint.model.api.visualizer.Visualization;

/**
 * Formats object deltas and item deltas for notification purposes.
 */
public interface DeltaFormatter {
    String formatVisualization(Visualization visualization);
}
