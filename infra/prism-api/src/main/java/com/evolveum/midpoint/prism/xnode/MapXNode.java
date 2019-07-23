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

package com.evolveum.midpoint.prism.xnode;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 *  Note we cannot use "extends Map" here, because in that case we would have to declare XNodeImpl as map value parameter.
 */
public interface MapXNode extends XNode, Serializable, DebugDumpable {

	boolean containsKey(Object key);
	boolean containsValue(Object value);
	XNode get(Object key);

	boolean isEmpty();

	@NotNull
	MapXNode clone();

	int size();

	Set<QName> keySet();

	RootXNode getEntryAsRoot(@NotNull QName key);

	Map.Entry<QName, ? extends XNode> getSingleSubEntry(String errorContext) throws SchemaException;

	RootXNode getSingleSubEntryAsRoot(String errorContext) throws SchemaException;

	// EXPERIMENTAL
	Map<QName, ? extends XNode> asMap();
}
