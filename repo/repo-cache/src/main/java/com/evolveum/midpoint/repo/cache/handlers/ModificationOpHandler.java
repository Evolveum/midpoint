/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.handlers;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.cache.other.MonitoringUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Random;

import static com.evolveum.midpoint.repo.cache.RepositoryCache.CLASS_NAME_WITH_DOT;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastMinimal;
import static com.evolveum.midpoint.schema.util.TraceUtil.isAtLeastNormal;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

/**
 * Handles modification operations: add, modify, delete, and a couple of others.
 * What they have in common is that they invalidate cache entries.
 */
@Component
public class ModificationOpHandler extends BaseOpHandler {

    private static final String ADD_OBJECT = CLASS_NAME_WITH_DOT + "addObject";
    private static final String MODIFY_OBJECT = CLASS_NAME_WITH_DOT + "modifyObject";
    private static final String DELETE_OBJECT = CLASS_NAME_WITH_DOT + "deleteObject";
    private static final String ADVANCE_SEQUENCE = CLASS_NAME_WITH_DOT + "advanceSequence";
    private static final String RETURN_UNUSED_VALUES_TO_SEQUENCE = CLASS_NAME_WITH_DOT + "returnUnusedValuesToSequence";
    private static final String ADD_DIAGNOSTIC_INFORMATION = CLASS_NAME_WITH_DOT + "addDiagnosticInformation";

    private Integer modifyRandomDelayRange;

    private static final Random RND = new Random();

    public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {
        OperationResult result = parentResult.subresult(ADD_OBJECT)
                .addQualifier(object.asObjectable().getClass().getSimpleName())
                .addParam("type", object.getCompileTimeClass())
                .addParam("overwrite", RepoAddOptions.isOverwrite(options))
                .addArbitraryObjectAsParam("options", options)
                .build();
        RepositoryAddTraceType trace;
        TracingLevelType level = result.getTracingLevel(RepositoryAddTraceType.class);
        if (isAtLeastMinimal(level)) {
            trace = new RepositoryAddTraceType()
                    .options(String.valueOf(options));
            result.addTrace(trace);
        } else {
            trace = null;
        }

        try {
            String oid;
            Long startTime = MonitoringUtil.repoOpStart();
            try {
                oid = repositoryService.addObject(object, options, result);
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
            }
            // DON't cache the object here. The object may not have proper "JAXB" form, e.g. some pieces may be
            // DOM element instead of JAXB elements. Not to cache it is safer and the performance loss
            // is acceptable.
            Class<T> type = stateNonNull(object.getCompileTimeClass(), "object without type: %s", object);
            if (options != null && options.isOverwrite()) {
                invalidator.invalidateCacheEntries(
                        type, oid, new ModifyObjectResult<>(object, Collections.emptyList(), options.isOverwrite()), result);
            } else {
                // just for sure (the object should not be there but ...)
                invalidator.invalidateCacheEntries(type, oid, new AddObjectResult<>(object), result);
            }
            if (trace != null) {
                trace.setOid(oid);
                if (isAtLeastNormal(level)) {
                    // We put the object into the trace here, because now it has OID set
                    trace.setObjectRef(ObjectTypeUtil.createObjectRefWithFullObject(object.clone()));
                }
            }
            return oid;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(@NotNull Class<T> type, @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        OperationResult result = parentResult.subresult(MODIFY_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .addArbitraryObjectAsParam("options", options)
                .build();

        if (result.isTracingAny(RepositoryModifyTraceType.class)) {
            RepositoryModifyTraceType trace = new RepositoryModifyTraceType()
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .oid(oid)
                    .options(String.valueOf(options));
            for (ItemDelta<?, ?> modification : modifications) {
                // todo only if configured?
                trace.getModification().addAll(DeltaConvertor.toItemDeltaTypes(modification));
            }
            result.addTrace(trace);
        }

        try {
            randomDelay();
            Long startTime = MonitoringUtil.repoOpStart();
            ModifyObjectResult<T> modifyInfo = null;
            try {
                modifyInfo = repositoryService.modifyObject(type, oid, modifications, precondition, options, result);
                return modifyInfo;
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
                // this changes the object. We are too lazy to apply changes ourselves, so just invalidate
                // the object in cache
                invalidator.invalidateCacheEntries(type, oid, modifyInfo, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    public <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        OperationResult result = parentResult.subresult(DELETE_OBJECT)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .build();

        if (result.isTracingAny(RepositoryDeleteTraceType.class)) {
            RepositoryDeleteTraceType trace = new RepositoryDeleteTraceType()
                    .cache(true)
                    .objectType(prismContext.getSchemaRegistry().determineTypeForClass(type))
                    .oid(oid);
            result.addTrace(trace);
        }
        Long startTime = MonitoringUtil.repoOpStart();
        DeleteObjectResult deleteInfo = null;
        try {
            try {
                deleteInfo = repositoryService.deleteObject(type, oid, result);
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
                invalidator.invalidateCacheEntries(type, oid, deleteInfo, result);
            }
            return deleteInfo;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
            SchemaException {
        OperationResult result = parentResult.subresult(ADVANCE_SEQUENCE)
                .addParam("oid", oid)
                .build();
        try {
            Long startTime = MonitoringUtil.repoOpStart();
            try {
                return repositoryService.advanceSequence(oid, result);
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
                invalidator.invalidateCacheEntries(SequenceType.class, oid, null, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = parentResult.subresult(RETURN_UNUSED_VALUES_TO_SEQUENCE)
                .addParam("oid", oid)
                .addArbitraryObjectCollectionAsParam("unusedValues", unusedValues)
                .build();
        try {
            Long startTime = MonitoringUtil.repoOpStart();
            try {
                repositoryService.returnUnusedValuesToSequence(oid, unusedValues, result);
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
                invalidator.invalidateCacheEntries(SequenceType.class, oid, null, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }


    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid, DiagnosticInformationType information,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        OperationResult result = parentResult.subresult(ADD_DIAGNOSTIC_INFORMATION)
                .addQualifier(type.getSimpleName())
                .addParam("type", type)
                .addParam("oid", oid)
                .build();
        try {
            randomDelay();
            Long startTime = MonitoringUtil.repoOpStart();
            try {
                repositoryService.addDiagnosticInformation(type, oid, information, result);
            } finally {
                MonitoringUtil.repoOpEnd(startTime);
                // this changes the object. We are too lazy to apply changes ourselves, so just invalidate
                // the object in cache
                // TODO specify additional info more precisely (but currently we use this method only in connection with TaskType
                //  and this kind of object is not cached anyway, so let's ignore this
                invalidator.invalidateCacheEntries(type, oid, null, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public void setModifyRandomDelayRange(Integer modifyRandomDelayRange) {
        this.modifyRandomDelayRange = modifyRandomDelayRange;
    }

    private void randomDelay() {
        if (modifyRandomDelayRange == null) {
            return;
        }
        int delay = RND.nextInt(modifyRandomDelayRange);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // Nothing to do
        }
    }
}
