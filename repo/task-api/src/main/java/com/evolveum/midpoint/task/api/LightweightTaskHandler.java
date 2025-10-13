/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

/**
 * Handler for a lightweight task. These tasks execute in dedicated thread pool, being not managed by Quartz.
 *
 * See https://docs.evolveum.com/midpoint/devel/design/lightweight-asynchronous-tasks/.
 */
@FunctionalInterface
public interface LightweightTaskHandler {

    void run(RunningLightweightTask task);

}
