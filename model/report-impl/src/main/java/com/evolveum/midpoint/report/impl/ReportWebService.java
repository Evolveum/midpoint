package com.evolveum.midpoint.report.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Fault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.report.api.ReportPort;
import com.evolveum.midpoint.report.api.ReportService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.SelectorQualifiedGetOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParameterType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.RemoteReportParametersType;
import com.evolveum.midpoint.xml.ns._public.report.report_3.ReportPortType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@Service
public class ReportWebService implements ReportPortType, ReportPort {

	private static transient Trace LOGGER = TraceManager.getTrace(ReportWebService.class);

	@Autowired(required = true)
	private PrismContext prismContext;

	@Autowired(required = true)
	private ReportService reportService;

	@Override
	public QueryType parseQuery(String query, RemoteReportParametersType parametersType) {

		// Map<QName, Object> params = getParamsMap(parameters);

		try {
			Map<QName, Object> parametersMap = getParamsMap(parametersType);

			ObjectQuery q = reportService.parseQuery(query, parametersMap);
			SearchFilterType filterType = QueryConvertor.createSearchFilterType(q.getFilter(), prismContext);
			QueryType queryType = new QueryType();
			queryType.setFilter(filterType);
			return queryType;
			// return prismContext.serializeAtomicValue(filterType,
			// SearchFilterType.COMPLEX_TYPE, PrismContext.LANG_XML);
		} catch (SchemaException | ObjectNotFoundException | ExpressionEvaluationException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}

	}

	@Override
	public ObjectListType searchObjects(QueryType query, SelectorQualifiedGetOptionsType options) {

		try {
			// Map<QName, Object> params = getParamsMap(parameters);
			// ObjectQuery objectQuery = reportService.parseQuery(query, null);
			// GetOperationOptions getOpts = GetOperationOptions.createRaw();
			// getOpts.setResolveNames(Boolean.TRUE);
			// SearchFilterType filterType =
			// prismContext.parseAtomicValue(query,
			// SearchFilterType.COMPLEX_TYPE);
			// ObjectFilter filter = QueryConvertor.parseFilter(query,
			// UserType.class, prismContext);
			ObjectQuery objectQuery = QueryJaxbConvertor.createObjectQuery(UserType.class, query,
					prismContext);

			Collection<PrismObject<? extends ObjectType>> resultList = reportService.searchObjects(
					objectQuery, MiscSchemaUtil.optionsTypeToOptions(options));

			return createObjectListType(resultList);
		} catch (SchemaException | ObjectNotFoundException | SecurityViolationException
				| CommunicationException | ConfigurationException e) {
			// TODO Auto-generated catch block
			throw new Fault(e);
		}
	}

	@Override
	public ObjectListType evaluateScript(String script, RemoteReportParametersType parameters) {
		try {
			Map<QName, Object> params = getParamsMap(parameters);
			Collection<PrismObject<? extends ObjectType>> resultList = reportService.evaluateScript(script,
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

	private ObjectListType createObjectListType(Collection<PrismObject<? extends ObjectType>> resultList) {
		if (resultList == null) {
			return new ObjectListType();
		}

		ObjectListType results = new ObjectListType();
		for (PrismObject<? extends ObjectType> prismObject : resultList) {
			results.getObject().add(prismObject.asObjectable());
		}

		return results;
	}

	private AuditEventRecordListType createAuditEventRecordListType(Collection<AuditEventRecord> resultList) {
		if (resultList == null) {
			return new AuditEventRecordListType();
		}

		AuditEventRecordListType results = new AuditEventRecordListType();
		for (AuditEventRecord auditRecord : resultList) {
			results.getObject().add(auditRecord.createAuditEventRecordType());
		}

		return results;
	}

	@Override
	public ReportParameterType getFieldValue(String parameterName, ObjectType object) {
		try {
			prismContext.adopt(object);
		} catch (SchemaException e) {
			throw new Fault(e);
		}

		PrismObject<? extends ObjectType> prismObject = object.asPrismObject();

		QName itemName = QNameUtil.uriToQName(parameterName);

		Item i = prismObject.findItem(itemName);
		if (i == null) {
			return null;
			// throw new JRException("Object of type " +
			// currentObject.getCompileTimeClass().getSimpleName() +
			// " does not contain field " + fieldName +".");
		}

		ReportParameterType param = new ReportParameterType();

		if (i instanceof PrismProperty) {

			if (i.isSingleValue()) {
				param.getAny().add(
						new JAXBElement(i.getElementName(), ((PrismProperty) i).getValueClass(),
								((PrismProperty) i).getRealValue()));
			} else {
				for (Object o : ((PrismProperty) i).getRealValues()) {
					param.getAny().add(new JAXBElement(i.getElementName(), o.getClass(), o));
				}
			}
		} else if (i instanceof PrismReference) {
			if (i.isSingleValue()) {
				param.getAny().add(
						new JAXBElement(i.getElementName(), ObjectReferenceType.class, ((PrismReference) i)
								.getValue().asReferencable()));
			} else {
				for (PrismReferenceValue refVal : ((PrismReference) i).getValues()) {
					param.getAny().add(
							new JAXBElement(i.getElementName(), ObjectReferenceType.class, refVal
									.asReferencable()));
				}
			}
		} else if (i instanceof PrismContainer) {
			if (i.isSingleValue()) {
				param.getAny().add(
						new JAXBElement(i.getElementName(), ObjectReferenceType.class, ((PrismContainer) i)
								.getValue().asContainerable()));
			} else {
				for (Object pcv : i.getValues()) {
					if (pcv instanceof PrismContainerValue) {
						param.getAny().add(
								new JAXBElement(i.getElementName(), ObjectReferenceType.class,
										((PrismContainerValue) pcv).asContainerable()));
					}
				}
			}

		} else
			throw new Fault(new IllegalArgumentException("Could not get value of the field: " + itemName));

		// ReportParameterType param = new ReportParameterType();
		// PrismContainerValue pcv = param.asPrismContainerValue();
		// try {
		// pcv.add(i.clone());
		// } catch (SchemaException e) {
		// throw new Fault(e);
		// }

		return param;
		// return
		// throw new
		// UnsupportedOperationException("dataSource.getFiledValue() not supported");

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
