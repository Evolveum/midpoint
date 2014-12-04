/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.model.api.context.ModelContext;

/**
 * An interface that model uses to report operation progress to any interested party (e.g. GUI or WS client).
 * Useful for long-running operations, like provisioning a focus object with many projections.
 *
 * EXPERIMENTAL.
 *
 * @author mederly
 */
public interface ProgressListener {

    /**
     * Reports a progress achieved. The idea is to provide as much information as practically possible,
     * so the client could take whatever it wants.
     *
     * Obviously, the method should not take too much time in order not to slow down the main execution routine.
     *
     * @param modelContext Current context of the model operation.
     * @param progressInformation Specific progress information.
     */
    void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation);

    boolean isAbortRequested();
}
