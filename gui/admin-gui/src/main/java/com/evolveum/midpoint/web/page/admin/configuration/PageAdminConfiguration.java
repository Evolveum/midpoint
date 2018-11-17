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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import javax.xml.namespace.QName;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author lazyman
 */
public class PageAdminConfiguration extends PageAdmin {

    public static final String AUTH_CONFIGURATION_ALL = AuthorizationConstants.AUTZ_UI_CONFIGURATION_ALL_URL;
    public static final String AUTH_CONFIGURATION_ALL_LABEL = "PageAdminConfiguration.auth.configurationAll.label";
    public static final String AUTH_CONFIGURATION_ALL_DESCRIPTION = "PageAdminConfiguration.auth.configurationAll.description";

    public PageAdminConfiguration() {
    }

    public PageAdminConfiguration(PageParameters parameters) {
        super(parameters);
    }
    
    protected String deleteObjectsAsync(QName type, ObjectQuery objectQuery, boolean raw, String taskName,
			OperationResult result)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

    	Task task = createSimpleTask(result.getOperation());
    	task.setHandlerUri(ModelPublicConstants.DELETE_TASK_HANDLER_URI);

		if (objectQuery == null) {
			objectQuery = new ObjectQuery();
		}

		QueryType query = getQueryConverter().createQueryType(objectQuery);

		PrismPropertyDefinition queryDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.COMPLEX_TYPE, getPrismContext());
		PrismProperty<QueryType> queryProp = queryDef.instantiate();
		queryProp.setRealValue(query);
		task.setExtensionProperty(queryProp);

		PrismPropertyDefinition typeDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, DOMUtil.XSD_QNAME, getPrismContext());
		PrismProperty<QName> typeProp = typeDef.instantiate();
		typeProp.setRealValue(type);
		task.setExtensionProperty(typeProp);

		PrismPropertyDefinition rawDef = new PrismPropertyDefinitionImpl(
				SchemaConstants.MODEL_EXTENSION_OPTION_RAW, DOMUtil.XSD_BOOLEAN, getPrismContext());
		PrismProperty<Boolean> rawProp = rawDef.instantiate();
		rawProp.setRealValue(raw);
		task.setExtensionProperty(rawProp);

		task.setName(taskName);
		task.savePendingModifications(result);

		TaskManager taskManager = getTaskManager();
		taskManager.switchToBackground(task, result);
		result.setBackgroundTaskOid(task.getOid());
		return task.getOid();
	}
}
