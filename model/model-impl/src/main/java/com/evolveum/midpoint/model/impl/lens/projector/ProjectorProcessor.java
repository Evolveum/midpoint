/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
