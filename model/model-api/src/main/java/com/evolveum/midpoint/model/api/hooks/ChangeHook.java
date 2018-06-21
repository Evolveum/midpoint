/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.api.hooks;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;

/**
 * TODO
 *
 * This applies to all changes, therefore it will "hook" into addObject, modifyObject
 * and also deleteObject.
 *
 * @author semancik
 *
 */
public interface ChangeHook {

    /**
     * Generic method to be implemented by the hook. It is invoked by the Model Clockwork at these occasions:
     *  - after PRIMARY state has been entered,
     *  - after SECONDARY state has been entered, and
     *  - after each of secondary-state waves has been executed (i.e. with the state of SECONDARY for all except
     *    the last one, will have state set to FINAL).
     *
     *  TODO: what about EXECUTION and POSTEXECUTION states?
     *
     *  @return
     *   - FOREGROUND, if the processing of model operation should continue on the foreground
     *   - BACKGROUND, if the hook switched further processing into background (and, therefore,
     *     current execution of model operation should end immediately, in the hope it will eventually
     *     be resumed later)
     *   - ERROR, if the hook encountered an error which prevents model operation from continuing
     *     (this case is currently not defined very well)
     */
    <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task task, @NotNull OperationResult result) throws PolicyViolationException;

    /**
     * This method is invoked by the clockwork when an exception occurs.
     *
     * It is intended e.g. to implement a notification to the user.
     *
     * @param context actual model context at the point of processing the exception
     * @param throwable the exception itself
     * @param task actual task, in context of which the operation was carried out
     * @param result actual operation result - the handler should create a subresult here for its operation
     *
     * This method has no return value, as it is not expected that the processing would continue in
     * the background. (This could change in the future.)
     */
    void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result);

    default int getPriority() {
        return Integer.MAX_VALUE;
    }

    /**
     * Invokes the hook in "preview mode". It is expected that hook will set its output into "hookPreviewResultsMap" in the model context.
     * Hooks can decide whether to do anything based on partial processing options. The defaults are expected to be different
     * from defaults during normal processing, e.g. they might be set to "skip". If the caller wants preview processing by
     * a particular hook, it might enable it using partial processing option set to "process".
     */
	default void invokePreview(@NotNull ModelContext<? extends ObjectType> context, Task task, OperationResult result) {
    }
}
