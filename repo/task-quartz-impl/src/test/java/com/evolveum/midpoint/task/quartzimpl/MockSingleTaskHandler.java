/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.handlers.NoOpTaskHandler;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScheduleType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Radovan Semancik
 *
 */
public class MockSingleTaskHandler implements TaskHandler {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(MockSingleTaskHandler.class);
    private String MOCK_HANDLER_URI = "http://midpoint.evolveum.com/test/mock";
    private QName L1_FLAG_QNAME = new QName(MOCK_HANDLER_URI, "l1Flag");
    private QName WFS_FLAG_QNAME = new QName(MOCK_HANDLER_URI, "wfsFlag");

    private TaskManagerQuartzImpl taskManager;

	private String id;
	
	MockSingleTaskHandler(String id, TaskManagerQuartzImpl taskManager) {
		this.id = id;
        this.taskManager = taskManager;
	}

	private boolean hasRun = false;
	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.info("MockSingle.run starting (id = " + id + ")");

        PrismPropertyDefinition l1FlagDefinition = new PrismPropertyDefinition(L1_FLAG_QNAME, L1_FLAG_QNAME, DOMUtil.XSD_BOOLEAN, taskManager.getPrismContext());
        PrismPropertyDefinition wfsFlagDefinition = new PrismPropertyDefinition(WFS_FLAG_QNAME, WFS_FLAG_QNAME, DOMUtil.XSD_BOOLEAN, taskManager.getPrismContext());

		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(MockSingleTaskHandler.class.getName()+".run");
		TaskRunResult runResult = new TaskRunResult();

		runResult.setOperationResult(opResult);
		
		// TODO
		progress++;
		
		opResult.recordSuccess();
		
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
		
		hasRun = true;

        if ("L1".equals(id)) {
            PrismProperty<Boolean> l1flag = (PrismProperty<Boolean>) task.getExtension(L1_FLAG_QNAME);

            if (l1flag == null || l1flag.getRealValue() == false) {

                LOGGER.info("L1 handler, first run - scheduling L2 handler");
                ScheduleType l2Schedule = new ScheduleType();
                l2Schedule.setInterval(2);
                task.pushHandlerUri(TestQuartzTaskManagerContract.L2_TASK_HANDLER_URI, l2Schedule, TaskBinding.TIGHT, task.createExtensionDelta(l1FlagDefinition, true));
                try {
                    task.savePendingModifications(opResult);
                } catch(Exception e) {
                    throw new SystemException("Cannot schedule L2 handler", e);
                }
                runResult.setRunResultStatus(TaskRunResultStatus.RESTART_REQUESTED);
            } else {
                LOGGER.info("L1 handler, not the first run (progress = " + progress + ", l1Flag = " + l1flag.getRealValue() + "), exiting.");
            }
        } else if ("L2".equals(id)) {
            if (progress == 5) {
                LOGGER.info("L2 handler, fourth run - scheduling L3 handler");
                task.pushHandlerUri(TestQuartzTaskManagerContract.L3_TASK_HANDLER_URI, new ScheduleType(), null);
                try {
                    task.savePendingModifications(opResult);
                } catch(Exception e) {
                    throw new SystemException("Cannot schedule L3 handler", e);
                }
                runResult.setRunResultStatus(TaskRunResultStatus.RESTART_REQUESTED);
            } else if (progress < 5) {
                LOGGER.info("L2 handler, progress = " + progress + ", continuing.");
            } else if (progress > 5) {
                LOGGER.info("L2 handler, progress too big, i.e. " + progress + ", exiting.");
                try {
                    ((TaskQuartzImpl) task).finishHandler(opResult);
                } catch (Exception e) {
                    throw new SystemException("Cannot finish L2 handler", e);
                }
            }
        } else if ("L3".equals(id)) {
            LOGGER.info("L3 handler, simply exiting. Progress = " + progress);
        } else if ("WFS".equals(id)) {

            PrismProperty<Boolean> wfsFlag = (PrismProperty<Boolean>) task.getExtension(WFS_FLAG_QNAME);

            if (wfsFlag == null || wfsFlag.getRealValue() == false) {

                LOGGER.info("Wait-for-subtasks creating subtasks...");

                Task t1 = task.createSubtask();
                t1.setHandlerUri(TestQuartzTaskManagerContract.L3_TASK_HANDLER_URI);
                taskManager.switchToBackground(t1, opResult);

                Task t2 = task.createSubtask();
                t2.setHandlerUri(TestQuartzTaskManagerContract.SINGLE_TASK_HANDLER_URI);
                taskManager.switchToBackground(t2, opResult);

                try {
                    ArrayList<ItemDelta<?>> deltas = new ArrayList<ItemDelta<?>>();
                    deltas.add(task.createExtensionDelta(wfsFlagDefinition, true));
                    runResult = task.waitForSubtasks(2, deltas, opResult);
                    runResult.setProgress(1);
                } catch (Exception e) {
                    throw new SystemException("WaitForSubtasks failed.", e);
                }
            } else {
                LOGGER.info("Wait-for-subtasks seems to finish successfully; progress = " + progress + ", wfsFlag = " + wfsFlag.getRealValue());
            }

        }
		
		LOGGER.info("MockSingle.run stopping");
		return runResult;
	}
	
	@Override
	public Long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return 0L;
	}
	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean hasRun() {
		return hasRun;
	}
	
	public void resetHasRun() {
		hasRun = false;
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.MOCK;
    }

    @Override
    public List<String> getCategoryNames() {
        return null;
    }

    public TaskManagerQuartzImpl getTaskManager() {
        return taskManager;
    }

    public void setTaskManager(TaskManagerQuartzImpl taskManager) {
        this.taskManager = taskManager;
    }
}
