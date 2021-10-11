/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProhibitedValueItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyOriginType;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyOriginType.OBJECT;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author semancik
 *
 */
public abstract class AbstractValuePolicyOriginResolver<O extends ObjectType> implements ObjectBasedValuePolicyOriginResolver<O> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractValuePolicyOriginResolver.class);

    private final PrismObject<O> object;
    private final ObjectResolver objectResolver;

    AbstractValuePolicyOriginResolver(PrismObject<O> object, ObjectResolver objectResolver) {
        super();
        this.object = object;
        this.objectResolver = objectResolver;
    }

    @Override
    public PrismObject<O> getObject() {
        return object;
    }

    public abstract ObjectQuery getOwnerQuery();

    public <R extends ObjectType> Class<R> getOwnerClass() {
        return (Class<R>) UserType.class;
    }

    @Override
    public <R extends ObjectType> void resolve(ProhibitedValueItemType prohibitedValueItem, ResultHandler<R> handler,
            String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ValuePolicyOriginType origin = defaultIfNull(prohibitedValueItem.getOrigin(), OBJECT);
        switch (origin) {
            case OBJECT:
                handleObject(handler, result);
                break;
            case OWNER:
                handleOwner(handler, contextDescription, task, result);
                break;
            case PERSONA:
                handlePersonas(handler, contextDescription, task, result);
                break;
            case PROJECTION:
                handleProjections(handler, prohibitedValueItem, contextDescription, task, result);
                break;
            default:
                throw new IllegalArgumentException("Unexpected origin type " + origin);
        }
    }

    private <R extends ObjectType> void handleObject(ResultHandler<R> handler, OperationResult result) {
        handler.handle((PrismObject<R>) object, result);
    }

    private <P extends ObjectType> void handlePersonas(ResultHandler<P> handler, String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (!object.canRepresent(UserType.class)) {
            return;
        }
        for (ObjectReferenceType personaRef: ((UserType)object.asObjectable()).getPersonaRef()) {
            UserType persona = objectResolver.resolve(personaRef, UserType.class, SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), "resolving persona in " + contextDescription, task, result);
            //noinspection unchecked
            handler.handle((PrismObject<P>) persona.asPrismObject(), result);
        }
    }

    private <P extends ObjectType> void handleProjections(ResultHandler<P> handler, ProhibitedValueItemType prohibitedValueItemType, String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        // Not very efficient. We will usually read the shadows again, as they are already in model context.
        // It will also work only for the items that are stored in shadow (usually not attributes, unless caching is enabled).
        // But this is good enough for now.
        FocusType focusType;
        if (object.canRepresent(FocusType.class)) {
            focusType = ((FocusType)object.asObjectable());
        } else if (object.canRepresent(ShadowType.class)) {
            ObjectQuery query = object.getPrismContext()
                    .queryFor(FocusType.class)
                    .item(FocusType.F_LINK_REF).ref(object.getOid())
                    .build();
            final Holder<FocusType> focusTypeHolder = new Holder<>();
            try {
                objectResolver.searchIterative(FocusType.class, query,
                        SelectorOptions.createCollection(GetOperationOptions.createReadOnly()),
                        (foundObject, objectResult) -> {
                            focusTypeHolder.setValue(foundObject.asObjectable());
                            return true;
                        }, task, result);
            } catch (CommunicationException | ConfigurationException | SecurityViolationException
                    | ExpressionEvaluationException e) {
                throw new SystemException(e.getMessage(), e);
            }
            focusType = focusTypeHolder.getValue();
        } else {
            return;
        }
        // We want to provide default intent to allow configurators to be a little lazy and skip intent specification.
        // Consider changing this if necessary.
        ResourceShadowDiscriminator shadowDiscriminator = ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(
                prohibitedValueItemType.getProjectionDiscriminator(), true);
        for (ObjectReferenceType linkRef: focusType.getLinkRef()) {
            GetOperationOptions options = GetOperationOptions.createReadOnly();
            options.setNoFetch(true);
            ShadowType resolvedShadow = objectResolver.resolve(linkRef, ShadowType.class,
                    SelectorOptions.createCollection(options),
                    "resolving projection shadow in " + contextDescription, task, result);
            if (shadowDiscriminator != null) {
                if (!ShadowUtil.matches(resolvedShadow.asPrismObject(), shadowDiscriminator)) {
                    LOGGER.trace("Skipping evaluation of projection {} in {} because it does not match discriminator", resolvedShadow, contextDescription);
                    continue;
                }
            }
            //noinspection unchecked
            handler.handle((PrismObject<P>) resolvedShadow.asPrismObject(), result);
        }
    }

    private <P extends ObjectType> void handleOwner(ResultHandler<P> handler, String contextDescription, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery ownerQuery = getOwnerQuery();
        if (ownerQuery != null) {
            objectResolver.searchIterative(getOwnerClass(), ownerQuery,
                    SelectorOptions.createCollection(GetOperationOptions.createReadOnly()), handler, task, result);
        }
    }
}
