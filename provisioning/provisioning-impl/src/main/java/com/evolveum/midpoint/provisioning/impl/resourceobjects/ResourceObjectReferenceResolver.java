/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.prism.Referencable.getOid;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.util.QueryConversionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Resolves resource objects or secondary identifiers.
 *
 * This class invokes functionality outside "resource objects" package by looking shadows up
 * via {@link ShadowFinder} and {@link ShadowsFacade}.
 *
 * Intentionally not a public class.
 *
 * @author semancik
 */
@Component
class ResourceObjectReferenceResolver {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectReferenceResolver.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private ShadowsFacade shadowsFacade;

    /**
     * Resolves a {@link ResourceObjectReferenceType}. Uses raw class definition for this purpose.
     */
    @Nullable ShadowType resolveUsingRawClass(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectReferenceType resourceObjectReference,
            @NotNull String desc,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ObjectReferenceType shadowRef = resourceObjectReference.getShadowRef();
        ResourceObjectReferenceResolutionFrequencyType resolutionFrequency =
                Objects.requireNonNullElse(resourceObjectReference.getResolutionFrequency(), ONCE);

        String shadowOid = getOid(shadowRef);
        if (shadowOid != null) {
            if (resolutionFrequency != ALWAYS) {
                PrismObject<ShadowType> shadow = shadowFinder.getShadow(shadowOid, result);
                shadowsFacade.applyDefinition(shadow, ctx.getTask(), result);
                return shadow.asObjectable();
            }
        } else if (resolutionFrequency == NEVER) {
            throw new ConfigurationException("No shadowRef OID in " + desc + " and resolution frequency set to NEVER");
        }

        QName objectClassName =
                QNameUtil.qualifyIfNeeded(
                        configNonNull(
                                resourceObjectReference.getObjectClass(),
                                "No object class name in object reference in %s", desc),
                        MidPointConstants.NS_RI);

        ProvisioningContext subCtx = ctx.spawnForObjectClassWithRawDefinition(objectClassName);
        subCtx.assertDefinition();

        ObjectQuery refQuery =
                ObjectQueryUtil.createQuery(
                        QueryConversionUtil.parseFilter(
                                resourceObjectReference.getFilter(), subCtx.getObjectDefinitionRequired()));
        // No variables. At least not now. We expect that mostly constants will be used here.
        VariablesMap variables = new VariablesMap();
        ObjectQuery evaluatedRefQuery =
                ExpressionUtil.evaluateQueryExpressions(
                        refQuery, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory,
                        desc, ctx.getTask(), result);
        ObjectFilter baseFilter =
                ObjectQueryUtil.createResourceAndObjectClassFilter(ctx.getResource().getOid(), objectClassName);
        ObjectFilter filter = prismContext.queryFactory().createAnd(baseFilter, evaluatedRefQuery.getFilter());
        ObjectQuery query = prismContext.queryFactory().createQuery(filter);

        // TODO: implement "repo" search strategies, don't forget to apply definitions

        Holder<PrismObject<ShadowType>> shadowHolder = new Holder<>();
        ResultHandler<ShadowType> handler = (shadow, objResult) -> {
            if (shadowHolder.getValue() != null) {
                throw new IllegalStateException("More than one search results for " + desc);
            }
            shadowHolder.setValue(shadow);
            return true;
        };

        shadowsFacade.searchObjectsIterative(subCtx, query, null, handler, result);

        // TODO: implement storage of OID (ONCE search frequency)

        return shadowHolder.getValue().asObjectable();
    }

    /**
     * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers.
     *
     * We accept also dead shadows, but only if there is only one. (This is a bit inconsistent, should be fixed somehow.)
     * Actually, we could be more courageous, and reject dead shadows altogether, as we use the result for object fetching;
     * but there is a theoretical chance that the shadow is dead in the repo but alive on the resource.
     */
    @SuppressWarnings("unchecked")
    ResourceObjectIdentification.Primary resolvePrimaryIdentifiers(
            ProvisioningContext ctx, ResourceObjectIdentification identification, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (identification == null) {
            return null;
        }
        if (identification instanceof ResourceObjectIdentification.Primary primary) {
            return primary;
        }
        Collection<ResourceAttribute<?>> secondaryIdentifiers =
                (Collection<ResourceAttribute<?>>) identification.getSecondaryIdentifiers();
        PrismObject<ShadowType> repoShadow = shadowFinder.lookupShadowBySecondaryIds(ctx, secondaryIdentifiers, result);
        if (repoShadow == null) {
            // TODO: we could attempt resource search here
            throw new ObjectNotFoundException(
                    "No repository shadow for %s, cannot resolve identifiers (%s)".formatted(
                            secondaryIdentifiers, ctx.getExceptionDescription()),
                    ShadowType.class,
                    null);
        }
        shadowsFacade.applyDefinition(repoShadow, ctx.getTask(), result);
        PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            throw new SchemaException(
                    "No attributes in %s, cannot resolve identifiers %s (%s)".formatted(
                            repoShadow, secondaryIdentifiers, ctx.getExceptionDescription()));
        }
        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();
        Collection<ResourceAttribute<?>> primaryIdentifiers = new ArrayList<>();
        for (Item<?, ?> item: attributesContainer.getValue().getItems()) {
            ItemName itemName = item.getElementName();
            if (objDef.isPrimaryIdentifier(itemName)) {
                ResourceAttributeDefinition<?> attrDef = objDef.findAttributeDefinitionRequired(itemName);
                @SuppressWarnings("rawtypes")
                ResourceAttribute primaryIdentifier = attrDef.instantiate();
                primaryIdentifier.setRealValue(item.getRealValue());
                primaryIdentifiers.add(primaryIdentifier);
            }
        }
        LOGGER.trace("Resolved {} to primary identifiers {} (object class {})", identification, primaryIdentifiers, objDef);
        if (primaryIdentifiers.isEmpty()) {
            throw new SchemaException(
                    "No primary identifiers in %s (secondary identifiers: %s) in %s".formatted(
                            repoShadow, secondaryIdentifiers, ctx.getExceptionDescription()));
        }
        return identification.primary(primaryIdentifiers);
    }
}
