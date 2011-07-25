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
 */
package com.evolveum.midpoint.model.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.XPathUtil;
import com.evolveum.midpoint.common.XmlUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.model.expr.ExpressionHandlerImpl;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AttributeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType.Attributes;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueFilterType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.ValueAssignmentHolder;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * 
 * @author lazyman
 * 
 */
@Component("schemaHandler")
public class SchemaHandlerImpl implements SchemaHandler {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlerImpl.class);
	@Autowired(required = true)
	private transient ExpressionHandler expressionHandler;
	@Autowired(required = true)
	private transient FilterManager<Filter> filterManager;
	private ModelController model;

	public void setModel(ModelController model) {
		this.model = model;
	}

	@Override
	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	@SuppressWarnings("unchecked")
	@Override
	public UserType processInboundHandling(UserType user, ResourceObjectShadowType resourceObjectShadow,
			OperationResult result) throws SchemaHandlerException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(resourceObjectShadow, "Resource object shadow must not be null.");
		Validate.notNull(result, "Operation result must not be null.");
		LOGGER.debug("Processing inbound handling for user {} with oid {} and resource object shadow {}.",
				new Object[] { user.getName(), user.getOid(), resourceObjectShadow.getName() });

		Element domUser = null;
		try {
			domUser = JAXBUtil.objectTypeToDom(user, null);
		} catch (JAXBException ex) {
			throw new SchemaHandlerException(ex.getMessage(), ex);
		}

		OperationResult subResult = new OperationResult("Process Inbound Handling");
		result.addSubresult(subResult);

		// ObjectModificationType changes = new ObjectModificationType();
		// changes.setOid(user.getOid());

		ResourceType resource = resolveResource(resourceObjectShadow, subResult);
		AccountType accountType = ModelUtils.getAccountTypeFromHandling(resourceObjectShadow, resource);
		if (accountType == null) {
			subResult.recordWarning("Account type in schema handling was not found for shadow type '"
					+ resourceObjectShadow.getObjectClass() + "'.");
			return user;
			// return changes;
		}
		ResourceObjectDefinition objectDefinition = getResourceObjectDefinition(resource,
				resourceObjectShadow);
		if (objectDefinition == null) {
			subResult.recordWarning("Resource object definition was not found for shadow type '"
					+ resourceObjectShadow.getObjectClass() + "'");
			return user;
			// return changes;
		}

		Map<QName, Variable> variables = ExpressionHandlerImpl.getDefaultXPathVariables(user,
				resourceObjectShadow, resource);
		for (AttributeDescriptionType attribute : accountType.getAttribute()) {
			ResourceObjectAttributeDefinition definition = objectDefinition.findAttributeDefinition(attribute
					.getRef());
			if (definition != null) {
				domUser = (Element) processInboundAttribute(attribute, variables, resourceObjectShadow,
						domUser, user, subResult);
			} else {
				LOGGER.debug("Attribute {} defined in schema handling is not defined in the resource {}. "
						+ "Attribute was not processed",
						new Object[] { attribute.getRef(), resource.getName() });
			}
		}

		try {
			JAXBElement<UserType> jaxbUser = (JAXBElement<UserType>) JAXBUtil.unmarshal(DOMUtil
					.serializeDOMToString(domUser));
			user = jaxbUser.getValue();
		} catch (JAXBException ex) {
			throw new SchemaHandlerException(ex.getMessage(), ex);
		}

		subResult.recordSuccess();
		return user;
		// return changes;
	}

	@Override
	public ObjectModificationType processOutboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result)
			throws SchemaHandlerException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(resourceObjectShadow, "Resource object shadow must not be null.");
		Validate.notNull(result, "Operation result must not be null.");
		LOGGER.debug("Processing outbound handling for user {} with oid {} and resource object shadow {}.",
				new Object[] { user.getName(), user.getOid(), resourceObjectShadow.getName() });

		try {
			resourceObjectShadow = (ResourceObjectShadowType) JAXBUtil.clone(resourceObjectShadow);
		} catch (JAXBException ex) {
			throw new SchemaHandlerException(ex.getMessage(), ex);
		}

		OperationResult subResult = new OperationResult("Process Outbound Handling");
		result.addSubresult(subResult);

		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(resourceObjectShadow.getOid());

		ResourceType resource = resolveResource(resourceObjectShadow, subResult);
		AccountType accountType = ModelUtils.getAccountTypeFromHandling(resourceObjectShadow, resource);
		if (accountType == null) {
			subResult.recordWarning("Account type in schema handling was not found for shadow type '"
					+ resourceObjectShadow.getObjectClass() + "'.");
			return changes;
		}

		ResourceObjectDefinition objectDefinition = getResourceObjectDefinition(resource,
				resourceObjectShadow);
		if (objectDefinition == null) {
			subResult.recordWarning("Resource object definition was not found for shadow type '"
					+ resourceObjectShadow.getObjectClass() + "'");
			return changes;
		}

		Map<QName, Variable> variables = ExpressionHandlerImpl.getDefaultXPathVariables(user,
				resourceObjectShadow, resource);
		for (AttributeDescriptionType attribute : accountType.getAttribute()) {
			ResourceObjectAttributeDefinition attributeDefinition = objectDefinition
					.findAttributeDefinition(attribute.getRef());
			if (attributeDefinition == null) {
				LOGGER.trace("Attribute {} defined in schema handling is not defined in the resource "
						+ "schema {}. Attribute was not processed", new Object[] { attribute.getRef(),
						resource.getName() });
				continue;
			}
			insertUserDefinedVariables(attribute, variables, subResult);
			changes.getPropertyModification().addAll(
					processOutboundAttribute(attribute, variables, resourceObjectShadow));
		}
		subResult.recordSuccess();		
		return changes;
	}

	private ResourceType resolveResource(ResourceObjectShadowType shadow, OperationResult result)
			throws SchemaHandlerException {
		if (shadow.getResource() != null) {
			return shadow.getResource();
		}

		ObjectReferenceType ref = shadow.getResourceRef();
		if (ref == null) {
			throw new SchemaHandlerException("Resource shadow object '" + shadow.getName() + "', oid '"
					+ shadow.getOid() + "' doesn't have defined resource.");
		}

		try {
			return model.getObject(ref.getOid(), new PropertyReferenceListType(), result, ResourceType.class,
					true);
		} catch (Exception ex) {
			throw new SchemaHandlerException("Couldn't get resource object.", ex);
		}
	}

	private ResourceObjectDefinition getResourceObjectDefinition(ResourceType resource,
			ResourceObjectShadowType shadow) throws SchemaHandlerException {
		PropertyContainerDefinition objectDefinition = null;
		try {
			Schema schema = Schema.parse(resource.getSchema().getAny().get(0));
			objectDefinition = schema.findContainerDefinitionByType(shadow.getObjectClass());
		} catch (Exception ex) {
			throw new SchemaHandlerException(ex.getMessage(), ex);
		}

		return (ResourceObjectDefinition) objectDefinition;
	}

	private void insertUserDefinedVariables(AttributeDescriptionType attributeHandling,
			Map<QName, Variable> variables, OperationResult result) {
		// clear old variables defined by user
		for (Iterator<Entry<QName, Variable>> i = variables.entrySet().iterator(); i.hasNext();) {
			Entry<QName, Variable> entry = i.next();
			if (entry.getValue().isUserDefined()) {
				i.remove();
			}
		}

		if (attributeHandling.getOutbound() == null) {
			return;
		}

		List<VariableDefinitionType> variableDefinitions = attributeHandling.getOutbound().getVariable();
		for (VariableDefinitionType varDef : variableDefinitions) {
			OperationResult subResult = new OperationResult("Insert User Variable");
			result.addSubresult(subResult);

			Object object = varDef.getValue();
			if (object == null) {
				ObjectReferenceType reference = varDef.getObjectRef();
				if (reference != null) {
					object = resolveObject(reference.getOid(), result);
				}
			}

			if (object != null) {
				variables.put(varDef.getName(), new Variable(object));
				subResult.recordSuccess();
			} else {
				subResult.recordWarning("Couldn't resolve variable '" + varDef.getName() + "'.");
				LOGGER.debug("Variable {} couldn't be resolved, skipping.", new Object[] { varDef.getName() });
			}
		}
	}

	private Object resolveObject(String oid, OperationResult result) {
		Object object = null;

		try {
			ObjectType objectType = model.getObject(oid, new PropertyReferenceListType(), ObjectType.class,
					result);
			if (objectType == null) {
				return null;
			}

			// We have got JAXB object here, but the code expects DOM
			object = JAXBUtil.objectTypeToDom(objectType, null);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Variable {} couldn't be serialized to XML, skipping", ex);
		}

		return object;
	}

	private List<PropertyModificationType> processOutboundAttribute(AttributeDescriptionType attribute,
			Map<QName, Variable> variables, ResourceObjectShadowType resourceObjectShadow)
			throws SchemaHandlerException {
		List<PropertyModificationType> modifications = new ArrayList<PropertyModificationType>();

		QName attributeName = attribute.getRef();
		ValueConstructionType outbound = attribute.getOutbound();

		LOGGER.debug("Start outbound processing of attribute handling for attribute {}", attributeName);
		if (outbound == null) {
			LOGGER.debug("Outbound not found, skipping.");
			return modifications;
		}

		ExpressionHolder expression = outbound.getValueExpression() == null ? null : new ExpressionHolder(
				outbound.getValueExpression());
		XPathType xpathType = getXPathForAttribute(attributeName);

		String attributeValue;
		// if (expression != null
		// && StringUtils.isNotEmpty(expression.getExpressionAsString())
		// &&
		// isApplicablePropertyConstruction(attribute.getOutbound().isDefault(),
		// xpathType,
		// variables, resourceObjectShadow)) {
		//
		// }
		// TODO: refactor this if clausule
		if (isApplicablePropertyConstruction(outbound.isDefault(), xpathType, variables, resourceObjectShadow)) {
			if (null != expression && !StringUtils.isEmpty(expression.getExpressionAsString())) {
				attributeValue = evaluateExpression(variables, expression);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
							"Outbound: expression {} for attribute {}, resource {}, was evaluated to {}",
							new Object[] { expression.getExpressionAsString(), attributeName,
									DebugUtil.resourceFromShadow(resourceObjectShadow), attributeValue });
					if (variables != null) {
						for (Entry<QName, Variable> entry : variables.entrySet()) {
							LOGGER.trace("Outbound:  variable {}: {}", entry.getKey(), entry.getValue());
						}
					}
				}
			} else {
				attributeValue = extractValue(attribute);
				LOGGER.trace("Account's attribute value is '{}'", attributeValue);
			}

			modifications.addAll(applyValue(resourceObjectShadow, attributeName, attributeValue));
		}
		LOGGER.trace("Finished outbound processing of attribute handling for attribute '{}'", attributeName);

		return modifications;
	}

	private boolean isApplicablePropertyConstruction(boolean isDefault, XPathType xpathType,
			Map<QName, Variable> variables, ObjectType objectType) throws SchemaHandlerException {
		if (isDefault) {
			QName elementQName = null;
			if (objectType instanceof UserType) {
				elementQName = SchemaConstants.I_USER_TYPE;
			} else if (objectType instanceof AccountShadowType) {
				elementQName = SchemaConstants.I_ACCOUNT_TYPE;
			} else {
				throw new SchemaHandlerException("Provided unsupported object type: " + objectType.getClass());
			}

			try {
				Node domObject = JAXBUtil.jaxbToDom(objectType, elementQName, DOMUtil.getDocument());
				NodeList nodes;
				try {
					XPathUtil xpathUtil = new XPathUtil();
					nodes = xpathUtil.matchedNodesByXPath(xpathType, variables, domObject);
				} catch (XPathExpressionException ex) {
					throw new SchemaHandlerException(ex.getMessage(), ex);
				}
				if (null != nodes && nodes.getLength() > 0) {
					return false;
				}
			} catch (JAXBException ex) {
				throw new SchemaHandlerException("Couldn't transform jaxb object '" + objectType
						+ "' to dom.", ex);
			}
		}

		return true;
	}

	/**
	 * construct XPath that points to resource object shadow's attribute
	 */
	private XPathType getXPathForAttribute(QName attributeName) {
		List<XPathSegment> segments = new ArrayList<XPathSegment>();
		segments.add(new XPathSegment(SchemaConstants.I_ATTRIBUTES));
		segments.add(new XPathSegment(attributeName));

		return new XPathType(segments);
	}

	private String extractValue(AttributeDescriptionType attribute) {
		JAXBElement<Value> value = attribute.getOutbound().getValue();
		if (value == null) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		for (Object content : value.getValue().getContent()) {
			if (content instanceof String) {
				builder.append(content);
			} else {
				throw new IllegalArgumentException("Unsupported content (" + content
						+ ") for value element in attribute's handling outbound section for attribute: "
						+ attribute.getRef());
			}
		}
		return builder.toString();
	}

	private String evaluateExpression(Map<QName, Variable> variables, ExpressionHolder expressionHolder) {
		return (String) XPathUtil.evaluateExpression(variables, expressionHolder, XPathConstants.STRING);
	}

	private List<PropertyModificationType> applyValue(ResourceObjectShadowType resourceObjectShadow,
			QName attributeName, String attributeValue) {
		List<PropertyModificationType> modifications = new ArrayList<PropertyModificationType>();

		// TODO: multi value attributes are not supported, yet
		// TODO: attributes could have only simple values
		LOGGER.trace("Account's attribute '{}' assign value '{}'", attributeName, attributeValue);
		String namespace = attributeName.getNamespaceURI();
		String localName = attributeName.getLocalPart();
		// LOGGER.trace("Account's attribute namespace = '{}' and name = '{}'",
		// new Object[] { namespace,
		// localName });

		ResourceObjectShadowType.Attributes attrs = resourceObjectShadow.getAttributes();
		if (null == attrs) {
			attrs = new AccountShadowType.Attributes();
			resourceObjectShadow.setAttributes(attrs);
		}

		List<XPathSegment> segments = new ArrayList<XPathSegment>();
		// segments.add(new XPathSegment(SchemaConstants.C_OBJECT));
		segments.add(new XPathSegment(SchemaConstants.I_ATTRIBUTES));
		XPathType xpathType = new XPathType(segments);

		List<Element> attributes = attrs.getAny();
		for (Element attribute : attributes) {
			LOGGER.trace("attribute: {}, name {}",
					new Object[] { attribute.getNamespaceURI(), attribute.getLocalName() });
			if (localName.equals(attribute.getLocalName()) && namespace.equals(attribute.getNamespaceURI())) {
				attribute.setTextContent(attributeValue);
				LOGGER.trace("Changed account's attribute {} value to {}", new Object[] { attributeName,
						attributeValue });

				PropertyModificationType modification = ObjectTypeUtil.createPropertyModificationType(
						PropertyModificationTypeType.replace, xpathType, attribute);
				modifications.add(modification);

				return modifications;
			}
		}

		// no value was set for the attribute, create new attribute with value
		Element element = createAttributeElement(namespace, localName, attributeValue);
		attributes.add(element);

		PropertyModificationType modification = ObjectTypeUtil.createPropertyModificationType(
				PropertyModificationTypeType.add, xpathType, element);
		modifications.add(modification);

		LOGGER.trace("Created account's attribute {} with value {}", new Object[] { attributeName,
				attributeValue });

		return modifications;
	}

	private Element createAttributeElement(String namespace, String localName, String value) {
		Document doc = DOMUtil.getDocument();
		Element element = null;
		if (StringUtils.isNotEmpty(namespace)) {
			element = doc.createElementNS(namespace, localName);
		} else {
			element = doc.createElementNS(SchemaConstants.NS_C, localName);
		}
		element.setTextContent(value);

		return element;
	}

	private List<ValueAssignmentHolder> getInbound(AttributeDescriptionType attribute) {
		List<ValueAssignmentHolder> holders = new ArrayList<ValueAssignmentHolder>();
		if (attribute.getInbound() == null) {
			return holders;
		}

		for (Element element : attribute.getInbound()) {
			holders.add(new ValueAssignmentHolder(element));
		}

		return holders;
	}

	private Node processInboundAttribute(AttributeDescriptionType attribute, Map<QName, Variable> variables,
			ResourceObjectShadowType resourceObjectShadow, Node domUser, UserType user, OperationResult result)
			throws SchemaHandlerException {

		QName attributeName = attribute.getRef();
		LOGGER.trace("Start inbound processing of attribute handling for attribute {}",
				new Object[] { attributeName });

		List<ValueAssignmentHolder> inbounds = getInbound(attribute);
		if (inbounds.isEmpty()) {
			LOGGER.trace("Finished inbound processing for attribute {}, nothing to process",
					new Object[] { attributeName });
			return domUser;
		}

		OperationResult subResult;
		for (ValueAssignmentHolder inbound : inbounds) {
			subResult = result.createSubresult("Process Inbound");
			if (null != inbound.getSource()) {
				subResult.recordWarning("Inbound attribute where source is not null is not supported yet.");
				continue;
			}

			List<ValueFilterType> filters = inbound.getFilter();
			XPathType xpathType = inbound.getTarget();
			NodeList matchedNodes;
			try {
				matchedNodes = new XPathUtil().matchedNodesByXPath(xpathType, variables, domUser);
			} catch (XPathExpressionException ex) {
				throw new SchemaHandlerException(ex.getMessage(), ex);
			}

			// I. extract new nodesAttributesFromAccountShadow with values from
			// accountShadow
			List<Element> nodesAttributesFromAccountShadow = extractAttributeValuesFromShadow(
					resourceObjectShadow, attribute);

			Node parentNode;
			QName userPropertyQName;
			if (matchedNodes.getLength() == 0) {
				// if the value does not exists
				LOGGER.debug(
						"No nodes were matched by xpath {} in context of namespaces {} and variables {}",
						new Object[] { xpathType.getXPath(), xpathType.getNamespaceMap(), variables });
				// if no matches found, then we will search for the parent of
				// the element and add new node to it

				XPathType parentXpath = constructParentXpath(xpathType);
				try {
					matchedNodes = new XPathUtil().matchedNodesByXPath(parentXpath, variables, domUser);
					parentNode = matchedNodes.item(0);

					if (null == parentNode) {
						// if there is no extension section at all and we have
						// extension attributes then we will create it
						List<XPathSegment> segments = parentXpath.toSegments();
						XPathType relativeParentXPath = null;
						// if first element in the xpath is variable, we assume
						// it is referencing the doc and we will replace it
						if (segments != null && !segments.isEmpty() && segments.get(0).isVariable()) {
							// we will remove the segment, because
							// DOMUtil.createNodesDefinedByXPath expects
							// relative xpath
							relativeParentXPath = new XPathType(segments.subList(1, segments.size()));
							Document doc = DOMUtil.getDocument();
							doc.adoptNode(domUser);
							doc.appendChild(domUser);
							XPathUtil.createNodesDefinedByXPath(doc, relativeParentXPath);
							domUser = doc.getFirstChild();
							Validate.notNull(domUser, "null value assigned to domUser");
							variables.put(SchemaConstants.I_USER, new Variable(domUser, false));
							matchedNodes = new XPathUtil().matchedNodesByXPath(parentXpath, variables,
									domUser);
						}
					}
				} catch (XPathExpressionException ex) {
					throw new SchemaHandlerException(ex.getMessage(), ex);
				}
				parentNode = matchedNodes.item(0);
				// TODO: reimplement
				boolean wasEmpty = false;
				if (null == parentNode) {
					parentNode = domUser;
					wasEmpty = true;
				}
				userPropertyQName = getUserPropertyQNameFromXPathType(xpathType);
				// II. transform tag names from account's attribute to user tags
				List<Element> newNodes = transformAccountAttributeToUserProperty(
						nodesAttributesFromAccountShadow, userPropertyQName, filters);
				// III.b set new values in transformed
				// nodesAttributesFromAccountShadow to user
				XmlUtil.addChildNodes(parentNode, newNodes);
				// IV. set modified user
				if (wasEmpty) {
					domUser = parentNode;
					Validate.notNull(domUser, "null value assigned to domUser");
				} else {
					domUser = parentNode.getOwnerDocument().getFirstChild();
					Validate.notNull(domUser, "null value assigned to domUser");
				}
				LOGGER.debug(
						"INBOUND: expression [attribute:{}] =(new)=> [user:{},path:{}] {}",
						new Object[] { attribute.getName(), DebugUtil.prettyPrint(user), xpathType,
								DebugUtil.prettyPrint(newNodes) });

			} else {
				userPropertyQName = new QName(matchedNodes.item(0).getNamespaceURI(), matchedNodes.item(0)
						.getLocalName());
				parentNode = matchedNodes.item(0).getParentNode();
				Validate.notNull(parentNode, "parentNode is null");
				// II. transform tag names from account's attribute to user tags
				List<Element> newNodes = transformAccountAttributeToUserProperty(
						nodesAttributesFromAccountShadow, userPropertyQName, filters);
				// III.a replace values in transformed
				// nodesAttributesFromAccountShadow to user
				XmlUtil.replaceChildNodes(parentNode, newNodes);
				// IV. set modified user
				domUser = parentNode.getOwnerDocument().getDocumentElement();// getFirstChild();
				LOGGER.debug(
						"INBOUND: expression [attribute:{}] =(replace)=> [user:{}, path:{}] {}",
						new Object[] { attribute.getName(), DebugUtil.prettyPrint(user), xpathType,
								DebugUtil.prettyPrint(newNodes) });
				Validate.notNull(domUser, "null value assigned to domUser");
			}

		}
		LOGGER.trace("Finished inbound processing of attribute handling for attribute '{}'", attributeName);
		// if (null != domUser) {
		// domUser.normalize();
		// }

		return domUser;
	}

	private XPathType constructParentXpath(XPathType xpathType) {
		List<XPathSegment> segments = xpathType.toSegments();
		List<XPathSegment> parentSegments = new ArrayList<XPathSegment>();
		parentSegments.addAll(segments.subList(0, segments.size() - 1));
		XPathType parentXpath = new XPathType(parentSegments);

		return parentXpath;
	}

	private List<Element> extractAttributeValuesFromShadow(ResourceObjectShadowType resourceObjectShadow,
			AttributeDescriptionType attribute) {
		List<Element> values = new ArrayList<Element>();
		QName attributeName = attribute.getRef();
		Attributes attributes = resourceObjectShadow.getAttributes();
		if (attributes == null) {
			return values;
		}

		for (Element attrElement : attributes.getAny()) {
			if (StringUtils.equals(attributeName.getNamespaceURI(), attrElement.getNamespaceURI())
					&& StringUtils.equals(attributeName.getLocalPart(), attrElement.getLocalName())) {
				values.add(attrElement);
			}
		}

		return values;
	}

	private List<Element> transformAccountAttributeToUserProperty(List<Element> nodes, QName propertyQName,
			List<ValueFilterType> filters) throws DOMException {
		Validate.notNull(nodes, "nodes are null");
		Validate.notNull(propertyQName, "propertyQName are null");

		List<Element> newNodes = new ArrayList<Element>();
		Document doc = DOMUtil.getDocument();
		for (Element element : nodes) {
			Element transformedElement = doc.createElementNS(propertyQName.getNamespaceURI(),
					propertyQName.getLocalPart());
			// transformedElement.setPrefix("attr" + (new
			// Random()).nextInt(Integer.MAX_VALUE));
			if (StringUtils.isNotEmpty(propertyQName.getPrefix())) {
				transformedElement.setPrefix(propertyQName.getPrefix());
			} else {
				transformedElement.setPrefix(propertyQName.getLocalPart());
			}

			Node valueNode = applyFilters(filters, element.getFirstChild());
			if (null != valueNode) {
				transformedElement.getOwnerDocument().adoptNode(valueNode);
				transformedElement.appendChild(valueNode);
			}
			newNodes.add(transformedElement);
		}
		return newNodes;
	}

	private QName getUserPropertyQNameFromXPathType(XPathType xpathType) {
		List<XPathSegment> segments = xpathType.toSegments();
		XPathSegment lastSegment = segments.get(segments.size() - 1);
		return lastSegment.getQName();
	}

	private Node applyFilters(List<ValueFilterType> filters, Node node) {
		if (null == filters || filters.isEmpty()) {
			// no filters defined return immediately
			LOGGER.trace("No filters defined");
			return node;
		}

		Node returnNode = node;
		for (ValueFilterType filterType : filters) {
			Filter filter = filterManager.getFilterInstance(filterType.getType(), filterType.getAny());
			if (null != filter) {
				returnNode = filter.apply(returnNode);
			} else {
				LOGGER.warn("Filter not found for uri {}", filterType.getType());
			}
		}

		return returnNode;
	}
}
