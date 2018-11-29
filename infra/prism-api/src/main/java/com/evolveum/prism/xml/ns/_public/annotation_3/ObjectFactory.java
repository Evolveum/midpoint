
package com.evolveum.prism.xml.ns._public.annotation_3;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the com.evolveum.prism.xml.ns._public.annotation_2 package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 *
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Extension_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "extension");
    private final static QName _Access_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "access");
    private final static QName _Deprecated_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "deprecated");
    private final static QName _Ignore_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "ignore");
    private final static QName _Indexed_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "indexed");
    private final static QName _Container_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "container");
    private final static QName _Operational_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "operational");
    private final static QName _Composite_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "composite");
    private final static QName _Help_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "help");
    private final static QName _ObjectReferenceTargetType_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "objectReferenceTargetType");
    private final static QName _DisplayOrder_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "displayOrder");
    private final static QName _DisplayName_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "displayName");
    private final static QName _MaxOccurs_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "maxOccurs");
    private final static QName _Object_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "object");
    private final static QName _ObjectReference_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "objectReference");
    private final static QName _Type_QNAME = new QName("http://prism.evolveum.com/xml/ns/public/annotation-3", "type");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.evolveum.prism.xml.ns._public.annotation_2
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "extension")
    public JAXBElement<QName> createExtension(QName value) {
        return new JAXBElement<>(_Extension_QNAME, QName.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AccessAnnotationType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "access")
    public JAXBElement<AccessAnnotationType> createAccess(AccessAnnotationType value) {
        return new JAXBElement<>(_Access_QNAME, AccessAnnotationType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "deprecated")
    public JAXBElement<Boolean> createDeprecated(Boolean value) {
        return new JAXBElement<>(_Deprecated_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "ignore")
    public JAXBElement<Boolean> createIgnore(Boolean value) {
        return new JAXBElement<>(_Ignore_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "indexed")
    public JAXBElement<Boolean> createIndexed(Boolean value) {
        return new JAXBElement<>(_Indexed_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "container")
    public JAXBElement<Object> createContainer(Object value) {
        return new JAXBElement<>(_Container_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "operational")
    public JAXBElement<Boolean> createOperational(Boolean value) {
        return new JAXBElement<>(_Operational_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "composite")
    public JAXBElement<Boolean> createComposite(Boolean value) {
        return new JAXBElement<>(_Composite_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "help")
    public JAXBElement<String> createHelp(String value) {
        return new JAXBElement<>(_Help_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "objectReferenceTargetType")
    public JAXBElement<QName> createObjectReferenceTargetType(QName value) {
        return new JAXBElement<>(_ObjectReferenceTargetType_QNAME, QName.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Integer }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "displayOrder")
    public JAXBElement<Integer> createDisplayOrder(Integer value) {
        return new JAXBElement<>(_DisplayOrder_QNAME, Integer.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "displayName")
    public JAXBElement<String> createDisplayName(String value) {
        return new JAXBElement<>(_DisplayName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "maxOccurs")
    public JAXBElement<String> createMaxOccurs(String value) {
        return new JAXBElement<>(_MaxOccurs_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "object")
    public JAXBElement<Object> createObject(Object value) {
        return new JAXBElement<>(_Object_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Object }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "objectReference")
    public JAXBElement<Object> createObjectReference(Object value) {
        return new JAXBElement<>(_ObjectReference_QNAME, Object.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QName }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://prism.evolveum.com/xml/ns/public/annotation-3", name = "type")
    public JAXBElement<QName> createType(QName value) {
        return new JAXBElement<>(_Type_QNAME, QName.class, null, value);
    }

}
