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

import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.OperationStatus;
import com.evolveum.midpoint.model.api.OperationStatusListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
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
import com.evolveum.midpoint.web.component.status.StatusDto;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.formula.functions.T;
import org.apache.wicket.Page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.model.api.OperationStatus.EventType.FOCUS_OPERATION;
import static com.evolveum.midpoint.model.api.OperationStatus.EventType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.model.api.OperationStatus.EventType.WORKFLOWS;
import static com.evolveum.midpoint.model.api.OperationStatus.StateType;
import static com.evolveum.midpoint.model.api.OperationStatus.StateType.ENTERING;
import static com.evolveum.midpoint.web.component.status.StatusDto.StatusItem;
import static com.evolveum.midpoint.web.page.PageBase.createStringResourceStatic;

/**
* @author mederly
*/
public class DefaultGuiStatusListener implements OperationStatusListener {

    private static final Trace LOGGER = TraceManager.getTrace(DefaultGuiStatusListener.class);

    private StatusDto statusDto;
    private PageBase parentPage;

    public DefaultGuiStatusListener(PageBase parentPage, StatusDto statusDto) {
        this.parentPage = parentPage;
        this.statusDto = statusDto;
    }

    @Override
    public void onStateUpdate(ModelContext modelContext, OperationStatus operationStatus, String message) {

        LOGGER.info("onStateUpdate: {}; operationStatus = {}\n, modelContext = \n{}", new Object[]{message, operationStatus.debugDump(), modelContext.debugDump(2)});

        if (statusDto == null) {
            LOGGER.error("No statusDto, exiting");
        }

        if (StringUtils.isNotEmpty(message)) {
            statusDto.log(message);
        }

        List<StatusItem> statusItems = statusDto.getStatusItems();

        if (operationStatus != null) {

            StatusItem si = findRelevantStatusItem(statusItems, operationStatus);

            if (si == null) {
                statusDto.add(createStatusItem(operationStatus));
            } else {
                updateStatusItemState(si, operationStatus);
            }
        }

        addExpectedStatusItems(statusItems, modelContext);
    }

    private void addExpectedStatusItems(List<StatusItem> statusItems, ModelContext modelContext) {
        if (modelContext.getFocusContext() != null) {
            ModelElementContext fc = modelContext.getFocusContext();
            if (isNotEmpty(fc.getPrimaryDelta()) || isNotEmpty(fc.getSecondaryDelta())) {
                OperationStatus modelStatus = new OperationStatus(FOCUS_OPERATION, (StateType) null);
                if (findRelevantStatusItem(statusItems, modelStatus) == null) {
                    statusItems.add(createStatusItem(modelStatus));
                }
            }
        }
        if (modelContext.getProjectionContexts() != null) {
            Collection<ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
            for (ModelProjectionContext mpc : projectionContexts) {
                OperationStatus projectionStatus = new OperationStatus(RESOURCE_OBJECT_OPERATION, mpc.getResourceShadowDiscriminator(), (StateType) null);
                if (findRelevantStatusItem(statusItems, projectionStatus) == null) {
                    statusItems.add(createStatusItem(projectionStatus));
                }
            }
        }
    }

    private boolean isNotEmpty(ObjectDelta delta) {
        return delta != null && !delta.isEmpty();
    }

    private StatusItem findRelevantStatusItem(List<StatusItem> statusItems, OperationStatus operationStatus) {
        for (StatusItem si : statusItems) {
            if (si.correspondsTo(operationStatus)) {
                return si;
            }
        }
        return null;
    }

    private void updateStatusItemState(StatusItem si, OperationStatus operationStatus) {
        si.setEventType(operationStatus.getEventType());
        si.setResourceShadowDiscriminator(operationStatus.getResourceShadowDiscriminator());
        if (operationStatus.getResourceShadowDiscriminator() != null) {
            si.setResourceName(getResourceName(operationStatus.getResourceShadowDiscriminator().getResourceOid()));
        }
        if (operationStatus.getStateType() == null) {
            si.setState(null);
        } else if (operationStatus.getStateType() == ENTERING) {
            si.setState(OperationResultStatusType.IN_PROGRESS);
        } else {
            OperationResult result = operationStatus.getOperationResult();
            if (result != null) {
                OperationResultStatus status = result.getStatus();
                if (status == OperationResultStatus.UNKNOWN) {
                    status = result.getComputeStatus();
                }
                si.setState(status.createStatusType());
            } else {
                si.setState(OperationResultStatusType.UNKNOWN);
            }
        }
    }

    private StatusItem createStatusItem(OperationStatus operationStatus) {
        StatusItem si = new StatusItem();
        updateStatusItemState(si, operationStatus);
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
