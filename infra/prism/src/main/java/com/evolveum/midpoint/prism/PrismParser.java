/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.List;

/**
 * Parses a given input into prism or POJO objects.
 *
 * The interface is pretty straightforward; only two things are of interest:
 * 1. how to determine the type of data to be retrieved,
 * 2. how to determine the name of the item that is to be created (in case of prism items).
 *
 * For most cases, both can be determined from the input. E.g. if we are parsing a prism object that is rooted at the
 * "user" XML element, it is clear that the type is c:UserType and the name is c:user. In other cases, the algorithms
 * are the following:
 *
 * Data type determination: We collect all the available data, i.e.
 *    - explicit type specification in source data (xsi:type/@type),
 *    - itemDefinition provided by the caller,
 *    - item name in source data,
 *    - itemName provided by the caller,
 *    - typeName provided by the caller,
 *    - typeClass provided by the caller
 * and take the most specific of these. In case of conflict we report an error.
 *
 * Data name determination: First name that is present takes precedence:
 *  1. itemName
 *  2. source data (if namespace is missing, it is filled from item definition)
 *  3. name from itemDefinition
 *  4. name from item definition derived from type name
 *  5. name from item definition derived from type class
 *
 * General post-conditions: (For items as well as item values; and for all parsing methods.)
 * - All recognizable definitions are set.
 * - Prism context is set on all items and PCVs.
 * - No unresolved raw values with known types are present.
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
	 * Provides a parsing context for the parser. The context contains e.g. selection of strict/compat
	 * mode of operation (set by client) or a list of warnings (maintained by the parser).
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
	 * Tells parser what name to use for parsed item. Optional.
	 * @param itemName Item name to use.
	 * @return Updated parser.
	 */
	@NotNull
	PrismParser name(QName itemName);

	/**
	 * Parses the input as a prism object.
	 * @return The object.
	 */
	@NotNull
	<O extends Objectable> PrismObject<O> parse() throws SchemaException, IOException;

	/**
	 * Parses the input as a prism item. (Object, container, reference, value.)
	 * May return raw property values as part of the prism structure, if definitions are not known.
	 * @return The item.
	 */
	<IV extends PrismValue, ID extends ItemDefinition> Item<IV,ID> parseItem() throws SchemaException, IOException;

	/**
	 * Parses the input as a prism value. (Container value, reference value, property value.)
	 * May return raw property values as part of the prism structure, if definitions are not known.
	 * @return The item.
	 */
	<IV extends PrismValue> IV parseItemValue() throws SchemaException, IOException;

	/**
	 * Parses a real value - either property or container value.
	 * @param clazz Expected class of the data. May be null if unknown.
	 * @return Real value - POJO, Containerable, Objectable or Referencable.
	 */
	<T> T parseRealValue(@Nullable Class<T> clazz) throws IOException, SchemaException;

	/**
	 * Parses a real value. The class is not supplied by the caller, so it has to be determined from the data source.
	 * @return Real value - POJO, Containerable, Objectable or Referencable.
	 */
	<T> T parseRealValue() throws IOException, SchemaException;

	/**
	 * Parses a real value and stores it into JAXBElement, using item name derived in the usual way.
	 */
	<T> JAXBElement<T> parseRealValueToJaxbElement() throws IOException, SchemaException;

	/**
	 * Parses the input into RootXNode. This is a bit unusual approach, skipping the unmarshalling phase altogether.
	 * But it is useful at some places.
	 * @return RootXNode corresponding to the input.
	 */
	RootXNode parseToXNode() throws IOException, SchemaException;

	/**
	 * Parses either an item, or a real value. It depends on the type declaration or item name in the source data.
	 * 1) If explicit type is present, it is taken into account. If it corresponds to a prism item, the input is parsed
	 *    as a prism item. Otherwise, it is parsed as a real value (containerable or simple POJO), if possible.
	 * 2) If there is no type, the item name is consulted. If it corresponds to a prism item, the input is parsed
	 *    as a prism item. Otherwise, an exception is thrown.
	 *
	 * Pre-set parameters (itemDefinition, typeName, itemName) must NOT be present.
	 *
	 * Use with utmost care. If at all possible, avoid it.
	 *
	 * @return either prism item (Item) or a real value (Object)
	 */
	@Deprecated
	Object parseItemOrRealValue() throws IOException, SchemaException;

	/**
	 * Parses the input as a collection of prism objects.
	 * For XML the input must be formatted as a collection of objects (TODO change this).
	 * For other languages the input may contain one or more objects.
	 *
	 * @return A list of objects.
	 */
	@NotNull
	List<PrismObject<? extends Objectable>> parseObjects() throws SchemaException, IOException;

	interface ObjectHandler {
		/**
		 * Called when an object was successfully retrieved from the input.
		 * @return true if the processing should continue
		 */
		boolean handleData(PrismObject<?> object);

		/**
		 * Called when an object could not be successfully retrieved from the input.
		 * No data is provided; instead a Throwable is given to the caller.
		 * @return true if the processing should continue
		 */
		boolean handleError(Throwable t);
	}

	void parseObjectsIteratively(@NotNull ObjectHandler handler) throws SchemaException, IOException;

	// ============= other methods (convenience ones, deprecated ones etc) =============

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


}
