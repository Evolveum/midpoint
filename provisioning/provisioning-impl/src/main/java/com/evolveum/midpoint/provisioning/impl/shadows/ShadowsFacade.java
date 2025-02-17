/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.Collection;

import com.evolveum.midpoint.schema.util.RawRepoShadow;

import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.Shadow;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsUtil.*;

/**
 * Facade for the whole "shadows" package.
 *
 * Basically, it only dispatches method calls to a set of helper classes, like {@link ShadowGetOperation},
 * {@link ShadowSearchLikeOperation}, {@link ShadowAddOperation}, {@link ShadowModifyOperation}, {@link ShadowDeleteOperation},
 * {@link ShadowRefreshOperation}, {@link ShadowOperationPropagationHelper}, and so on.
 *
 * @author Radovan Semancik
 * @author Katarina Valalikova
 * @see com.evolveum.midpoint.provisioning.impl.shadows
 */
@Component
public class ShadowsFacade {

    static final String OP_DELAYED_OPERATION = ShadowsFacade.class.getName() + ".delayedOperation";

    @Autowired private DefinitionsHelper definitionsHelper;
    @Autowired private ShadowOperationPropagationHelper propagationHelper;
    @Autowired private ShadowCompareHelper compareHelper;
    @Autowired private ShadowFinder shadowFinder;

    /**
     * Gets the shadow. Assumes that the mode is not raw.
     *
     * @param oid OID of the shadow to be fetched
     * @param repositoryShadow (Optional) current shadow in the repository. If not specified, this method will retrieve it by OID.
     * @param identifiersOverride (Optional) identifiers that are known to the caller and that should override
     * the ones (if any) in the shadow.
     * @param options "read only" option is ignored
     */
    public @NotNull Shadow getShadow(
            @NotNull String oid,
            @Nullable RawRepoShadow repositoryShadow,
            @Nullable Collection<ShadowSimpleAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        assert !GetOperationOptions.isRaw(options);
        var shadow = ShadowGetOperation.execute(oid, repositoryShadow, identifiersOverride, options, context, task, result);
        ReturnedShadowValidityChecker.check(shadow, options);
        return shadow;
    }

    public String addResourceObject(
            @NotNull ShadowType resourceObjectToAdd,
            OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        return ShadowAddOperation.executeDirectly(resourceObjectToAdd, scripts, options, context, task, result);
    }

    public ShadowType deleteShadow(
            @NotNull RawRepoShadow rawRepoShadow,
            ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts,
            @NotNull ProvisioningOperationContext context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
            SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        return ShadowDeleteOperation.executeDirectly(rawRepoShadow, options, scripts, context, task, result);
    }

    public void refreshShadow(
            @NotNull String shadowOid,
            ProvisioningOperationOptions options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {
        // Re-reading the shadow in order to get the really "raw" version.
        var rawRepoShadow = shadowFinder.getRepoShadow(shadowOid, null, result);
        ShadowRefreshOperation.executeFull(rawRepoShadow, options, context, task, result);
    }

    public void applyDefinition(
            ObjectDelta<ShadowType> delta,
            ShadowType repoShadow,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(delta, repoShadow, task, result);
    }

    public void applyDefinition(PrismObject<ShadowType> shadow, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        definitionsHelper.applyDefinition(shadow.asObjectable(), task, result);
    }

    public void applyDefinition(ObjectQuery query, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(query, task, result);
    }

    public SearchResultMetadata searchShadowsIterative(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ResultHandler<ShadowType> handler,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(query, options, context, task, result)
                .executeIterativeSearch(
                        ReturnedShadowValidityChecker.createCheckingHandler(handler, options),
                        result);
    }

    public @NotNull SearchResultList<PrismObject<ShadowType>> searchShadows(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        var shadows = ShadowSearchLikeOperation
                .create(query, options, context, task, result)
                .executeNonIterativeSearch(result);
        ReturnedShadowValidityChecker.check(shadows, options);
        return shadows;
    }

    public void searchShadowsIterative(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull ResultHandler<ShadowType> handler,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ShadowSearchLikeOperation
                .create(ctx, query, options)
                .executeIterativeSearch(handler, result);
    }

    public Integer countObjects(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ProvisioningOperationContext context,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(query, options, context, task, result)
                .executeCount(result);
    }

    /** We assume that the `repoShadow` was retrieved right from the repository. */
    public void propagateOperations(
            @NotNull ResourceType resource,
            @NotNull ShadowType repoShadow,
            @NotNull Task task,
            @NotNull OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException, EncryptionException {
        propagationHelper.propagateOperations(resource, RawRepoShadow.of(repoShadow), task, result);
    }

    public <T> ItemComparisonResult compare(
            @NotNull ShadowType repositoryShadow,
            ItemPath path,
            T expectedValue,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        return compareHelper.compare(repositoryShadow, path, expectedValue, task, result);
    }
}
