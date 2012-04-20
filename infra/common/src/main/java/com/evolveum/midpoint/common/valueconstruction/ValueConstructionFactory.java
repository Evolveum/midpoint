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

import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AsIsValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenerateValueConstructorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;

/**
 * @author Radovan Semancik
 *
 */
public class ValueConstructionFactory {

	ObjectFactory objectFactory = new ObjectFactory();
	
	private Map<QName,ValueConstructor> constructors;
	private ExpressionFactory expressionFactory;
	private Protector protector;
	
	public ValueConstructionFactory() {
		constructors = null;
	}
	
	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}
	
	public Protector getProtector() {
		return protector;
	}

	public void setProtector(Protector protector) {
		this.protector = protector;
	}

	private void initialize() {
		constructors = new HashMap<QName, ValueConstructor>();
		createLiteralConstructor();
		createAsIsConstructor();
		createExpressionConstructor();
		createGenerateConstructor();
	}

	private void createLiteralConstructor() {
		ValueConstructor constructor = new LiteralValueConstructor();
		JAXBElement<Object> element = objectFactory.createValue(null);
		constructors.put(element.getName(), constructor);
	}

	private void createAsIsConstructor() {
		ValueConstructor constructor = new AsIsValueConstructor();
		JAXBElement<AsIsValueConstructorType> element = objectFactory.createAsIs(objectFactory.createAsIsValueConstructorType());
		constructors.put(element.getName(), constructor);
	}
	
	private void createExpressionConstructor() {
		ValueConstructor constructor = new ExpressionValueConstructor(expressionFactory);
		JAXBElement<ExpressionType> element = objectFactory.createExpression(objectFactory.createExpressionType());
		constructors.put(element.getName(), constructor);
	}
	
	private void createGenerateConstructor() {
		ValueConstructor constructor = new GenerateValueConstructor(protector);
		JAXBElement<GenerateValueConstructorType> element = objectFactory.createGenerate(objectFactory.createGenerateValueConstructorType());
		constructors.put(element.getName(), constructor);
	}

	public <V extends PrismValue> ValueConstruction<V> createValueConstruction(ValueConstructionType valueConstructionType, ItemDefinition outputDefinition,
			String shortDesc) {
		if (constructors == null) {
			initialize();
		}
		ValueConstruction<V> construction = new ValueConstruction<V>(valueConstructionType, outputDefinition, shortDesc, constructors, expressionFactory);
		return construction;
	}
	
}
