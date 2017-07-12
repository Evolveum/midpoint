/*
 * Copyright (c) 2010-2017 Evolveum
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
