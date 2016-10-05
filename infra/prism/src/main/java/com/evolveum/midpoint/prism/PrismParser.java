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

import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @author mederly
 */
public interface PrismParser {

	PrismParser language(String language);
	PrismParser xml();
	PrismParser json();
	PrismParser yaml();
	PrismParser context(@NotNull ParsingContext context);
	PrismParser strict();
	PrismParser compat();

	<O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException;
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException;
	<C extends Containerable> PrismContainer<C> parseContainer(Class<C> clazz) throws SchemaException, IOException;
	<C extends Containerable> PrismContainer<C> parseContainer(PrismContainerDefinition<C> definition) throws SchemaException, IOException;

	/**
	 * Parses an atomic value - i.e. something that could present a property
	 * value, if such a property would exist.
	 */
	<T> T parseAtomicValue(QName typeName) throws IOException, SchemaException;

	/**
	 * Parses (almost) anything: either an item with a definition, or an atomic (i.e. property-like) value.
	 * Does not care for schemaless items!
	 *
	 * CAUTION: EXPERIMENTAL - Avoid using this method if at all possible.
	 * Its result is not well defined, namely, whether it returns Item or a value.
	 *
	 * @return either Item or an unmarshalled bean value
	 * @throws SchemaException
	 */
	Object parseAnyData() throws IOException, SchemaException;

	/**
	 * Emulates JAXB unmarshal method.
	 *
	 * TODO
	 *
	 * @return
	 * @throws SchemaException
	 */
	<T> T parseAnyValue() throws IOException, SchemaException;

	// experimental!
	<T> JAXBElement<T> parseAnyValueAsJAXBElement() throws IOException, SchemaException;

	XNode parseToXNode() throws IOException, SchemaException;
}
