/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.correlator.*;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.correlator.CorrelatorUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

@Component
public class CorrelationServiceImpl implements CorrelationService {

    @Autowired ModelBeans beans;
    @Autowired CorrelatorFactoryRegistry correlatorFactoryRegistry;
    @Autowired SystemObjectCache systemObjectCache;

    @Override
    public CorrelationResult correlate(ShadowType shadowedResourceObject, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CorrelatorInstantiationContext instantiationContext = getInstantiationContext(shadowedResourceObject, task, result);
        CorrelationContext correlationContext = createCorrelationContext(instantiationContext, result);
        return correlatorFactoryRegistry
                .instantiateCorrelator(instantiationContext.correlators, task, result)
                .correlate(correlationContext, task, result);
    }

    @Override
    public @NotNull CorrelatorInstantiationContext getInstantiationContext(
            @NotNull PrismObject<CaseType> aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        return getInstantiationContext(
                CorrelatorUtil.getShadowFromCorrelationCase(aCase),
                task, result);
    }

    private @NotNull CorrelatorInstantiationContext getInstantiationContext(ShadowType shadow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        String resourceOid = ShadowUtil.getResourceOidRequired(shadow);
        ResourceType resource =
                beans.provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();

        // We expect that the shadow is classified + reasonably fresh (= not legacy), so it has kind+intent present.
        ShadowKindType kind = MiscUtil.requireNonNull(shadow.getKind(), () -> new IllegalStateException("No kind in " + shadow));
        String intent = MiscUtil.requireNonNull(shadow.getIntent(), () -> new IllegalStateException("No intent in " + shadow));
        // TODO check for "unknown" ?

        // We'll look for type definition in the future (after synchronization is integrated into it).
        ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
        ResourceObjectTypeDefinition typeDefinition = schema.findObjectTypeDefinitionRequired(kind, intent);

        for (ObjectSynchronizationType config : resource.getSynchronization().getObjectSynchronization()) {
            if (config.getKind() == kind && intent.equals(config.getIntent())) {
                return new CorrelatorInstantiationContext(
                        shadow,
                        resource,
                        typeDefinition,
                        config,
                        MiscUtil.requireNonNull(
                                config.getCorrelators(),
                                () -> new IllegalStateException("No correlators in " + config)));
            }
        }
        throw new IllegalStateException(
                "No " + kind + "/" + intent + " (kind/intent) definition in " + resource + " (for " + shadow + ")");
    }

    @Override
    public Correlator instantiateCorrelator(
            @NotNull PrismObject<CaseType> aCase,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {
        CorrelatorInstantiationContext instantiationContext = getInstantiationContext(aCase, task, result);
        return correlatorFactoryRegistry.instantiateCorrelator(instantiationContext.correlators, task, result);
    }

    @Override
    public @NotNull Correlator instantiateCorrelator(
            @NotNull ShadowType shadowedResourceObject,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        CorrelatorInstantiationContext fullContext = getInstantiationContext(shadowedResourceObject, task, result);
        return correlatorFactoryRegistry.instantiateCorrelator(fullContext.correlators, task, result);
    }

    public @NotNull Correlator instantiateCorrelator(
            @NotNull CorrelatorInstantiationContext fullContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        return correlatorFactoryRegistry.instantiateCorrelator(fullContext.correlators, task, result);
    }

    private CorrelationContext createCorrelationContext(CorrelatorInstantiationContext fullContext, OperationResult result)
            throws SchemaException {
        Class<ObjectType> objectTypeClass = ObjectTypes.getObjectTypeClass(
                Objects.requireNonNull(
                        fullContext.synchronizationBean.getFocusType(),
                        () -> "No focus type for " + fullContext.typeDefinition));
        return new CorrelationContext(
                fullContext.shadow,
                (FocusType) PrismContext.get().createObjectable(objectTypeClass), // TODO
                fullContext.resource,
                fullContext.typeDefinition,
                asObjectable(systemObjectCache.getSystemConfiguration(result)));
    }
}
