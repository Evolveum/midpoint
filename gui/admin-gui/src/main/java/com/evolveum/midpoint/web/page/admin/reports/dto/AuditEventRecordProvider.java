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
package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuditSearchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.util.DashboardUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by honchar.
 */
public class AuditEventRecordProvider extends BaseSortableDataProvider<AuditEventRecordType> {
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Trace LOGGER = TraceManager.getTrace(BaseSortableDataProvider.class);

	public static final String PARAMETER_VALUE_REF_TARGET_NAMES = "valueRefTargetNames";
	public static final String PARAMETER_CHANGED_ITEM = "changedItem";
	public static final String PARAMETER_FROM = "from";
	public static final String PARAMETER_TO = "to";
	public static final String PARAMETER_EVENT_TYPE = "eventType";
	public static final String PARAMETER_EVENT_STAGE = "eventStage";
	public static final String PARAMETER_OUTCOME = "outcome";
	public static final String PARAMETER_INITIATOR_NAME = "initiatorName";
	public static final String PARAMETER_CHANNEL = "channel";
	public static final String PARAMETER_HOST_IDENTIFIER = "hostIdentifier";
	public static final String PARAMETER_TARGET_OWNER_NAME = "targetOwnerName";
	public static final String PARAMETER_TARGET_NAMES = "targetNames";
	public static final String PARAMETER_TASK_IDENTIFIER = "taskIdentifier";

	@Nullable private final IModel<ObjectCollectionType> objectCollectionModel;
	@NotNull private final SerializableSupplier<Map<String, Object>> parametersSupplier;

	private static final String AUDIT_RECORDS_QUERY_CORE = "from RAuditEventRecord as aer";
	private static final String AUDIT_RECORDS_QUERY_ITEMS_CHANGED = " right join aer.changedItems as item";
	private static final String AUDIT_RECORDS_QUERY_REF_VALUES = " left outer join aer.referenceValues as rv";
	private static final String AUDIT_RECORDS_QUERY_COUNT = "select count(*) ";
	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp desc";
	private static final String SET_FIRST_RESULT_PARAMETER = "setFirstResult";
	private static final String SET_MAX_RESULTS_PARAMETER = "setMaxResults";
//	private static final String TIMESTAMP_VALUE_NAME = "aer.timestamp";

	public AuditEventRecordProvider(Component component, @Nullable IModel<ObjectCollectionType> objectCollectionModel, @NotNull SerializableSupplier<Map<String, Object>> parametersSupplier) {
		super(component);
		this.objectCollectionModel = objectCollectionModel;
		this.parametersSupplier = parametersSupplier;
	}

	@Override
	public Iterator<AuditEventRecordType> internalIterator(long first, long count) {
		saveCurrentPage(first, count);
		List<AuditEventRecordType> recordsList = listRecords(true, first, count);
		return recordsList.iterator();
	}

	protected int internalSize() {
		String query;
		String origQuery;
		Map<String, Object> parameters = new HashMap<String, Object>();
		origQuery = DashboardUtils.createQuery(getCollectionForQuery(), parameters, false, getPage().getClock());
		if(StringUtils.isNotBlank(origQuery)) {
			query = generateFullQuery(origQuery, false, true);
		} else {
			parameters = parametersSupplier.get();
			query = generateFullQuery(parameters, false, true);
		}
		try {
			Task task = getPage().createSimpleTask("internalSize");
			return (int) getAuditService().countObjects(query, parameters, task, task.getResult());
		} catch (SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
			// TODO: proper error handling (MID-3536)
			throw new SystemException(e.getMessage(), e);
		}
 	}

