
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
 * <p>Java class for UnaryLogicalOperatorFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="UnaryLogicalOperatorFilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://prism.evolveum.com/xml/ns/public/query-2}LogicalOperatorFilterType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://prism.evolveum.com/xml/ns/public/query-2}filter"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnaryLogicalOperatorFilterType", propOrder = {
    "filter"
})
public class UnaryLogicalOperatorFilterType
    extends LogicalOperatorFilterType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAnyElement
    protected Element filter;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "UnaryLogicalOperatorFilterType");

    /**
     * Creates a new {@code UnaryLogicalOperatorFilterType} instance.
     *
     */
    public UnaryLogicalOperatorFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code UnaryLogicalOperatorFilterType} instance by deeply copying a given {@code UnaryLogicalOperatorFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public UnaryLogicalOperatorFilterType(final UnaryLogicalOperatorFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'UnaryLogicalOperatorFilterType' from 'null'.");
        }
        // CWildcardTypeInfo: org.w3c.dom.Element
        this.filter = ((o.filter == null)?null:((o.getFilter() == null)?null:((Element) o.getFilter().cloneNode(true))));
    }

    /**
     * Gets the value of the filter property.
     *
     * @return
     *     possible object is
     *     {@link Element }
     *
     */
    public Element getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     *
     * @param value
     *     allowed object is
     *     {@link Element }
     *
     */
    public void setFilter(Element value) {
        this.filter = value;
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

    @Override
    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = super.hashCode(locator, strategy);
        {
            Element theFilter;
            theFilter = this.getFilter();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "filter", theFilter), currentHashCode, theFilter);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof UnaryLogicalOperatorFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final UnaryLogicalOperatorFilterType that = ((UnaryLogicalOperatorFilterType) object);
        {
            Element lhsFilter;
            lhsFilter = this.getFilter();
            Element rhsFilter;
            rhsFilter = that.getFilter();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "filter", lhsFilter), LocatorUtils.property(thatLocator, "filter", rhsFilter), lhsFilter, rhsFilter)) {
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
    public UnaryLogicalOperatorFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final UnaryLogicalOperatorFilterType clone = ((UnaryLogicalOperatorFilterType) super.clone());
            // CWildcardTypeInfo: org.w3c.dom.Element
            clone.filter = ((this.filter == null)?null:((this.getFilter() == null)?null:((Element) this.getFilter().cloneNode(true))));
            return clone;
        }
    }

}
