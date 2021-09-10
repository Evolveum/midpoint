/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.tasks;

public class TestThresholdsSingleTaskMultipleThreads extends TestThresholdsSingleTask {

    @Override
    int getWorkerThreads() {
        return 4;
    }
}
