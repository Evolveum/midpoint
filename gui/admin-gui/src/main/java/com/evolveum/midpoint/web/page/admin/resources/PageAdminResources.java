/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources;


import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class PageAdminResources extends PageAdmin {

    private static final String DOT_CLASS = PageAdminResources.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
    private static final String OPERATION_DELETE_SYNC_TOKEN = DOT_CLASS + "deleteSyncToken";
    private static final String OPERATION_SAVE_SYNC_TASK = DOT_CLASS + "saveSyncTask";

    protected static final Trace LOGGER = TraceManager.getTrace(PageAdminResources.class);

    public static final String AUTH_CONNECTOR_HOSTS_ALL = AuthorizationConstants.AUTZ_UI_CONNECTOR_HOSTS_ALL_URL;
    public static final String AUTH_CONNECTOR_HOSTS_ALL_LABEL = "PageAdminResources.auth.connectorHostsAll.label";
    public static final String AUTH_CONNECTOR_HOSTS_ALL_DESCRIPTION = "PageAdminResources.auth.connectorHostsAll.description";

    public static final String AUTH_RESOURCE_ALL = AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL;
    public static final String AUTH_RESOURCE_ALL_LABEL = "PageAdminResources.auth.resourcesAll.label";
    public static final String AUTH_RESOURCE_ALL_DESCRIPTION = "PageAdminResources.auth.resourcesAll.description";

    protected boolean isResourceOidAvailable() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return resourceOid != null && StringUtils.isNotEmpty(resourceOid.toString());
    }

    protected String getResourceOid() {
        StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        return resourceOid != null ? resourceOid.toString() : null;
    }

    protected PrismObject<ResourceType> loadResource(Collection<SelectorOptions<GetOperationOptions>> options) {
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
        PrismObject<ResourceType> resource = null;

        try {
            Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
            LOGGER.trace("getObject(resource) oid={}, options={}", getResourceOid(), options);
            resource = getModelService().getObject(ResourceType.class, getResourceOid(), options, task, result);
            result.recomputeStatus();

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("getObject(resource) result\n:{}", result.debugDump());
            }

        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource", ex);
            result.recordFatalError("Couldn't get resource, reason: " + ex.getMessage(), ex);
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            if (resource != null) {
                showResult(result);
            } else {
                getSession().error(getString("pageAdminResources.message.cantLoadResource"));
                throw new RestartResponseException(PageResources.class);
            }
        }

        return resource;
    }

    protected void deleteSyncTokenPerformed(AjaxRequestTarget target, ResourceType resourceType){
//        ResourceDto dto = model.getObject();
        String resourceOid = resourceType.getOid();
        String handlerUri = "http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/live-sync/handler-3";
        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        PrismObject<TaskType> oldTask;

        OperationResult result = new OperationResult(OPERATION_DELETE_SYNC_TOKEN);
        ObjectQuery query = QueryBuilder.queryFor(TaskType.class, getPrismContext())
                .item(TaskType.F_OBJECT_REF).ref(resourceOid)
                .and().item(TaskType.F_HANDLER_URI).eq(handlerUri)
                .build();

        List<PrismObject<TaskType>> taskList = WebModelServiceUtils.searchObjects(TaskType.class, query, result, this);

        if (taskList.size() != 1) {
            error(getString("pageResource.message.invalidTaskSearch"));
        } else {
            oldTask = taskList.get(0);
            saveTask(oldTask, result);
        }

        result.recomputeStatus();
        showResult(result);
        target.add(getFeedbackPanel());
    }

    private void saveTask(PrismObject<TaskType> oldTask, OperationResult result){
        Task task = createSimpleTask(OPERATION_SAVE_SYNC_TASK);

        PrismProperty property = oldTask.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN));

        if(property == null){
            return;
        }
        Object value = property.getRealValue();

        ObjectDelta<TaskType> delta = ObjectDelta.createModifyDelta(oldTask.getOid(),
                PropertyDelta.createModificationDeleteProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.SYNC_TOKEN), property.getDefinition(), value),
                TaskType.class, getPrismContext());

        if(LOGGER.isTraceEnabled()){
            LOGGER.trace(delta.debugDump());
        }

        try {
            getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
        } catch (Exception e){
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't save task.", e);
            result.recordFatalError("Couldn't save task.", e);
        }
        result.recomputeStatus();
    }
}
