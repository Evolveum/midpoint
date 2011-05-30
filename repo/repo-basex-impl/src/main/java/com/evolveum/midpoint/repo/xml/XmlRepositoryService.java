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
 * Portions Copyrighted 2011 Igor Farinic
 */
package com.evolveum.midpoint.repo.xml;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

public class XmlRepositoryService implements RepositoryPortType {

	private static final Trace logger = TraceManager.getTrace(XmlRepositoryService.class);
	private Collection collection;

	private final Marshaller marshaller;
	private final Unmarshaller unmarshaller;

	XmlRepositoryService(Collection collection) {
		super();
		this.collection = collection;

		JAXBContext ctx;
		try {
			ctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
			this.unmarshaller = ctx.createUnmarshaller();
			this.marshaller = ctx.createMarshaller();
			this.marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
			// jaxb_fragment has to be set to true and we have to marshal object
			// into stream to avoid generation of xml declaration
			this.marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		} catch (JAXBException e) {
			logger.error("Problem initializing XML Repository Service", e);
			throw new RuntimeException("Problem initializing XML Repository Service", e);
		}

	}

	private final <T> String marshalWrap(T jaxbObject, QName elementQName) throws JAXBException {
		JAXBElement<T> jaxbElement = new JAXBElement<T>(elementQName, (Class<T>) jaxbObject.getClass(),
				jaxbObject);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter xmlStreamWriter;
		try {
			xmlStreamWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
			this.marshaller.marshal(jaxbElement, xmlStreamWriter);
			xmlStreamWriter.flush();
			return new String(out.toByteArray(), "UTF-8");
		} catch (XMLStreamException e) {
			logger.error("JAXB object marshal to Xml stream failed", e);
			throw new JAXBException("JAXB object marshal to Xml stream failed", e);
		} catch (FactoryConfigurationError e) {
			logger.error("JAXB object marshal to Xml stream failed", e);
			throw new JAXBException("JAXB object marshal to Xml stream failed", e);
		} catch (UnsupportedEncodingException e) {
			logger.error("UTF-8 is unsupported encoding", e);
			throw new JAXBException("UTF-8 is unsupported encoding", e);
		}
	}

	@Override
	public String addObject(ObjectContainerType objectContainer) throws FaultMessage {
		String oid = null;
		try {

			ObjectType payload = (ObjectType) objectContainer.getObject();

			oid = (null != payload.getOid() ? payload.getOid() : UUID.randomUUID().toString());
			payload.setOid(oid);

			String serializedObject = marshalWrap(payload, SchemaConstants.C_OBJECT);

			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n")
					.append("insert node ").append(serializedObject).append(" into //c:objects");

			// Execute the query and receives all results.
			ResourceSet set = service.query(query.toString());

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
		} catch (JAXBException ex) {
			logger.error("Failed to (un)marshal object", ex);
			throw new FaultMessage("Failed to (un)marshal object", new IllegalArgumentFaultType());
		} catch (XMLDBException ex) {
			logger.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType());

		}
	}

	@Override
	public ObjectContainerType getObject(String oid, PropertyReferenceListType resolve) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectListType listObjects(String objectType, PagingType paging) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void modifyObject(ObjectModificationType objectChange) throws FaultMessage {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteObject(String oid) throws FaultMessage {
		// TODO Auto-generated method stub

	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UserContainerType listAccountShadowOwner(String accountOid) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType) throws FaultMessage {
		// TODO Auto-generated method stub
		return null;
	}

}
