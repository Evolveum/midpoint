/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.WebXmlUtil;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import org.apache.wicket.Application;

import javax.xml.namespace.QName;

/**
 * TODO use composition instead of inheritance
 *
 * @author mederly
 */
public class QueryBasedHandlerDto extends HandlerDto {

	public static final String F_OBJECT_TYPE_KEY = "objectTypeKey";
	public static final String F_OBJECT_QUERY = "objectQuery";

	public QueryBasedHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public String getObjectTypeKey() {
		QName objectType = taskDto.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_TYPE, QName.class);
		ObjectTypeGuiDescriptor descriptor = ObjectTypeGuiDescriptor.getDescriptor(ObjectTypes.getObjectTypeFromTypeQName(objectType));
		return descriptor != null ? descriptor.getLocalizationKey() : null;
	}

	public String getObjectQuery() {
		QueryType query = taskDto.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY, QueryType.class);
		if (query == null) {
			return null;
		}
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return WebXmlUtil.stripNamespaceDeclarations(
					prismContext.xmlSerializer().serializeAnyData(query, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize query: " + e.getMessage(), e);
		}
	}


}
