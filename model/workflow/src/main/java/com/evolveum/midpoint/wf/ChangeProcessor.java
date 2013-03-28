package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Manages workflow-related aspects of a change.
 *
 * (1) recognizes the instance (instances) of given kind of change within model context
 * (2) processes the result of the workflow(s) started
 *
 * Currently, there are the following change processors implemented or planned:
 * - PrimaryUserChangeProcessor: manages approvals of changes of user objects (in model's primary stage)
 * - ResourceModificationProcessor: manages approvals of changes related to individual resources (in model's secondary stage) - planned
 *
 * TODO find a better name (ChangeHandler, ChangeCategory, ...)
 *
 * @author mederly
 */
public interface ChangeProcessor {

    /**
     * Processes workflow-related aspect of a model operation. Namely, tries to find whether user interaction is necessary,
     * and arranges everything to carry out that interaction.
     *
     * @param context
     * @param task
     * @param result
     * @return non-null value if it processed the request;
     *              BACKGROUND = the process continues in background,
     *              FOREGROUND = nothing was left background, the model operation should continue in foreground,
     *              ERROR = something wrong has happened, there's no point in continuing with this operation.
     *         null if the request is not relevant to this processor
     */
    HookOperationMode startProcessesIfNeeded(ModelContext context, Task task, OperationResult result) throws SchemaException;

}
