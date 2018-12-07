/*
 * Copyright (c) 2018 Evolveum
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PolyStringNormalizerConfigurationType", propOrder = {
    "className",
    "trim",
    "nfkd",
    "trimWhitespace",
    "lowercase"
})
public class PolyStringNormalizerConfigurationType implements Serializable, Cloneable, ShortDumpable {

	public static final QName COMPLEX_TYPE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "PolyStringNormalizerConfigurationType");
	public static final QName F_CLASS_NAME = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "className");
	public static final QName F_TRIM = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "trim");
	public static final QName F_NFKD = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "nfkd");
	public static final QName F_TRIM_WHITESPACE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "trimWhitespace");
	public static final QName F_LOWERCASE = new QName("http://prism.evolveum.com/xml/ns/public/types-3", "lowercase");

    @XmlElement(required = false)
    protected String className;

    @XmlElement(required = false)
    protected Boolean trim;

    @XmlElement(required = false)
    protected Boolean nfkd;

    @XmlElement(required = false)
    protected Boolean trimWhitespace;

    @XmlElement(required = false)
    protected Boolean lowercase;
    
    public String getClassName() {
		return className;
	}

    public void setClassName(String className) {
		this.className = className;
	}

    public Boolean isTrim() {
		return trim;
	}

    public void setTrim(Boolean trim) {
		this.trim = trim;
	}

    public Boolean isNfkd() {
		return nfkd;
	}

    public void setNfkd(Boolean nfkd) {
		this.nfkd = nfkd;
	}
    
    public Boolean isTrimWhitespace() {
		return trimWhitespace;
	}

    public void setTrimWhitespace(Boolean trimWhitespace) {
		this.trimWhitespace = trimWhitespace;
	}

    public Boolean isLowercase() {
		return lowercase;
	}

    public void setLowercase(Boolean lowercase) {
		this.lowercase = lowercase;
	}

	@Override
    public PolyStringNormalizerConfigurationType clone() {
        PolyStringNormalizerConfigurationType clone = new PolyStringNormalizerConfigurationType();
        clone.setClassName(getClassName());
        clone.setTrim(isTrim());
        clone.setTrimWhitespace(isTrimWhitespace());
        clone.setLowercase(isLowercase());
        return clone;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
		result = prime * result + ((lowercase == null) ? 0 : lowercase.hashCode());
		result = prime * result + ((nfkd == null) ? 0 : nfkd.hashCode());
		result = prime * result + ((trim == null) ? 0 : trim.hashCode());
		result = prime * result + ((trimWhitespace == null) ? 0 : trimWhitespace.hashCode());
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
		PolyStringNormalizerConfigurationType other = (PolyStringNormalizerConfigurationType) obj;
		if (className == null) {
			if (other.className != null) {
				return false;
			}
		} else if (!className.equals(other.className)) {
			return false;
		}
		if (lowercase == null) {
			if (other.lowercase != null) {
				return false;
			}
		} else if (!lowercase.equals(other.lowercase)) {
			return false;
		}
		if (nfkd == null) {
			if (other.nfkd != null) {
				return false;
			}
		} else if (!nfkd.equals(other.nfkd)) {
			return false;
		}
		if (trim == null) {
			if (other.trim != null) {
				return false;
			}
		} else if (!trim.equals(other.trim)) {
			return false;
		}
		if (trimWhitespace == null) {
			if (other.trimWhitespace != null) {
				return false;
			}
		} else if (!trimWhitespace.equals(other.trimWhitespace)) {
			return false;
		}
		return true;
	}

	@Override
	public void shortDump(StringBuilder sb) {
		DebugUtil.shortDumpAppendProperty(sb, "className", className);
		DebugUtil.shortDumpAppendProperty(sb, "trim", trim);
		DebugUtil.shortDumpAppendProperty(sb, "nfkd", nfkd);
		DebugUtil.shortDumpAppendProperty(sb, "trimWhitespace", trimWhitespace);
		DebugUtil.shortDumpAppendProperty(sb, "lowercase", lowercase);
		DebugUtil.shortDumpRemoveLastComma(sb);
	}

	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PolyStringNormalizerConfigurationType(");
		shortDump(sb);
		sb.append(")");
		return sb.toString();
	}

}
