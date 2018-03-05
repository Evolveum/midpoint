
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

package com.evolveum.midpoint.prism.foo;

import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;


/**
 *
 *                 An event handler - typically either a filter, a notifier, a fork (fan-out), or a chain of handlers.
 *
 *
 * <p>Java class for EventHandlerType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="EventHandlerType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EventHandlerType", propOrder = {

})
@XmlSeeAlso({
    EventHandlerChainType.class,
    EventCategoryFilterType.class,
    EventStatusFilterType.class,
    EventOperationFilterType.class
})
public class EventHandlerType implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Creates a new {@code EventHandlerType} instance.
     *
     */
    public EventHandlerType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code EventHandlerType} instance by deeply copying a given {@code EventHandlerType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public EventHandlerType(final EventHandlerType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'EventHandlerType' from 'null'.");
        }
        // CBuiltinLeafInfo: java.lang.String
        this.name = ((o.name == null)?null:o.getName());
    }

    /**
     * Gets the value of the description property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */

    /**
     * Sets the value of the description property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     *
     */

    @Override
    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        {
            String theName;
            theName = this.getName();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "name", theName), currentHashCode, theName);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof EventHandlerType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final EventHandlerType that = ((EventHandlerType) object);
        {
            String lhsName;
            lhsName = this.getName();
            String rhsName;
            rhsName = that.getName();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "name", lhsName), LocatorUtils.property(thatLocator, "name", rhsName), lhsName, rhsName)) {
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
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public EventHandlerType clone() {
        try {
            {
                // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
                final EventHandlerType clone = ((EventHandlerType) super.clone());
                // CBuiltinLeafInfo: java.lang.String
                clone.name = ((this.name == null)?null:this.getName());
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
            throw new AssertionError(e);
        }
    }

    @Override
    public String toString() {
        return "EventHandlerType{" +
                "name='" + name + '\'' +
                '}';
    }
}
