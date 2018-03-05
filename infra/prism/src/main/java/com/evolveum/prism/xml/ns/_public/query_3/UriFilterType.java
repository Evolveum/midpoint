
package com.evolveum.prism.xml.ns._public.query_3;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
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
 * <p>Java class for UriFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="UriFilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://prism.evolveum.com/xml/ns/public/query-2}FilterType"&gt;
 *       &lt;attribute name="uri" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UriFilterType")
public class UriFilterType
    extends FilterClauseType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAttribute(name = "uri")
    @XmlSchemaType(name = "anyURI")
    protected String uri;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "UriFilterType");
    public final static QName F_URI = new QName(PrismConstants.NS_QUERY, "uri");

    /**
     * Creates a new {@code UriFilterType} instance.
     *
     */
    public UriFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code UriFilterType} instance by deeply copying a given {@code UriFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public UriFilterType(final UriFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'UriFilterType' from 'null'.");
        }
        // CBuiltinLeafInfo: java.lang.String
        this.uri = ((o.uri == null)?null:o.getUri());
    }

    /**
     * Gets the value of the uri property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUri(String value) {
        this.uri = value;
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
            String theUri;
            theUri = this.getUri();
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "uri", theUri), currentHashCode, theUri);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @Override
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof UriFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final UriFilterType that = ((UriFilterType) object);
        {
            String lhsUri;
            lhsUri = this.getUri();
            String rhsUri;
            rhsUri = that.getUri();
            if (!strategy.equals(LocatorUtils.property(thisLocator, "uri", lhsUri), LocatorUtils.property(thatLocator, "uri", rhsUri), lhsUri, rhsUri)) {
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
    public UriFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final UriFilterType clone = ((UriFilterType) super.clone());
            // CBuiltinLeafInfo: java.lang.String
            clone.uri = ((this.uri == null)?null:this.getUri());
            return clone;
        }
    }

}
