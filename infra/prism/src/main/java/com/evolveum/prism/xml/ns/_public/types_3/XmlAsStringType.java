/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.prism.xml.ns._public.types_3;

import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;
import org.w3c.dom.Node;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class used to hold string represented either as plain string or as XML markup. (Useful e.g. for jasper templates.)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "XmlAsStringType", propOrder = {
        "content"
})
public class XmlAsStringType implements Serializable, Cloneable, Equals, HashCode {

    private final static long serialVersionUID = 201105211233L;
    @XmlMixed
    @XmlAnyElement(lax = false)             // JAXB should not try to unmarshal inner elements
    protected List<Object> content;

    /**
     * Creates a new {@code XmlAsStringType} instance.
     *
     */
    public XmlAsStringType() {
    }

    public XmlAsStringType(String value) {
        content = new ArrayList<>();
        content.add(value);
    }

    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<>();
        }
        return this.content;
    }

    public String getContentAsString() {
        StringBuilder sb = new StringBuilder();
        for (Object object : getContent()) {
            if (object instanceof String) {
                sb.append(object);
            } else if (object instanceof Node) {
                sb.append(DOMUtil.serializeDOMToString((Node) object));
            } else {
                throw new IllegalStateException("Unexpected content in XmlAsStringType: " + (object!=null?object.getClass():"(null)"));
            }
        }
        return sb.toString();
    }

    @Override
    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        {
            List<Object> theContent;
            theContent = (((this.content!= null)&&(!this.content.isEmpty()))?this.getContent():null);
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "content", theContent), currentHashCode, theContent);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof XmlAsStringType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final XmlAsStringType that = ((XmlAsStringType) object);
        {
            List<Object> lhsContent;
            lhsContent = (((this.content!= null)&&(!this.content.isEmpty()))?this.getContent():null);
            List<Object> rhsContent;
            rhsContent = (((that.content!= null)&&(!that.content.isEmpty()))?that.getContent():null);
            if (!strategy.equals(LocatorUtils.property(thisLocator, "content", lhsContent), LocatorUtils.property(thatLocator, "content", rhsContent), lhsContent, rhsContent)) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object object) {
        final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public XmlAsStringType clone() {
        final XmlAsStringType clone;
        try {
            clone = ((XmlAsStringType) super.clone());
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Couldn't clone object's superclass", e);
        }
        if (this.content != null) {
            clone.content = new ArrayList<>();
            for (Object o : this.getContent()) {
                if (o instanceof String) {
                    clone.content.add(o);
                } else if (o instanceof Node) {
                    clone.content.add(((Node) o).cloneNode(true));
                } else {
                    throw new IllegalStateException("XmlAsStringType.clone: unexpected item in content: " + (o!=null?o.getClass():"(null)"));
                }
            }
        }
        return clone;
    }
}
