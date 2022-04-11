/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
