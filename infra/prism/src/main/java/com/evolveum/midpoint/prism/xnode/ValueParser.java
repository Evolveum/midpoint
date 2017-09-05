/*
 * Copyright (c) 2014 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Map;

/**
 * Objects of type ValueParser should be immutable. (E.g. when cloning PrimitiveXNode, they are copied, not cloned.)
 *
 * @param <T>
 */

public interface ValueParser<T> {

	T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException;

	// This has to work even without the type
	boolean isEmpty();

	/**
	 * Returns the value represented as string - in the best format that we can.
	 * This has to work even without knowing the exact data type. Therefore
	 * there is no guarantee that the returned value will be precise.
	 * This method is used as a "last instance" if everything else fails.
	 * Invocation of this method will not change the state of the xnode, e.g.
	 * it will NOT cause it to be parsed. It can be invoked without any side effects.
	 */
	String getStringValue();

    /**
     * Returns namespaces that could be relevant when serializing unparsed string value
     * of this item. Used to preserve xmlns declarations for QNames and ItemPaths in
     * unparsed data. (MID-2196)
     *
     * @return May return null if not supported or no namespace declarations are present.
     */
    Map<String,String> getPotentiallyRelevantNamespaces();
}
