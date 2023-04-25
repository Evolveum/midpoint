/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

/**
 * @author semancik
 *
 */
public class LensOwnerResolver<F extends ObjectType> implements OwnerResolver {

    private static final Trace LOGGER = TraceManager.getTrace(LensOwnerResolver.class);

    private final LensContext<F> context;
    private final ObjectResolver objectResolver;
    private final Task task;
    private final OperationResult result;

    public LensOwnerResolver(LensContext<F> context, ObjectResolver objectResolver, Task task,
            OperationResult result) {
        super();
        this.context = context;
        this.objectResolver = objectResolver;
        this.task = task;
        this.result = result;
    }

    @Override
    public <FO extends FocusType, O extends ObjectType> PrismObject<FO> resolveOwner(PrismObject<O> object)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (object == null) {
            return null;
        }
        if (object.canRepresent(ShadowType.class)) {
            LensFocusContext<F> focusContext = context.getFocusContext();
            if (focusContext == null) {
                return null;
            } else if (focusContext.getObjectNew() != null) {
                // If we create both owner and shadow in the same operation (see e.g. MID-2027), we have to provide object new
                // Moreover, if the authorization would be based on a property that is being changed along with the
                // the change being authorized, we would like to use changed version.
                //noinspection unchecked
                return (PrismObject<FO>) focusContext.getObjectNew();
            } else if (focusContext.getObjectCurrent() != null) {
                // This could be useful if the owner is being deleted.
                //noinspection unchecked
                return (PrismObject<FO>) focusContext.getObjectCurrent();
            } else {
                return null;
            }
        } else if (object.canRepresent(UserType.class)) {
            if (context.getOwnerOid() != null) {
                if (context.getCachedOwner() == null) {
                    ObjectReferenceType ref = new ObjectReferenceType();
                    ref.setOid(context.getOwnerOid());
                    UserType ownerType;
                    try {
                        ownerType = objectResolver.resolve(ref, UserType.class, null, "context owner", task, result);
                    } catch (ObjectNotFoundException | SchemaException e) {
                        LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
                        return null;
                    }
                    context.setCachedOwner(ownerType.asPrismObject());
                }
                //noinspection unchecked
                return (PrismObject<FO>) context.getCachedOwner();
            }

            if (object.getOid() == null) {
                // No reason to query. We will find nothing, but the query may take a long time.
                return null;
            }

            ObjectQuery query = PrismContext.get().queryFor(UserType.class)
                    .item(FocusType.F_PERSONA_REF).ref(object.getOid()).build();
            List<PrismObject<UserType>> owners = new ArrayList<>();
            try {
                objectResolver.searchIterative(UserType.class, query, createReadOnlyCollection(),
                        (o,result) -> owners.add(o), task, result);
            } catch (ObjectNotFoundException | CommunicationException | ConfigurationException
                    | SecurityViolationException | SchemaException | ExpressionEvaluationException e) {
                LOGGER.warn("Cannot resolve owner of {}: {}", object, e.getMessage(), e);
                return null;
            }
            if (owners.isEmpty()) {
                return null;
            }
            if (owners.size() > 1) {
                LOGGER.warn("More than one owner of {}: {}", object, owners);
            }
            //noinspection unchecked
            return (PrismObject<FO>) owners.get(0);
        } else if (object.canRepresent(AbstractRoleType.class)) {
            // TODO: get owner from roleMembershipRef;relation=owner (MID-5689)
            return null;
        } else if (object.canRepresent(TaskType.class)) {
            ObjectReferenceType ownerRef = ((TaskType)(object.asObjectable())).getOwnerRef();
            if (ownerRef == null) {
                return null;
            }
            try {
                ObjectType ownerType = objectResolver.resolve(ownerRef, ObjectType.class, null, "resolving owner of "+object, task, result);
                if (ownerType == null) {
                    return null;
                }
                //noinspection unchecked
                return (PrismObject<FO>) ownerType.asPrismObject();
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.error("Error resolving owner of {}: {}", object, e.getMessage(), e);
                return null;
            }
        } else {
            LOGGER.warn("Cannot resolve owner of {}, owners can be resolved only for shadows, users (personas), "
                    + "tasks, and (eventually) abstract roles", object);
            return null;
        }
    }
}
