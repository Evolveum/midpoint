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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.XPathQueryService;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.patch.PatchException;
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
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.IllegalArgumentFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectAlreadyExistsFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.ObjectNotFoundFaultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1.SystemFaultType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;

public class XmlRepositoryService implements RepositoryPortType {

	private static final Trace TRACE = TraceManager.getTrace(XmlRepositoryService.class);
	private Collection collection;

	// FIXME: switch to new version JAXB utils and remove local
	// marshaller/unmarshaller
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
			TRACE.error("Problem initializing XML Repository Service", e);
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
			TRACE.error("JAXB object marshal to Xml stream failed", e);
			throw new JAXBException("JAXB object marshal to Xml stream failed", e);
		} catch (FactoryConfigurationError e) {
			TRACE.error("JAXB object marshal to Xml stream failed", e);
			throw new JAXBException("JAXB object marshal to Xml stream failed", e);
		} catch (UnsupportedEncodingException e) {
			TRACE.error("UTF-8 is unsupported encoding", e);
			throw new JAXBException("UTF-8 is unsupported encoding", e);
		}
	}

	@Override
	public String addObject(ObjectContainerType objectContainer) throws FaultMessage {
		String oid = null;
		try {
			ObjectType payload = (ObjectType) objectContainer.getObject();

			if (StringUtils.isNotEmpty(payload.getOid())) {
				try {
					ObjectContainerType retrievedObject = getObject(payload.getOid(), null);
					if (null != retrievedObject) {
						throw new FaultMessage("Object with oid " + payload.getOid() + " already exists", new ObjectAlreadyExistsFaultType());
					}
				} catch (FaultMessage e) {
					if (e.getFaultInfo() instanceof ObjectNotFoundFaultType) {
						//ignore;
					} else {
						throw e;
					}
				}
			}
			
			// generate new oid, if necessary
			oid = (null != payload.getOid() ? payload.getOid() : UUID.randomUUID().toString());
			payload.setOid(oid);

			String serializedObject = marshalWrap(payload, SchemaConstants.C_OBJECT);
			// FIXME: try to find another solution how to escape XQuery special characters in XMLs 
			serializedObject = StringUtils.replace(serializedObject, "{", "{{");
			serializedObject = StringUtils.replace(serializedObject, "}", "}}");

			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n")
					.append("let $x := ").append(serializedObject).append("\n")
					.append("return insert node $x into //c:objects");

			TRACE.trace("generated query: " + query);

			service.query(query.toString());

			return oid;
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new FaultMessage("Failed to (un)marshal object", new IllegalArgumentFaultType(), ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType(), ex);
		}
	}

	@Override
	public ObjectContainerType getObject(String oid, PropertyReferenceListType resolve) throws FaultMessage {
		validateOid(oid);

		ByteArrayInputStream in = null;
		ObjectContainerType objectContainer = null;
		try {

			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
			query.append("for $x in //c:object where $x/@oid=\"").append(oid).append("\" return $x");

			TRACE.trace("generated query: " + query);

			ResourceSet set = service.query(query.toString());
			ResourceIterator iter = set.getIterator();
			// Loop through all result items.
			while (iter.hasMoreResources()) {
				Resource res = iter.nextResource();

				if (null != objectContainer) {
					TRACE.error("More than one object with oid {} found", oid);
					throw new FaultMessage("More than one object with oid " + oid + " found",
							new ObjectAlreadyExistsFaultType());
				}
				Object c = res.getContent();
				if (c instanceof String) {
					in = new ByteArrayInputStream(((String) c).getBytes("UTF-8"));
					JAXBElement<ObjectType> o = (JAXBElement<ObjectType>) unmarshaller.unmarshal(in);
					if (o != null) {
						objectContainer = new ObjectContainerType();
						objectContainer.setObject(o.getValue());
					}
				}
			}
		} catch (UnsupportedEncodingException ex) {
			TRACE.error("UTF-8 is unsupported encoding", ex);
			throw new FaultMessage("UTF-8 is unsupported encoding", new SystemFaultType(), ex);
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new FaultMessage("Failed to (un)marshal object", new IllegalArgumentFaultType(), ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType(), ex);
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException ex) {
			}
		}
		if (objectContainer == null) {
			throw new FaultMessage("Object not found. OID: " + oid, new ObjectNotFoundFaultType());
		}
		return objectContainer;
	}

	private ObjectListType searchObjects(String objectType, PagingType paging, Map<String, String> filters)
			throws FaultMessage {
		if (StringUtils.isEmpty(objectType)) {
			TRACE.error("objectType is empty");
			throw new FaultMessage("objectType is empty", new IllegalArgumentFaultType());
		}

		ByteArrayInputStream in = null;
		ObjectListType objectList = new ObjectListType();
		;
		try {

			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
			// TODO: namespace declaration has to be generated dynamically
			query.append("declare namespace idmdn='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
			query.append("declare namespace i='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
			query.append("declare namespace dj='http://midpoint.evolveum.com/xml/ns/samples/localhostOpenDJ';\n");
			query.append("declare namespace xsi='http://www.w3.org/2001/XMLSchema-instance';\n");
			query.append("declare namespace s='http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd';\n");
			// FIXME: possible problems with object type checking. Now it is
			// simple string checking, because import schema is not supported
			query.append("for $x in //c:object where $x/@xsi:type=\"")
					.append(objectType.substring(objectType.lastIndexOf("#") + 1)).append("\"");
			if (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) {
				query.append("[fn:position() = ( ")
						.append(paging.getOffset() * paging.getMaxSize())
						.append(" to ")
						.append(((paging.getOffset() +1) * paging.getMaxSize()) - 1).append(") ] ");
			}
			if (filters != null) {
				for (Map.Entry<String, String> filterEntry : filters.entrySet()) {
					// FIXME: now only refs are searched by attributes values
					if (StringUtils.contains(filterEntry.getKey(), "Ref")) {
						// search based on attribute value
						query.append(" and $x/").append(filterEntry.getKey()).append("/@oid='")
								.append(filterEntry.getValue()).append("'");
					} else {
						// search based on element value
						query.append(" and $x/").append(filterEntry.getKey()).append("='")
								.append(filterEntry.getValue()).append("'");
					}
				}
			}
			if (null != paging && null != paging.getOrderBy()) {
				XPathType xpath = new XPathType(paging.getOrderBy().getProperty());
				String orderBy = xpath.getXPath();
				query.append(" order by $x/").append(orderBy);
				if (null != paging.getOrderDirection()) {
					query.append(" ");
					query.append(StringUtils.lowerCase(paging.getOrderDirection().toString()));
				}
			}
			query.append(" return $x ");

			TRACE.trace("generated query: " + query);

			ResourceSet set = service.query(query.toString());
			ResourceIterator iter = set.getIterator();
			while (iter.hasMoreResources()) {
				Resource res = iter.nextResource();

				Object c = res.getContent();
				if (c instanceof String) {
					in = new ByteArrayInputStream(((String) c).getBytes("UTF-8"));
					JAXBElement<ObjectType> o = (JAXBElement<ObjectType>) unmarshaller.unmarshal(in);
					if (o != null) {
						objectList.getObject().add(o.getValue());
					}
				}
			}
		} catch (UnsupportedEncodingException ex) {
			TRACE.error("UTF-8 is unsupported encoding", ex);
			throw new FaultMessage("UTF-8 is unsupported encoding", new SystemFaultType(), ex);
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new FaultMessage("Failed to (un)marshal object", new IllegalArgumentFaultType(), ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType(), ex);
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException ex) {
			}
		}

		return objectList;
	}

	@Override
	public ObjectListType listObjects(String objectType, PagingType paging) throws FaultMessage {
		return searchObjects(objectType, paging, null);
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging) throws FaultMessage {
		validateQuery(query);

		NodeList children = query.getFilter().getChildNodes();
		String objectType = null;
		Map<String, String> filters = new HashMap<String, String>();

		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() != Node.ELEMENT_NODE) {
				// Skipping all non-element nodes
				continue;
			}

			if (!StringUtils.equals(SchemaConstants.NS_C, child.getNamespaceURI())) {
				TRACE.warn("Found query's filter element from unsupported namespace. Ignoring filter {}",
						child);
				continue;
			}

			if (validateFilterElement(SchemaConstants.NS_C, "type", child)) {
				objectType = child.getAttributes().getNamedItem("uri").getTextContent();
			} else if (validateFilterElement(SchemaConstants.NS_C, "equal", child)) {
				Node criteria = DOMUtil.getFirstChildElement(child);

				if (validateFilterElement(SchemaConstants.NS_C, "path", criteria)) {
					XPathType xpathType = new XPathType((Element) criteria);
					String parentPath = xpathType.getXPath();

					Node criteriaValueNode = DOMUtil.getNextSiblingElement(criteria);
					processValueNode(criteriaValueNode, filters, parentPath);
				}

				if (validateFilterElement(SchemaConstants.NS_C, "value", criteria)) {
					processValueNode(criteria, filters, null);
				}
			}
		}

		return searchObjects(objectType, paging, filters);
	}

	private void processValueNode(Node criteriaValueNode, Map<String, String> filters, String parentPath) {
		// TODO: Translate IllegalArgumentException to Fault types
		if (null == criteriaValueNode) {
			throw new IllegalArgumentException("Query filter does not contain any values to search by");
		}
		if (validateFilterElement(SchemaConstants.NS_C, "value", criteriaValueNode)) {
			Node firstChild = DOMUtil.getFirstChildElement(criteriaValueNode);
			if (null == firstChild) {
				throw new IllegalArgumentException("Query filter contains empty list of values to search by");
			}
			// FIXME: possible problem with prefixes
			String lastPathSegment;
			if (!StringUtils.isEmpty(firstChild.getPrefix())) {
				lastPathSegment = firstChild.getPrefix() + ":" + firstChild.getLocalName();
			} else {
				lastPathSegment = "c:" + firstChild.getLocalName();
			}
			// some search filters does not contain element's text value, for
			// these filters the value is stored in attribute
			String criteriaValue = StringUtils.trim(firstChild.getTextContent());
			if (StringUtils.isEmpty(criteriaValue)) {
				// FIXME: for now it is ok to get value of the first attribute
				if (firstChild.getAttributes().getLength() == 1) {
					criteriaValue = StringUtils.trim(firstChild.getAttributes().item(0).getNodeValue());
				} else {
					throw new IllegalArgumentException("Incorrect number of attributes in query filter "
							+ firstChild + ", expected was 1, but actual size was "
							+ criteriaValueNode.getAttributes().getLength());
				}
			}
			if (StringUtils.isEmpty(criteriaValue)) {
				throw new IllegalArgumentException("Could not extract filter value from search query filter "
						+ criteriaValueNode);
			}

			if (parentPath != null) {
				filters.put(parentPath + "/" + lastPathSegment, criteriaValue);
			} else {
				filters.put(lastPathSegment, criteriaValue);
			}

		} else {
			throw new IllegalArgumentException("Found unexpected element in query filter "
					+ criteriaValueNode);
		}
	}

	private boolean validateFilterElement(String elementNamespace, String elementName, Node criteria) {
		if (StringUtils.equals(elementName, criteria.getLocalName())
				&& StringUtils.equalsIgnoreCase(elementNamespace, criteria.getNamespaceURI())) {
			return true;
		}
		return false;
	}

	@Override
	public void modifyObject(ObjectModificationType objectChange) throws FaultMessage {
		validateObjectChange(objectChange);

		try {
			// get object from repo
			// FIXME: possible problems with resolving property reference before
			// xml patching
			ObjectContainerType retrievedObjectContainer = this.getObject(objectChange.getOid(),
					new PropertyReferenceListType());
			ObjectType objectType = retrievedObjectContainer.getObject();

			// modify the object
			PatchXml xmlPatchTool = new PatchXml();
			String serializedObject = xmlPatchTool.applyDifferences(objectChange, objectType);

			// HACK:
			serializedObject = serializedObject.substring(38);

			// store modified object in repo
			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n")
					.append("replace node //c:object[@oid=\"").append(objectChange.getOid())
					.append("\"] with ").append(serializedObject);

			TRACE.trace("generated query: " + query);

			service.query(query.toString());

		} catch (PatchException ex) {
			TRACE.error("Failed to modify object", ex);
			throw new FaultMessage("Failed to modify object", new IllegalArgumentFaultType(), ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType(), ex);
		}
	}

	@Override
	public void deleteObject(String oid) throws FaultMessage {
		validateOid(oid);

		ByteArrayInputStream in = null;
		ObjectContainerType out = null;
		try {

			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder QUERY = new StringBuilder(
					"declare namespace c='http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd';\n");
			QUERY.append("delete nodes //c:object[@oid=\"").append(oid).append("\"]");

			service.query(QUERY.toString());

		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new FaultMessage("Reported error by XML Database", new SystemFaultType());
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException ex) {
			}
		}
	}

	private void validateOid(String oid) throws FaultMessage {
		if (StringUtils.isEmpty(oid)) {
			throw new FaultMessage("Invalid OID", new IllegalArgumentFaultType());
		}

		try {
			UUID id = UUID.fromString(oid);
		} catch (IllegalArgumentException e) {
			throw new FaultMessage("Invalid OID format", new IllegalArgumentFaultType(), e);
		}
	}

	private void validateObjectChange(ObjectModificationType objectChange) throws FaultMessage {
		if (null == objectChange) {
			throw new FaultMessage("Provided null object modifications", new IllegalArgumentFaultType());
		}
		validateOid(objectChange.getOid());

		if (null == objectChange.getPropertyModification()
				|| objectChange.getPropertyModification().size() == 0) {
			throw new FaultMessage("No property modifications provided", new IllegalArgumentFaultType());
		}
	}

	private void validateQuery(QueryType query) throws FaultMessage {
		if (null == query) {
			throw new FaultMessage("Provided null query", new IllegalArgumentFaultType());
		}

		if (null == query.getFilter()) {
			throw new FaultMessage("No filter in query", new IllegalArgumentFaultType());
		}
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties) throws FaultMessage {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public UserContainerType listAccountShadowOwner(String accountOid) throws FaultMessage {
		Map<String, String> filters = new HashMap<String, String>();
		// FIXME: hardcoded prefix c:
		filters.put("c:accountRef", accountOid);
		ObjectListType retrievedObjects = searchObjects(QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE),
				null, filters);
		List<ObjectType> objects = retrievedObjects.getObject();

		if (null == retrievedObjects || objects == null || objects.size() == 0) {
			return null;
		}
		if (objects.size() > 1) {
			throw new FaultMessage("Found incorrect number of objects " + objects.size(),
					new SystemFaultType());
		}

		UserContainerType userContainer = new UserContainerType();
		userContainer.setUser((UserType) objects.get(0));

		return userContainer;
	}

	@Override
	public ResourceObjectShadowListType listResourceObjectShadows(String resourceOid,
			String resourceObjectShadowType) throws FaultMessage {
		Map<String, String> filters = new HashMap<String, String>();
		// FIXME: hardcoded prefix c:
		filters.put("c:resourceRef", resourceOid);
		ObjectListType retrievedObjects = searchObjects(
				QNameUtil.qNameToUri(SchemaConstants.I_ACCOUNT_SHADOW_TYPE), null, filters);
		
		@SuppressWarnings("unchecked")
		List<ResourceObjectShadowType> objects = (List<ResourceObjectShadowType>) CollectionUtils.collect(retrievedObjects.getObject(),
				new Transformer() {
					@Override
					public Object transform(final Object input) {
						return (ResourceObjectShadowType) input;
					}
				});

		ResourceObjectShadowListType ros = new ResourceObjectShadowListType();
		ros.getObject().addAll(objects);
		return ros;
	}

}
