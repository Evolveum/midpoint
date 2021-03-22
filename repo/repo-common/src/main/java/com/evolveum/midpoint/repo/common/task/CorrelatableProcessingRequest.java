/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

public interface CorrelatableProcessingRequest {

    /**
     * @return The value against which we match other requests to be aligned with this one.
     */
    Object getCorrelationValue();

}
