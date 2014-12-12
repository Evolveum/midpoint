/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.expr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.common.expression.script.xpath.XPathScriptEvaluator;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;
	
	@Autowired(required = true)
	private ExpressionFactory expressionFactory;
	
	@Autowired(required = true)
	private ModelObjectResolver modelObjectResolver;

    @Autowired(required = true)
    private PrismContext prismContext;
	
	private XPathScriptEvaluator xpathEvaluator = null;
	

	private ModelController getModel() {
		if (model == null) {
			throw new IllegalStateException("Model controller is null. Please set model  property "
					+ "before using expression handler.");
		}
		return model;
	}

	public String evaluateExpression(ShadowType shadow, ExpressionType expressionType,
			String shortDesc, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		ResourceType resource = resolveResource(shadow, result);
		
		ExpressionVariables variables = getDefaultXPathVariables(null, shadow, resource);
		
		PrismPropertyDefinition outputDefinition = new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_STRING, prismContext);
		Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(expressionType,
				outputDefinition, shortDesc, result);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(params);
		if (outputTriple == null) {
			return null;
		}
		Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			return null;
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        return nonNegativeValues.iterator().next().getValue();
	}

	public boolean evaluateConfirmationExpression(UserType user, ShadowType shadow,
			ExpressionType expressionType, Task task, OperationResult result) 
					throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		Validate.notNull(user, "User must not be null.");
		Validate.notNull(shadow, "Resource object shadow must not be null.");
		Validate.notNull(expressionType, "Expression must not be null.");
		Validate.notNull(result, "Operation result must not be null.");

		ResourceType resource = resolveResource(shadow, result);
		ExpressionVariables variables = getDefaultXPathVariables(user, shadow, resource);
		String shortDesc = "confirmation expression for "+resource.asPrismObject();
		
		PrismPropertyDefinition outputDefinition = new PrismPropertyDefinition(ExpressionConstants.OUTPUT_ELMENT_NAME, 
				DOMUtil.XSD_BOOLEAN, prismContext);
		Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, 
				outputDefinition, shortDesc, result);

		ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, variables, shortDesc, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = expression.evaluate(params);
		Collection<PrismPropertyValue<Boolean>> nonNegativeValues = outputTriple.getNonNegativeValues();
		if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
			throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
		}
        if (nonNegativeValues.size() > 1) {
        	throw new ExpressionEvaluationException("Expression returned more than one value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        PrismPropertyValue<Boolean> resultpval = nonNegativeValues.iterator().next();
        if (resultpval == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
        Boolean resultVal = resultpval.getValue();
        if (resultVal == null) {
        	throw new ExpressionEvaluationException("Expression returned no value ("+nonNegativeValues.size()+") in "+shortDesc);
        }
		return resultVal;
	}

	// TODO: refactor - this method is also in SchemaHandlerImpl
	private ResourceType resolveResource(ShadowType shadow, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException
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

		return modelObjectResolver.getObjectSimple(ResourceType.class, ref.getOid(), null, null, result);
	}

	public static ExpressionVariables getDefaultXPathVariables(UserType user,
			ShadowType shadow, ResourceType resource) {
		
		ExpressionVariables variables = new ExpressionVariables();
		if (user != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_USER, user.asPrismObject());
		}

		if (shadow != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_ACCOUNT, shadow.asPrismObject());
		}

		if (resource != null) {
			variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource.asPrismObject());
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
		
}
