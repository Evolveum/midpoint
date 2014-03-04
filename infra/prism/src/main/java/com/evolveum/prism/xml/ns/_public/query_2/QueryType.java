
package com.evolveum.prism.xml.ns._public.query_2;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;


/**
 * 
 *                 TODO
 *             
 * 
 * <p>Java class for QueryType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QueryType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="condition" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element ref="{http://prism.evolveum.com/xml/ns/public/query-2}filter"/>
 *         &lt;element name="paging" type="{http://prism.evolveum.com/xml/ns/public/query-2}PagingType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QueryType", propOrder = {
    "description",
    "condition",
    "filter",
    "paging"
})
public class QueryType implements Serializable, Cloneable
{
    private final static long serialVersionUID = 201105211233L;
    public final static QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "QueryType");
    public final static QName F_DESCRIPTION = new QName(PrismConstants.NS_QUERY, "description");
    public final static QName F_FILTER = new QName(PrismConstants.NS_QUERY, "filter");
    public final static QName F_CONDITION = new QName(PrismConstants.NS_QUERY, "condition");
    public final static QName F_PAGING = new QName(PrismConstants.NS_QUERY, "paging");

    protected String description;
    protected Object condition;
    @XmlElement(required = true)
    protected SearchFilterType filter;
    protected PagingType paging;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the condition property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getCondition() {
        return condition;
    }

    /**
     * Sets the value of the condition property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setCondition(Object value) {
        this.condition = value;
    }

    /**
     * Gets the value of the filter property.
     * 
     * @return
     *     possible object is
     *     {@link SearchFilterType }
     *     
     */
    public SearchFilterType getFilter() {
        return filter;
    }

    /**
     * Sets the value of the filter property.
     * 
     * @param value
     *     allowed object is
     *     {@link SearchFilterType }
     *     
     */
    public void setFilter(SearchFilterType value) {
        this.filter = value;
    }

    /**
     * Gets the value of the paging property.
     * 
     * @return
     *     possible object is
     *     {@link PagingType }
     *     
     */
    public PagingType getPaging() {
        return paging;
    }

    /**
     * Sets the value of the paging property.
     * 
     * @param value
     *     allowed object is
     *     {@link PagingType }
     *     
     */
    public void setPaging(PagingType value) {
        this.paging = value;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((condition == null) ? 0 : condition.hashCode());
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((filter == null) ? 0 : filter.hashCode());
		result = prime * result + ((paging == null) ? 0 : paging.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryType other = (QueryType) obj;
		if (condition == null) {
			if (other.condition != null)
				return false;
		} else if (!condition.equals(other.condition))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (filter == null) {
			if (other.filter != null)
				return false;
		} else if (!filter.equals(other.filter))
			return false;
		if (paging == null) {
			if (other.paging != null)
				return false;
		} else if (!paging.equals(other.paging))
			return false;
		return true;
	}

	
}
