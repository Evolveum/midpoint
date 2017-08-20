/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users;

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
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.progress.ProgressDto;
import com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.*;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.EXITING;
import static com.evolveum.midpoint.web.component.progress.ProgressReportActivityDto.ResourceOperationResult;

/**
* @author mederly
*/
public class DefaultGuiProgressListener implements ProgressListener, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultGuiProgressListener.class);

    private ProgressDto progressDto;
    private ProgressReportingAwarePage parentPage;

    private boolean abortRequested;

    public DefaultGuiProgressListener(ProgressReportingAwarePage parentPage, ProgressDto progressDto) {
        this.parentPage = parentPage;
        this.progressDto = progressDto;
    }

    @Override
    public void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("onProgressAchieved: {}\n, modelContext = \n{}", progressInformation.debugDump(), modelContext.debugDump(2));
        }

        if (progressDto == null) {
            LOGGER.error("No progressDto, exiting");      // should not occur
            return;
        }

        if (StringUtils.isNotEmpty(progressInformation.getMessage())) {
            progressDto.log(progressInformation.getMessage());
        }

        ProgressInformation.ActivityType activity = progressInformation.getActivityType();
        if (activity != CLOCKWORK && activity != WAITING) {
            List<ProgressReportActivityDto> progressReportActivities = progressDto.getProgressReportActivities();
            ProgressReportActivityDto si = findRelevantStatusItem(progressReportActivities, progressInformation);
            if (si == null) {
                progressDto.add(createStatusItem(progressInformation, modelContext));
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

    private void addExpectedStatusItems(List<ProgressReportActivityDto> progressReportActivities, ModelContext modelContext) {
        if (modelContext.getFocusContext() != null) {
            ModelElementContext fc = modelContext.getFocusContext();
            if (isNotEmpty(fc.getPrimaryDelta()) || isNotEmpty(fc.getSecondaryDelta())) {
                ProgressInformation modelStatus = new ProgressInformation(FOCUS_OPERATION, (StateType) null);
                if (findRelevantStatusItem(progressReportActivities, modelStatus) == null) {
                    progressReportActivities.add(createStatusItem(modelStatus, modelContext));
                }
            }
        }
        if (modelContext.getProjectionContexts() != null) {
            Collection<ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
            for (ModelProjectionContext mpc : projectionContexts) {
                ProgressInformation projectionStatus = new ProgressInformation(RESOURCE_OBJECT_OPERATION, mpc.getResourceShadowDiscriminator(), (StateType) null);
                if (findRelevantStatusItem(progressReportActivities, projectionStatus) == null) {
                    progressReportActivities.add(createStatusItem(projectionStatus, modelContext));
                }
            }
        }
    }

    private boolean isNotEmpty(ObjectDelta delta) {
        return delta != null && !delta.isEmpty();
    }

    private ProgressReportActivityDto findRelevantStatusItem(List<ProgressReportActivityDto> progressReportActivities, ProgressInformation progressInformation) {
        for (ProgressReportActivityDto si : progressReportActivities) {
            if (si.correspondsTo(progressInformation)) {
                return si;
            }
        }
        return null;
    }

    private void updateStatusItemState(ProgressReportActivityDto si, ProgressInformation progressInformation, ModelContext modelContext) {
        si.setActivityType(progressInformation.getActivityType());
        si.setResourceShadowDiscriminator(progressInformation.getResourceShadowDiscriminator());
        if (progressInformation.getResourceShadowDiscriminator() != null) {
            String resourceOid = progressInformation.getResourceShadowDiscriminator().getResourceOid();
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
                si.setStatus(status.createStatusType());
            } else {
                si.setStatus(OperationResultStatusType.UNKNOWN);
            }
        }

        // information about modifications on a resource
        if (progressInformation.getActivityType() == RESOURCE_OBJECT_OPERATION &&
                progressInformation.getStateType() == EXITING &&
                progressInformation.getResourceShadowDiscriminator() != null &&
                progressInformation.getResourceShadowDiscriminator().getResourceOid() != null) {
            ModelProjectionContext mpc = modelContext.findProjectionContext(progressInformation.getResourceShadowDiscriminator());
            if (mpc != null) {      // it shouldn't be null!

                // operations performed (TODO aggregate them somehow?)
                List<ResourceOperationResult> resourceOperationResultList = new ArrayList<>();
                List<? extends ObjectDeltaOperation> executedDeltas = mpc.getExecutedDeltas();
                for (ObjectDeltaOperation executedDelta : executedDeltas) {
                    ObjectDelta delta = executedDelta.getObjectDelta();
                    if (delta != null) {
                        OperationResult r = executedDelta.getExecutionResult();
                        OperationResultStatus status = r.getStatus();
                        if (status == OperationResultStatus.UNKNOWN) {
                            status = r.getComputeStatus();
                        }
                        resourceOperationResultList.add(new ResourceOperationResult(delta.getChangeType(), status));
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

    private Map<String,String> nameCache = new HashMap<>();

    private String getResourceName(@NotNull String oid) {
        String name = nameCache.get(oid);
        if (name != null) {
            return name;
        }
        Task task = parentPage.createSimpleTask("getResourceName");
        OperationResult result = new OperationResult("getResourceName");
        Collection<SelectorOptions<GetOperationOptions>> raw = SelectorOptions.createCollection(GetOperationOptions.createRaw());       // todo what about security?
        try {
            PrismObject<ResourceType> object = parentPage.getModelService().getObject(ResourceType.class, oid, raw, task, result);
            name = PolyString.getOrig(object.asObjectable().getName());
        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|CommunicationException|ConfigurationException|ExpressionEvaluationException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine the name of resource {}", e, oid);
            name = "(" + oid + ")";
        }
        nameCache.put(oid, name);
        return name;
    }
}
