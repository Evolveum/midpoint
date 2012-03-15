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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.UUID;
import javax.xml.bind.JAXBException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.basex.core.BaseXException;
import org.basex.server.ClientQuery;
import org.basex.server.ClientSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultArrayList;
import com.evolveum.midpoint.schema.ResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.EscapeStringBuilder;
import com.evolveum.midpoint.util.XQueryEscapeStringBuilder;
import com.evolveum.midpoint.util.exception.ConcurrencyException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

public class XmlRepositoryService implements RepositoryService {

	private static final String OBJECT_ALREADY_EXISTS = "ObjectAlreadyExists";

	private static final String C_PREFIX = "c";

	private static final String DECLARE_NAMESPACE_C = "declare namespace c='" + SchemaConstants.NS_C + "';\n";
	private static final Trace LOGGER = TraceManager.getTrace(XmlRepositoryService.class);
	private ObjectPool sessions;

	@Autowired(required = true)
	private PrismContext prismContext;

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
		PoolableObjectFactory factory = new BaseXClientSessionPoolableObjectFactory(host, port, username, password, dbName);
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
	public <T extends ObjectType> String addObject(PrismObject<T> object, OperationResult parentResult)
			throws ObjectAlreadyExistsException, SchemaException {
		String oid = null;
//		ClientQuery cq = null;
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".addObject");
		result.addParam("object", object);

		if (object.getDefinition() != null) {
			object.setName(object.getDefinition().getName());
		}

		if (object.getName() == null) {
			String message = "Attempt to add object without name";
			result.recordFatalError(message);
			throw new SchemaException(message);
		}

		EscapeStringBuilder query = new XQueryEscapeStringBuilder();
		String origOid = null;
		try {
			// generate new oid, if necessary
			origOid = object.getOid();
			oid = (StringUtils.isNotEmpty(object.getOid()) ? object.getOid() : UUID.randomUUID().toString());
			object.setOid(oid);

			// Map<String, Object> properties = new HashMap<String, Object>();
			// properties.put(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
			String serializedObject = prismContext.getPrismDomProcessor().serializeObjectToString(object);
			// String serializedObject = JAXBUtil.marshalWrap(properties,
			// object, SchemaConstants.C_OBJECT);
			if (serializedObject == null) {
				throw new IllegalArgumentException("Serialized object is null.");
			}
			if (object.canRepresent(ResourceObjectShadowType.class)) {
				query.append(DECLARE_NAMESPACE_C).append("let $x := ").eappend(serializedObject).append("\n")
						.append("return insert node $x into //c:objects");
			} else {
				// FIXME?

				String containerName = object.getDefinition().getName().getLocalPart();
				query.append(DECLARE_NAMESPACE_C).append("if (every $object in //c:objects/c:")
						.append(containerName).append(" satisfies $object/c:name !='")
						.eappend(object.findProperty(ObjectType.F_NAME).getRealValue())
						.append("' and $object[@oid!='").eappend(oid).append("']")
						.append(" ) then  let $x := ").eappend(serializedObject).append("\n")
						.append("return insert node $x into //c:objects else (fn:error(null,'")
						.append(OBJECT_ALREADY_EXISTS).append("'))");
			}
			LOGGER.trace("generated query: " + query);

			executeQuery(query.toStringBuilder(), result);

			result.recordSuccess();
			return oid;
			// } catch (SchemaException ex) {
			// LoggingUtils.logException(LOGGER, "Failed to (un)marshal object",
			// ex);
			// result.recordFatalError("Failed to (un)marshal object", ex);
			// object.setOid(origOid);
			// throw new
			// IllegalArgumentException("Failed to (un)marshal object", ex);
		} catch (RuntimeException ex) {
			object.setOid(origOid);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			object.setOid(origOid);
			throw ex;
		}
		// catch (SchemaException ex) {
		// object.setOid(null);
		// throw ex;
		// }

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
	public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
			PropertyReferenceListType resolve, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {

		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".getObject");
		result.addParam(OperationResult.PARAM_OID, oid);
		result.addParam(OperationResult.PARAM_TYPE, type);
		result.addParam("resolve", resolve);

