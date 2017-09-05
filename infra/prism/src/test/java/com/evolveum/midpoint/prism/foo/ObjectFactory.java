
/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;


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
    private final static QName _Resource_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "resource");
    private final static QName _User_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "user");
    private final static QName _DisplayName_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "displayName");
    private final static QName _Account_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "account");
    private final static QName _Description_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "description");
    private final static QName _Note_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "note");
    private final static QName _StatusFilter_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "statusFilter");
    private final static QName _CategoryFilter_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "categoryFilter");
    private final static QName _OperationFilter_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "operationFilter");
    private final static QName _HandlerChain_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "handlerChain");
    private final static QName _Handler_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "handler");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.evolveum.midpoint.xml.ns.test.foo_1
     *
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ObjectReferenceType }
     *
     */
    public ObjectReferenceType createObjectReferenceType() {
        return new ObjectReferenceType();
    }

    /**
     * Create an instance of {@link Extension }
     *
     */
    public Extension createExtension() {
        return new Extension();
    }

    /**
     * Create an instance of {@link ResourceType }
     *
     */
    public ResourceType createResourceType() {
        return new ResourceType();
    }

    /**
     * Create an instance of {@link AccountType }
     *
     */
    public AccountType createAccountType() {
        return new AccountType();
    }

    /**
     * Create an instance of {@link UserType }
     *
     */
    public UserType createUserType() {
        return new UserType();
    }

    /**
     * Create an instance of {@link FooObjectClass }
     *
     */
    public FooObjectClass createFooObjectClass() {
        return new FooObjectClass();
    }

    /**
     * Create an instance of {@link ActivationType }
     *
     */
    public ActivationType createActivationType() {
        return new ActivationType();
    }

    /**
     * Create an instance of {@link AttributesType }
     *
     */
    public AttributesType createAttributesType() {
        return new AttributesType();
    }

    /**
     * Create an instance of {@link AccountConstructionType }
     *
     */
    public AccountConstructionType createAccountConstructionType() {
        return new AccountConstructionType();
    }

    /**
     * Create an instance of {@link DummyProtectedStringType }
     *
     */
    public DummyProtectedStringType createDummyProtectedStringType() {
        return new DummyProtectedStringType();
    }

    /**
     * Create an instance of {@link AssignmentType }
     *
     */
    public AssignmentType createAssignmentType() {
        return new AssignmentType();
    }

    /**
     * Create an instance of {@link ObjectReferenceType.Filter }
     *
     */
    public ObjectReferenceType.Filter createObjectReferenceTypeFilter() {
        return new ObjectReferenceType.Filter();
    }

    /**
     * Create an instance of {@link ObjectReferenceType.Object }
     *
     */
    public ObjectReferenceType.Object createObjectReferenceTypeObject() {
        return new ObjectReferenceType.Object();
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
     * Create an instance of {@link JAXBElement }{@code <}{@link PolyStringType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "name")
    public JAXBElement<PolyStringType> createName(PolyStringType value) {
        return new JAXBElement<PolyStringType>(_Name_QNAME, PolyStringType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ResourceType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "resource")
    public JAXBElement<ResourceType> createResource(ResourceType value) {
        return new JAXBElement<ResourceType>(_Resource_QNAME, ResourceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UserType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "user")
    public JAXBElement<UserType> createUser(UserType value) {
        return new JAXBElement<UserType>(_User_QNAME, UserType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "displayName")
    public JAXBElement<String> createDisplayName(String value) {
        return new JAXBElement<String>(_DisplayName_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AccountType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "account")
    public JAXBElement<AccountType> createAccount(AccountType value) {
        return new JAXBElement<AccountType>(_Account_QNAME, AccountType.class, null, value);
    }


    /**
     * Create an instance of {@link EventHandlerType }
     *
     */
    public EventHandlerType createEventHandlerType() {
        return new EventHandlerType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EventHandlerType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "handler")
    public JAXBElement<EventHandlerType> createHandler(EventHandlerType value) {
        return new JAXBElement<EventHandlerType>(_Handler_QNAME, EventHandlerType.class, null, value);
    }

    /**
     * Create an instance of {@link EventHandlerChainType }
     *
     */
    public EventHandlerChainType createEventHandlerChainType() {
        return new EventHandlerChainType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EventHandlerChainType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "handlerChain", substitutionHeadNamespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", substitutionHeadName = "handler")
    public JAXBElement<EventHandlerChainType> createHandlerChain(EventHandlerChainType value) {
        return new JAXBElement<EventHandlerChainType>(_HandlerChain_QNAME, EventHandlerChainType.class, null, value);
    }

    /**
     * Create an instance of {@link EventStatusFilterType }
     *
     */
    public EventStatusFilterType createEventStatusFilterType() {
        return new EventStatusFilterType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EventStatusFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "statusFilter", substitutionHeadNamespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", substitutionHeadName = "handler")
    public JAXBElement<EventStatusFilterType> createStatusFilter(EventStatusFilterType value) {
        return new JAXBElement<EventStatusFilterType>(_StatusFilter_QNAME, EventStatusFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link EventOperationFilterType }
     *
     */
    public EventOperationFilterType createEventOperationFilterType() {
        return new EventOperationFilterType();
    }


    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EventOperationFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "operationFilter", substitutionHeadNamespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", substitutionHeadName = "handler")
    public JAXBElement<EventOperationFilterType> createOperationFilter(EventOperationFilterType value) {
        return new JAXBElement<EventOperationFilterType>(_OperationFilter_QNAME, EventOperationFilterType.class, null, value);
    }

    /**
     * Create an instance of {@link EventCategoryFilterType }
     *
     */
    public EventCategoryFilterType createEventCategoryFilterType() {
        return new EventCategoryFilterType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EventCategoryFilterType }{@code >}}
     *
     */
    @XmlElementDecl(namespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", name = "categoryFilter", substitutionHeadNamespace = "http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", substitutionHeadName = "handler")
    public JAXBElement<EventCategoryFilterType> createCategoryFilter(EventCategoryFilterType value) {
        return new JAXBElement<EventCategoryFilterType>(_CategoryFilter_QNAME, EventCategoryFilterType.class, null, value);
    }

}
