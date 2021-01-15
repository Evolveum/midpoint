/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Repository implementation based on SQL, JDBC and Querydsl without any ORM.
 * WORK IN PROGRESS:
 * It will be PostgreSQL only or at least PG optimized with generic SQL support for other unsupported DB.
 * Possible Oracle support is in play.
 */
public class SqaleRepositoryService implements RepositoryService {

    private final SqlRepoContext sqlRepoContext;

    public SqaleRepositoryService(SqlRepoContext sqlRepoContext) {
        this.sqlRepoContext = sqlRepoContext;
    }

    @Override
    public @NotNull <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return null;
        // TODO
    }

    @Override
    public <T extends ObjectType> String getVersion(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return null;
        // TODO
    }

    // Add/modify/delete

    @Override
    public <T extends ObjectType> String addObject(
            PrismObject<T> object, RepoAddOptions options, OperationResult parentResult)
            throws ObjectAlreadyExistsException, SchemaException {
        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            Class<T> type, String oid,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            Class<T> type, String oid, Collection<? extends ItemDelta<?, ?>> modifications,
            RepoModifyOptions options, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type,
            @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition,
            RepoModifyOptions options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {

        return null;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException {
        return null;
        // TODO
    }

    // Counting/searching

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return 0;
        // TODO
    }

    @Override
    public @NotNull <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return null;
        // TODO
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential,
            OperationResult parentResult) throws SchemaException {
        return null;
        // TODO
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return 0;
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(
            Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        return null;
        // TODO
    }

    @Override
    public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid)
            throws SchemaException {
        return false;
        // TODO
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return null;
        // TODO
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        return 0;
        // TODO
    }

    @Override
    public void returnUnusedValuesToSequence(
            String oid, Collection<Long> unusedValues, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {

        // TODO
    }

    @Override
    public RepositoryDiag getRepositoryDiag() {
        return null;
        // TODO
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {

        // TODO
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {

        // TODO
    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult result) {
        return null;
        // TODO
    }

    @Override
    public <O extends ObjectType> boolean selectorMatches(
            ObjectSelectorType objectSelector, PrismObject<O> object,
            ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        return false;
        // TODO
    }

    @Override
    public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {
        // TODO
    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return null;
        // TODO
    }

    @Override
    public void postInit(OperationResult result) throws SchemaException {
        // TODO
    }

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid) {
        return null;
        // TODO
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {
        // TODO
    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
        return false;
        // TODO
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid,
            DiagnosticInformationType information, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        // TODO
    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return null;
        // TODO
    }
}
