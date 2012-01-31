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
package com.evolveum.midpoint.common.valueconstruction;

import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author Radovan Semancik
 * 
 */
public interface ValueConstructor {

	Property construct(JAXBElement<?> constructorElement, PropertyDefinition outputDefinition, PropertyPath propertyParentPath,
			Property input, Map<QName, Object> variables, String contextDescription, OperationResult result) throws SchemaException,
			ExpressionEvaluationException, ObjectNotFoundException;

}
