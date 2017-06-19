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

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Objects;

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
public class PrismObjectValue<O extends Objectable> extends PrismContainerValue<O> {

	protected String oid;
	protected String version;

	public PrismObjectValue() {
	}

	public PrismObjectValue(PrismContext prismContext) {
		super(prismContext);
	}

	public PrismObjectValue(O objectable) {
		super(objectable);
	}

	public PrismObjectValue(O objectable, PrismContext prismContext) {
		super(objectable, prismContext);
	}

	private PrismObjectValue(OriginType type, Objectable source, PrismContainerable container, Long id,
			ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, String oid, String version) {
		super(type, source, container, id, complexTypeDefinition, prismContext);
		this.oid = oid;
		this.version = version;
	}

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		checkMutability();
		this.oid = oid;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		checkMutability();
		this.version = version;
	}

	public O asObjectable() {
		return asContainerable();
	}

	public PrismObject<O> asPrismObject() {
		return asObjectable().asPrismObject();
	}

	public PolyString getName() {
		return asPrismObject().getName();
	}

	public PrismContainer<?> getExtension() {
		return asPrismObject().getExtension();
	}

	@Override
	public PrismObjectValue<O> clone() {
		PrismObjectValue<O> clone = new PrismObjectValue<O>(
				getOriginType(), getOriginObject(), getParent(), getId(), null, this.prismContext, oid, version);
		copyValues(clone);
		return clone;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof PrismObjectValue))
			return false;
		if (!super.equals(o))
			return false;
		PrismObjectValue<?> that = (PrismObjectValue<?>) o;
		return Objects.equals(oid, that.oid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), oid);
	}

	@Override
	public boolean equivalent(PrismContainerValue<?> other) {
		if (!(other instanceof PrismObjectValue)) {
			return false;
		}
		PrismObjectValue otherPov = (PrismObjectValue) other;
		return StringUtils.equals(oid, otherPov.oid) && super.equivalent(other);
	}

	@Override
	public String toString() {
		// we don't delegate to PrismObject, because various exceptions during that process could in turn call this method
		StringBuilder sb = new StringBuilder();
		sb.append("POV:");
		if (getParent() != null) {
			sb.append(getParent().getElementName().getLocalPart()).append(":");
		} else if (getComplexTypeDefinition() != null) {
			sb.append(getComplexTypeDefinition().getTypeName().getLocalPart()).append(":");
		}
		sb.append(oid).append("(");
		PrismProperty nameProperty = findProperty(new QName(PrismConstants.NAME_LOCAL_NAME));
		sb.append(nameProperty != null ? nameProperty.getRealValue() : null);
		sb.append(")");
		return sb.toString();
	}

	@Override
	protected void detailedDebugDumpStart(StringBuilder sb) {
		sb.append("POV").append(": ");
	}

	@Override
	protected void debugDumpIdentifiers(StringBuilder sb) {
		sb.append("oid=").append(oid);
		sb.append(", version=").append(version);
	}

	@Override
	public String toHumanReadableString() {
		return "oid="+oid+": "+items.size()+" items";
	}

	@Override
	public PrismContainer<O> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException {
		throw new UnsupportedOperationException("Not supported for PrismObjectValue yet.");
	}

	public static <T extends Objectable> T asObjectable(PrismObject<T> object) {
		return object != null ? object.asObjectable() : null;
	}
}
