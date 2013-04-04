package com.evolveum.midpoint.wf.taskHandlers;

/**
 * @author mederly
 */

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.processors.primary.PrimaryChangeProcessor;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * This handler prepares model operation to be executed within the context of child task:
 * - prepares model operation context - currently, adds OID if necessary (if delta0 was 'add object' delta)
 *
 * @author mederly
 */

@Component
public class WfPrepareChildOperationTaskHandler implements TaskHandler {

    public static final String HANDLER_URI = "http://midpoint.evolveum.com/wf-child-task-uri";

    private static final Trace LOGGER = TraceManager.getTrace(WfPrepareChildOperationTaskHandler.class);

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfTaskUtil wfTaskUtil;

    @Autowired
    private WfConfiguration wfConfiguration;

    @PostConstruct
    public void init() {
        LOGGER.trace("Registering with taskManager as a handler for " + HANDLER_URI);
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(Task task) {

        TaskRunResult.TaskRunResultStatus status = TaskRunResult.TaskRunResultStatus.FINISHED;

        if (wfConfiguration.isEnabled()) {

            try {

                OperationResult result = task.getResult();

                ModelContext modelContext = wfTaskUtil.retrieveModelContext(task, result);
                if (modelContext == null) {
                    throw new IllegalStateException("There's no model context in child task; task = " + task);
                }

                List<Task> prerequisities = task.listPrerequisiteTasks(result);
                if (!prerequisities.isEmpty()) {

                    if (prerequisities.size() > 1) {
                        throw new IllegalStateException("Child task should have at least one prerequisite (task0); this one has " + prerequisities.size() + "; task = " + task);
                    }

                    Task task0 = prerequisities.get(0);
                    Validate.isTrue(task0.isClosed(), "Task0 should be already closed; it is " + task0.getExecutionStatus());

                    LensContext context0 = (LensContext) wfTaskUtil.retrieveModelContext(task0, result);
                    if (context0 == null) {
                        throw new IllegalStateException("There's no model context in task0; task0 = " + task);
                    }

                    String oid = context0.getFocusContext().getOid();
                    if (oid == null) {
                        throw new IllegalStateException("There's no focus OID in model context in task0; task0 = " + task + "; context = " + context0.debugDump());
                    }

                    if (modelContext.getFocusContext().getPrimaryDelta() == null) {

                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("There's no primary delta in focus context; task = " + task + ", model context = " + modelContext.debugDump());
                            LOGGER.trace("We'll set skip model context processing property.");
                        }

                        wfTaskUtil.setSkipModelContextProcessingProperty(task, true, result);
                        task.savePendingModifications(result);

                    } else {

                        String currentOid = modelContext.getFocusContext().getPrimaryDelta().getOid();
                        LOGGER.trace("Object OID = " + oid + ", current OID = " + currentOid);

                        if (PrimaryChangeProcessor.UNKNOWN_OID.equals(currentOid)) {
                            modelContext.getFocusContext().getPrimaryDelta().setOid(oid);
                            wfTaskUtil.storeModelContext(task, modelContext);
                            task.savePendingModifications(result);
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Replaced delta OID with " + oid + " in task " + task);
                                LOGGER.trace("Resulting model context:\n{}", modelContext.debugDump(0));
                            }
                        } else {
                            if (!oid.equals(currentOid)) {
                                throw new IllegalStateException("Object OID in partial child task (" + currentOid + ") differs from OID in task0 (" + oid + ")");
                            }
                            LOGGER.trace("Delta OID is current, we will not change it.");
                        }
                    }
                }

            } catch (SchemaException e) {
                LoggingUtils.logException(LOGGER, "Couldn't update object OID due to schema exception", e);
                status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
            } catch (ObjectNotFoundException e) {
                LoggingUtils.logException(LOGGER, "Couldn't update object OID", e);
                status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
            } catch (ObjectAlreadyExistsException e) {
                LoggingUtils.logException(LOGGER, "Couldn't update object OID", e);
                status = TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
            }

        } else {
            LOGGER.info("Workflows are disabled, skipping " + WfPrepareChildOperationTaskHandler.class + " run.");
        }

        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(status);
        return runResult;
    }

    @Override
    public Long heartbeat(Task task) {
        return null;		// null - as *not* to record progress (which would overwrite operationResult!)
    }

    @Override
    public void refreshStatus(Task task) {
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.WORKFLOW;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
