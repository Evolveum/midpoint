/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * The callback from some object utilities to resolve objects.
 *
 * The classes implementing this will most likely fetch the objects from the
 * repository or from some kind of object cache.
 *
 * @author Radovan Semancik
 */
public interface ObjectResolver {

    /**
     * Resolve the provided reference to object (ObjectType).
     *
     * Note: The reference is used instead of just OID because the reference
     * also contains object type. This speeds up the repository operations.
     *
     * @param ref object reference to resolve
     * @param contextDescription short description of the context of resolution, e.g. "executing expression FOO". Used in error messages.
     * @return resolved object
     * @throws ObjectNotFoundException requested object does not exist
     * @throws SchemaException error dealing with storage schema
     * @throws IllegalArgumentException wrong OID format, etc.
     *
     * TODO resolve module dependencies to allow task to be of type Task
     */
    @NotNull <O extends ObjectType> O resolve(
            Referencable ref,
            Class<O> expectedType,
            Collection<SelectorOptions<GetOperationOptions>> options,
            String contextDescription,
            Task task,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException;

    <O extends ObjectType> O getObject(
            @NotNull Class<O> clazz,
            @NotNull String oid,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Task task,
            @NotNull OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    <O extends ObjectType> void searchIterative(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<O> handler, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    <O extends ObjectType> SearchResultList<PrismObject<O>> searchObjects(Class<O> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

    // EXPERIMENTAL (implemented only for ModelObjectResolver)
    // TODO clean up this mess
    default void resolveAllReferences(Collection<PrismContainerValue<?>> pcvs, Object taskObject, OperationResult result) {
        throw new UnsupportedOperationException();
    }

    interface Session {
        GetOperationOptions getOptions();
        boolean contains(String oid);
        void put(String oid, PrismObject<?> object);
        PrismObject<?> get(String oid);
    }

    default Session openResolutionSession(GetOperationOptions options) {
        return new Session() {
            private final Map<String, PrismObject<?>> objects = new HashMap<>();

            @Override
            public GetOperationOptions getOptions() {
                return options;
            }

            @Override
            public boolean contains(String oid) {
                return objects.containsKey(oid);
            }

            @Override
            public void put(String oid, PrismObject<?> object) {
                objects.put(oid, object);
            }

            @Override
            public PrismObject<?> get(String oid) {
                return objects.get(oid);
            }
        };
    }

    default void resolveReference(PrismReferenceValue ref, String contextDescription,
            Session session, Object task, OperationResult result) {
        throw new UnsupportedOperationException();
    }
}
