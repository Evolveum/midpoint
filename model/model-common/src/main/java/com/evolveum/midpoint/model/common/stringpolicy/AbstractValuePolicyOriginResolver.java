/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.stringpolicy;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;
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
    public <R extends ObjectType> void resolve(
            ProhibitedValueItemType prohibitedValueItem,
            ResultHandler<R> handler,
            String contextDescription,
            Task task,
            OperationResult result) throws ObjectNotFoundException, SchemaException,
            CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ValuePolicyOriginType origin = defaultIfNull(prohibitedValueItem.getOrigin(), OBJECT);
        switch (origin) {
            case OBJECT:
                handleObject(handler, result);
                break;
            case OWNER:
                handleOwner(handler, task, result);
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

    private <P extends ObjectType> void handleProjections(
            ResultHandler<P> handler,
            ProhibitedValueItemType prohibitedValueItem,
            String contextDescription,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        // Not very efficient. We will usually read the shadows again, as they are already in model context.
        // It will also work only for the items that are stored in shadow (usually not attributes, unless caching is enabled).
        // But this is good enough for now.
        FocusType focus;
        if (object.canRepresent(FocusType.class)) {
            focus = (FocusType) object.asObjectable();
        } else if (object.canRepresent(ShadowType.class)) {
            if (object.getOid() == null) {
                // FIXME: If shadow is new, it has oid=null,
                //  so reverse search does not make sense (or work since oid=null search is search for all focuses),
                //  we should probably exit
                //  Probably there should be special case for shadow.oid == null, which will
                //  process only provided shadow or do not handle projection?
                return;
            }

            ObjectQuery query = PrismContext.get()
                    .queryFor(FocusType.class)
                    .item(FocusType.F_LINK_REF).ref(object.getOid())
                    .maxSize(1)
                    .build();
            try {
                List<PrismObject<FocusType>> objects =
                        objectResolver.searchObjects(FocusType.class, query, createReadOnlyCollection(), task, result);
                if (objects.isEmpty()) {
                    return;
                }
                focus = MiscUtil.extractSingleton(objects).asObjectable();
            } catch (CommunicationException | ConfigurationException | SecurityViolationException
                    | ExpressionEvaluationException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            return;
        }
        ShadowDiscriminatorType discriminator = prohibitedValueItem.getProjectionDiscriminator();
        for (ObjectReferenceType linkRef: focus.getLinkRef()) {
            GetOperationOptions options = GetOperationOptions.createReadOnly();
            options.setNoFetch(true);
            ShadowType resolvedShadow = objectResolver.resolve(linkRef, ShadowType.class,
                    SelectorOptions.createCollection(options),
                    "resolving projection shadow in " + contextDescription, task, result);
            if (discriminator != null && !ShadowUtil.matches(resolvedShadow, discriminator)) {
                LOGGER.trace("Skipping evaluation of projection {} in {} because it does not match discriminator",
                        resolvedShadow, contextDescription);
                continue;
            }
            //noinspection unchecked
            handler.handle((PrismObject<P>) resolvedShadow.asPrismObject(), result);
        }
    }

    private <P extends ObjectType> void handleOwner(ResultHandler<P> handler, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery ownerQuery = getOwnerQuery();
        if (ownerQuery != null) {
            objectResolver.searchIterative(getOwnerClass(), ownerQuery, createReadOnlyCollection(), handler, task, result);
        }
    }
}
