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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
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
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;

/**
 * Created by honchar.
 */
public class AuditEventRecordProvider extends BaseSortableDataProvider<AuditEventRecordType> {
	private static final long serialVersionUID = 1L;

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

	private IModel<List<AuditEventRecordType>> model;

	private String auditEventQuery;
	private Map<String, Object> parameters = new HashMap<>();

	private static final String AUDIT_RECORDS_QUERY_CORE = "from RAuditEventRecord as aer left outer join aer.referenceValues as rv where 1=1 and ";
	private static final String AUDIT_RECORDS_QUERY_COUNT = "select count(*) ";
	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp desc";
	private static final String SET_FIRST_RESULT_PARAMETER = "setFirstResult";
	private static final String SET_MAX_RESULTS_PARAMETER = "setMaxResults";

	public AuditEventRecordProvider(Component component){
		this(component, AUDIT_RECORDS_QUERY_CORE, new HashMap<>());
	}

	public AuditEventRecordProvider(Component component, String auditEventQuery, Map<String, Object> parameters ){
		super(component);
		this.auditEventQuery = auditEventQuery;
		this.parameters = parameters;

		initModel();
	}


	private void initModel(){
		model = new IModel<List<AuditEventRecordType>>() {
			@Override
			public List<AuditEventRecordType> getObject() {
				return listRecords(auditEventQuery, true);
			}

			@Override
			public void detach() {

			}

			@Override
			public void setObject(List<AuditEventRecordType> object) {
				// TODO Auto-generated method stub

			}
		};
	}

	@Override
	public Iterator<AuditEventRecordType> internalIterator(long first, long count) {
		saveCurrentPage(first, count);
		List<AuditEventRecordType> recordsList = listRecords(auditEventQuery, true, first, count);
		return recordsList.iterator();
	}


	protected int internalSize() {
 		String query = generateFullQuery(AUDIT_RECORDS_QUERY_COUNT + auditEventQuery, false, true);
		long count;
		try {
			Task task = getPage().createSimpleTask("internalSize");
			count = getAuditService().countObjects(query, parameters, task, task.getResult());
		} catch (SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
			// TODO: proper error handling (MID-3536)
			throw new SystemException(e.getMessage(), e);
		}

		return ((Long)count).intValue();
 	}



	private List<AuditEventRecordType> listRecords(String query, boolean orderBy){
        return listRecords(query, orderBy, 0, getPage().getItemsPerPage(UserProfileStorage.TableId.PAGE_AUDIT_LOG_VIEWER));
    }

	private List<AuditEventRecordType> listRecords(String query, boolean orderBy, long first, long count){
		String parameterQuery = generateFullQuery(query, orderBy, false);

        if (parameters.containsKey(SET_FIRST_RESULT_PARAMETER)){
            parameters.remove(SET_FIRST_RESULT_PARAMETER);
        }
        parameters.put(SET_FIRST_RESULT_PARAMETER, ((Long) first).intValue());
        if (parameters.containsKey(SET_MAX_RESULTS_PARAMETER)){
            parameters.remove(SET_MAX_RESULTS_PARAMETER);
        }
        parameters.put(SET_MAX_RESULTS_PARAMETER, ((Long) count).intValue());


        List<AuditEventRecord> auditRecords;
		try {
			Task task = getPage().createSimpleTask("listRecords");
			auditRecords = getAuditService().listRecords(parameterQuery, parameters,
					task, task.getResult());
		} catch (SecurityViolationException | SchemaException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | ConfigurationException e) {
			// TODO: proper error handling (MID-3536)
			throw new SystemException(e.getMessage(), e);
		}
		if (auditRecords == null){
			auditRecords = new ArrayList<>();
		}
		List<AuditEventRecordType> auditRecordList = new ArrayList<>();
		for (AuditEventRecord record : auditRecords){
			auditRecordList.add(record.createAuditEventRecordType());
		}

		return auditRecordList;
	}

	public String getAuditEventQuery() {
		return auditEventQuery;
	}

	public void setAuditEventQuery(String auditEventQuery) {
		this.auditEventQuery = auditEventQuery;
	}

