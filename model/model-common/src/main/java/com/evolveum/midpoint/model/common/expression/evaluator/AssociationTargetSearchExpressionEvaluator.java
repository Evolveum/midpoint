/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AbstractSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.model.common.expression.evaluator.caching.AssociationSearchExpressionEvaluatorCache;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 */
public class AssociationTargetSearchExpressionEvaluator
			extends AbstractSearchExpressionEvaluator<PrismContainerValue<ShadowAssociationType>,
			                                          PrismContainerDefinition<ShadowAssociationType>> {

	private static final Trace LOGGER = TraceManager.getTrace(AssociationTargetSearchExpressionEvaluator.class);

	public AssociationTargetSearchExpressionEvaluator(SearchObjectExpressionEvaluatorType expressionEvaluatorType,
			PrismContainerDefinition<ShadowAssociationType> outputDefinition, Protector protector, ObjectResolver objectResolver,
			ModelService modelService, PrismContext prismContext, SecurityContextManager securityContextManager,
			LocalizationService localizationService) {
		super(expressionEvaluatorType, outputDefinition, protector, objectResolver, modelService, prismContext,
				securityContextManager, localizationService);
	}

	@Override
	protected AbstractSearchExpressionEvaluatorCache getCache() {
		return AssociationSearchExpressionEvaluatorCache.getCache();
	}

	@Override
	protected ObjectQuery extendQuery(ObjectQuery query, ExpressionEvaluationContext params) throws SchemaException, ExpressionEvaluationException {
		RefinedObjectClassDefinition rAssocTargetDef = (RefinedObjectClassDefinition) params.getVariables().get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
		if (rAssocTargetDef == null) {
			throw new ExpressionEvaluationException("No association target object class definition variable in "+
					params.getContextDescription()+"; the expression may be used in a wrong place. It is only supposed to create an association.");
		}
		ObjectFilter resourceFilter = ObjectQueryUtil.createResourceFilter(rAssocTargetDef.getResourceOid(), getPrismContext());
		ObjectFilter objectClassFilter = ObjectQueryUtil.createObjectClassFilter(rAssocTargetDef.getObjectClassDefinition().getTypeName(),
				getPrismContext());
		ObjectFilter extendedFilter = AndFilter.createAnd(resourceFilter, objectClassFilter, query.getFilter());
		query.setFilter(extendedFilter);
		return query;
	}

	@Override
	protected void extendOptions(Collection<SelectorOptions<GetOperationOptions>> options,
			boolean searchOnResource) {
		super.extendOptions(options, searchOnResource);
		// We do not need to worry about associations of associations here
		// (nested associations). Avoiding that will make the query faster.
		options.add(SelectorOptions.create(ShadowType.F_ASSOCIATION, GetOperationOptions.createDontRetrieve()));
	}

	@Override
    protected PrismContainerValue<ShadowAssociationType> createPrismValue(String oid, QName targetTypeQName, List<ItemDelta<PrismContainerValue<ShadowAssociationType>, PrismContainerDefinition<ShadowAssociationType>>> additionalAttributeDeltas, ExpressionEvaluationContext params) {
		ShadowAssociationType associationType = new ShadowAssociationType();
		PrismContainerValue<ShadowAssociationType> associationCVal = associationType.asPrismContainerValue();
		associationType.setName(params.getMappingQName());
		ObjectReferenceType targetRef = new ObjectReferenceType();
		targetRef.setOid(oid);
		targetRef.setType(targetTypeQName);
		associationType.setShadowRef(targetRef);

		try {

			if (additionalAttributeDeltas != null) {
				ItemDelta.applyTo(additionalAttributeDeltas, associationCVal);
			}

			getPrismContext().adopt(associationCVal, ShadowType.COMPLEX_TYPE, new ItemPath(ShadowType.F_ASSOCIATION));
			if (InternalsConfig.consistencyChecks) {
				associationCVal.assertDefinitions("associationCVal in assignment expression in "+params.getContextDescription());
			}
		} catch (SchemaException e) {
			// Should not happen
			throw new SystemException(e);
		}

		return associationCVal;
	}

	@Override
	protected QName getDefaultTargetType() {
		return ShadowType.COMPLEX_TYPE;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "associationExpression";
	}

}
