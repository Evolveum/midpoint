/*
 * Copyright (c) 2015-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.util.QueryConversionUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsFacade;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.ucf.api.AttributesToReturn;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
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
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceResolutionFrequencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

/**
 * Resolves resource objects (also) with the help of the repository / shadow manager.
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

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    /**
     * Resolves a {@link ResourceObjectReferenceType}.
     *
     * @param useRawDefinition If true, raw object class definition is used (instead of refined definition).
     * This is to avoid endless recursion when resolving the base context for object type.
     */
    @Nullable PrismObject<ShadowType> resolve(
            @NotNull ProvisioningContext ctx,
            @NotNull ResourceObjectReferenceType resourceObjectReference,
            boolean useRawDefinition,
            @NotNull String desc,
            @NotNull OperationResult result)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                    SecurityViolationException, ExpressionEvaluationException {

        ObjectReferenceType shadowRef = resourceObjectReference.getShadowRef();
        ResourceObjectReferenceResolutionFrequencyType resolutionFrequency =
                Objects.requireNonNullElse(resourceObjectReference.getResolutionFrequency(), ONCE);
        if (shadowRef != null && shadowRef.getOid() != null) {
            if (resolutionFrequency != ALWAYS) {
                PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), null, result);
                shadowsFacade.applyDefinition(shadow, ctx.getTask(), result);
                return shadow;
            }
        } else if (resolutionFrequency == NEVER) {
            // TODO looks more like ConfigurationException
            throw new ObjectNotFoundException(
                    "No shadowRef OID in "+desc+" and resolution frequency set to NEVER",
                    ShadowType.class,
                    null);
        }

        argCheck(resourceObjectReference.getObjectClass() != null,
                "No object class name in object reference in %s", desc);

        QName objectClassName =
                QNameUtil.qualifyIfNeeded(
                        resourceObjectReference.getObjectClass(),
                        MidPointConstants.NS_RI);

        ProvisioningContext subCtx =
                useRawDefinition ?
                        ctx.spawnForObjectClassWithRawDefinition(objectClassName) :
                        ctx.spawnForObjectClass(objectClassName);

        subCtx.assertDefinition();

        ObjectQuery refQuery =
                ObjectQueryUtil.createQuery(
                        QueryConversionUtil.parseFilter(
                                resourceObjectReference.getFilter(), subCtx.getObjectDefinitionRequired()));
        // No variables. At least not now. We expect that mostly constants will be used here.
        VariablesMap variables = new VariablesMap();
        ObjectQuery evaluatedRefQuery =
                ExpressionUtil.evaluateQueryExpressions(
                        refQuery, variables, MiscSchemaUtil.getExpressionProfile(), expressionFactory, prismContext,
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

        return shadowHolder.getValue();
    }

    /**
     * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Collection<? extends ResourceAttribute<?>> resolvePrimaryIdentifier(ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers, final String desc, OperationResult result)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
                    ExpressionEvaluationException {
        if (identifiers == null) {
            return null;
        }
        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(identifiers, objDef);
        PrismObject<ShadowType> repoShadow = shadowFinder.lookupShadowBySecondaryIds(ctx, secondaryIdentifiers, result);
        if (repoShadow == null) {
            return null;
        }
        shadowsFacade.applyDefinition(repoShadow, ctx.getTask(), result);
        PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            return null;
        }
        Collection primaryIdentifiers = new ArrayList<>();
        for (PrismProperty property: attributesContainer.getValue().getProperties()) {
            if (objDef.isPrimaryIdentifier(property.getElementName())) {
                ResourceAttributeDefinition<?> attrDef = objDef.findAttributeDefinition(property.getElementName());
                if (attrDef == null) {
                    throw new IllegalStateException("No definition for attribute " + property);
                }
                ResourceAttribute primaryIdentifier = attrDef.instantiate();
                primaryIdentifier.setRealValue(property.getRealValue());
                primaryIdentifiers.add(primaryIdentifier);
            }
        }
        LOGGER.trace("Resolved identifiers {} to primary identifiers {} (object class {})", identifiers, primaryIdentifiers, objDef);
        return primaryIdentifiers;
    }

    /**
     * @param repoShadow Used when read capability is "caching only"
     */
    PrismObject<ShadowType> fetchResourceObject(ProvisioningContext ctx,
            Collection<? extends ResourceAttribute<?>> identifiers,
            AttributesToReturn attributesToReturn,
            @Nullable PrismObject<ShadowType> repoShadow,
            OperationResult parentResult) throws ObjectNotFoundException,
            CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        ConnectorInstance connector = ctx.getConnector(ReadCapabilityType.class, parentResult);
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();

        try {

            ReadCapabilityType readCapability = ctx.getEnabledCapability(ReadCapabilityType.class);
            if (readCapability == null) {
                throw new UnsupportedOperationException(
                        "Resource does not support 'read' operation: " + ctx.toHumanReadableDescription());
            }

            if (Boolean.TRUE.equals(readCapability.isCachingOnly())) {
                return repoShadow;
            }

            ResourceObjectIdentification identification = ResourceObjectIdentification.create(objectDefinition, identifiers);
            ResourceObjectIdentification resolvedIdentification = resolvePrimaryIdentifiers(ctx, identification, parentResult);
            resolvedIdentification.validatePrimaryIdentifiers();
            return connector.fetchObject(resolvedIdentification, attributesToReturn, ctx.getUcfExecutionContext(), parentResult);
        } catch (ObjectNotFoundException e) {
            // Not finishing the result because we did not create it! (The same for other catch clauses.)
            // We do not use simple "e.wrap" because there is a lot of things to be filled-in here.
            ObjectNotFoundException objectNotFoundException = new ObjectNotFoundException(
                    "Object not found. identifiers=" + identifiers + ", objectclass=" +
                            PrettyPrinter.prettyPrint(objectDefinition.getTypeName()) + ": " + e.getMessage(),
                    e,
                    ShadowType.class,
                    repoShadow != null ? repoShadow.getOid() : null,
                    ctx.isAllowNotFound());
            parentResult.recordExceptionNotFinish(objectNotFoundException);
            throw objectNotFoundException;
        } catch (CommunicationException e) {
            parentResult.setFatalError("Error communication with the connector " + connector
                    + ": " + e.getMessage(), e);
            throw e;
        } catch (GenericFrameworkException e) {
            parentResult.setFatalError(
                    "Generic error in the connector " + connector + ". Reason: " + e.getMessage(), e);
            throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
                    + e.getMessage(), e);
        } catch (SchemaException ex) {
            parentResult.setFatalError("Can't get resource object, schema error: " + ex.getMessage(), ex);
            throw ex;
        } catch (ExpressionEvaluationException ex) {
            parentResult.setFatalError("Can't get resource object, expression error: " + ex.getMessage(), ex);
            throw ex;
        } catch (ConfigurationException e) {
            parentResult.setFatalError(e);
            throw e;
        }
    }

    /**
     * Resolve primary identifier from a collection of identifiers that may contain only secondary identifiers.
     */
    @SuppressWarnings("unchecked")
    private ResourceObjectIdentification resolvePrimaryIdentifiers(ProvisioningContext ctx,
            ResourceObjectIdentification identification, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        if (identification == null) {
            return null;
        }
        if (identification.hasPrimaryIdentifiers()) {
            return identification;
        }
        Collection<ResourceAttribute<?>> secondaryIdentifiers = (Collection<ResourceAttribute<?>>) identification.getSecondaryIdentifiers();
        PrismObject<ShadowType> repoShadow = shadowFinder.lookupShadowBySecondaryIds(ctx, secondaryIdentifiers, result);
        if (repoShadow == null) {
            // TODO: we should attempt resource search here
            throw new ObjectNotFoundException(
                    "No repository shadow for " + secondaryIdentifiers + ", cannot resolve identifiers",
                    ShadowType.class,
                    null);
        }
        shadowsFacade.applyDefinition(repoShadow, ctx.getTask(), result);
        PrismContainer<Containerable> attributesContainer = repoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer == null) {
            throw new SchemaException("No attributes in "+repoShadow+", cannot resolve identifiers "+secondaryIdentifiers);
        }
        ResourceObjectDefinition objDef = ctx.getObjectDefinitionRequired();
        Collection primaryIdentifiers = new ArrayList<>();
        for (PrismProperty<?> property: attributesContainer.getValue().getProperties()) {
            if (objDef.isPrimaryIdentifier(property.getElementName())) {
                ResourceAttributeDefinition<?> attrDef = objDef.findAttributeDefinition(property.getElementName());
                if (attrDef == null) {
                    throw new IllegalStateException("No definition for attribute " + property);
                }
                @SuppressWarnings("rawtypes")
                ResourceAttribute primaryIdentifier = attrDef.instantiate();
                primaryIdentifier.setRealValue(property.getRealValue());
                primaryIdentifiers.add(primaryIdentifier);
            }
        }
        LOGGER.trace("Resolved {} to primary identifiers {} (object class {})", identification, primaryIdentifiers, objDef);
        return new ResourceObjectIdentification(
                identification.getResourceObjectDefinition(),
                primaryIdentifiers,
                identification.getSecondaryIdentifiers());
    }
}
