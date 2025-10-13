/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run.processing;

public interface CorrelatableProcessingRequest {

    /**
     * @return The value against which we match other requests to be aligned with this one.
     */
    Object getCorrelationValue();

}
