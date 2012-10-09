
package com.evolveum.midpoint.prism.foo;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;


/**
 * <p>Java class for ObjectType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ObjectType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd}name" minOccurs="0"/>
 *         &lt;element ref="{http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd}description" minOccurs="0"/>
 *         &lt;element ref="{http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd}extension" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="oid" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="version" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ObjectType", propOrder = {
    "name",
    "description",
    "extension"
})
@XmlSeeAlso({
    UserType.class
})
public abstract class ObjectType
    implements Serializable, Objectable
{

    private final static long serialVersionUID = 201202081233L;
    protected PolyStringType name;
    protected String description;
    protected Extension extension;
    @XmlAttribute(name = "oid")
    protected String oid;
    @XmlAttribute(name = "version")
    protected String version;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public PolyStringType getName() {
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
    public void setName(PolyStringType value) {
        this.name = value;
    }

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
     * Gets the value of the extension property.
     * 
     * @return
     *     possible object is
     *     {@link Extension }
     *     
     */
    public Extension getExtension() {
        return extension;
    }

    /**
     * Sets the value of the extension property.
     * 
     * @param value
     *     allowed object is
     *     {@link Extension }
     *     
     */
    public void setExtension(Extension value) {
        this.extension = value;
    }

    /**
     * Gets the value of the oid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOid() {
        return oid;
    }

    /**
     * Sets the value of the oid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOid(String value) {
        this.oid = value;
    }

    /**
     * Gets the value of the version property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the value of the version property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersion(String value) {
        this.version = value;
    }

	@Override
	public String toDebugName() {
		return toDebugType()+":"+getOid()+"("+getName()+")";
	}

	@Override
	public String toDebugType() {
		return "object";
	}
	
	@Override
    public PrismObject asPrismObject() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void setupContainer(PrismObject object) {
        throw new UnsupportedOperationException("not implemented yet");
    }

	@Override
	public PrismContainerValue asPrismContainerValue() {
		 throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public void setupContainerValue(PrismContainerValue container) {
		throw new UnsupportedOperationException("not implemented yet");
	}

}
