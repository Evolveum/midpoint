/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
