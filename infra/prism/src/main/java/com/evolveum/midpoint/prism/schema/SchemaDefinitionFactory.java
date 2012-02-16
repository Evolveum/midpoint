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
package com.evolveum.midpoint.prism.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSParticle;

/**
 * @author semancik
 *
 */
public class SchemaDefinitionFactory {
	
	PrismPropertyDefinition createPropertyDefinition(QName elementName, QName typeName, PrismContext prismContext, 
			XSAnnotation annotation, XSParticle elementParticle) {
		return new PrismPropertyDefinition(elementName, elementName, typeName, prismContext);
	}
	
	PrismReferenceDefinition createReferenceDefinition(QName primaryElementName, QName typeName, PrismContext prismContext, 
			XSAnnotation annotation, XSParticle elementParticle) {
		return new PrismReferenceDefinition(primaryElementName, primaryElementName, typeName, prismContext);
	}
	
	PrismContainerDefinition createContainerDefinition(QName elementName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) {
		return new PrismContainerDefinition(elementName, complexTypeDefinition, prismContext);
	}

	public PrismObjectDefinition createObjectDefinition(QName elementName,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, Class compileTimeClass,
			XSAnnotation annotation, XSParticle elementParticle) {
		return new PrismObjectDefinition(elementName, complexTypeDefinition, prismContext, compileTimeClass );
	}
	
}
