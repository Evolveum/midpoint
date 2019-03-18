/*
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

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Experimental
public interface UcfExpressionEvaluator {

	/**
	 * Evaluates given expression.
	 */
	<O> List<O> evaluate(ExpressionType expressionBean, Map<QName, Object> variables, QName outputPropertyName,
			String contextDescription)
			throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException;
}
