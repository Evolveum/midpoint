/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.ObjectResolver;

/**
 * @author Radovan Semancik
 *
 */
public interface ExpressionEvaluator {

	public <T> T evaluateScalar(Class<T> type, Element code, Map<QName,Object> variables, ObjectResolver objectResolver, String contextDescription) 
		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException;

	public <T> List<T> evaluateList(Class<T> type, Element code, Map<QName,Object> variables, ObjectResolver objectResolver, String contextDescription) 
		throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException;

}
