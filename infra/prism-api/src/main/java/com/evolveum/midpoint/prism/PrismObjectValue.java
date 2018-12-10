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

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * Extension of PrismContainerValue that holds object-specific data (OID and version).
 * It was created to make methods returning/accepting ItemValue universally usable;
 * not losing OID/version data when object values are passed via such interfaces.
 *
 * This value is to be held by PrismObject. And such object should hold exactly one
 * PrismObjectValue.
 *
 * @author mederly
 */
public interface PrismObjectValue<O extends Objectable> extends PrismContainerValue<O> {

	String getOid();

	void setOid(String oid);

	String getVersion();

	void setVersion(String version);

	O asObjectable();

	PrismObject<O> asPrismObject();

	PolyString getName();

	PrismContainer<?> getExtension();

	@Override
	PrismObjectValue<O> clone();

	@Override
	PrismObjectValue<O> cloneComplex(CloneStrategy strategy);

	@Override
	boolean equals(Object o);

	@Override
	int hashCode();

	@Override
	boolean equivalent(PrismContainerValue<?> other);

	@Override
	String toString();

	@Override
	String toHumanReadableString();

	@Override
	PrismContainer<O> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException;

	static <T extends Objectable> T asObjectable(PrismObject<T> object) {
		return object != null ? object.asObjectable() : null;
	}
}
