package com.evolveum.midpoint.model.impl.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class AuditReindexTaskHandler implements TaskHandler {

	static final Trace LOGGER = TraceManager.getTrace(AuditReindexTaskHandler.class);

	public static final String HANDLER_URI = ModelPublicConstants.AUDIT_REINDEX_TASK_HANDLER_URI;

	private static final String taskName = "AuditReindex";

	private int maxResults = 20;
	private int firstResult = 0;

	@Autowired
	protected AuditService auditService;

	@Autowired
	protected TaskManager taskManager;

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@Override
	public TaskRunResult run(Task coordinatorTask) {
		OperationResult opResult = new OperationResult(OperationConstants.AUDIT_REINDEX + ".run");
		opResult.setStatus(OperationResultStatus.IN_PROGRESS);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		final Long expectedTotal = auditService.countObjects("select count(*) from RAuditEventRecord as aer where 1=1", null);
		AuditResultHandler resultHandler = new AuditResultHandler() {

			private AtomicInteger processedObjects =  new AtomicInteger();

			@Override
			public boolean handle(AuditEventRecord auditRecord) {

				auditService.reindexEntry(auditRecord);
				processedObjects.incrementAndGet();

				return true;
			}

			@Override
			public int getProgress() {
				return processedObjects.get();
			}
		};
		if (resultHandler == null) {
			// the error should already be in the runResult
			return runResult;
		}


		try {
			LOGGER.trace("{}: expecting {} objects to be processed", taskName, expectedTotal);

			runResult.setProgress(0);
			coordinatorTask.setProgress(0);
			if (expectedTotal != null) {
				coordinatorTask.setExpectedTotal(expectedTotal);
			}
			try {
				coordinatorTask.savePendingModifications(opResult);
			} catch (ObjectAlreadyExistsException e) { // other exceptions are
														// handled in the outer
														// try block
				throw new IllegalStateException(
						"Unexpected ObjectAlreadyExistsException when updating task progress/expectedTotal",
						e);
			}
			Map<String, Object> params = new HashMap<String, Object>();
			while (true) {
				params.put("setFirstResult", firstResult);
				params.put("setMaxResults", maxResults);
				List<AuditEventRecord> records = auditService.listRecords(null, params);
				if (CollectionUtils.isNotEmpty(records)){
					for (AuditEventRecord record : records) {
						resultHandler.handle(record);
						runResult.setProgress(resultHandler.getProgress());
					}
					firstResult += maxResults;
					maxResults = ( (expectedTotal.intValue() - firstResult) > maxResults ? maxResults : (expectedTotal.intValue() - firstResult));
				} else {
					break;
				}
			}
			opResult.recordSuccess();

		} catch (ObjectNotFoundException e) {
			// This is bad. The resource does not exist. Permanent problem.
			logErrorAndSetResult(runResult, resultHandler, "Object not found", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		} catch (SchemaException e) {
			// Not sure about this. But most likely it is a misconfigured
			// resource or connector
			// It may be worth to retry. Error is fatal, but may not be
			// permanent.
			logErrorAndSetResult(runResult, resultHandler, "Error dealing with schema", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.TEMPORARY_ERROR);
			return runResult;
		} catch (RuntimeException e) {
			// Can be anything ... but we can't recover from that.
			// It is most likely a programming error. Does not make much sense
			// to retry.
			logErrorAndSetResult(runResult, resultHandler, "Internal error", e,
					OperationResultStatus.FATAL_ERROR, TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
		}

		// TODO: check last handler status

		runResult.setProgress(resultHandler.getProgress());
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);

		String finishMessage = "Finished " + taskName + " (" + coordinatorTask + "). ";
		String statistics = "Processed " + resultHandler.getProgress() + " objects";

		opResult.createSubresult(OperationConstants.AUDIT_REINDEX + ".statistics")
				.recordStatus(OperationResultStatus.SUCCESS, statistics);

		LOGGER.info(finishMessage + statistics);

		LOGGER.trace("{} run finished (task {}, run result {})", taskName, coordinatorTask, runResult);

		return runResult;

	}

	@Override
	public Long heartbeat(Task task) {
		return task.getProgress();
	}

	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCategoryName(Task task) {
		return TaskCategory.UTIL;
	}

	@Override
	public List<String> getCategoryNames() {
		// TODO Auto-generated method stub
		return null;
	}

	// TODO: copied from abstract search iterative handler
	private TaskRunResult logErrorAndSetResult(TaskRunResult runResult, AuditResultHandler resultHandler,
			String message, Throwable e, OperationResultStatus opStatus, TaskRunResultStatus status) {
		LOGGER.error("{}: {}: {}", taskName, message, e.getMessage(), e);
		runResult.getOperationResult().recordStatus(opStatus, message + ": " + e.getMessage(), e);
		runResult.setRunResultStatus(status);
		runResult.setProgress(resultHandler.getProgress());
		return runResult;

	}

}
