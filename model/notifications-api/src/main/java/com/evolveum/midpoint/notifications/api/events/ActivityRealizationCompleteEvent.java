/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * A realization of an {@link Activity} is complete.
 *
 * Preliminary implementation!
 */
@Experimental
public interface ActivityRealizationCompleteEvent extends ActivityEvent {
}
