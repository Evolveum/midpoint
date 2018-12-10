
package com.evolveum.prism.xml.ns._public.query_3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
 * <p>Java class for NAryLogicalOperatorFilterType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="NAryLogicalOperatorFilterType"&gt;
 *   &lt;complexContent&gt;
 *     &lt;extension base="{http://prism.evolveum.com/xml/ns/public/query-2}LogicalOperatorFilterType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element ref="{http://prism.evolveum.com/xml/ns/public/query-2}filter" maxOccurs="unbounded"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/extension&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "NAryLogicalOperatorFilterType", propOrder = {
    "filter"
})
public class NAryLogicalOperatorFilterType
    extends LogicalOperatorFilterType
    implements Serializable, Cloneable, Equals, HashCode
{

    private final static long serialVersionUID = 201105211233L;
    @XmlAnyElement
    protected List<Element> filter;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "NAryLogicalOperatorFilterType");

    /**
     * Creates a new {@code NAryLogicalOperatorFilterType} instance.
     *
     */
    public NAryLogicalOperatorFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code NAryLogicalOperatorFilterType} instance by deeply copying a given {@code NAryLogicalOperatorFilterType} instance.
     *
     *
     * @param o
     *     The instance to copy.
     * @throws NullPointerException
     *     if {@code o} is {@code null}.
     */
    public NAryLogicalOperatorFilterType(final NAryLogicalOperatorFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super(o);
        if (o == null) {
            throw new NullPointerException("Cannot create a copy of 'NAryLogicalOperatorFilterType' from 'null'.");
        }
        // 'Filter' collection.
        if (o.filter!= null) {
            copyFilter(o.getFilter(), this.getFilter());
        }
    }

    /**
     * Gets the value of the filter property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the filter property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getFilter().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Element }
     *
     *
     */
    public List<Element> getFilter() {
        if (filter == null) {
            filter = new ArrayList<>();
        }
        return this.filter;
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
            List<Element> theFilter;
            theFilter = (((this.filter!= null)&&(!this.filter.isEmpty()))?this.getFilter():null);
            currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "filter", theFilter), currentHashCode, theFilter);
        }
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof NAryLogicalOperatorFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        if (!super.equals(thisLocator, thatLocator, object, strategy)) {
            return false;
        }
        final NAryLogicalOperatorFilterType that = ((NAryLogicalOperatorFilterType) object);
        {
            List<Element> lhsFilter;
            lhsFilter = (((this.filter!= null)&&(!this.filter.isEmpty()))?this.getFilter():null);
            List<Element> rhsFilter;
            rhsFilter = (((that.filter!= null)&&(!that.filter.isEmpty()))?that.getFilter():null);
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
     * Copies all values of property {@code Filter} deeply.
     *
     * @param source
     *     The source to copy from.
     * @param target
     *     The target to copy {@code source} to.
     * @throws NullPointerException
     *     if {@code target} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private static void copyFilter(final List<Element> source, final List<Element> target) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        if ((source!= null)&&(!source.isEmpty())) {
            for (final Iterator<?> it = source.iterator(); it.hasNext(); ) {
                final Object next = it.next();
                if (next instanceof Element) {
                    // CWildcardTypeInfo: org.w3c.dom.Element
                    target.add(((Element)((Element) next).cloneNode(true)));
                    continue;
                }
                // Please report this at https://apps.sourceforge.net/mantisbt/ccxjc/
                throw new AssertionError((("Unexpected instance '"+ next)+"' for property 'Filter' of class 'com.evolveum.prism.xml.ns._public.query_3.NAryLogicalOperatorFilterType'."));
            }
        }
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     *
     * @return
     *     A deep copy of this object.
     */
    @Override
    public NAryLogicalOperatorFilterType clone() {
        {
            // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
            final NAryLogicalOperatorFilterType clone = ((NAryLogicalOperatorFilterType) super.clone());
            // 'Filter' collection.
            if (this.filter!= null) {
                clone.filter = null;
                copyFilter(this.getFilter(), clone.getFilter());
            }
            return clone;
        }
    }

}
