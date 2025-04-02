/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.io.Serializable;

import com.evolveum.midpoint.model.api.visualizer.localization.WrapableLocalization;
import com.evolveum.midpoint.util.LocalizableMessage;

/**
 * Name of a visualization or a visualization item.
 */
public interface Name extends Serializable {

    LocalizableMessage getOverview();

    WrapableLocalization<String, LocalizationCustomizationContext> getCustomizableOverview();

    LocalizableMessage getDisplayName();

    LocalizableMessage getSimpleName();

    LocalizableMessage getDescription();

    String getId();
}
