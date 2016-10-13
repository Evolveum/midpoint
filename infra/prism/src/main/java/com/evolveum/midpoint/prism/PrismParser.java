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
import org.jetbrains.annotations.Nullable;
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

	/**
	 * Sets the language of the parser. null means auto-detect.
	 * @param language The language
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser language(@Nullable String language);

	/**
	 * Sets the language of the parser to be XML.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser xml();

	/**
	 * Sets the language of the parser to be JSON.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser json();

	/**
	 * Sets the language of the parser to be YAML.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser yaml();

	/**
	 * Provides a parsing context for the parser. The context contains e.g. mode of operations (set by client)
	 * or a list of warnings (maintained by the parser).
	 * @param context The parsing context.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser context(@NotNull ParsingContext context);

	/**
	 * Switches the parser into "strict" parsing mode.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser strict();

	/**
	 * Switches the parser into "compatibility" (or relaxed) parsing mode.
	 * TODO description here
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser compat();

	/**
	 * Parses the input as a prism object.
	 * @return The object.
	 */
	@NotNull
	<O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException;

	/**
	 * Parses the input as a collection of prism objects.
	 * Currently supported only for XML. (For the time being, it is used only in tests.)
	 * @return A list of objects.
	 */
	@NotNull
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException;

	/**
	 * Parses the input as a single value of a prism container.
	 * @param clazz Type of a container content.
	 * @return Single-valued container.
	 */
	@NotNull
	<C extends Containerable> PrismContainer<C> parseContainer(@NotNull Class<C> clazz) throws SchemaException, IOException;

	/**
	 * Parses the input as a single value of a prism container.
	 * @param definition Container definition.
	 * @return Single-valued container.
	 */
	@NotNull
	<C extends Containerable> PrismContainer<C> parseContainer(@NotNull PrismContainerDefinition<C> definition) throws SchemaException, IOException;

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
