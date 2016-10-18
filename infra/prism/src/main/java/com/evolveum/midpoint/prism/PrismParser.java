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

import com.evolveum.midpoint.prism.xnode.RootXNode;
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
 * Parses a given input into prism or POJO objects.
 *
 * The interface is pretty straightforward; only two things are of interest:
 * 1. how to determine the type of data to be retrieved,
 * 2. how to determine the name of the item that is to be created (in case of prism items).
 *
 * For most cases, this data can be determined from the input. E.g. if we are parsing a prism object that is rooted at the
 * "user" XML element, it is clear that the type is c:UserType and the name is c:user. In other cases, the algorithms
 * are the following:
 *
 * Data type determination: We collect all the available data, i.e.
 *    - source data (xsi:type/@type),
 *    - itemDefinition,
 *    - itemName,
 *    - typeName,
 *    - typeClass
 *    and take the most specific of these.
 *
 * Data name determination: First name that is present takes precedence:
 *  1. itemName
 *  2. source data (if namespace is missing, it is filled from item definition)
 *  3. name from itemDefinition
 *  4. name from item definition derived from type name
 *  5. name from item definition derived from type class
 *
 * @author mederly
 */
public interface PrismParser {

	/**
	 * For string inputs: sets the data language that the parser will try to parse; null means auto-detect.
	 * For other kinds of input (DOM and XNode) the language is fixed to XML or none, respectively.
	 *
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
	 * Tells parser which definition to use when parsing item (or an item value). Optional.
	 * @param itemDefinition The definition
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser definition(ItemDefinition<?> itemDefinition);

	/**
	 * Tells parser what name to use for parsed item. Optional.
	 * @param itemName Item name to use.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser name(QName itemName);

	/**
	 * Tells parser what data type to expect. Optional.
	 * @param typeName Data type to expect.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser type(QName typeName);

	/**
	 * Tells parser what data type to expect. Optional.
	 * @param typeClass Data type to expect.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser type(Class<?> typeClass);

	/**
	 * Parses the input as a prism object.
	 * @return The object.
	 */
	@NotNull
	<O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException;

	/**
	 * Parses the input as a prism item. (Object, container, reference, value.)
	 * @return The item.
	 */
	<IV extends PrismValue, ID extends ItemDefinition> Item<IV,ID> parseItem() throws SchemaException, IOException;

	/**
	 * Parses the input as a prism value. (Container value, reference value, property value.)
	 * @return The item.
	 */
	<IV extends PrismValue> IV parseItemValue() throws SchemaException, IOException;

	/**
	 * Parses a real value - either property or container value.
	 * @param clazz Expected class of the data (can be Object.class when unknown).
	 * @return Real value - POJO, Containerable or Objectable.
	 */
	<T> T parseRealValue(Class<T> clazz) throws IOException, SchemaException;

	/**
	 * Parses a real value with an unknown class.
	 * @return Real value - POJO, Containerable or Objectable.
	 */
	<T> T parseRealValue() throws IOException, SchemaException;

	/**
	 * Parses a real value and stores it into JAXBElement, using item name derived in the usual way.
	 */
	<T> JAXBElement<T> parseRealValueToJaxbElement() throws IOException, SchemaException;

	/**
	 * Parses the input into RootXNode.
	 * @return RootXNode corresponding to the input.
	 */
	RootXNode parseToXNode() throws IOException, SchemaException;

	// ============= other methods (convenience ones, deprecated ones etc) =============

	/**
	 * Parses the input as a collection of prism objects.
	 * Currently supported only for XML. (For the time being, it is used only in tests.)
	 * @return A list of objects.
	 */
	@NotNull
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException;

//	/**
//	 * Parses the input as a single value of a prism container.
//	 * @return Single-valued container.
//	 */
//	@NotNull
//	<C extends Containerable> PrismContainer<C> parseContainer() throws SchemaException, IOException;

//	/**
//	 * Parses an atomic value - i.e. something that could present a property
//	 * value, if such a property would exist.
//	 */
//	<T> T parseAtomicValue(QName typeName) throws IOException, SchemaException;


	/**
	 * Parses (almost) anything: either an item with a definition, or an atomic (i.e. property-like) value.
	 * Does not care for schemaless items!
	 *
	 * CAUTION: EXPERIMENTAL - Avoid using this method if at all possible.
	 * Its result is not well defined, namely, whether it returns Item or a value.
	 *
	 * Used for scripting and wf-related data serialization. To be replaced.
	 *
	 * @return either Item or an unmarshalled bean value
	 */
	@Deprecated
	Object parseAnyData() throws IOException, SchemaException;

}
