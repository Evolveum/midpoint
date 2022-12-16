/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.progress;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.*;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Viliam Repan (lazyman)
 */
public class ProgressReporter implements ProgressListener {

    private static final Trace LOGGER = TraceManager.getTrace(ProgressReporter.class);

    private final MidPointApplication application;

    private final Map<String, String> nameCache = new HashMap<>();
    private final ProgressDto progress = new ProgressDto();

    private volatile boolean abortRequested;

    // Operation result got from the asynchronous operation (null if async op not yet finished)
    private OperationResult asyncOperationResult;
    private ModelContext<? extends ObjectType> previewResult;
    private Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperation;

    private long operationStartTime;            // if 0, operation hasn't start yet
    private long operationDurationTime;         // if >0, operation has finished

    // configuration properties
    private int refreshInterval;
    private boolean asynchronousExecution;
    private boolean abortEnabled;
    private boolean writeOpResultForProgressActivity;

    public ProgressReporter(MidPointApplication application) {
        this.application = application;
    }

    public OperationResult getAsyncOperationResult() {
        return asyncOperationResult;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public boolean isAsynchronousExecution() {
        return asynchronousExecution;
    }

    public boolean isAbortEnabled() {
        return abortEnabled;
    }

    private boolean isWriteOpResultForProgressActivity(){
        return writeOpResultForProgressActivity;
    }

    public void setWriteOpResultForProgressActivity(boolean writeOpResultForProgressActivity) {
        this.writeOpResultForProgressActivity = writeOpResultForProgressActivity;
    }

    public ModelContext<? extends ObjectType> getPreviewResult() {
        return previewResult;
    }

    public void setAsyncOperationResult(OperationResult asyncOperationResult) {
        this.asyncOperationResult = asyncOperationResult;
    }

    public Collection<ObjectDeltaOperation<? extends ObjectType>> getObjectDeltaOperation() {
        return objectDeltaOperation;
    }

    public void setObjectDeltaOperation(Collection<ObjectDeltaOperation<? extends ObjectType>> objectDeltaOperation) {
        this.objectDeltaOperation = objectDeltaOperation;
    }

    public void setPreviewResult(ModelContext<? extends ObjectType> previewResult) {
        this.previewResult = previewResult;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public void setAsynchronousExecution(boolean asynchronousExecution) {
        this.asynchronousExecution = asynchronousExecution;
    }

    public void setAbortEnabled(boolean abortEnabled) {
        this.abortEnabled = abortEnabled;
    }

    public void recordExecutionStart() {
        operationDurationTime = 0;
        operationStartTime = System.currentTimeMillis();
    }

    public void recordExecutionStop() {
        operationDurationTime = System.currentTimeMillis() - operationStartTime;
    }

    public ProgressDto getProgress() {
        return progress;
    }

    public long getOperationStartTime() {
        return operationStartTime;
    }

    public long getOperationDurationTime() {
        return operationDurationTime;
    }

    @Override
    public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("onProgressAchieved: {}\n, modelContext = \n{}", progressInformation.debugDump(),
                    modelContext.debugDump(2));
        }

        if (StringUtils.isNotEmpty(progressInformation.getMessage())) {
            progress.log(progressInformation.getMessage());
        }

        ProgressInformation.ActivityType activity = progressInformation.getActivityType();
        if (activity != CLOCKWORK && activity != WAITING) {
            List<ProgressReportActivityDto> progressReportActivities = progress.getProgressReportActivities();
            ProgressReportActivityDto si = findRelevantStatusItem(progressReportActivities, progressInformation);
            if (si == null) {
                progress.add(createStatusItem(progressInformation, modelContext));
            } else {
                updateStatusItemState(si, progressInformation, modelContext);
            }
            addExpectedStatusItems(progressReportActivities, modelContext);
        } else {
            // these two should not be visible in the list of activities
        }
    }

    @Override
    public boolean isAbortRequested() {
        return abortRequested;
    }

    public void setAbortRequested(boolean value) {
        this.abortRequested = value;
    }

    private void addExpectedStatusItems(List<ProgressReportActivityDto> progressReportActivities, ModelContext<?> modelContext) {
        if (modelContext.getFocusContext() != null) {
            ModelElementContext<?> fc = modelContext.getFocusContext();
            if (isNotEmpty(fc.getPrimaryDelta()) || isNotEmpty(fc.getSecondaryDelta())) {
                ProgressInformation modelStatus = new ProgressInformation(FOCUS_OPERATION, (ProgressInformation.StateType) null);
                if (findRelevantStatusItem(progressReportActivities, modelStatus) == null) {
                    progressReportActivities.add(createStatusItem(modelStatus, modelContext));
                }
            }
        }
        for (ModelProjectionContext mpc : modelContext.getProjectionContexts()) {
            ProgressInformation projectionStatus = new ProgressInformation(RESOURCE_OBJECT_OPERATION,
                    mpc.getKey(), (ProgressInformation.StateType) null);
            if (findRelevantStatusItem(progressReportActivities, projectionStatus) == null) {
                progressReportActivities.add(createStatusItem(projectionStatus, modelContext));
            }
        }
    }

