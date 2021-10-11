/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.task.api.Tracer;

/**
 *
 */
public interface TracerAware {

    @SuppressWarnings("unused")
    Tracer getTracer();

    void setTracer(Tracer tracer);
}
