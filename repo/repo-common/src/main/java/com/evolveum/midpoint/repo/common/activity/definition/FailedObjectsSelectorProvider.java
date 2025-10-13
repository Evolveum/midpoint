/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FailedObjectsSelectorType;

/**
 * Provides specification (selector) of failed objects that are to be re-processed by an activity.
 */
public interface FailedObjectsSelectorProvider {

    FailedObjectsSelectorType getFailedObjectsSelector();
}