	private List<AuditEventRecordType> listRecords(boolean ordered, long first, long count) {
		String query;
		String origQuery;
		Map<String, Object> parameters = new HashMap<String, Object>();
		origQuery = DashboardUtils.createQuery(getCollectionForQuery(), parameters, false, getPage().getClock());
		if(StringUtils.isNotBlank(origQuery)) {
			query = generateFullQuery(origQuery, ordered, false);
		} else {
			parameters = parametersSupplier.get();
			query = generateFullQuery(parameters, ordered, false);
		}
		
        parameters.put(SET_FIRST_RESULT_PARAMETER, (int) first);
        parameters.put(SET_MAX_RESULTS_PARAMETER, (int) count);

        List<AuditEventRecord> auditRecords;
		try {
			Task task = getPage().createSimpleTask("listRecords");
			auditRecords = getAuditService().listRecords(query, parameters, task, task.getResult());
		} catch (SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
			// TODO: proper error handling (MID-3536)
			throw new SystemException(e.getMessage(), e);
		}
		if (auditRecords == null) {
			auditRecords = new ArrayList<>();
		}
		List<AuditEventRecordType> auditRecordList = new ArrayList<>();
		for (AuditEventRecord record : auditRecords){
			auditRecordList.add(record.createAuditEventRecordType());
		}
		return auditRecordList;
	}
	
//	public static String createQuery(ObjectCollectionType collectionForQuery, Map<String, Object> parameters,
//			boolean forDomain, Clock clock) {
//		if(collectionForQuery == null) {
//			return null;
//		}
//		AuditSearchType auditSearch = collectionForQuery.getAuditSearch();
//		if(auditSearch != null || StringUtils.isNotBlank(auditSearch.getRecordQuery())) {
//			Duration interval = auditSearch.getInterval();
//			if(interval == null) {
//				return auditSearch.getRecordQuery();
//			}
//			String origQuery = auditSearch.getRecordQuery();
//			if(forDomain) {
//				origQuery = auditSearch.getDomainQuery();
//				if(origQuery == null) {
//					return null;
//				}
//			}
//			String [] partsOfQuery = origQuery.split("where");
//			if(interval.getSign() == 1) {
//				interval = interval.negate();
//			}
//			Date date = new Date(clock.currentTimeMillis());
//			interval.addTo(date);
//			String query = partsOfQuery[0] + "where " + TIMESTAMP_VALUE_NAME + " >= " + ":from" + " ";
//			parameters.put(PARAMETER_FROM, date);
//			if(partsOfQuery.length > 1) {
//				query+= "and" +partsOfQuery[1]; 
//			}
//			return query;
//		}
//		return null;
//	}


	@SuppressWarnings("unused")
	@Nullable
	public ObjectCollectionType getCollectionForQuery() {
		if(objectCollectionModel == null) {
			return null;
		}
		return objectCollectionModel.getObject();
	}

