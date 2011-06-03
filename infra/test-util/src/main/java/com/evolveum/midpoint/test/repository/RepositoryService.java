/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.test.repository;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.FilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserListType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.xml.bind.JAXBException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

/**
 * Mock implementation of repository interface.
 * 
 * It will use BaseX XML database as a mock
 * repository implementation.
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class RepositoryService implements RepositoryPortType {

    public static final String code_id = "$Id$";
    private static final String NS_COMMON = "http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd";
    private static final String NS_DECLARE = "declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n ";
    private Collection collection = null;
    private Unmarshaller unmarshaller = null;
    private Marshaller marshaller = null;

    void initialis(Collection collection, JAXBContext ctx) throws JAXBException {
        this.collection = collection;
        this.unmarshaller = ctx.createUnmarshaller();
        this.marshaller = ctx.createMarshaller();
        this.marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        // features supported
        //String INDENT_STRING = "com.sun.xml.internal.bind.indentString";
        //String PREFIX_MAPPER = "com.sun.xml.internal.bind.namespacePrefixMapper";
        //String ENCODING_HANDLER = "com.sun.xml.internal.bind.characterEscapeHandler";
        //String ENCODING_HANDLER2 = "com.sun.xml.internal.bind.marshaller.CharacterEscapeHandler";
        //String XMLDECLARATION = "com.sun.xml.internal.bind.xmlDeclaration";
        //String XML_HEADERS = "com.sun.xml.internal.bind.xmlHeaders";
        //String C14N = JAXBRIContext.CANONICALIZATION_SUPPORT;
        //String OBJECT_IDENTITY_CYCLE_DETECTION = "com.sun.xml.internal.bind.objectIdentitityCycleDetection";
        //this.marshaller.setProperty("com.sun.xml.bind.xmlDeclaration", Boolean.FALSE);
        //this.marshaller.setProperty("com.sun.xml.internal.bind.xmlDeclaration", Boolean.FALSE);
        this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
    }

    public String addObject(ObjectContainerType oct) throws FaultMessage {

        String oid = null;
        try {
            if (oct.getObject() instanceof ExtensibleObjectType) {

                ExtensibleObjectType payload = (ExtensibleObjectType) oct.getObject();

                // Store new XMLResource
                oid = null != payload.getOid() ? payload.getOid() : UUID.randomUUID().toString();
                payload.setOid(oid);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                XMLStreamWriter xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                xmlStreamWriter.setPrefix("c", NS_COMMON);
                JAXBElement<ExtensibleObjectType> o = new JAXBElement<ExtensibleObjectType>(new QName(NS_COMMON, "object", "c"), (Class<ExtensibleObjectType>) payload.getClass(), null, payload);
                marshaller.marshal(o, xmlStreamWriter);
                xmlStreamWriter.flush();

                //Resource document = collection.createResource(oid, XMLResource.RESOURCE_TYPE);
                // allow only strings, byte arrays and {@link File} instances
                //document.setContent(out.toByteArray());
                //System.out.println("Storing document " + oid + "...");
                //collection.storeResource(document);


                // Receive the XPath query service.
                XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

                StringBuilder QUERY = new StringBuilder(NS_DECLARE).append("insert node ").append(new String(out.toByteArray(), "UTF-8")).append(" into //c:objects");

                // Execute the query and receives all results.
                ResourceSet set = service.query(QUERY.toString());

                // Create a result iterator.
                ResourceIterator iter = set.getIterator();

                // Loop through all result items.
                while (iter.hasMoreResources()) {
                    // Receive the next results.
                    Resource res = iter.nextResource();

                    // Write the result to the console.
                    System.out.println(res.getContent());
                }
                return oid;
            }
        } catch (XMLStreamException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XMLDBException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    @Override
    public ObjectContainerType getObject(String oid, PropertyReferenceListType prlt) throws FaultMessage {
        UUID id = UUID.fromString(oid);
        if (null == id) {
            throw new FaultMessage("Invalid OID", new ObjectNotFoundFaultType());
        }

        ByteArrayInputStream in = null;
        ObjectContainerType out = null;
        try {

            // Receive the XPath query service.
            XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

            StringBuilder QUERY = new StringBuilder("declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
            QUERY.append("for $x in //c:object where $x/@oid=\"").append(oid).append("\" return $x");

            // Execute the query and receives all results.
            ResourceSet set = service.query(QUERY.toString());

            // Create a result iterator.
            ResourceIterator iter = set.getIterator();

            // Loop through all result items.
            while (iter.hasMoreResources()) {
                // Receive the next results.
                Resource res = iter.nextResource();

                if (null != out) {
                    throw new FaultMessage("NoSingleResult", new ObjectNotFoundFaultType());
                }
                // Write the result to the console.
                //System.out.println(res.getContent());
                Object c = res.getContent();
                if (c instanceof String) {
                    in = new ByteArrayInputStream(((String) c).getBytes("UTF-8"));
                    JAXBElement<ExtensibleObjectType> o = (JAXBElement<ExtensibleObjectType>) unmarshaller.unmarshal(in);
                    if (o != null) {
                        out = new ObjectContainerType();
                        out.setObject(o.getValue());
                    }
                }
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JAXBException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XMLDBException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }
        if (out == null) {
        	throw new FaultMessage("Object not found. OID: "+oid, new ObjectNotFoundFaultType());
        }
        return out;
    }

    public ObjectListType listObjects(String string,PagingType paging) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ObjectListType searchObjects(QueryType query,PagingType paging) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void modifyObject(ObjectModificationType oct) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteObject(String string) throws FaultMessage {
        UUID id = UUID.fromString(string);
        if (null == id) {
            throw new FaultMessage("Invalid OID", new ObjectNotFoundFaultType());
        }

        ByteArrayInputStream in = null;
        ObjectContainerType out = null;
        try {

            // Receive the XPath query service.
            XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

            StringBuilder QUERY = new StringBuilder("declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
            QUERY.append("delete nodes //c:object[@oid=\"").append(string).append("\"]");

            // Execute the query and receives all results.
            ResourceSet set = service.query(QUERY.toString());

            // Create a result iterator.
            ResourceIterator iter = set.getIterator();

            // Loop through all result items.
            while (iter.hasMoreResources()) {
                // Receive the next results.
                Resource res = iter.nextResource();
                // Write the result to the console.
                System.out.println(res.getContent());
            }
        } catch (XMLDBException ex) {
            Logger.getLogger(RepositoryService.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    public PropertyAvailableValuesListType getPropertyAvailableValues(String string, PropertyReferenceListType prlt) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UserContainerType listAccountShadowOwner(String string) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public ResourceObjectShadowListType listResourceObjectShadows(String string, String string1) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String addUser(UserContainerType uct) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UserContainerType getUser(String string) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public UserListType listUsers() throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void modifyUser(ObjectModificationType oct) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteUser(String string) throws FaultMessage {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
