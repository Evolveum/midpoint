package com.evolveum.midpoint.report.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SelectorQualifiedGetOptionsType;
import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;

@Service
public class ReportWebService implements ReportPortType, ReportPort {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ReportService reportService;


	@Override
	public ObjectListType evaluateScript(String script, RemoteReportParametersType parameters) {
		try {
			Map<QName, Object> params = getParamsMap(parameters);
			Collection resultList = reportService.evaluateScript(script,
					params);
			return createObjectListType(resultList);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}

	}

	@Override
	public AuditEventRecordListType evaluateAuditScript(String script, RemoteReportParametersType parameters) {

		try {
			Map<QName, Object> params = getParamsMap(parameters);
			Collection<AuditEventRecord> resultList = reportService.evaluateAuditScript(script, params);
			return createAuditEventRecordListType(resultList);
		} catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}

	}

	private Map<QName, Object> getParamsMap(RemoteReportParametersType parametersType) throws SchemaException {

		prismContext.adopt(parametersType);
		Map<QName, Object> parametersMap = new HashMap<>();
		if (parametersType == null || parametersType.getRemoteParameter() == null
				|| parametersType.getRemoteParameter().isEmpty()) {
			return parametersMap;
		}
		List<RemoteReportParameterType> items = parametersType.getRemoteParameter();
		for (RemoteReportParameterType item : items) {
			QName paramName = new QName(SchemaConstants.NS_REPORT, item.getParameterName());
			ReportParameterType param = item.getParameterValue();
			if (param == null){
				parametersMap.put(paramName, null);
				continue;
			}
			if (param.getAny().size() == 1) {
				parametersMap.put(paramName, param.getAny().get(0));
			} else {
				parametersMap.put(paramName, param.getAny());
			}

		}

		return parametersMap;


	}

	private ObjectListType createObjectListType(Collection resultList) {
		if (resultList == null) {
			return new ObjectListType();
		}

		ObjectListType results = new ObjectListType();
		int skipped = 0;
		for (Object object : resultList) {
			if (object instanceof PrismObject) {
				results.getObject().add(((PrismObject<ObjectType>) object).asObjectable());
			} else if (object instanceof ObjectType) {
				results.getObject().add((ObjectType) object);
			} else {
				skipped++;
			}
		}
		if (skipped > 0) {
			LOGGER.warn("{} non-PrismObject data objects not returned, as these are not supported by ReportWebService yet", skipped);
		}

		return results;
	}

	private AuditEventRecordListType createAuditEventRecordListType(Collection<AuditEventRecord> resultList) {
		if (resultList == null) {
			return new AuditEventRecordListType();
		}

		AuditEventRecordListType results = new AuditEventRecordListType();
		for (AuditEventRecord auditRecord : resultList) {
			results.getObject().add(auditRecord.createAuditEventRecordType(true));
		}

		return results;
	}


	@Override
	public ObjectListType processReport(String query, RemoteReportParametersType parameters,
			SelectorQualifiedGetOptionsType options) {

		try {

			Map<QName, Object> parametersMap = getParamsMap(parameters);
			ObjectQuery q = reportService.parseQuery(query, parametersMap);
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.searchObjects(q,
					MiscSchemaUtil.optionsTypeToOptions(options));

			return createObjectListType(resultList);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}

	}

}
