
package com.evolveum.prism.xml.ns._public.query_3;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSeeAlso;
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


/**
 * <p>Java class for FilterType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="matching" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FilterType", propOrder = {
    "matching"
})
@XmlSeeAlso({
    PropertyNoValueFilterType.class,
    PropertyComplexValueFilterType.class,
    PropertySimpleValueFilterType.class,
    UriFilterType.class,
    LogicalOperatorFilterType.class
})
public class FilterClauseType implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    protected String matching;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "FilterType");
    public final static QName F_MATCHING = new QName(PrismConstants.NS_QUERY, "matching");

    /**
     * Creates a new {@code FilterType} instance.
     * 
     */
    public FilterClauseType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code FilterType} instance by deeply copying a given {@code FilterType} instance.
     * 
     * 
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public FilterClauseType(final FilterClauseType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'FilterType' from 'null'.");
        }
        // CBuiltinLeafInfo: java.lang.String
        this.matching = ((o.matching == null)?null:o.getMatching());
    }

    /**
     * Gets the value of the matching property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMatching() {
        return matching;
    }

    /**
     * Sets the value of the matching property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMatching(String value) {
        this.matching = value;
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
        int currentHashCode = 1;
        {
            String theMatching;
            theMatching = this.getMatching();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "matching", theMatching), currentHashCode, theMatching);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof FilterClauseType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final FilterClauseType that = ((FilterClauseType) object);
        {
            String lhsMatching;
            lhsMatching = this.getMatching();
            String rhsMatching;
            rhsMatching = that.getMatching();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "matching", lhsMatching), LocatorUtils.property(thatLocator, "matching", rhsMatching), lhsMatching, rhsMatching)) {
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
    public FilterClauseType clone() {
        try {
            {
                // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
                final FilterClauseType clone = ((FilterClauseType) super.clone());
                // CBuiltinLeafInfo: java.lang.String
                clone.matching = ((this.matching == null)?null:this.getMatching());
                return clone;
            }
        } catch (CloneNotSupportedException e) {
            // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
            throw new AssertionError(e);
        }
    }

}
