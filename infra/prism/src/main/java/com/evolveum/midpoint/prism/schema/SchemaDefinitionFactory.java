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

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSParticle;

/**
 * @author semancik
 *
 */
public class SchemaDefinitionFactory {

	public ComplexTypeDefinition createComplexTypeDefinition(XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		
		QName typeName = new QName(complexType.getTargetNamespace(),complexType.getName());
		return new ComplexTypeDefinition(null, typeName, prismContext);
	}
	
	public PrismPropertyDefinition createPropertyDefinition(QName elementName, QName typeName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismPropertyDefinition(elementName, elementName, typeName, prismContext);
	}
	
	public PrismReferenceDefinition createReferenceDefinition(QName primaryElementName, QName typeName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismReferenceDefinition(primaryElementName, primaryElementName, typeName, prismContext);
	}
	
	public PrismContainerDefinition createContainerDefinition(QName elementName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismContainerDefinition(elementName, complexTypeDefinition, prismContext);
	}

	public <T extends Objectable> PrismObjectDefinition<T> createObjectDefinition(QName elementName,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, Class<T> compileTimeClass,
			XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismObjectDefinition<T>(elementName, complexTypeDefinition, prismContext, compileTimeClass );
	}

	/**
	 * Create optional extra definition form a top-level complex type definition.
	 * This is used e.g. to create object class definitions in midPoint
	 */
	public PrismContainerDefinition createExtraDefinitionFromComplexType(XSComplexType complexType,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, 
			XSAnnotation annotation) throws SchemaException {
		// Create nothing by default
		return null;
	}

	/**
	 * Called after the complex type definition is filled with items. It may be used to finish building
	 * the definition, e.g. by adding data that depends on existing internal definitions.
	 */
	public void finishComplexTypeDefinition(ComplexTypeDefinition complexTypeDefinition, XSComplexType complexType,
			PrismContext prismContext, XSAnnotation annotation) throws SchemaException {
		// Nothing to do by default
	}

	/**
	 * Add extra annotations to a complexType DOM model. Used when serializing schema to DOM.
	 */
	public void addExtraComplexTypeAnnotations(ComplexTypeDefinition definition, Element appinfo, SchemaToDomProcessor schemaToDomProcessor) {
		// Nothing to do by default
	}

	/**
	 * Add extra annotations to a property DOM model. Used when serializing schema to DOM.
	 */
	public void addExtraPropertyAnnotations(PrismPropertyDefinition definition, Element appinfo, SchemaToDomProcessor schemaToDomProcessor) {
		// Nothing to do by default
	}

	/**
	 * Add extra annotations to a reference DOM model. Used when serializing schema to DOM.
	 */
	public void addExtraReferenceAnnotations(PrismReferenceDefinition definition, Element appinfo, SchemaToDomProcessor schemaToDomProcessor) {
		// Nothing to do by default
	}
	
}
