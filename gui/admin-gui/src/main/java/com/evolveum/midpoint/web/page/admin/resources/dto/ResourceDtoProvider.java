/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import org.apache.wicket.Component;

/**
 * @author lazyman
 */
public class ResourceDtoProvider extends BaseSortableDataProvider<ResourceDto> {

    private static final String DOT_CLASS = ResourceDtoProvider.class.getName() + ".";
    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";
    private static final String OPERATION_LIST_RESOURCE = DOT_CLASS + "listResource";
    private static final String OPERATION_COUNT_RESOURCES = DOT_CLASS + "countResources";

    public ResourceDtoProvider(Component component) {
        super(component);
    }

    @Override
    public Iterator<? extends ResourceDto> internalIterator(long first, long count) {
        getAvailableData().clear();

        OperationResult result = new OperationResult(OPERATION_LIST_RESOURCES);
        try {
        	ObjectPaging paging = createPaging(first, count);
            ObjectQuery query = getQuery();
            if (query == null){
            	query = new ObjectQuery();
            }
            query.setPaging(paging);
            Task task = getPage().createSimpleTask(OPERATION_LIST_RESOURCES);
            List<PrismObject<ResourceType>> resources = getModel().searchObjects(ResourceType.class, query, null, task, result);

            for (PrismObject<ResourceType> resource : resources) {

                OperationResult result1 = result.createMinorSubresult(OPERATION_LIST_RESOURCE);
                ResourceType resourceType = resource.asObjectable();

                PrismObject<ConnectorType> connector = null;
                try {
                    connector = resolveConnector(resourceType, task, result1);
                } catch (ObjectNotFoundException e) {
                    result1.recordWarning("Connector for resource " + resource.getOid() + " couldn't be resolved", e);
                }
                ConnectorType connectorType = connector != null ? connector.asObjectable() : null;
                getAvailableData().add(new ResourceDto(resource, connectorType));

                result1.recordSuccessIfUnknown();
            }
            result.computeStatus();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't list resources.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return getAvailableData().iterator();
    }

    private PrismObject<ConnectorType> resolveConnector(ResourceType resource, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {

        ObjectReferenceType ref = resource.getConnectorRef();
        String oid = ref != null ? ref.getOid() : null;
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        return getModel().getObject(ConnectorType.class, oid, null, task, result);
    }

    @Override
    protected int internalSize() {
        OperationResult result = new OperationResult(OPERATION_COUNT_RESOURCES);
        int count = 0;
        try {
            Task task = getPage().createSimpleTask(OPERATION_COUNT_RESOURCES);
            count = getModel().countObjects(ResourceType.class, getQuery(), null, task, result);

            result.recordSuccess();
        } catch (Exception ex) {
            result.recomputeStatus();
            result.recordFatalError("Couldn't count resource objects.", ex);
        }

        if (!result.isSuccess()) {
            getPage().showResult(result);
        }

        return count;
    }
}
