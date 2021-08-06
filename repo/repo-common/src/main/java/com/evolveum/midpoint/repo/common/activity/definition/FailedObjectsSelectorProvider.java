/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;

/**
 * Provides specification (selector) of failed objects that are to be re-processed by an activity.
 */
public interface FailedObjectsSelectorProvider {

    FailedObjectsSelectorType getFailedObjectsSelector();
}
