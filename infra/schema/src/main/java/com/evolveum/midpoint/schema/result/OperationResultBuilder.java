/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *  Used to postpone initialization of OperationResult until parameters and context items are set - in order to log
 *  operation entry correctly.
 *
 *  Operation names are not "builder-style" but let's ignore this for the moment. This is to simplify the implementation;
 *  can be fixed later.
 */
public interface OperationResultBuilder {
	OperationResult build();

	OperationResultBuilder addParam(String name, String value);

	OperationResult addParam(String name, PrismObject<? extends ObjectType> value);

	OperationResultBuilder addParam(String name, ObjectType value);

	OperationResultBuilder addParam(String name, boolean value);

	OperationResultBuilder addParam(String name, long value);

	OperationResultBuilder addParam(String name, int value);

	OperationResultBuilder addParam(String name, Class<?> value);

	OperationResultBuilder addParam(String name, QName value);

	OperationResultBuilder addParam(String name, PolyString value);

	OperationResultBuilder addParam(String name, ObjectQuery value);

	OperationResultBuilder addParam(String name, ObjectDelta<?> value);

	OperationResultBuilder addParam(String name, String... values);

	OperationResultBuilder addArbitraryObjectAsParam(String paramName, Object paramValue);

	OperationResultBuilder addArbitraryObjectCollectionAsParam(String name, Collection<?> value);

	OperationResultBuilder addContext(String name, String value);

	OperationResultBuilder addContext(String name, PrismObject<? extends ObjectType> value);

	OperationResultBuilder addContext(String name, ObjectType value);

	OperationResultBuilder addContext(String name, boolean value);

	OperationResultBuilder addContext(String name, long value);

	OperationResultBuilder addContext(String name, int value);

	OperationResultBuilder addContext(String name, Class<?> value);

	OperationResultBuilder addContext(String name, QName value);

	OperationResultBuilder addContext(String name, PolyString value);

	OperationResultBuilder addContext(String name, ObjectQuery value);

	OperationResultBuilder addContext(String name, ObjectDelta<?> value);

	OperationResultBuilder addContext(String name, String... values);

	OperationResultBuilder addArbitraryObjectAsContext(String name, Object value);

	@SuppressWarnings("unused")
	OperationResultBuilder addArbitraryObjectCollectionAsContext(String paramName, Collection<?> paramValue);

	OperationResultBuilder setMinor(boolean value);
}
