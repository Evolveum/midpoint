/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ItemComparisonResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Facade for the whole "shadows" package.
 *
 * Basically, it only dispatches method calls to a set of helper classes, like {@link ShadowGetOperation}, {@link ShadowSearchLikeOperation},
 * {@link ModifyHelper}, {@link DeleteHelper}, and so on.
 *
 * @see com.evolveum.midpoint.provisioning.impl.shadows
 *
 * @author Radovan Semancik
 * @author Katarina Valalikova
 */
@Component
public class ShadowsFacade {

    static final String OP_DELAYED_OPERATION = ShadowsFacade.class.getName() + ".delayedOperation";
    static final String OP_HANDLE_OBJECT = ShadowsFacade.class.getName() + ".handleObject";

    @Autowired private ShadowAddHelper addHelper;
    @Autowired private RefreshHelper refreshHelper;
    @Autowired private ModifyHelper modifyHelper;
    @Autowired private DeleteHelper deleteHelper;
    @Autowired private DefinitionsHelper definitionsHelper;
    @Autowired private PropagateHelper propagateHelper;
    @Autowired private CompareHelper compareHelper;
    @Autowired private ShadowsLocalBeans localBeans;

    /**
     * @param oid OID of the shadow to be fetched
     * @param repositoryShadow (Optional) current shadow in the repository. If not specified, this method will retrieve it by OID.
     * @param identifiersOverride (Optional) identifiers that are known to the caller and that should override
     * the ones (if any) in the shadow.
     * @param options "read only" option is ignored
     */
    public @NotNull ShadowType getShadow(
            @NotNull String oid,
            @Nullable ShadowType repositoryShadow,
            @Nullable Collection<ResourceAttribute<?>> identifiersOverride,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        return ShadowGetOperation
                .create(oid, repositoryShadow, identifiersOverride, options, task, result, localBeans)
                .execute(result);
    }

    public String addResourceObject(ShadowType resourceObjectToAdd, OperationProvisioningScriptsType scripts,
            ProvisioningOperationOptions options, Task task, OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectAlreadyExistsException, SchemaException,
            ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException, EncryptionException {
        return addHelper.addResourceObject(resourceObjectToAdd, scripts, options, task, result);
    }

    public String modifyShadow(
            @NotNull ShadowType repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @Nullable OperationProvisioningScriptsType scripts,
            @Nullable ProvisioningOperationOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException, SchemaException,
            ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException,
            EncryptionException, ObjectAlreadyExistsException {
        return modifyHelper.modifyShadow(repoShadow, modifications, scripts, options, task, result);
    }

    public ShadowType deleteShadow(
            ShadowType repoShadow, ProvisioningOperationOptions options,
            OperationProvisioningScriptsType scripts, Task task, OperationResult result)
            throws CommunicationException, GenericFrameworkException, ObjectNotFoundException,
            SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException,
            ExpressionEvaluationException {
        return deleteHelper.deleteShadow(repoShadow, options, scripts, task, result);
    }

    @Nullable
    public RefreshShadowOperation refreshShadow(ShadowType repoShadow, ProvisioningOperationOptions options,
            Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, EncryptionException {
        return refreshHelper.refreshShadow(repoShadow, options, task, result);
    }

    public void applyDefinition(ObjectDelta<ShadowType> delta, ShadowType repoShadow,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException,
                    CommunicationException, ConfigurationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(delta, repoShadow, task, result);
    }

    public void applyDefinition(PrismObject<ShadowType> shadow, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(shadow.asObjectable(), task, result);
    }

    public void applyDefinition(ObjectQuery query, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        definitionsHelper.applyDefinition(query, task, result);
    }

    public SearchResultMetadata searchObjectsIterative(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<ShadowType> handler,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(query, options, task, result, localBeans)
                .executeIterativeSearch(handler, result);
    }

    public @NotNull SearchResultList<PrismObject<ShadowType>> searchObjects(
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(query, options, task, result, localBeans)
                .executeNonIterativeSearch(result);
    }

    public SearchResultMetadata searchObjectsIterative(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            ResultHandler<ShadowType> handler,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(ctx, query, options, localBeans)
                .executeIterativeSearch(handler, result);
    }

    public @NotNull SearchResultList<PrismObject<ShadowType>> searchObjects(
            ProvisioningContext ctx,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            final OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(ctx, query, options, localBeans)
                .executeNonIterativeSearch(result);
    }

    public Integer countObjects(
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return ShadowSearchLikeOperation
                .create(query, options, task, result, localBeans)
                .executeCount(result);
    }

    public void propagateOperations(ResourceType resource, ShadowType shadow, Task task,
            OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, GenericFrameworkException, ObjectAlreadyExistsException,
            SecurityViolationException, PolicyViolationException, EncryptionException {
        propagateHelper.propagateOperations(resource, shadow, task, result);
    }

    public <T> ItemComparisonResult compare(@NotNull ShadowType repositoryShadow, ItemPath path, T expectedValue, Task task,
            OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
        return compareHelper.compare(repositoryShadow, path, expectedValue, task, result);
    }

    // temporary
    ShadowsLocalBeans getLocalBeans() {
        return localBeans;
    }
}
