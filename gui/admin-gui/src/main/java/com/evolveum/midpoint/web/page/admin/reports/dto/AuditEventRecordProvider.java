package com.evolveum.midpoint.web.page.admin.reports.dto;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.*;

/**
 * Created by honchar.
 */
public class AuditEventRecordProvider extends BaseSortableDataProvider<AuditEventRecordType> {
	private IModel<List<AuditEventRecordType>> model;

	private String auditEventQuery;
	private Map<String, Object> parameters = new HashMap<>();

	private static final String AUDIT_RECORDS_QUERY_CORE = "from RAuditEventRecord as aer where 1=1 and ";
	private static final String AUDIT_RECORDS_QUERY_COUNT = "select count(*) ";
	private static final String AUDIT_RECORDS_ORDER_BY = " order by aer.timestamp asc";
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
		Map<String, Object> queryParameters = getParameters();
		if (queryParameters.containsKey(SET_FIRST_RESULT_PARAMETER)){
			queryParameters.remove(SET_FIRST_RESULT_PARAMETER);
		}
		queryParameters.put(SET_FIRST_RESULT_PARAMETER, ((Long) first).intValue());
		if (queryParameters.containsKey(SET_MAX_RESULTS_PARAMETER)){
			queryParameters.remove(SET_MAX_RESULTS_PARAMETER);
		}
		queryParameters.put(SET_MAX_RESULTS_PARAMETER, ((Long) count).intValue());

		List<AuditEventRecordType> recordsList = listRecords(auditEventQuery, true);
		return recordsList.iterator();
	}


	@Override
	protected int internalSize(){
		// Map<String, Object> queryParameters = getParameters();
		String query = generateFullQuery(AUDIT_RECORDS_QUERY_COUNT + auditEventQuery, false);
		/*System.out.println(query);
		for (Map.Entry<String, Object> entry : parameters.entrySet())
		{
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}*/
		long count = getAuditService().countObjects(query, parameters);

		return ((Long)count).intValue();
	}

	private List<AuditEventRecordType> listRecords(String query, boolean orderBy){
		// Map<String, Object> queryParameters = getParameters();
		String parameterQuery = generateFullQuery(query, orderBy);
		System.out.println(parameterQuery);
		for (Map.Entry<String, Object> entry : parameters.entrySet())
		{
			System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		List<AuditEventRecord> auditRecords = getAuditService().listRecords(parameterQuery, parameters);
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

	private String generateFullQuery(String query, boolean orderBy){
		Map<String, Object> queryParameters = getParameters();
		if (queryParameters.get("from") != null) {
			query += "(aer.timestamp >= :from) and ";
		} else {
			queryParameters.remove("from");
		}
		if (queryParameters.get("to") != null) {
			query += "(aer.timestamp <= :to) and ";
		} else {
			queryParameters.remove("to");
		}
		if (queryParameters.get("eventType") != null) {
			query += "(aer.eventType = :eventType) and ";
		} else {
			queryParameters.remove("eventType");
		}
		if (queryParameters.get("eventStage") != null) {
			query += "(aer.eventStage = :eventStage) and ";
		} else {
			queryParameters.remove("eventStage");
		}
		if (queryParameters.get("outcome") != null) {
			query += "(aer.outcome = :outcome) and ";
		} else {
			queryParameters.remove("outcome");
		}
		if (queryParameters.get("initiatorName") != null) {
			query += "(aer.initiatorName = :initiatorName) and ";
		} else {
			queryParameters.remove("initiatorName");
		}
		if (queryParameters.get("targetName") != null) {
			query += "(aer.targetName = :targetName) and ";
		} else {
			queryParameters.remove("targetName");
		}
		if (queryParameters.get("channel") != null) {
			query += "(aer.channel = :channel) and ";
		} else {
			queryParameters.remove("channel");
		}
		if (queryParameters.get("hostIdentifier") != null) {
			query += "(aer.hostIdentifier = :hostIdentifier) and ";
		} else {
			queryParameters.remove("hostIdentifier");
		}
		if (queryParameters.get("targetOwnerName") != null) {
			query += "(aer.targetOwnerName = :targetOwnerName) and ";
		} else {
			queryParameters.remove("targetOwnerName");
		}
		if (queryParameters.get("targetOwnerName") != null) {
			query += "(aer.targetOwnerName = :targetOwnerName) and ";
		} else {
			queryParameters.remove("targetOwnerName");
		}
		query = query.substring(0, query.length()-5); // remove trailing " and "
		if (orderBy){
			query +=  AUDIT_RECORDS_ORDER_BY;
		}
		parameters = queryParameters;
		return query;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}
}
