
package com.evolveum.prism.xml.ns._public.query_3;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;
import org.w3c.dom.Element;


/**
 * <p>Java class for PropertyNoValueFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="PropertyNoValueFilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://prism.evolveum.com/xml/ns/public/query-2}FilterType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="property" type="{http://prism.evolveum.com/xml/ns/public/types-3}XPathType"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertyNoValueFilterType", propOrder = {
    "property"
})
public class PropertyNoValueFilterType
    extends FilterClauseType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAnyElement
    protected Element property;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "PropertyNoValueFilterType");

    /**
     * Creates a new {@code PropertyNoValueFilterType} instance.
     *
     */
    public PropertyNoValueFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code PropertyNoValueFilterType} instance by deeply copying a given {@code PropertyNoValueFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public PropertyNoValueFilterType(final PropertyNoValueFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'PropertyNoValueFilterType' from 'null'.");
        }
        // CWildcardTypeInfo: org.w3c.dom.Element
        this.property = ((o.property == null)?null:((o.getProperty() == null)?null:((Element) o.getProperty().cloneNode(true))));
    }

    /**
     * Gets the value of the property property.
     *
     * @return
     *     possible object is
     *     {@link Element }
     *
     */
    public Element getProperty() {
        return property;
    }

    /**
     * Sets the value of the property property.
     *
     * @param value
     *     allowed object is
     *     {@link Element }
     *
     */
    public void setProperty(Element value) {
        this.property = value;
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     *
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = super.hashCode(locator, strategy);
        {
            Element theProperty;
            theProperty = this.getProperty();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "property", theProperty), currentHashCode, theProperty);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof PropertyNoValueFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final PropertyNoValueFilterType that = ((PropertyNoValueFilterType) object);
        {
            Element lhsProperty;
            lhsProperty = this.getProperty();
            Element rhsProperty;
            rhsProperty = that.getProperty();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "property", lhsProperty), LocatorUtils.property(thatLocator, "property", rhsProperty), lhsProperty, rhsProperty)) {
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
    public PropertyNoValueFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final PropertyNoValueFilterType clone = ((PropertyNoValueFilterType) super.clone());
            // CWildcardTypeInfo: org.w3c.dom.Element
            clone.property = ((this.property == null)?null:((this.getProperty() == null)?null:((Element) this.getProperty().cloneNode(true))));
            return clone;
        }
    }

}
