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

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AsIsValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LiteralValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionXType;

/**
 * @author Radovan Semancik
 *
 */
public class ValueConstructionFactory {

	ObjectFactory objectFactory = new ObjectFactory();
	
	private Map<QName,ValueConstructor> constructors;
	
	public ValueConstructionFactory() {
		constructors = new HashMap<QName, ValueConstructor>();
	}
	
	public void initialize() {
		createLiteralConstructor();
		createAsIsConstructor();
	}

	private void createLiteralConstructor() {
		ValueConstructor constructor = new LiteralValueConstructor();
		JAXBElement<LiteralValueConstructorType> element = objectFactory.createLiteral(objectFactory.createLiteralValueConstructorType());
		constructors.put(element.getName(), constructor);
	}

	private void createAsIsConstructor() {
		ValueConstructor constructor = new AsIsValueConstructor();
		JAXBElement<AsIsValueConstructorType> element = objectFactory.createAsIs(objectFactory.createAsIsValueConstructorType());
		constructors.put(element.getName(), constructor);
	}

	public ValueConstruction createValueConstruction(ValueConstructionXType valueConstructionType, PropertyDefinition outputDefinition, String shortDesc) {
		ValueConstruction construction = new ValueConstruction(valueConstructionType, outputDefinition, shortDesc, constructors);
		return construction;
	}
	
}
