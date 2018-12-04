/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.soap.Detail;
import java.io.Serializable;

/**
 * TEMPORARY.
 *
 * This interface belongs to a coursebook on Software Engineering as a horrific design example ;)
 *
 * Prism API and/or client code should be modified to get rid of these hacks.
 */
public interface Miscellaneous {

	@Nullable
	Serializable guessFormattedValue(Serializable value) throws SchemaException;

	void serializeFaultMessage(Detail detail, Object faultInfo, QName faultMessageElementName, Trace logger);

	<T> void setPrimitiveXNodeValue(PrimitiveXNode<T> node, T value, QName typeName);

	void putToMapXNode(MapXNode map, QName key, XNode value);

	<T> void parseProtectedType(ProtectedDataType<T> protectedType, MapXNode xmap, PrismContext prismContext, ParsingContext pc) throws SchemaException;

	Element serializeSingleElementMapToElement(MapXNode filterClauseXNode) throws SchemaException;

	void setXNodeType(XNode node, QName explicitTypeName, boolean explicitTypeDeclaration);

	void addToDefinition(ComplexTypeDefinition ctd, ItemDefinition other);

	void replaceDefinition(ComplexTypeDefinition ctd, ItemName name, ItemDefinition other);
}
