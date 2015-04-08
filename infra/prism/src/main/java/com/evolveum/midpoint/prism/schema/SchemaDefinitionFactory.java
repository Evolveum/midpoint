/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.prism.schema;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
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
		return new ComplexTypeDefinition(typeName, prismContext);
	}
	
	public <T> PrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismPropertyDefinition<T>(elementName, typeName, prismContext);
	}
	
	public <T> PrismPropertyDefinition<T> createPropertyDefinition(QName elementName, QName typeName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) throws SchemaException {
		return new PrismPropertyDefinition<T>(elementName, typeName, prismContext, allowedValues, defaultValue);
	}
	
	public PrismReferenceDefinition createReferenceDefinition(QName primaryElementName, QName typeName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismReferenceDefinition(primaryElementName, typeName, prismContext);
	}
	
	public <C extends Containerable> PrismContainerDefinition<C> createContainerDefinition(QName elementName, ComplexTypeDefinition complexTypeDefinition,
			PrismContext prismContext, XSAnnotation annotation, XSParticle elementParticle) throws SchemaException {
		return new PrismContainerDefinition<C>(elementName, complexTypeDefinition, prismContext);
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
	public <C extends Containerable> PrismContainerDefinition<C> createExtraDefinitionFromComplexType(XSComplexType complexType,
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
