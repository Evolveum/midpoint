package com.evolveum.midpoint.report.api;

import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public interface ReportService {
	
	public static final String PARAMETER_REPORT_SERVICE = "reportService";
	
	public ObjectQuery parseQuery(String query, Map<QName, Object> parameters) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException;
	
	public Collection<PrismObject<? extends ObjectType>> searchObjects(ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException;
	
	public Collection<PrismObject<? extends ObjectType>> evaluateScript(String script, Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException;

	
	
}
