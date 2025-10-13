/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story;

public class TestThresholdsStoryReconExecuteMultithreaded extends TestThresholdsStoryReconExecute {

    @Override
    protected int getWorkerThreads() {
        return 2;
    }
}
