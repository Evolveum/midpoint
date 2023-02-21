/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import java.io.Serializable;

/**
 * Name of a visualization or a visualization item.
 *
 * TODO reconsider this structure
 */
public interface Name extends Serializable {

    String getSimpleName();
    String getDisplayName();
    String getId();
    String getDescription();
    boolean namesAreResourceKeys();

    String getSimpleIcon();

    LocalizableMessage getSimpleDescription();
}
