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
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * JAXB representation of DigestMethodType.
 * Manually created (not generated)
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DigestMethodType", propOrder = {
    "algorithm",
	"salt",
    "workFactor"
})
public class DigestMethodType  implements Serializable, Cloneable {

    @XmlElement(required = true)
    @XmlSchemaType(name = "anyURI")
    protected String algorithm;
    
    @XmlElement(required = false)
    protected byte[] salt;
    
    @XmlElement(required = false)
    protected Integer workFactor;

    /**
     * Gets the value of the algorithm property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Sets the value of the algorithm property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAlgorithm(String value) {
        this.algorithm = value;
    }

	public byte[] getSalt() {
		return salt;
	}

	public void setSalt(byte[] salt) {
		this.salt = salt;
	}

	public Integer getWorkFactor() {
		return workFactor;
	}

	public void setWorkFactor(Integer workFactor) {
		this.workFactor = workFactor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
		result = prime * result + Arrays.hashCode(salt);
		result = prime * result + ((workFactor == null) ? 0 : workFactor.hashCode());
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
		DigestMethodType other = (DigestMethodType) obj;
		if (algorithm == null) {
			if (other.algorithm != null) {
				return false;
			}
		} else if (!algorithm.equals(other.algorithm)) {
			return false;
		}
		if (!Arrays.equals(salt, other.salt)) {
			return false;
		}
		if (workFactor == null) {
			if (other.workFactor != null) {
				return false;
			}
		} else if (!workFactor.equals(other.workFactor)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DigestMethodType(algorithm=" + algorithm + ", salt=" + (salt==null?"null":"["+salt.length+" bytes]") + ", workFactor=" + workFactor + ")";
	}

    public DigestMethodType clone() {
        DigestMethodType cloned = new DigestMethodType();
        cloned.setAlgorithm(getAlgorithm());
        cloned.setSalt(salt.clone());
        cloned.setWorkFactor(getWorkFactor());
        return cloned;
    }

}
