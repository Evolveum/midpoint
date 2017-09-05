package com.evolveum.midpoint.web.page.admin.reports.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.schema.result.OperationResult;
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
	public  static final String VALUE_REF_TARGET_NAMES_KEY = "valueRefTargetNames";
	private static final Trace LOGGER = TraceManager.getTrace(BaseSortableDataProvider.class);
	private IModel<List<AuditEventRecordType>> model;

	private String auditEventQuery;
	private Map<String, Object> parameters = new HashMap<>();

	private static final String AUDIT_RECORDS_QUERY_CORE = "from RAuditEventRecord as aer left outer join aer.referenceValues as rv where 1=1 and ";
	private static final String AUDIT_RECORDS_QUERY_COUNT = "select count(*) ";
	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp desc";
	private static final String SET_FIRST_RESULT_PARAMETER = "setFirstResult";
	private static final String SET_MAX_RESULTS_PARAMETER = "setMaxResults";

	public AuditEventRecordProvider(Component component){
		this(component, AUDIT_RECORDS_QUERY_CORE, new HashMap<String, Object>());
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
			count = getAuditService().countObjects(query, parameters, new OperationResult("internalSize"));
		} catch (SecurityViolationException | SchemaException e) {
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
			auditRecords = getAuditService().listRecords(parameterQuery, parameters,
					new OperationResult("listRecords"));
		} catch (SecurityViolationException | SchemaException e) {
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
		String valueRefFrom = constraintsValueRef(parameters) ? " left outer join aer.referenceValues as rv " : "";
		if (parameters.get("changedItem") != null) {
			if (isCount) {
				query = "select count(*) from RAuditEventRecord as aer right join aer.changedItems as item " + valueRefFrom + " where 1=1 and ";
			} else {
				query = "from RAuditEventRecord as aer right join aer.changedItems as item " + valueRefFrom + " where 1=1 and ";
			}
//			query += "INNER JOIN aer.changedItems as item on item.record_id = aer.id WHERE 1=1 and  "
//					+ "(item.changedItemPath = :changedItem) and ";
			query += "(item.changedItemPath = :changedItem) and ";

		} else {
            parameters.remove("changedItem");
//            query += "where 1=1 and ";
		}

		if (parameters.get("from") != null) {
			query += "(aer.timestamp >= :from) and ";
		} else {
            parameters.remove("from");
		}
		if (parameters.get("to") != null) {
			query += "(aer.timestamp <= :to) and ";
		} else {
            parameters.remove("to");
		}
		if (parameters.get("eventType") != null) {
			query += "(aer.eventType = :eventType) and ";
		} else {
            parameters.remove("eventType");
		}
		if (parameters.get("eventStage") != null) {
			query += "(aer.eventStage = :eventStage) and ";
		} else {
            parameters.remove("eventStage");
		}
		if (parameters.get("outcome") != null) {
			query += "(aer.outcome = :outcome) and ";
		} else {
            parameters.remove("outcome");
		}
		if (parameters.get("initiatorName") != null) {
			query += "(aer.initiatorOid = :initiatorName) and ";
		} else {
            parameters.remove("initiatorName");
		}
		if (parameters.get("channel") != null) {
			query += "(aer.channel = :channel) and ";
		} else {
            parameters.remove("channel");
		}
		if (parameters.get("hostIdentifier") != null) {
			query += "(aer.hostIdentifier = :hostIdentifier) and ";
		} else {
            parameters.remove("hostIdentifier");
		}
		if (parameters.get("targetOwnerName") != null) {
			query += "(aer.targetOwnerOid = :targetOwnerName) and ";
		} else {
            parameters.remove("targetOwnerName");
		}
		if (parameters.get("targetNames") != null) {
			query += "(aer.targetOid in ( :targetNames )) and ";
		} else {
            parameters.remove("targetNames");
		}
		if (parameters.get("taskIdentifier") != null) {
			query += "(aer.taskIdentifier = :taskIdentifier) and ";
		} else {
            parameters.remove("taskIdentifier");
		}
		if (valueRefTargetIsNotEmpty(parameters.get(VALUE_REF_TARGET_NAMES_KEY))) {
			query += "(rv.targetName.orig in ( :valueRefTargetNames )) and ";
		} else {
            parameters.remove(VALUE_REF_TARGET_NAMES_KEY);
		}

		query = query.substring(0, query.length()-5); // remove trailing " and "
		if (orderBy){
			query +=  AUDIT_RECORDS_ORDER_BY;
		}
		return query;
	}

	private boolean constraintsValueRef(Map<String, Object> parameters2) {
		return valueRefTargetIsNotEmpty(parameters2.get(VALUE_REF_TARGET_NAMES_KEY));
	}

	private boolean valueRefTargetIsNotEmpty(Object valueRefTargetNamesParam) {
		if(valueRefTargetNamesParam instanceof String) {
			return StringUtils.isNotBlank((String)valueRefTargetNamesParam);
		} else if(valueRefTargetNamesParam instanceof Collection){
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