	private String generateFullQuery(Map<String, Object> parameters, boolean ordered, boolean isCount) {
		boolean filteredOnChangedItem = parameters.get(PARAMETER_CHANGED_ITEM) != null;
		boolean filteredOnValueRefTargetNames = filteredOnValueRefTargetNames(parameters);
		List<String> conditions = new ArrayList<>();
		if (parameters.get(PARAMETER_FROM) != null) {
			conditions.add("aer.timestamp >= :from");
		} else {
			parameters.remove(PARAMETER_FROM);
		}
		if (parameters.get(PARAMETER_TO) != null) {
			conditions.add("aer.timestamp <= :to");
		} else {
			parameters.remove(PARAMETER_TO);
		}
		if (parameters.get(PARAMETER_EVENT_TYPE) != null) {
			conditions.add("aer.eventType = :eventType");
		} else {
			parameters.remove(PARAMETER_EVENT_TYPE);
		}
		if (parameters.get(PARAMETER_EVENT_STAGE) != null) {
			conditions.add("aer.eventStage = :eventStage");
		} else {
			parameters.remove(PARAMETER_EVENT_STAGE);
		}
		Object outcomeValue = parameters.get(PARAMETER_OUTCOME);
		if (outcomeValue != null) {
			if (outcomeValue != OperationResultStatusType.UNKNOWN) {
				conditions.add("aer.outcome = :outcome");
			} else {
				// this is a bit questionable; but let us do it in this way to ensure compliance with GUI (null is shown as UNKNOWN)
				// see MID-3903
				conditions.add("(aer.outcome = :outcome or aer.outcome is null)");
			}
		} else {
			parameters.remove(PARAMETER_OUTCOME);
		}
		if (parameters.get(PARAMETER_INITIATOR_NAME) != null) {
			conditions.add("aer.initiatorOid = :initiatorName");
		} else {
			parameters.remove(PARAMETER_INITIATOR_NAME);
		}
		if (parameters.get(PARAMETER_CHANNEL) != null) {
			conditions.add("aer.channel = :channel");
		} else {
			parameters.remove(PARAMETER_CHANNEL);
		}
		if (parameters.get(PARAMETER_HOST_IDENTIFIER) != null) {
			conditions.add("aer.hostIdentifier = :hostIdentifier");
		} else {
			parameters.remove(PARAMETER_HOST_IDENTIFIER);
		}
		if (parameters.get(PARAMETER_TARGET_OWNER_NAME) != null) {
			conditions.add("aer.targetOwnerOid = :targetOwnerName");
		} else {
			parameters.remove(PARAMETER_TARGET_OWNER_NAME);
		}
		if (parameters.get(PARAMETER_TARGET_NAMES) != null) {
			conditions.add("aer.targetOid in ( :targetNames )");
		} else {
			parameters.remove(PARAMETER_TARGET_NAMES);
		}
		if (parameters.get(PARAMETER_TASK_IDENTIFIER) != null) {
			conditions.add("aer.taskIdentifier = :taskIdentifier");
		} else {
			parameters.remove(PARAMETER_TASK_IDENTIFIER);
		}
		if (filteredOnChangedItem) {
			conditions.add("item.changedItemPath = :changedItem");
		} else {
			parameters.remove(PARAMETER_CHANGED_ITEM);
		}
		if (filteredOnValueRefTargetNames) {
			conditions.add("rv.targetName.orig in ( :valueRefTargetNames )");
		} else {
			parameters.remove(PARAMETER_VALUE_REF_TARGET_NAMES);
		}
		ObjectCollectionType collection = getCollectionForQuery();
		String query = "";
		if (collection == null || collection.getAuditSearch() == null
				|| collection.getAuditSearch().getRecordQuery() == null) {
			query = AUDIT_RECORDS_QUERY_CORE;
			if (filteredOnChangedItem) {
				query += AUDIT_RECORDS_QUERY_ITEMS_CHANGED;
			}
			if (filteredOnValueRefTargetNames) {
				query += AUDIT_RECORDS_QUERY_REF_VALUES;
			}
			if (!conditions.isEmpty()) {
				query += " where ";
			}
		} else {
			query = collection.getAuditSearch().getRecordQuery();
		}
		if (isCount) {
			query = AUDIT_RECORDS_QUERY_COUNT + query;
		}
		query += conditions.stream().collect(Collectors.joining(" and "));
		if (ordered) {
			query += AUDIT_RECORDS_ORDER_BY;
		}
		return query;
	}
	
	private String generateFullQuery(String origQuery, boolean ordered, boolean isCount) {
		String query = origQuery;
		if (isCount) {
			query = AUDIT_RECORDS_QUERY_COUNT + query;
		}
		if (ordered) {
			query += AUDIT_RECORDS_ORDER_BY;
		}
		return query;
	}
	
	private boolean filteredOnValueRefTargetNames(Map<String, Object> parameters2) {
		return valueRefTargetIsNotEmpty(parameters2.get(PARAMETER_VALUE_REF_TARGET_NAMES));
	}

	private boolean valueRefTargetIsNotEmpty(Object valueRefTargetNamesParam) {
		if (valueRefTargetNamesParam instanceof String) {
			return StringUtils.isNotBlank((String)valueRefTargetNamesParam);
		} else if (valueRefTargetNamesParam instanceof Collection) {
			return CollectionUtils.isNotEmpty((Collection)valueRefTargetNamesParam);
		} else {
			return valueRefTargetNamesParam != null;
		}
	}

	protected void saveCurrentPage(long from, long count) {
	}
}
