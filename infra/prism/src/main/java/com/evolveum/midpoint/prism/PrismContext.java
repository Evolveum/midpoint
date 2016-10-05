/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.JaxbDomHack;
import com.evolveum.midpoint.prism.marshaller.PrismBeanConverter;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessor;
import com.evolveum.midpoint.prism.parser.dom.DomParser;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.schema.SchemaDefinitionFactory;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author semancik
 * @author mederly
 */
public interface PrismContext {

	String LANG_XML = "xml";
	String LANG_JSON = "json";
	String LANG_YAML = "yaml";

	void initialize() throws SchemaException, SAXException, IOException;

	SchemaRegistry getSchemaRegistry();

	XNodeProcessor getXnodeProcessor();

	DomParser getParserDom();

	PrismBeanConverter getBeanConverter();

	JaxbDomHack getJaxbDomHack();

	SchemaDefinitionFactory getDefinitionFactory();

	PolyStringNormalizer getDefaultPolyStringNormalizer();

	Protector getDefaultProtector();

	PrismMonitor getMonitor();

	void setMonitor(PrismMonitor monitor);

	//region Parsing
	PrismParser parserFor(File file);
	PrismParser parserFor(InputStream stream);
	PrismParserNoIO parserFor(String data);
	@Deprecated PrismParserNoIO parserFor(Element element);

	@Deprecated	// user parserFor + parse instead
	<T extends Objectable> PrismObject<T> parseObject(File file) throws SchemaException, IOException;

	@Deprecated	// user parserFor + parse instead
	<T extends Objectable> PrismObject<T> parseObject(String dataString) throws SchemaException;
	//endregion

	//region Adopt methods
	<T extends Objectable> void adopt(PrismObject<T> object, Class<T> declaredType) throws SchemaException;

	<T extends Objectable> void adopt(PrismObject<T> object) throws SchemaException;

	void adopt(Objectable objectable) throws SchemaException;

	void adopt(Containerable containerable) throws SchemaException;

	void adopt(PrismContainerValue value) throws SchemaException;

	<T extends Objectable> void adopt(ObjectDelta<T> delta) throws SchemaException;

	<C extends Containerable, O extends Objectable> void adopt(C containerable, Class<O> type, ItemPath path) throws SchemaException;

	<C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, Class<O> type,
			ItemPath path) throws SchemaException;

	<C extends Containerable, O extends Objectable> void adopt(PrismContainerValue<C> prismContainerValue, QName typeName,
			ItemPath path) throws SchemaException;
	//endregion

	//region Serializing objects, containers, atomic values (properties)

	PrismSerializer<String> serializerFor(String language);
	PrismSerializer<String> xmlSerializer();
	PrismSerializer<String> jsonSerializer();
	PrismSerializer<String> yamlSerializer();
	PrismSerializer<Element> domSerializer();
	PrismSerializer<XNode> xnodeSerializer();

	<O extends Objectable> String serializeObjectToString(PrismObject<O> object, String language) throws SchemaException;

	boolean canSerialize(Object value);

	@Deprecated
	Element serializeValueToDom(PrismValue pval, QName elementName, Document document) throws SchemaException;

	RawType toRawType(Item item) throws SchemaException;

	<T extends Objectable> PrismObject<T> createObject(Class<T> clazz) throws SchemaException;

	<T extends Objectable> T createObjectable(Class<T> clazz) throws SchemaException;

}
