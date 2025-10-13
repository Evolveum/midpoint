/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Marker interface for processors in Projector.
 *
 * Note that functionality of the processor (i.e. some "process" method) is not exposed via this interface
 * because many of the processors have multiple entry points. Moreover, to make code navigation in IDE easy
 * (between processors and their caller) it is better to have direct caller-callee relations.
 */
@Experimental
public interface ProjectorProcessor {

}
