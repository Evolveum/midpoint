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
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.status.ProgressDto;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.FOCUS_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.ActivityType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType;
import static com.evolveum.midpoint.model.api.ProgressInformation.StateType.ENTERING;

import com.evolveum.midpoint.web.component.status.ProgressReportActivityDto;

/**
* @author mederly
*/
public class DefaultGuiProgressListener implements ProgressListener, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultGuiProgressListener.class);

    private ProgressDto progressDto;
    private PageBase parentPage;

    private boolean abortRequested;

    public DefaultGuiProgressListener(PageBase parentPage, ProgressDto progressDto) {
        this.parentPage = parentPage;
        this.progressDto = progressDto;
    }

    @Override
    public void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("onProgressAchieved: {}\n, modelContext = \n{}", new Object[]{progressInformation.debugDump(), modelContext.debugDump(2)});
        }

        if (progressDto == null) {
            LOGGER.error("No progressDto, exiting");      // should not occur
        }

        if (StringUtils.isNotEmpty(progressInformation.getMessage())) {
            progressDto.log(progressInformation.getMessage());
        }

        List<ProgressReportActivityDto> progressReportActivities = progressDto.getProgressReportActivities();

        if (progressInformation != null) {

            ProgressReportActivityDto si = findRelevantStatusItem(progressReportActivities, progressInformation);

            if (si == null) {
                progressDto.add(createStatusItem(progressInformation));
            } else {
                updateStatusItemState(si, progressInformation);
            }
        }

        addExpectedStatusItems(progressReportActivities, modelContext);
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
                    progressReportActivities.add(createStatusItem(modelStatus));
                }
            }
        }
        if (modelContext.getProjectionContexts() != null) {
            Collection<ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
            for (ModelProjectionContext mpc : projectionContexts) {
                ProgressInformation projectionStatus = new ProgressInformation(RESOURCE_OBJECT_OPERATION, mpc.getResourceShadowDiscriminator(), (StateType) null);
                if (findRelevantStatusItem(progressReportActivities, projectionStatus) == null) {
                    progressReportActivities.add(createStatusItem(projectionStatus));
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

    private void updateStatusItemState(ProgressReportActivityDto si, ProgressInformation progressInformation) {
        si.setActivityType(progressInformation.getActivityType());
        si.setResourceShadowDiscriminator(progressInformation.getResourceShadowDiscriminator());
        if (progressInformation.getResourceShadowDiscriminator() != null) {
            si.setResourceName(getResourceName(progressInformation.getResourceShadowDiscriminator().getResourceOid()));
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
    }

    private ProgressReportActivityDto createStatusItem(ProgressInformation progressInformation) {
        ProgressReportActivityDto si = new ProgressReportActivityDto();
        updateStatusItemState(si, progressInformation);
        return si;
    }

    private Map<String,String> nameCache = new HashMap<>();

    private String getResourceName(String oid) {
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
        } catch (ObjectNotFoundException|SchemaException|SecurityViolationException|CommunicationException|ConfigurationException e) {
            LoggingUtils.logException(LOGGER, "Couldn't determine the name of resource {}", e, oid);
            name = "(" + oid + ")";
        }
        nameCache.put(oid, name);
        return name;
    }
}