	private String generateFullQuery(String query, boolean orderBy, boolean isCount){
		parameters = getParameters();
		String valueRefFromClause = constraintsValueRef(parameters) ? " left outer join aer.referenceValues as rv " : "";
		if (parameters.get(PARAMETER_CHANGED_ITEM) != null) {
			if (isCount) {
				query = "select count(*) from RAuditEventRecord as aer right join aer.changedItems as item " + valueRefFromClause + " where 1=1 and ";
			} else {
				query = "from RAuditEventRecord as aer right join aer.changedItems as item " + valueRefFromClause + " where 1=1 and ";
			}
			query += "(item.changedItemPath = :changedItem) and ";

		} else {
            parameters.remove(PARAMETER_CHANGED_ITEM);
		}

		if (parameters.get(PARAMETER_FROM) != null) {
			query += "(aer.timestamp >= :from) and ";
		} else {
            parameters.remove(PARAMETER_FROM);
		}
		if (parameters.get(PARAMETER_TO) != null) {
			query += "(aer.timestamp <= :to) and ";
		} else {
            parameters.remove(PARAMETER_TO);
		}
		if (parameters.get(PARAMETER_EVENT_TYPE) != null) {
			query += "(aer.eventType = :eventType) and ";
		} else {
            parameters.remove(PARAMETER_EVENT_TYPE);
		}
		if (parameters.get(PARAMETER_EVENT_STAGE) != null) {
			query += "(aer.eventStage = :eventStage) and ";
		} else {
            parameters.remove(PARAMETER_EVENT_STAGE);
		}
		Object outcomeValue = parameters.get(PARAMETER_OUTCOME);
		if (outcomeValue != null) {
			if (outcomeValue != OperationResultStatusType.UNKNOWN) {
				query += "(aer.outcome = :outcome) and ";
			} else {
				// this is a bit questionable; but let us do it in this way to ensure compliance with GUI (null is shown as UNKNOWN)
				// see MID-3903
				query += "(aer.outcome = :outcome or aer.outcome is null) and ";
			}
		} else {
            parameters.remove(PARAMETER_OUTCOME);
		}
		if (parameters.get(PARAMETER_INITIATOR_NAME) != null) {
			query += "(aer.initiatorOid = :initiatorName) and ";
		} else {
            parameters.remove(PARAMETER_INITIATOR_NAME);
		}
		if (parameters.get(PARAMETER_CHANNEL) != null) {
			query += "(aer.channel = :channel) and ";
		} else {
            parameters.remove(PARAMETER_CHANNEL);
		}
		if (parameters.get(PARAMETER_HOST_IDENTIFIER) != null) {
			query += "(aer.hostIdentifier = :hostIdentifier) and ";
		} else {
            parameters.remove(PARAMETER_HOST_IDENTIFIER);
		}
		if (parameters.get(PARAMETER_TARGET_OWNER_NAME) != null) {
			query += "(aer.targetOwnerOid = :targetOwnerName) and ";
		} else {
            parameters.remove(PARAMETER_TARGET_OWNER_NAME);
		}
		if (parameters.get(PARAMETER_TARGET_NAMES) != null) {
			query += "(aer.targetOid in ( :targetNames )) and ";
		} else {
            parameters.remove(PARAMETER_TARGET_NAMES);
		}
		if (parameters.get(PARAMETER_TASK_IDENTIFIER) != null) {
			query += "(aer.taskIdentifier = :taskIdentifier) and ";
		} else {
            parameters.remove(PARAMETER_TASK_IDENTIFIER);
		}
		if (valueRefTargetIsNotEmpty(parameters.get(PARAMETER_VALUE_REF_TARGET_NAMES))) {
			query += "(rv.targetName.orig in ( :valueRefTargetNames )) and ";
		} else {
            parameters.remove(PARAMETER_VALUE_REF_TARGET_NAMES);
		}

		query = query.substring(0, query.length()-5); // remove trailing " and "
		if (orderBy) {
			query += AUDIT_RECORDS_ORDER_BY;
		}
		return query;
	}

	private boolean constraintsValueRef(Map<String, Object> parameters2) {
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

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	protected void saveCurrentPage(long from, long count){

	}

}
