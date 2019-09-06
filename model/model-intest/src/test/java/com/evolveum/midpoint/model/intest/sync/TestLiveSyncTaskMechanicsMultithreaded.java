/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.sync;

/**
 *
 */
public class TestLiveSyncTaskMechanicsMultithreaded extends TestLiveSyncTaskMechanics {

    @Override
    int getWorkerThreads() {
        return 3;
    }
}