    private boolean isNotEmpty(ObjectDelta delta) {
        return delta != null && !delta.isEmpty();
    }

    private ProgressReportActivityDto findRelevantStatusItem(List<ProgressReportActivityDto> progressReportActivities,
            ProgressInformation progressInformation) {

        for (ProgressReportActivityDto si : progressReportActivities) {
            if (si.correspondsTo(progressInformation)) {
                return si;
            }
        }
        return null;
    }

    private void updateStatusItemState(ProgressReportActivityDto si, ProgressInformation progressInformation,
            ModelContext modelContext) {

        si.setActivityType(progressInformation.getActivityType());
        si.setProjectionContextKey(progressInformation.getProjectionContextKey());
        if (progressInformation.getProjectionContextKey() != null) {
            String resourceOid = progressInformation.getProjectionContextKey().getResourceOid();
            String resourceName = resourceOid != null ? getResourceName(resourceOid) : "";
            si.setResourceName(resourceName);
        }
        if (progressInformation.getStateType() == null) {
            si.setStatus(null);
        } else if (progressInformation.getStateType() == ENTERING) {
            si.setStatus(OperationResultStatusType.IN_PROGRESS);
        } else {
            OperationResult result = progressInformation.getOperationResult();
            if (result != null) {
                OperationResultStatus status = result.getStatus();
                if (status == OperationResultStatus.UNKNOWN) {
                    status = result.getComputeStatus();
                }
                if (isWriteOpResultForProgressActivity()) {
                    si.setOperationResult(result);
                }
                si.setStatus(status.createStatusType());
            } else {
                si.setStatus(OperationResultStatusType.UNKNOWN);
            }
        }

        // information about modifications on a resource
        if (progressInformation.getActivityType() == RESOURCE_OBJECT_OPERATION &&
                progressInformation.getStateType() == EXITING &&
                progressInformation.getProjectionContextKey() != null &&
                progressInformation.getProjectionContextKey().getResourceOid() != null) {
            ModelProjectionContext mpc =
                    modelContext.findProjectionContextByKeyExact(progressInformation.getProjectionContextKey());
            if (mpc != null) { // it shouldn't be null!

                // operations performed (TODO aggregate them somehow?)
                List<ProgressReportActivityDto.ResourceOperationResult> resourceOperationResultList = new ArrayList<>();
                List<? extends ObjectDeltaOperation> executedDeltas = mpc.getExecutedDeltas();
                for (ObjectDeltaOperation executedDelta : executedDeltas) {
                    ObjectDelta delta = executedDelta.getObjectDelta();
                    if (delta != null) {
                        OperationResult r = executedDelta.getExecutionResult();
                        OperationResultStatus status = r.getStatus();
                        if (status == OperationResultStatus.UNKNOWN) {
                            status = r.getComputeStatus();
                        }
                        resourceOperationResultList.add(
                                new ProgressReportActivityDto.ResourceOperationResult(delta.getChangeType(), status));
                    }
                }
                si.setResourceOperationResultList(resourceOperationResultList);

                // object name
                PrismObject<ShadowType> object = mpc.getObjectNew();
                if (object == null) {
                    object = mpc.getObjectOld();
                }
                String name = null;
                if (object != null) {
                    if (object.asObjectable().getName() != null) {
                        name = PolyString.getOrig(object.asObjectable().getName());
                    } else {
                        // determine from attributes
                        ResourceAttribute nameAttribute = ShadowUtil.getNamingAttribute(object);
                        if (nameAttribute != null) {
                            name = String.valueOf(nameAttribute.getAnyRealValue());
                        }
                    }
                }
                if (name != null) {
                    si.setResourceObjectName(name);
                }
            }
        }
    }

    private ProgressReportActivityDto createStatusItem(ProgressInformation progressInformation, ModelContext modelContext) {
        ProgressReportActivityDto si = new ProgressReportActivityDto();
        updateStatusItemState(si, progressInformation, modelContext);
        return si;
    }

    private String getResourceName(@NotNull String oid) {
        String name = nameCache.get(oid);
        if (name != null) {
            return name;
        }
        Task task = application.createSimpleTask("getResourceName");
        OperationResult result = new OperationResult("getResourceName");
        Collection<SelectorOptions<GetOperationOptions>> options = GetOperationOptions.createNoFetchCollection();
        try {
            PrismObject<ResourceType> object = application.getModel().getObject(ResourceType.class, oid, options, task, result);
            name = PolyString.getOrig(object.asObjectable().getName());
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine the name of resource {}", e, oid);
            name = "(" + oid + ")";
        }
        nameCache.put(oid, name);
        return name;
    }
}
