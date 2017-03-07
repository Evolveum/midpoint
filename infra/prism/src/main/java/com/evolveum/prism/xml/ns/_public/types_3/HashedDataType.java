/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.prism.xml.ns._public.types_3;

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
public class HashedDataType implements Serializable, Cloneable {

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
}
