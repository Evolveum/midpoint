/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

/**
 *
 */
public class TestLiveSyncTaskMultithreaded extends TestLiveSyncTask {

    @Override
    int getWorkerThreads() {
        return 3;
    }
}
