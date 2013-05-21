
/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.foo;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.evolveum.midpoint.xml.ns.test.foo_1 package. 
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

    private final static QName _Object_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "object");
    private final static QName _Name_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "name");
    private final static QName _Description_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "description");
    private final static QName _DisplayName_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "displayName");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.evolveum.midpoint.xml.ns.test.foo_1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Extension }
     * 
     */
    public Extension createExtension() {
        return new Extension();
    }

    /**
     * Create an instance of {@link UserType }
     * 
     */
    public UserType createUserType() {
        return new UserType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ObjectType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "object")
    public JAXBElement<ObjectType> createObject(ObjectType value) {
        return new JAXBElement<ObjectType>(_Object_QNAME, ObjectType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "name")
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "description")
    public JAXBElement<String> createDescription(String value) {
        return new JAXBElement<String>(_Description_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "displayName")
    public JAXBElement<String> createDisplayName(String value) {
        return new JAXBElement<String>(_DisplayName_QNAME, String.class, null, value);
    }

}
