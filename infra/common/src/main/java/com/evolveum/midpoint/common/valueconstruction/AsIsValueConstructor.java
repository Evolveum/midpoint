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

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AsIsValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LiteralValueConstructorType;

/**
 * @author Radovan Semancik
 *
 */
public class AsIsValueConstructor implements ValueConstructor {
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.valueconstruction.ValueConstructor#construct(com.evolveum.midpoint.schema.processor.PropertyDefinition, com.evolveum.midpoint.schema.processor.Property)
	 */
	@Override
	public Property construct(JAXBElement<?> constructorElement, PropertyDefinition outputDefinition, Property input, String contextDescription) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		Object contstuctorTypeObject = constructorElement.getValue();
		if (!(contstuctorTypeObject instanceof AsIsValueConstructorType)) {
			throw new IllegalArgumentException("AsIs value constructor cannot handle elements of type "+contstuctorTypeObject.getClass().getName());
		}
		AsIsValueConstructorType constructorType = (AsIsValueConstructorType)contstuctorTypeObject;
		// Nothing to do, just use input
		return input;
	}

}
