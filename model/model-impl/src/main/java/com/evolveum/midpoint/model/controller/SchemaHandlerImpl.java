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
import java.util.HashMap;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.XPathUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.object.ObjectTypeUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AttributeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;
import com.evolveum.midpoint.xml.schema.ExpressionHolder;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
public class SchemaHandlerImpl implements SchemaHandler {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlerImpl.class);
	@Autowired(required = true)
	private transient ExpressionHandler expressionHandler;
	private ModelController model;

	public void setModel(ModelController model) {
		this.model = model;
	}

	@Override
	public ExpressionHandler getExpressionHandler() {
		return expressionHandler;
	}

	@Override
	public ObjectModificationType processInboundHandling(UserType user,
			ResourceObjectShadowType resourceObjectShadow, OperationResult result)
			throws SchemaHandlerException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(resourceObjectShadow, "Resource object shadow must not be null.");
		Validate.notNull(result, "Operation result must not be null.");
		LOGGER.debug("Processing inbound handling for user {} with oid {} and resource object shadow {}.",
				new Object[] { user.getName(), user.getOid(), resourceObjectShadow.getName() });

		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(user.getOid());

		// TODO: implement

		return changes;
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

		OperationResult subResult = new OperationResult("Process Outbound Handling");
		result.addSubresult(subResult);

		ObjectModificationType changes = new ObjectModificationType();
		changes.setOid(resourceObjectShadow.getOid());

		ResourceType resource = resolveResource(resourceObjectShadow, result);
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

		Map<QName, Variable> variables = getDefaultXPathVariables(user, resourceObjectShadow, resource);
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

		return changes;
	}

	private Map<QName, Variable> getDefaultXPathVariables(UserType user, ResourceObjectShadowType shadow,
			ResourceType resource) {
		Map<QName, Variable> variables = new HashMap<QName, Variable>();
		try {
			ObjectFactory of = new ObjectFactory();
			if (user != null) {
				JAXBElement<ObjectType> userJaxb = of.createObject(user);
				Element userEl = JAXBUtil.objectTypeToDom(userJaxb.getValue(), null);
				variables.put(SchemaConstants.I_USER, new Variable(userEl, false));
			}

			if (shadow != null) {
				JAXBElement<ObjectType> accountJaxb = of.createObject(shadow);
				Element accountEl = JAXBUtil.objectTypeToDom(accountJaxb.getValue(), null);
				variables.put(SchemaConstants.I_ACCOUNT, new Variable(accountEl, false));
			}

			if (resource != null) {
				JAXBElement<ObjectType> resourceJaxb = of.createObject(resource);
				Element resourceEl = JAXBUtil.objectTypeToDom(resourceJaxb.getValue(), null);
				variables.put(SchemaConstants.I_RESOURCE, new Variable(resourceEl, false));
			}
		} catch (JAXBException ex) {
			throw new IllegalArgumentException(ex);
		}
		return variables;
	}

	private ResourceType resolveResource(ResourceObjectShadowType shadow, OperationResult result)
			throws SchemaHandlerException {
		if (shadow.getResource() != null) {
			return shadow.getResource();
		}

		ObjectReferenceType ref = shadow.getResourceRef();
		if (ref == null) {
			throw new SchemaHandlerException(
					"Resource shadow object '', oid '' doesn't have defined resource.");
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
			ObjectType objectType = model.getObject(oid, new PropertyReferenceListType(), result,
					ObjectType.class);
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
		XPathSegment segment = new XPathSegment(SchemaConstants.I_ATTRIBUTES);
		segments.add(segment);
		segment = new XPathSegment(attributeName);
		segments.add(segment);
		XPathType xpathType = new XPathType(segments);
		return xpathType;
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
		LOGGER.trace("Account's attribute namespace = '{}' and name = '{}'", new Object[] { namespace,
				localName });

		ResourceObjectShadowType.Attributes attrs = resourceObjectShadow.getAttributes();
		if (null == attrs) {
			attrs = new AccountShadowType.Attributes();
			resourceObjectShadow.setAttributes(attrs);
		}
		List<Element> attributes = attrs.getAny();

		for (Element attribute : attributes) {
			LOGGER.trace("attribute: {}, name {}",
					new Object[] { attribute.getNamespaceURI(), attribute.getLocalName() });
			if (localName.equals(attribute.getLocalName()) && namespace.equals(attribute.getNamespaceURI())) {
				attribute.setTextContent(attributeValue);
				LOGGER.trace("Changed account's attribute {} value to {}", new Object[] { attributeName,
						attributeValue });

//				XPathType xpathType = null;
//				PropertyModificationType modification = ObjectTypeUtil.createPropertyModificationType(
//						PropertyModificationTypeType.replace, xpathType, attribute);
//				modifications.add(modification);

				return modifications;
			}
		}

		// no value was set for the attribute, create new attribute with value
		Element element = createAttributeElement(namespace, localName, attributeValue);
		attributes.add(element);

		XPathType xpathType = null;
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
}
