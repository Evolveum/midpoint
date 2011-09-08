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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.basex.core.BaseXException;
import org.basex.server.ClientQuery;
import org.basex.server.ClientSession;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.common.patch.PatchXml;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.patch.PatchException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyAvailableValuesListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public class XmlRepositoryService implements RepositoryService {

	private static final String OBJECT_WITH_THE_SAME_NAME_ALREADY_EXISTS = "ObjectWithTheSameNameAlreadyExists";

	private static final String C_PREFIX = "c";

	private static final String DECLARE_NAMESPACE_C = "declare namespace c='" + SchemaConstants.NS_C + "';\n";
	private static final Trace LOGGER = TraceManager.getTrace(XmlRepositoryService.class);
	private ObjectPool sessions;

	// TODO: inject from Configuration Object
	private String host;
	private int port;
	private String username;
	private String password;
	private String dbName;

	XmlRepositoryService(String host, int port, String username, String password, String dbName) {
		super();
		this.host = host; 
		this.port = port;
		this.username = username; 
		this.password = password;
		this.dbName = dbName;
	}

	public void init() {
		LOGGER.info("Initialize BaseX Client sessions pool");
		// TODO: make pool configuration external
		Config poolConfig = new Config();
		poolConfig.lifo = false;
		poolConfig.maxActive = 20;
		PoolableObjectFactory factory = new BaseXClientSessionPoolableObjectFactory(host, port, username,
				password, dbName);
		sessions = new GenericObjectPool(factory, poolConfig);
	}

	public void close() {
		try {
			LOGGER.info("Releasing and closing BaseX Client sessions pool");
			sessions.close();
		} catch (Exception ex) {
			LOGGER.error("Reported exception while closing BaseX client session pool", ex);
			throw new SystemException("Reported exception while closing BaseX client session pool", ex);
		}
	}
	
	@Override
	public String addObject(ObjectType object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = null;
		ClientQuery cq = null;
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".addObject");
		result.addParam("object", object);

		StringBuilder query = new StringBuilder();
		try {
			// generate new oid, if necessary
			oid = (null != object.getOid() ? object.getOid() : UUID.randomUUID().toString());
			object.setOid(oid);

			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			String serializedObject = JAXBUtil.marshalWrap(properties, object, SchemaConstants.C_OBJECT);
			serializedObject = escapeXQuerySpecialChars(serializedObject);

			if (object instanceof ResourceObjectShadowType) {
				query.append(DECLARE_NAMESPACE_C).append("let $x := ").append(serializedObject).append("\n")
						.append("return insert node $x into //c:objects");
			} else {
				ObjectTypes objType = ObjectTypes.getObjectType(object.getClass());
				String oType = objType.getValue();

				query.append(DECLARE_NAMESPACE_C).append("if (every $object in //c:objects/c:object[")
						.append("@xsi:type='").append(oType).append("']")
						.append(" satisfies $object/c:name !='").append(object.getName()).append("' )")
						.append(" then ").append(" let $x := ").append(serializedObject).append("\n")
						.append("return insert node $x into //c:objects ").append(" else (fn:error(null,'")
						.append(OBJECT_WITH_THE_SAME_NAME_ALREADY_EXISTS).append("'))");
			}
			LOGGER.trace("generated query: " + query);
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Failed to (un)marshal object", ex);
			result.recordFatalError("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		}

		executeQuery(query, result);

		result.recordSuccess();
		return oid;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.evolveum.midpoint.repo.api.RepositoryService#getObject(java.lang.
	 * Class, java.lang.String,
	 * com.evolveum.midpoint.xml.ns._public.common.common_1
	 * .PropertyReferenceListType,
	 * com.evolveum.midpoint.common.result.OperationResult)
	 */
	@Override
	public <T extends ObjectType> T getObject(Class<T> type, String oid, PropertyReferenceListType resolve,
			OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".getObject");
		result.addParam("oid", oid);
		result.addParam("resolve", resolve);

		validateOid(oid);

		T object = null;
		ClientQuery cq = null;

		StringBuilder query = new StringBuilder();
		query.append(DECLARE_NAMESPACE_C);
		query.append("for $x in //c:object where $x/@oid=\"").append(oid).append("\" return $x");

		LOGGER.trace("generated query: " + query);

		ClientSession session = null;
		try {
			session = (ClientSession) sessions.borrowObject();

			cq = session.query(query.toString());

			if (cq.init() != null) {
				while (cq.more()) {
					String c = cq.next();

					if (null != object) {
						LOGGER.error("More than one object with oid {} found", oid);
						throw new SystemException("More than one object with oid " + oid + " found");
					}

					JAXBElement<T> o = (JAXBElement<T>) JAXBUtil.unmarshal(type, c);
					if (o != null) {
						object = o.getValue();
					}
				}
			}
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Failed to (un)marshal object", ex);
			result.recordFatalError("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (BaseXException ex) {
			errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
		} catch (NoSuchElementException ex) {
			errorLogRecordAndRethrow("No BaseX Client session in the pool", result, ex);
		} catch (IllegalStateException ex) {
			errorLogRecordAndRethrow(
					"Illegal state of BaseX Client session pool while borrowing client session", result, ex);
		} catch (Exception ex) {
			errorLogRecordAndRethrow("Error borrowing BaseX Client session from the pool", result, ex);
		} finally {
			if (session != null) {
				try {
					sessions.returnObject(session);
				} catch (Exception ex) {
					errorLogRecordAndRethrow("Error returning BaseX Client session to pool", result, ex);
				}
			}
			if (null != cq) {
				try {
					cq.close();
				} catch (BaseXException ex) {
					errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
				}
			}

		}
		if (object == null) {
			result.recordFatalError("Object not found. OID: " + oid);
			throw new ObjectNotFoundException("Object not found. OID: " + oid);
		}
		result.recordSuccess();
		return object;
	}

	@Override
	public <T extends ObjectType> List<T> listObjects(Class<T> objectType, PagingType paging,
			OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".listObjects");
		result.addParam("objectType", objectType);
		result.addParam("paging", paging);

		if (null == objectType) {
			LOGGER.error("objectType is null");
			throw new IllegalArgumentException("objectType is null");
		}

		// validate, if object type is supported
		ObjectTypes objType = ObjectTypes.getObjectType(objectType);
		String oType = objType.getValue();

		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		namespaces.put("idmdn", SchemaConstants.NS_C);

		List<T> objects = searchObjects(objectType, oType, paging, null, namespaces, result);
		result.recordSuccess();
		return objects;
	}

	@Override
	public <T extends ObjectType> List<T> searchObjects(Class<T> clazz, QueryType query, PagingType paging,
			OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".searchObjects");
		result.addParam("query", query);
		result.addParam("paging", paging);

		LOGGER.trace("midPoint query: {}", JAXBUtil.silentMarshalWrap(query));

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
				LOGGER.warn("Found query's filter element from unsupported namespace. Ignoring filter {}",
						child);
				continue;
			}

			if (validateFilterElement(SchemaConstants.NS_C, "type", child)) {
				objectType = child.getAttributes().getNamedItem("uri").getTextContent();
			} else if (validateFilterElement(SchemaConstants.NS_C, "equal", child)) {
				Node criteria = DOMUtil.getFirstChildElement(child);

				if (validateFilterElement(SchemaConstants.NS_C, "path", criteria)) {
					XPathHolder xpathType = new XPathHolder((Element) criteria);
					String parentPath = xpathType.getXPath();

					Node criteriaValueNode = DOMUtil.getNextSiblingElement(criteria);
					processValueNode(criteriaValueNode, filters, namespaces, parentPath);
				}

				if (validateFilterElement(SchemaConstants.NS_C, "value", criteria)) {
					processValueNode(criteria, filters, namespaces, null);
				}
			}
		}

		return searchObjects(clazz, objectType, paging, filters, namespaces, result);
	}
	
	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, ObjectModificationType objectChange, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".modifyObject");
		result.addParam("objectChange", objectChange);

		validateObjectChange(objectChange);

		String serializedObject = null;
		try {
			// get object from repo
			// FIXME: possible problems with resolving property reference before
			// xml patching
			ObjectType objectType = this.getObject(ObjectType.class, objectChange.getOid(), new PropertyReferenceListType(),
					result);

			// modify the object
			PatchXml xmlPatchTool = new PatchXml();
			serializedObject = xmlPatchTool.applyDifferences(objectChange, objectType);
			serializedObject = escapeXQuerySpecialChars(serializedObject);

		} catch (PatchException ex) {
			errorLogRecordAndRethrow("Failed to modify object", result, ex);
		}

		// store modified object in repo
		StringBuilder query = new StringBuilder();
		query.append(DECLARE_NAMESPACE_C).append("replace node //c:object[@oid=\"")
				.append(objectChange.getOid()).append("\"] with ").append(serializedObject);

		LOGGER.trace("generated query: " + query);

		try {
			executeQuery(query, result);
		} catch (ObjectAlreadyExistsException ex) {
			// modify query will not generate this type of exception, yet!
			errorLogRecordAndRethrow(
					"Thrown unexpected exception - modify query should not generate this type of exception",
					result, ex);
		}

		result.recordSuccess();
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult) throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".deleteObject");
		result.addParam("oid", oid);

		validateOid(oid);

		// TODO: check has to be atomic
		try {
			ObjectType retrievedObject = getObject(ObjectType.class, oid, null, result);
		} catch (SchemaException ex) {
			LoggingUtils
					.logException(
							LOGGER,
							"Schema validation problem occured while checking existence of the object before its deletion",
							ex);
			result.recordFatalError(
					"Schema validation problem occured while checking existence of the object before its deletion",
					ex);
			throw new SystemException(
					"Schema validation problem occured while checking existence of the object before its deletion",
					ex);
		}

		StringBuilder query = new StringBuilder();
		query.append(DECLARE_NAMESPACE_C);
		query.append("delete nodes //c:object[@oid=\"").append(oid).append("\"]");

		LOGGER.trace("generated query: " + query);

		try {
			executeQuery(query, result);
		} catch (ObjectAlreadyExistsException ex) {
			// delete query will not generate this type of exception
			errorLogRecordAndRethrow(
					"Thrown unexpected exception - delete query should not generate this type of exception",
					result, ex);
		}

		result.recordSuccess();
	}

	@Override
	public <T extends ObjectType> PropertyAvailableValuesListType getPropertyAvailableValues(Class<T> type, String oid,
			PropertyReferenceListType properties, OperationResult parentResult)
			throws ObjectNotFoundException {
		throw new UnsupportedOperationException("Not implemented yet.");
	}

	@Override
	public UserType listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".listAccountShadowOwner");
		result.addParam("accountOid", accountOid);

		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:accountRef", accountOid);
		List<UserType> retrievedObjects = searchObjects(UserType.class, ObjectTypes.USER.getObjectTypeUri(),
				null, filters, namespaces, result);

		if (null == retrievedObjects || retrievedObjects.size() == 0) {
			result.recordSuccess();
			return null;
		}

		if (retrievedObjects.size() > 1) {
			result.recordFatalError("Found incorrect number of objects " + retrievedObjects.size());
			throw new SystemException("Found incorrect number of objects " + retrievedObjects.size());
		}

		UserType userType = retrievedObjects.get(0);

		result.recordSuccess();
		return userType;
	}

	@Override
	public <T extends ResourceObjectShadowType> List<T> listResourceObjectShadows(String resourceOid,
			Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".listResourceObjectShadows");
		result.addParam("resourceOid", resourceOid);
		result.addParam("resourceObjectShadowType", resourceObjectShadowType);

		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:resourceRef", resourceOid);
		List<T> retrievedObjects = searchObjects(resourceObjectShadowType,
				ObjectTypes.ACCOUNT.getObjectTypeUri(), null, filters, namespaces, result);

		result.recordSuccess();
		return retrievedObjects;
	}

	@Override
	public void claimTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {

		// TODO: atomicity

		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".claimTask");
		result.addParam(OperationResult.PARAM_OID, oid);

		// Check whether the task is claimed

		ObjectType object = getObject(ObjectType.class, oid, null, result);
		// TODO: check
		TaskType task = (TaskType) object;

		if (task.getExclusivityStatus() != TaskExclusivityStatusType.RELEASED) {
			// TODO: check whether the claim is not expired yet
			throw new ConcurrencyException("Attempt to claim already claimed task (OID:" + oid + ")");
		}

		// Modify the status to claim the task.
		// TODO: mark node identifier and claim expiration (later)

		ObjectModificationType modification = ObjectTypeUtil.createModificationReplaceProperty(oid,
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS, TaskExclusivityStatusType.CLAIMED.value());

		modifyObject(TaskType.class, modification, result);

	}

	@Override
	public void releaseTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {

		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".releaseTask");
		result.addParam(OperationResult.PARAM_OID, oid);

		// Modify the status to claim the task.

		ObjectModificationType modification = ObjectTypeUtil.createModificationReplaceProperty(oid,
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS, TaskExclusivityStatusType.RELEASED.value());

		modifyObject(TaskType.class, modification, result);

	}

	private <T extends ObjectType> List<T> searchObjects(Class<T> clazz, String objectType,
			PagingType paging, Map<String, String> filters, Map<String, String> namespaces,
			OperationResult result) {

		List<T> objectList = new ArrayList<T>();
		// FIXME: objectList.count has to contain all elements that match search
		// criteria, but not only from paging interval
		StringBuilder query = new StringBuilder();

		if (namespaces != null) {
			for (Entry<String, String> namespaceEntry : namespaces.entrySet()) {
				query.append("declare namespace ").append(namespaceEntry.getKey()).append("='")
						.append(namespaceEntry.getValue()).append("';\n");
			}
		}

		query.append("for $x in //c:object ");
		if (objectType != null
				|| (null != paging && null != paging.getOffset() && null != paging.getMaxSize())
				|| (filters != null && !filters.isEmpty())) {
			query.append("where ");
		}
		if (objectType != null) {
			// FIXME: possible problems with object type checking. Now it is
			// simple string checking, because import schema is not
			// supported by basex database
			query.append("$x/@xsi:type=\"").append(objectType.substring(objectType.lastIndexOf("#") + 1))
					.append("\"");
		}
		if (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) {
			query.append("[fn:position() = ( ").append(paging.getOffset() * paging.getMaxSize())
					.append(" to ").append(((paging.getOffset() + 1) * paging.getMaxSize()) - 1)
					.append(") ] ");
		}
		if (filters != null) {
			int pos = 0;
			for (Map.Entry<String, String> filterEntry : filters.entrySet()) {
				if ((pos > 0) || (pos == 0 && (objectType != null))) {
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
			XPathHolder xpath = new XPathHolder(paging.getOrderBy().getProperty());
			String orderBy = xpath.getXPath();
			query.append(" order by $x/").append(orderBy);
			if (null != paging.getOrderDirection()) {
				query.append(" ");
				query.append(StringUtils.lowerCase(paging.getOrderDirection().toString()));
			}
		}
		query.append(" return $x ");

		LOGGER.trace("generated query: " + query);

		ClientQuery cq = null;
		ClientSession session = null;
		try {
			session = (ClientSession) sessions.borrowObject();
			cq = session.query(query.toString());
			if (null != cq.init()) {
				while (cq.more()) {
					String c = cq.next();

					JAXBElement<T> o = JAXBUtil.unmarshal(clazz, c);
					if (o != null) {
						objectList.add(o.getValue());
					}
				}
			}
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Failed to (un)marshal object", ex);
			result.recordFatalError("Failed to (un)marshal object", ex);
			throw new IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (BaseXException ex) {
			errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
		} catch (NoSuchElementException ex) {
			errorLogRecordAndRethrow("No BaseX Client session in the pool", result, ex);
		} catch (IllegalStateException ex) {
			errorLogRecordAndRethrow(
					"Illegal state of BaseX Client session pool while borrowing client session", result, ex);
		} catch (Exception ex) {
			errorLogRecordAndRethrow("Error borrowing BaseX Client session from the pool", result, ex);
		} finally {
			if (session != null) {
				try {
					sessions.returnObject(session);
				} catch (Exception ex) {
					errorLogRecordAndRethrow("Error returning BaseX Client session to pool", result, ex);
				}
			}
			if (null != cq) {
				try {
					cq.close();
				} catch (BaseXException ex) {
					errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
				}
			}
		}

		result.recordSuccess();
		return objectList;
	}

	private void processValueNode(Node criteriaValueNode, Map<String, String> filters,
			Map<String, String> namespaces, String parentPath) {
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
				// if element has no prefix, then check if it has
				// defined/overriden default namespace
				String defaultNamespace = firstChild.lookupNamespaceURI(null);
				if (StringUtils.isNotEmpty(defaultNamespace)) {
					// FIXME: possible problem with many generated prefixes
					prefix = "ns" + (new Random()).nextInt(10000);
					namespace = defaultNamespace;
					lastPathSegment = prefix + ":" + firstChild.getLocalName();
				} else {
					// default action: no prefix, no default namespace
					prefix = C_PREFIX;
					namespace = SchemaConstants.NS_C;
					lastPathSegment = prefix + ":" + firstChild.getLocalName();
				}
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

	private void errorLogRecordAndRethrow(String message, OperationResult result, Exception ex) {
		LoggingUtils.logException(LOGGER, message, ex);
		result.recordFatalError(message, ex);
		throw new SystemException(message, ex);
	}

	private String escapeXQuerySpecialChars(String serializedObject) {
		if (serializedObject != null) {
			// FIXME: try to find another solution how to escape XQuery
			// special characters in XMLs
			serializedObject = StringUtils.replace(serializedObject, "{", "{{");
			serializedObject = StringUtils.replace(serializedObject, "}", "}}");
		}
		
		return serializedObject;
	}

	private void executeQuery(StringBuilder query, OperationResult result)
			throws ObjectAlreadyExistsException {
		ClientQuery cq;
		ClientSession session = null;
		try {
			session = (ClientSession) sessions.borrowObject();
			cq = session.query(query.toString());
			cq.execute();
		} catch (BaseXException ex) {
			if (StringUtils.contains(ex.getMessage(), OBJECT_WITH_THE_SAME_NAME_ALREADY_EXISTS)) {
				result.recordFatalError("Object with the same name already exists");
				throw new ObjectAlreadyExistsException("Object with the same name already exists");
			} else {
				errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
			}
		} catch (NoSuchElementException ex) {
			errorLogRecordAndRethrow("No BaseX Client session in the pool", result, ex);
		} catch (IllegalStateException ex) {
			errorLogRecordAndRethrow(
					"Illegal state of BaseX Client session pool while borrowing client session", result, ex);
		} catch (Exception ex) {
			errorLogRecordAndRethrow("Error borrowing BaseX Client session from the pool", result, ex);
		} finally {
			if (session != null) {
				try {
					sessions.returnObject(session);
				} catch (Exception ex) {
					errorLogRecordAndRethrow("Error returning BaseX Client session to pool", result, ex);
				}
			}
		}
	}

}
