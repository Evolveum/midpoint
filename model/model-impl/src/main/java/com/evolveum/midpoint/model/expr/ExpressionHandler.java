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
package com.evolveum.midpoint.model.expr;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.common.expression.xpath.XPathExpressionEvaluator;
import com.evolveum.midpoint.common.valueconstruction.ValueConstructionFactory;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.ExpressionCodeHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.Variable;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@Component
public class ExpressionHandler {
	@Autowired(required = true)
	private ModelController model;
	
	@Autowired(required = true)
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;
	
	private XPathExpressionEvaluator xpathEvaluator = null;
	

	private ModelController getModel() {
		if (model == null) {
			throw new IllegalStateException("Model controller is null. Please set model  property "
					+ "before using expression handler.");
		}
		return model;
	}

	public String evaluateExpression(ResourceObjectShadowType shadow, ExpressionType expressionType,
			String shortDesc, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		ResourceType resource = resolveResource(shadow, result);
		Map<QName, Object> variables = getDefaultXPathVariables(null, shadow, resource);
		
		Expression expression = expressionFactory.createExpression(expressionType, shortDesc);
		expression.addVariableDefinitions(variables);
		return expression.evaluateScalar(String.class, result).getValue();
	}

	public boolean evaluateConfirmationExpression(UserType user, ResourceObjectShadowType shadow,
			ExpressionType expressionType, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		ResourceType resource = resolveResource(shadow, result);
		String expressionResult = evaluateExpression(shadow, expressionType, "Confiration expression for "+resource.asPrismObject(), result);
		
		return Boolean.valueOf(expressionResult);
	}

	// TODO: refactor - this method is also in SchemaHandlerImpl
	private ResourceType resolveResource(ResourceObjectShadowType shadow, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException
			 {
		if (shadow.getResource() != null) {
			return shadow.getResource();
		}

		ObjectReferenceType ref = shadow.getResourceRef();
		if (ref == null) {
			throw new ExpressionEvaluationException("Resource shadow object " + shadow + " doesn't have defined resource.");
		}
		if (ref.getOid() == null) {
			throw new ExpressionEvaluationException("Resource shadow object " + shadow + " defines null resource OID.");
		}

		return getModel().getObject(ResourceType.class, ref.getOid(), new PropertyReferenceListType(),
				result).asObjectable();
	}

	public static Map<QName, Object> getDefaultXPathVariables(UserType user,
			ResourceObjectShadowType shadow, ResourceType resource) {
		
		Map<QName, Object> variables = new HashMap<QName, Object>();
		if (user != null) {
			variables.put(SchemaConstants.I_USER, user.asPrismObject());
		}

		if (shadow != null) {
			variables.put(SchemaConstants.I_ACCOUNT, shadow.asPrismObject());
		}

		if (resource != null) {
			variables.put(SchemaConstants.I_RESOURCE, resource.asPrismObject());
		}

		return variables;
	}

	// Called from the ObjectResolver.resolve
	public ObjectType resolveRef(ObjectReferenceType ref, String contextDescription, OperationResult result)
			throws ObjectNotFoundException, SchemaException {
		
		Class<? extends ObjectType> type = ObjectType.class;
		if (ref.getType() != null) {
			ObjectTypes objectTypeType = ObjectTypes.getObjectTypeFromTypeQName(ref.getType());
			type = objectTypeType.getClassDefinition();
		}
		
		return repositoryService.getObject(type, ref.getOid(), null, result).asObjectable();

	}
	
	public ValueConstructionFactory createValueConstructionFactory(ObjectResolver resolver) {
		ExpressionFactory expressionFactory = new ExpressionFactory();
		registerEvaluators(expressionFactory);
		expressionFactory.setObjectResolver(resolver);
		
		ValueConstructionFactory valueConstructionFactory = new ValueConstructionFactory();
		valueConstructionFactory.setExpressionFactory(expressionFactory);
		return valueConstructionFactory;
	}
	
	public void registerEvaluators(ExpressionFactory expressionFactory) {
		if (xpathEvaluator == null) {
			xpathEvaluator = new XPathExpressionEvaluator();
		}
		expressionFactory.registerEvaluator(XPathExpressionEvaluator.XPATH_LANGUAGE_URL, xpathEvaluator);
	}

}
