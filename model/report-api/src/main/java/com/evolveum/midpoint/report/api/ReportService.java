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
package com.evolveum.midpoint.report.api;

import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
	
	String PARAMETER_REPORT_SERVICE = "reportService";
	
	ObjectQuery parseQuery(String query, Map<QName, Object> parameters) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException;
	
	Collection<PrismObject<? extends ObjectType>> searchObjects(ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException;
	
	Collection<PrismContainerValue<? extends Containerable>> evaluateScript(String script, Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException;

	Collection<AuditEventRecord> evaluateAuditScript(String script, Map<QName, Object> parameters) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException;
}
