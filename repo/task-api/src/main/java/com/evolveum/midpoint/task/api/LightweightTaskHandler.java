/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * Handler for a lightweight task. These tasks execute in dedicated thread pool, being not managed by Quartz.
 *
 * See https://wiki.evolveum.com/display/midPoint/Lightweight+asynchronous+tasks.
 *
 * @author Pavol Mederly
 */
@FunctionalInterface
public interface LightweightTaskHandler {

    void run(RunningLightweightTask task);

}