		validateOid(oid);

		PrismObject<T> object = null;
		ClientQuery cq = null;

		String containerName = prismContext.getSchemaRegistry().determineDefinitionFromClass(type).getName()
				.getLocalPart();

		EscapeStringBuilder query = new XQueryEscapeStringBuilder();
		query.append(DECLARE_NAMESPACE_C);
		query.append("for $x in //c:").append(containerName).append(" where $x/@oid=\"").eappend(oid)
				.append("\" return $x");

		LOGGER.trace("GET generated query: " + query);

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

					object = prismContext.getPrismDomProcessor().parseObject(c, type);

					// JAXBElement<T> o = (JAXBElement<T>)
					// JAXBUtil.unmarshal(type, c);
					// if (o != null) {
					// object = o.getValue();
					// }
				}
			}
		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER, "Schema error", ex);
			result.recordFatalError("Schema error: " + ex.getMessage(), ex);
			throw new IllegalArgumentException("Schema error: " + ex.getMessage(), ex);
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
			if (null != cq) {
				try {
					cq.close();
				} catch (BaseXException ex) {
					errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
				}
			}
			if (session != null) {
				try {
					sessions.returnObject(session);
				} catch (Exception ex) {
					errorLogRecordAndRethrow("Error returning BaseX Client session to pool", result, ex);
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
	public <T extends ObjectType> ResultList<PrismObject<T>> listObjects(Class<T> objectType,
			PagingType paging, OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".listObjects");
		result.addParam("objectType", objectType);
		result.addParam("paging", paging);

		if (null == objectType) {
			LOGGER.error("objectType is null");
			throw new IllegalArgumentException("objectType is null");
		}

		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		namespaces.put("idmdn", SchemaConstants.NS_C);

		ResultList<PrismObject<T>> objects = searchObjects(objectType, paging, null, namespaces, result);
		result.recordSuccess();
		return objects;
	}

	@Override
	public <T extends ObjectType> ResultList<PrismObject<T>> searchObjects(Class<T> clazz, QueryType query,
			PagingType paging, OperationResult parentResult) throws SchemaException {
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".searchObjects");
		result.addParam("query", query);
		result.addParam("paging", paging);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("midPoint query: {}", QueryUtil.dump(query));
		}

		validateQuery(query);

		// String objectType = null;
		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		namespaces.put("idmdn", SchemaConstants.NS_C);

		if (validateFilterElement(SchemaConstants.NS_C, "and", query.getFilter())) {
			NodeList children = query.getFilter().getChildNodes();

			for (int index = 0; index < children.getLength(); index++) {
				Node child = children.item(index);
				if (child.getNodeType() != Node.ELEMENT_NODE) {
					// Skipping all non-element nodes
					continue;
				}

				if (!StringUtils.equals(SchemaConstants.NS_C, child.getNamespaceURI())) {
					LOGGER.warn(
							"Found query's filter element from unsupported namespace. Ignoring filter {}",
							child);
					continue;
				}

				processFilterBody(filters, namespaces, child);
			}
		} else {
			processFilterBody(filters, namespaces, query.getFilter());
		}

		return searchObjects(clazz, paging, filters, namespaces, result);
	}

	private void processFilterBody(Map<String, String> filters, Map<String, String> namespaces, Node child) {
		if (validateFilterElement(SchemaConstants.NS_C, "type", child)) {
			// objectType =
			// child.getAttributes().getNamedItem("uri").getTextContent();
			LOGGER.warn("Found deprecated argument c:type in search filter (MID-395). Ignoring it.");
		} else if (validateFilterElement(SchemaConstants.NS_C, "equal", child)) {
			Node criteria = DOMUtil.getFirstChildElement(child);

			if (validateFilterElement(SchemaConstants.NS_C, "path", criteria)) {
				XPathHolder xpathType = new XPathHolder((Element) criteria);
				String parentPath = xpathType.getXPath();

				Node criteriaValueNode = DOMUtil.getNextSiblingElement(criteria);
				processValueNode(criteriaValueNode, filters, namespaces, parentPath);
			}

			if (validateFilterElement(SchemaConstants.NS_C, "value", criteria)) {
				Node criteriaPathNode = DOMUtil.getNextSiblingElement(criteria);
				String parentPath = null;
				if (criteriaPathNode != null && validateFilterElement(SchemaConstants.NS_C, "path", criteriaPathNode)){
					XPathHolder xpathType = new XPathHolder((Element) criteriaPathNode);
					parentPath = xpathType.getXPath();
				}
				processValueNode(criteria, filters, namespaces, parentPath);
			}
		}
	}

	@Override
	public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
			Collection<? extends ItemDelta> modifications, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".modifyObject");
		result.addParam(OperationResult.PARAM_OID, modifications);
		result.addParam("objectChange", modifications);

		validateOid(oid);
		validateObjectModifications(modifications);

		PrismObject<T> object = this.getObject(type, oid, null, result);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("OBJECT before:\n{}", object.dump());
			LOGGER.trace("MODIFICATIONS:\n{}", DebugUtil.prettyPrint(modifications));
		}

		PropertyDelta.applyTo(modifications, object);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("OBJECT after:\n{}", object.dump());
		}

		String serializedObject = prismContext.getPrismDomProcessor().serializeObjectToString(object);

		String containerName = prismContext.getSchemaRegistry().determineDefinitionFromClass(type).getName()
				.getLocalPart();
		// store modified object in repo
		EscapeStringBuilder query = new XQueryEscapeStringBuilder();
		query.append(DECLARE_NAMESPACE_C).append("replace node //c:").append(containerName)
				.append("[@oid=\"").eappend(oid).append("\"] with ").eappend(serializedObject);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("generated query: " + query);
		}

		try {
			executeQuery(query.toStringBuilder(), result);
		} catch (ObjectAlreadyExistsException ex) {
			// modify query will not generate this type of exception, yet!
			errorLogRecordAndRethrow(
					"Thrown unexpected exception - modify query should not generate this type of exception",
					result, ex);
		}

		result.recordSuccess();
	}

	@Override
	public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult parentResult)
			throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".deleteObject");
		result.addParam("oid", oid);

		validateOid(oid);

		// TODO: check has to be atomic
		try {
			PrismObject<T> retrievedObject = getObject(type, oid, null, result);
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
		} catch (ObjectNotFoundException ex) {
			result.computeStatus("Trying to delete not existing object");
			throw ex;
		}

		String containerName = prismContext.getSchemaRegistry().determineDefinitionFromClass(type).getName()
				.getLocalPart();

		EscapeStringBuilder query = new XQueryEscapeStringBuilder();
		query.append(DECLARE_NAMESPACE_C);
		query.append("delete nodes //c:").append(containerName).append("[@oid=\"").eappend(oid).append("\"]");

		LOGGER.trace("DELETE generated query: " + query);

		try {
			executeQuery(query.toStringBuilder(), result);
		} catch (ObjectAlreadyExistsException ex) {
			// delete query will not generate this type of exception
			errorLogRecordAndRethrow(
					"Thrown unexpected exception - delete query should not generate this type of exception",
					result, ex);
		}

		result.recordSuccess();
	}

	@Override
	public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult parentResult)
			throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".listAccountShadowOwner");
		result.addParam("accountOid", accountOid);

		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:accountRef", accountOid);
		List<PrismObject<UserType>> retrievedObjects = searchObjects(UserType.class, null, filters,
				namespaces, result);

		if (null == retrievedObjects || retrievedObjects.size() == 0) {
			result.recordSuccess();
			return null;
		}

		if (retrievedObjects.size() > 1) {
			result.recordFatalError("Found incorrect number of objects " + retrievedObjects.size());
			throw new SystemException("Found incorrect number of objects " + retrievedObjects.size());
		}

		PrismObject<UserType> userType = retrievedObjects.get(0);

		result.recordSuccess();
		return userType;
	}

	@Override
	public <T extends ResourceObjectShadowType> ResultList<PrismObject<T>> listResourceObjectShadows(
			String resourceOid, Class<T> resourceObjectShadowType, OperationResult parentResult)
			throws ObjectNotFoundException {
		OperationResult result = parentResult.createSubresult(XmlRepositoryService.class.getName()
				+ ".listResourceObjectShadows");
		result.addParam("resourceOid", resourceOid);
		result.addParam("resourceObjectShadowType", resourceObjectShadowType);

		Map<String, String> filters = new HashMap<String, String>();
		Map<String, String> namespaces = new HashMap<String, String>();
		namespaces.put("c", SchemaConstants.NS_C);
		filters.put("c:resourceRef", resourceOid);
		ResultList<PrismObject<T>> retrievedObjects = searchObjects(resourceObjectShadowType, null, filters,
				namespaces, result);

		result.recordSuccess();
		return retrievedObjects;
	}

	@Override
	public void claimTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			ConcurrencyException, SchemaException {

		// TODO: atomicity

		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".claimTask");
		result.addParam(OperationResult.PARAM_OID, oid);

		// Check whether the task is claimed

		PrismObject<TaskType> task = getObject(TaskType.class, oid, null, result);
		TaskType taskType = task.asObjectable();

		if (taskType.getExclusivityStatus() != TaskExclusivityStatusType.RELEASED) {
			// TODO: check whether the claim is not expired yet
			throw new ConcurrencyException("Attempt to claim already claimed task (OID:" + oid + ")");
		}

		// Modify the status to claim the task.
		// TODO: mark node identifier and claim expiration (later)

		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>(1);
		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS, prismContext.getSchemaRegistry()
						.findObjectDefinitionByCompileTimeClass(TaskType.class),
				TaskExclusivityStatusType.CLAIMED.value());
		modifications.add(delta);

		modifyObject(TaskType.class, oid, modifications, result);

	}

	@Override
	public void releaseTask(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {

		OperationResult result = parentResult.createSubresult(RepositoryService.class.getName()
				+ ".releaseTask");
		result.addParam(OperationResult.PARAM_OID, oid);

		// Modify the status to claim the task.

		Collection<PropertyDelta> modifications = new ArrayList<PropertyDelta>(1);
		PropertyDelta delta = PropertyDelta.createModificationReplaceProperty(
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS, prismContext.getSchemaRegistry()
						.findObjectDefinitionByCompileTimeClass(TaskType.class),
				TaskExclusivityStatusType.RELEASED.value());
		modifications.add(delta);

		modifyObject(TaskType.class, oid, modifications, result);

	}

	private <T extends ObjectType> ResultList<PrismObject<T>> searchObjects(Class<T> clazz,
			PagingType paging, Map<String, String> filters, Map<String, String> namespaces,
			OperationResult result) {

		// convert class object type to String representation
		String objectType = null;
//		if (ObjectType.class.equals(clazz)) {
//			// object type will be empty, because we wan't to search through all
//			// the objects
//		} else {
//			objectType = ObjectTypes.getObjectType(clazz).getValue();
//		}

		ResultList<PrismObject<T>> objectList = new ResultArrayList<PrismObject<T>>();
		// FIXME: objectList.count has to contain all elements that match search
		// criteria, but not only from paging interval
		EscapeStringBuilder query = new XQueryEscapeStringBuilder();

		if (namespaces != null) {
			for (Entry<String, String> namespaceEntry : namespaces.entrySet()) {
				query.append("declare namespace ").append(namespaceEntry.getKey()).append("='")
						.append(namespaceEntry.getValue()).append("';\n");
			}
		}

		if (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) {
			query.append("subsequence(");
		}

	
		if (ObjectType.class.equals(clazz)) {
			query.append("for $x in //.");
		} else {
			String containerName = prismContext.getSchemaRegistry().determineDefinitionFromClass(clazz)
					.getName().getLocalPart();

			query.append("for $x in //c:").append(containerName);
		}
		if (objectType != null
				|| (null != paging && null != paging.getOffset() && null != paging.getMaxSize())
				|| (filters != null && !filters.isEmpty())) {
			// query.append(" where ");
		}
		if (objectType != null) {
			// FIXME: possible problems with object type checking. Now it is
			// simple string checking, because import schema is not
			// supported by basex database
			// query.append("($x/@xsi:type=\"").append(objectType).append("\"");
			if (objectType.equals(ObjectTypes.RESOURCE_OBJECT_SHADOW.getValue())) {
				// HACK. This is the only type in schema that has subtype.
				// Therefore append
				// also the subtype in the "or" statement
				// query.append(" or $x/@xsi:type=\"").append(ObjectTypes.ACCOUNT.getValue()).append("\"");
			}
			// query.append(")");
		}
		if (filters != null) {
			if (!filters.isEmpty()) {
				query.append(" where ");
			}
			int pos = 0;
			for (Map.Entry<String, String> filterEntry : filters.entrySet()) {
				if (pos > 0) {
					query.append(" and ");
				}
				// FIXME: now only refs are searched by attributes values
				if (StringUtils.contains(filterEntry.getKey(), "Ref")) {
					// search based on attribute value
					query.append(" $x/").append(filterEntry.getKey()).append("/@oid='")
							.eappend(filterEntry.getValue()).append("'");
				} else {
					// search based on element value
					query.append(" $x/").append(filterEntry.getKey()).append("='")
							.eappend(filterEntry.getValue()).append("'");
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
		query.append(" return $x");
		if (null != paging && null != paging.getOffset() && null != paging.getMaxSize()) {
			long from = paging.getOffset() + 1;
			long to = paging.getMaxSize();
			query.append(", ").append(from).append(", ").append(to).append(") ");

		}

		LOGGER.trace("SEARCH generated query: " + query);

		ClientQuery cq = null;
		ClientSession session = null;
		try {
			session = (ClientSession) sessions.borrowObject();
			cq = session.query(query.toString());
			if (null != cq.init()) {
				while (cq.more()) {
					String c = cq.next();

					PrismObject<T> object = prismContext.getPrismDomProcessor().parseObject(c, clazz);
					if (object != null) {
						objectList.add(object);
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
			if (null != cq) {
				try {
					cq.close();
				} catch (BaseXException ex) {
					errorLogRecordAndRethrow("Reported error by XML Database", result, ex);
				}
			}

			if (session != null) {
				try {
					sessions.returnObject(session);
				} catch (Exception ex) {
					errorLogRecordAndRethrow("Error returning BaseX Client session to pool", result, ex);
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
			throw new IllegalArgumentException("Invalid OID: OID is null or empty");
		}

		try {
			UUID.fromString(oid);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid OID format " + oid);
		}
	}

	private void validateObjectModifications(Collection<? extends ItemDelta> modifications) {
		if (null == modifications) {
			throw new IllegalArgumentException("Provided null object modifications");
		}
		if (modifications.size() == 0) {
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

	private void executeQuery(StringBuilder query, OperationResult result)
			throws ObjectAlreadyExistsException {
		ClientQuery cq;
		ClientSession session = null;
		try {
			session = (ClientSession) sessions.borrowObject();
			cq = session.query(query.toString());
			cq.execute();
		} catch (BaseXException ex) {
			if (StringUtils.contains(ex.getMessage(), OBJECT_ALREADY_EXISTS)) {
				result.recordFatalError("Object with the same name or oid already exists");
				throw new ObjectAlreadyExistsException("Object with the same name or oid already exists");
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
