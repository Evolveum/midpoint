/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
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
            objectQuery = getPrismContext().queryFactory().createQuery();
        }

        QueryType query = getQueryConverter().createQueryType(objectQuery);

        PrismPropertyDefinition queryDef = getPrismContext().definitionFactory().createPropertyDefinition(
                SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.COMPLEX_TYPE);
        PrismProperty<QueryType> queryProp = queryDef.instantiate();
        queryProp.setRealValue(query);
        task.setExtensionProperty(queryProp);

        PrismPropertyDefinition typeDef = getPrismContext().definitionFactory().createPropertyDefinition(
                SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, DOMUtil.XSD_QNAME);
        PrismProperty<QName> typeProp = typeDef.instantiate();
        typeProp.setRealValue(type);
        task.setExtensionProperty(typeProp);

        PrismPropertyDefinition rawDef = getPrismContext().definitionFactory().createPropertyDefinition(
                SchemaConstants.MODEL_EXTENSION_OPTION_RAW, DOMUtil.XSD_BOOLEAN);
        PrismProperty<Boolean> rawProp = rawDef.instantiate();
        rawProp.setRealValue(raw);
        task.setExtensionProperty(rawProp);

        task.setName(taskName);
        task.flushPendingModifications(result);

        task.addArchetypeInformationIfMissing(SystemObjectsType.ARCHETYPE_UTILITY_TASK.value());

        TaskManager taskManager = getTaskManager();
        taskManager.switchToBackground(task, result);
        result.setBackgroundTaskOid(task.getOid());
        return task.getOid();
    }
}
