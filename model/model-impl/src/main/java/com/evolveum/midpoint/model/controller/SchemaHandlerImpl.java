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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.model.expr.ExpressionHandler;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AttributeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.VariableDefinitionType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

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
		for (AttributeDescriptionType attributeHandling : accountType.getAttribute()) {
			ResourceObjectAttributeDefinition attributeDefinition = objectDefinition
					.findAttributeDefinition(attributeHandling.getRef());
			if (attributeDefinition == null) {
				LOGGER.trace("Attribute {} defined in schema handling is not defined in the resource "
						+ "schema {}. Attribute was not processed", new Object[] {
						attributeHandling.getRef(), resource.getName() });
				continue;
			}
			insertUserDefinedVariables(attributeHandling, variables, subResult);
			// processOutboundSection(attributeHandling, variables,
			// resourceObjectShadow);
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
}
