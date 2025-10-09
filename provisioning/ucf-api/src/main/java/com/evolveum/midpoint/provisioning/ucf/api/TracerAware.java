/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
