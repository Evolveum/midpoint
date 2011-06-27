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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

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
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathType;

public class XmlRepositoryService implements RepositoryService {

	private static final String DECLARE_NAMESPACE_C = "declare namespace c='"+SchemaConstants.NS_C+"';\n";
	private static final Trace TRACE = TraceManager.getTrace(XmlRepositoryService.class);
	private Collection collection;

	XmlRepositoryService(Collection collection) {
		super();
		this.collection = collection;

	}

	@Override
	public String addObject(ObjectType object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = null;
		try {

			checkAndFailIfObjectAlreadyExists(object.getOid());

			// generate new oid, if necessary
			oid = (null != object.getOid() ? object.getOid() : UUID.randomUUID().toString());
			object.setOid(oid);

			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			String serializedObject = JAXBUtil.marshalWrap(properties, object, SchemaConstants.C_OBJECT);
			// FIXME: try to find another solution how to escape XQuery special
			// characters in XMLs
			serializedObject = StringUtils.replace(serializedObject, "{", "{{");
			serializedObject = StringUtils.replace(serializedObject, "}", "}}");

			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					DECLARE_NAMESPACE_C)
					.append("let $x := ").append(serializedObject).append("\n")
					.append("return insert node $x into //c:objects");

			TRACE.trace("generated query: " + query);

			service.query(query.toString());

			return oid;
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new SystemException("Reported error by XML Database", ex);
		}
	}

	@Override
	public ObjectType getObject(String oid, PropertyReferenceListType resolve, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {

		validateOid(oid);

		ByteArrayInputStream in = null;
		ObjectType object = null;
		try {

			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					DECLARE_NAMESPACE_C);
			query.append("for $x in //c:object where $x/@oid=\"").append(oid).append("\" return $x");

			TRACE.trace("generated query: " + query);

			ResourceSet set = service.query(query.toString());
			ResourceIterator iter = set.getIterator();
			// Loop through all result items.
			while (iter.hasMoreResources()) {
				Resource res = iter.nextResource();

				if (null != object) {
					TRACE.error("More than one object with oid {} found", oid);
					throw new SystemException("More than one object with oid " + oid + " found");
				}
				Object c = res.getContent();
				if (c instanceof String) {
					in = new ByteArrayInputStream(((String) c).getBytes("UTF-8"));
					JAXBElement<ObjectType> o = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(in);
					if (o != null) {
						object = o.getValue();
					}
				}
			}
		} catch (UnsupportedEncodingException ex) {
			TRACE.error("UTF-8 is unsupported encoding", ex);
			throw new SystemException("UTF-8 is unsupported encoding", ex);
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new SystemException("Reported error by XML Database", ex);
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException ex) {
			}
		}
		if (object == null) {
			throw new ObjectNotFoundException("Object not found. OID: " + oid);
		}
		return object;
	}

	@Override
	public ObjectListType listObjects(Class objectType, PagingType paging, OperationResult parentResult) {
		if (null == objectType) {
			TRACE.error("objectType is null");
			throw new IllegalArgumentException("objectType is null");
		}
		
		//validate, if object type is supported
		ObjectTypes objType = ObjectTypes.getObjectType(objectType);
		String oType = objType.getValue();

		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		namespaces.put("idmdn", SchemaConstants.NS_C);
		
		return searchObjects(oType, paging, null, namespaces);
	}

	@Override
	public ObjectListType searchObjects(QueryType query, PagingType paging, OperationResult parentResult)
			throws SchemaException {
		validateQuery(query);

		NodeList children = query.getFilter().getChildNodes();
		String objectType = null;
		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		namespaces.put("idmdn", SchemaConstants.NS_C);
		
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
					processValueNode(criteriaValueNode, filters, namespaces, parentPath);
				}

				if (validateFilterElement(SchemaConstants.NS_C, "value", criteria)) {
					processValueNode(criteria, filters, namespaces, null);
				}
			}
		}

		return searchObjects(objectType, paging, filters, namespaces);
	}

	@Override
	public void modifyObject(ObjectModificationType objectChange, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		validateObjectChange(objectChange);

		try {
			// get object from repo
			// FIXME: possible problems with resolving property reference before
			// xml patching
			ObjectType objectType = this.getObject(objectChange.getOid(),
					new PropertyReferenceListType(), null);

			// modify the object
			PatchXml xmlPatchTool = new PatchXml();
			String serializedObject = xmlPatchTool.applyDifferences(objectChange, objectType);

			// store modified object in repo
			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder(
					DECLARE_NAMESPACE_C)
					.append("replace node //c:object[@oid=\"").append(objectChange.getOid())
					.append("\"] with ").append(serializedObject);

			TRACE.trace("generated query: " + query);

			service.query(query.toString());

		} catch (PatchException ex) {
			TRACE.error("Failed to modify object", ex);
			throw new SystemException("Failed to modify object", ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new SystemException("Reported error by XML Database", ex);
		}
	}

	@Override
	public void deleteObject(String oid, OperationResult parentResult) throws ObjectNotFoundException {
		validateOid(oid);

		try {
			ObjectType retrievedObject = getObject(oid, null, null);
		} catch (SchemaException ex) {
			TRACE.error("Schema validation problem occured while checking existence of the object before its deletion", ex);
			throw new SystemException("Schema validation problem occured while checking existence of the object before its deletion", ex);
		}

		ByteArrayInputStream in = null;
		ObjectContainerType out = null;
		try {

			// Receive the XPath query service.
			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder QUERY = new StringBuilder(
					DECLARE_NAMESPACE_C);
			QUERY.append("delete nodes //c:object[@oid=\"").append(oid).append("\"]");

			service.query(QUERY.toString());

		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new SystemException("Reported error by XML Database");
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException ex) {
			}
		}
	}

	@Override
	public PropertyAvailableValuesListType getPropertyAvailableValues(String oid,
			PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:accountRef", accountOid);
		ObjectListType retrievedObjects = searchObjects(ObjectTypes.USER.getObjectTypeUri(), null, filters, namespaces);
		List<ObjectType> objects = retrievedObjects.getObject();

		if (null == retrievedObjects || objects == null || objects.size() == 0) {
			return null;
		}
		if (objects.size() > 1) {
			throw new SystemException("Found incorrect number of objects " + objects.size());
		}

		UserType userType = (UserType) objects.get(0);

		return userType;
	}

	@Override
	public List<ResourceObjectShadowType> listResourceObjectShadows(String resourceOid,
			Class resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException {
		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:resourceRef", resourceOid);
		ObjectListType retrievedObjects = searchObjects(ObjectTypes.ACCOUNT.getObjectTypeUri(), null, filters, namespaces);

		@SuppressWarnings("unchecked")
		List<ResourceObjectShadowType> objects = (List<ResourceObjectShadowType>) CollectionUtils.collect(
				retrievedObjects.getObject(), new Transformer() {
					@Override
					public Object transform(final Object input) {
						return (ResourceObjectShadowType) input;
					}
				});

		List<ResourceObjectShadowType> ros = new ArrayList<ResourceObjectShadowType>();
		ros.addAll(objects);
		return ros;
	}

	private void checkAndFailIfObjectAlreadyExists(String oid) throws ObjectAlreadyExistsException, SchemaException {
		// check if object with the same oid already exists, if yes, then fail
		if (StringUtils.isNotEmpty(oid)) {
			try {
				ObjectType retrievedObject = getObject(oid, null, null);
				if (null != retrievedObject) {
					throw new ObjectAlreadyExistsException("Object with oid " + oid + " already exists");
				}
			} catch (ObjectNotFoundException e) {
				//ignore
			}
		}
	}

	private ObjectListType searchObjects(String objectType, PagingType paging, Map<String, String> filters, Map<String, String> namespaces) {

		ByteArrayInputStream in = null;
		ObjectListType objectList = new ObjectListType();
		// FIXME: objectList.count has to contain all elements that match search
		// criteria, but not only from paging interval
		objectList.setCount(0);
		try {

			XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");

			StringBuilder query = new StringBuilder();
			if (namespaces != null) {
				for (Entry<String, String> namespaceEntry: namespaces.entrySet()) {
					query.append("declare namespace ").append(namespaceEntry.getKey()).append("='").append(namespaceEntry.getValue()).append("';\n");
				}
			}

			query.append("for $x in //c:object ");
			if (objectType != null || (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) || filters != null) {
				query.append("where ");
			}
			if (objectType != null) {
				// FIXME: possible problems with object type checking. Now it is
				// simple string checking, because import schema is not supported by basex database
				query.append("$x/@xsi:type=\"")
					.append(objectType.substring(objectType.lastIndexOf("#") + 1)).append("\"");
			}
			if (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) {
				query.append("[fn:position() = ( ").append(paging.getOffset() * paging.getMaxSize())
						.append(" to ").append(((paging.getOffset() + 1) * paging.getMaxSize()) - 1)
						.append(") ] ");
			}
			if (filters != null) {
				int pos = 0;
				for (Map.Entry<String, String> filterEntry : filters.entrySet()) {
					if ( (pos > 0) || ( pos == 0 && (objectType != null) ) ) {
						query.append(" and ");
					}
					// FIXME: now only refs are searched by attributes values
					if (StringUtils.contains(filterEntry.getKey(), "Ref")) {
						// search based on attribute value
						query.append("$x/").append(filterEntry.getKey()).append("/@oid='")
								.append(filterEntry.getValue()).append("'");
					} else {
						// search based on element value
						query.append("$x/").append(filterEntry.getKey()).append("='")
								.append(filterEntry.getValue()).append("'");
					}
					
					pos++;
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
					JAXBElement<ObjectType> o = (JAXBElement<ObjectType>) JAXBUtil.unmarshal(in);
					if (o != null) {
						objectList.getObject().add(o.getValue());
					}
				}
			}
		} catch (UnsupportedEncodingException ex) {
			TRACE.error("UTF-8 is unsupported encoding", ex);
			throw new SystemException("UTF-8 is unsupported encoding", ex);
		} catch (JAXBException ex) {
			TRACE.error("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (XMLDBException ex) {
			TRACE.error("Reported error by XML Database", ex);
			throw new SystemException("Reported error by XML Database", ex);
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

	private void processValueNode(Node criteriaValueNode, Map<String, String> filters, Map<String, String> namespaces, String parentPath) {
		if (null == criteriaValueNode) {
			throw new IllegalArgumentException("Query filter does not contain any values to search by");
		}
		if (validateFilterElement(SchemaConstants.NS_C, "value", criteriaValueNode)) {
			Node firstChild = DOMUtil.getFirstChildElement(criteriaValueNode);
			if (null == firstChild) {
				throw new IllegalArgumentException("Query filter contains empty list of values to search by");
			}
			String lastPathSegment;
			String prefix;
			String namespace;
			if (!StringUtils.isEmpty(firstChild.getPrefix())) {
				prefix = firstChild.getPrefix();
				namespace = firstChild.getNamespaceURI();
				lastPathSegment = prefix + ":" + firstChild.getLocalName();
			} else {
				prefix = "c:";
				namespace = SchemaConstants.NS_C;
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
			namespaces.put(prefix, namespace);

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

	private void validateOid(String oid) {
		if (StringUtils.isEmpty(oid)) {
			throw new IllegalArgumentException("Invalid OID");
		}

		try {
			UUID.fromString(oid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid OID format " + oid);
		}
	}

	private void validateObjectChange(ObjectModificationType objectChange) {
		if (null == objectChange) {
			throw new IllegalArgumentException("Provided null object modifications");
		}
		validateOid(objectChange.getOid());

		if (null == objectChange.getPropertyModification()
				|| objectChange.getPropertyModification().size() == 0) {
			throw new IllegalArgumentException("No property modifications provided");
		}
	}

	private void validateQuery(QueryType query) {
		if (null == query) {
			throw new IllegalArgumentException("Provided null query");
		}

		if (null == query.getFilter()) {
			throw new IllegalArgumentException("No filter in query");
		}
	}
}
