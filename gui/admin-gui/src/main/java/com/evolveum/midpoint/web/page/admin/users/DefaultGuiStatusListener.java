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
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.hssf.record.formula.functions.T;
import org.apache.wicket.Page;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.model.api.OperationStatus.EventType.RESOURCE_OBJECT_OPERATION;
import static com.evolveum.midpoint.model.api.OperationStatus.EventType.WORKFLOWS;
import static com.evolveum.midpoint.model.api.OperationStatus.StateType.ENTERING;
import static com.evolveum.midpoint.web.component.status.StatusDto.StatusItem;

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

        //LOGGER.info("onStateUpdate: {}; modelContext = \n{}", message, modelContext.debugDump(2));

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
                // special treatment - removing all old success 'workflow' events if adding another one
                // because of model-impl/workflows-impl architecture there may be many 'WORKFLOWS'-type items
                if (operationStatus.getEventType() == WORKFLOWS && operationStatus.getStateType() == ENTERING) {
                    Iterator<StatusItem> it = statusItems.iterator();
                    while (it.hasNext()) {
                        StatusItem existingSi = it.next();
                        if (existingSi.getOperationStatus().getEventType() == WORKFLOWS && existingSi.isSuccess()) {
                            it.remove();
                        }
                    }
                }

                statusDto.add(createStatusItem(operationStatus));
            } else {
                updateStatusItemState(si, operationStatus);
            }
        }

        addExpectedStatusItems(statusItems, modelContext);
    }

    private void addExpectedStatusItems(List<StatusItem> statusItems, ModelContext modelContext) {
        if (modelContext.getProjectionContexts() == null) {
            return;
        }

        Collection<ModelProjectionContext> projectionContexts = modelContext.getProjectionContexts();
        for (ModelProjectionContext mpc : projectionContexts) {
            if (findRelevantStatusItem(statusItems, mpc.getResourceShadowDiscriminator()) == null) {
                statusItems.add(createAwaitingStatusItem(mpc.getResourceShadowDiscriminator()));
            }
        }
    }

    private StatusItem findRelevantStatusItem(List<StatusItem> statusItems, OperationStatus operationStatus) {
        for (StatusItem si : statusItems) {
            if (si.correspondsTo(operationStatus)) {
                return si;
            }
        }
        return null;
    }

    private StatusItem findRelevantStatusItem(List<StatusItem> statusItems, ResourceShadowDiscriminator discriminator) {
        for (StatusItem si : statusItems) {
            if (si.correspondsTo(discriminator)) {
                return si;
            }
        }
        return null;
    }

    private void updateStatusItemState(StatusItem si, OperationStatus operationStatus) {
        si.setAwaitingRsd(null);
        si.setOperationStatus(operationStatus);
        if (operationStatus.getStateType() == ENTERING) {
            si.setState("WORKING");
        } else {
            OperationResult result = operationStatus.getOperationResult();
            OperationResultStatus status = result.getStatus();
            if (status == OperationResultStatus.UNKNOWN) {
                status = result.getComputeStatus();
            }
            si.setState(status.toString());
            si.setSuccess(status == OperationResultStatus.SUCCESS || status == OperationResultStatus.HANDLED_ERROR);
        }
    }

    private StatusItem createStatusItem(OperationStatus operationStatus) {
        StatusItem si = new StatusItem(operationStatus);
        String description = operationStatus.getEventType().toString();
        if (operationStatus.getEventType() == RESOURCE_OBJECT_OPERATION && operationStatus.getResourceShadowDiscriminator() != null) {
            description = getResourceObjectDescription(operationStatus.getResourceShadowDiscriminator());
        }
        si.setDescription(description);
        updateStatusItemState(si, operationStatus);
        return si;
    }

    private String getResourceObjectDescription(ResourceShadowDiscriminator d) {
        return d.getKind() + " (" + d.getIntent() + ") on " + getResourceName(d.getResourceOid());
    }

    private StatusItem createAwaitingStatusItem(ResourceShadowDiscriminator discriminator) {
        StatusItem si = new StatusItem(null);
        String description = getResourceObjectDescription(discriminator);
        si.setDescription(description);
        si.setState("(expected)");
        si.setAwaitingRsd(discriminator);
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
