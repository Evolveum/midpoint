/**
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.model.api.util;

import java.util.Date;
import java.util.Map;

import javax.xml.datatype.Duration;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetPresentationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetSourceTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author skublik
 */
@Experimental
public class DashboardUtils {
	
	private static final Trace LOGGER = TraceManager.getTrace(DashboardUtils.class);

	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp desc";
	private static final String TIMESTAMP_VALUE_NAME = "aer.timestamp";
	public static final String PARAMETER_FROM = "from";
	
	public static DashboardWidgetSourceTypeType getSourceType(DashboardWidgetType widget) {
		if(isSourceTypeOfDataNull(widget)) {
			return null;
		}
		return widget.getData().getSourceType();
	}
	
	public static boolean isSourceTypeOfDataNull(DashboardWidgetType widget) {
		if(isDataNull(widget)) {
			return true;
		}
		if(widget.getData().getSourceType() == null) {
			LOGGER.error("SourceType of data is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}
	
	public static boolean isDataNull(DashboardWidgetType widget) {
		if(widget.getData() == null) {
			LOGGER.error("Data is not found in widget " + widget.getIdentifier());
			return true;
		}
		return false;
	}

	public static boolean isDataFieldsOfPresentationNullOrEmpty(DashboardWidgetPresentationType presentation) {
		if(presentation != null) {
			if(presentation.getDataField() != null) {
				if(!presentation.getDataField().isEmpty()) {
					return false;
				} else {
					LOGGER.error("DataField of presentation is empty");
				}
			} else {
				LOGGER.error("DataField of presentation is not defined");
			}
		} else {
			LOGGER.error("Presentation of widget is not defined");
		}
		
		return true;
	}
	
	public static String getQueryForListRecords(String query) {
		query = query + AUDIT_RECORDS_ORDER_BY;
		LOGGER.debug("Query for select: " + query);
		return query;
	}
	
	// TODO: This is quite a common things, isn't it? Maybe it should rather go to PageBase?
	// Ot maybe somehow user ObjectResolver?
	public static ObjectType getObjectTypeFromObjectRef(ObjectReferenceType ref, TaskManager taskManager, 
			PrismContext prismContext, ModelService modelService) {
		Task task = taskManager.createTaskInstance("Get object");
		Class<ObjectType> type = prismContext.getSchemaRegistry().determineClassForType(ref.getType());
		PrismObject<ObjectType> object;
		
		try {
			object = modelService.getObject(type,
					ref.getOid(), null, task, task.getResult());
			return object.asObjectable();
		} catch (Exception e) {
			LOGGER.error("Couldn't get object from objectRef " + ref, e);
		}
		return null;
	}
	
	public static String createQuery(ObjectCollectionType collectionForQuery, Map<String, Object> parameters,
			boolean forDomain, Clock clock) {
		if(collectionForQuery == null) {
			return null;
		}
		AuditSearchType auditSearch = collectionForQuery.getAuditSearch();
		if(auditSearch != null || StringUtils.isNotBlank(auditSearch.getRecordQuery())) {
			Duration interval = auditSearch.getInterval();
			if(interval == null) {
				return auditSearch.getRecordQuery();
			}
			String origQuery = auditSearch.getRecordQuery();
			if(forDomain) {
				origQuery = auditSearch.getDomainQuery();
				if(origQuery == null) {
					return null;
				}
			}
			String [] partsOfQuery = origQuery.split("where");
			if(interval.getSign() == 1) {
				interval = interval.negate();
			}
			Date date = new Date(clock.currentTimeMillis());
			interval.addTo(date);
			String query = partsOfQuery[0] + "where " + TIMESTAMP_VALUE_NAME + " >= " + ":from" + " ";
			parameters.put(PARAMETER_FROM, date);
			if(partsOfQuery.length > 1) {
				query+= "and" +partsOfQuery[1]; 
			}
			return query;
		}
		return null;
	}

}
