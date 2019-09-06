/*
 * Copyright (c) 2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.prism.JaxbVisitable;
import com.evolveum.midpoint.prism.JaxbVisitor;

import java.io.Serializable;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * JAXB representation of HashedDataType.
 * Manually created (not generated)
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HashedDataType", propOrder = {
    "digestMethod",
    "digestValue"
})
public class HashedDataType implements Serializable, Cloneable, JaxbVisitable {

	@XmlElement(required = true)
    protected DigestMethodType digestMethod;

    @XmlElement(required = true)
    protected byte[] digestValue;

	public DigestMethodType getDigestMethod() {
		return digestMethod;
	}

	public void setDigestMethod(DigestMethodType digestMethod) {
		this.digestMethod = digestMethod;
	}

	public byte[] getDigestValue() {
		return digestValue;
	}

	public void setDigestValue(byte[] digestValue) {
		this.digestValue = digestValue;
	}

	@Override
	public String toString() {
		return "HashedDataType(digestMethod=" + digestMethod + ", digestValue=" + (digestValue==null?"null":"["+digestValue.length+" bytes])");
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((digestMethod == null) ? 0 : digestMethod.hashCode());
		result = prime * result + Arrays.hashCode(digestValue);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		HashedDataType other = (HashedDataType) obj;
		if (digestMethod == null) {
			if (other.digestMethod != null) {
				return false;
			}
		} else if (!digestMethod.equals(other.digestMethod)) {
			return false;
		}
		if (!Arrays.equals(digestValue, other.digestValue)) {
			return false;
		}
		return true;
	}

	@Override
    public HashedDataType clone() {
        HashedDataType cloned = new HashedDataType();
        cloned.setDigestMethod(getDigestMethod().clone());
        cloned.setDigestValue(digestValue.clone());
        return cloned;
    }

	@Override
	public void accept(JaxbVisitor visitor) {
		visitor.visit(this);
		if (digestMethod != null) {
			digestMethod.accept(visitor);
		}
	}
}
