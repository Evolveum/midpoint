/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.util.mock;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.ConflictWatcher;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.ModificationPrecondition;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.RepositoryQueryDiagRequest;
import com.evolveum.midpoint.schema.RepositoryQueryDiagResponse;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RepositoryServiceMock implements RepositoryService {

    @Override
    @NotNull
    public <O extends ObjectType> PrismObject<O> getObject(Class<O> type,
            String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult) {
        return null;
    }

    @Override
    public <T extends ObjectType> @NotNull String addObject(
            @NotNull PrismObject<T> object, RepoAddOptions options, @NotNull OperationResult parentResult) {
        return UUID.randomUUID().toString();
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return 0;
    }

    @NotNull
    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
            @NotNull Class<T> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
        return new SearchResultList<>(List.of());
    }

    @Override
    public <T extends Containerable> int countContainers(Class<T> type,
            ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
        return 0;
    }

    @Override
    public @NotNull <T extends Containerable> SearchResultList<T> searchContainers(@NotNull Class<T> type,
            @Nullable ObjectQuery query, @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        return new SearchResultList<>(List.of());
    }

    @Override
    public int countReferences(@Nullable ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
        return 0;
    }

    @Override
    public @NotNull SearchResultList<ObjectReferenceType> searchReferences(@NotNull ObjectQuery query,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
        return new SearchResultList<>(List.of());
    }

    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            boolean strictlySequential, OperationResult parentResult) {
        return null;
    }

    @Override
    public SearchResultMetadata searchReferencesIterative(
            @Nullable ObjectQuery query,
            @NotNull ObjectHandler<ObjectReferenceType> handler,
            @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull OperationResult parentResult) {
        return null;
    }

    @Override
    public <T extends Containerable> SearchResultMetadata searchContainersIterative(
            Class<T> type, ObjectQuery query, ObjectHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult parentResult) throws SchemaException {
        return null;
    }

    @Override
    public <O extends ObjectType> boolean isDescendant(
            PrismObject<O> object, String ancestorOrgOid) {
        return false;
    }

    @Override
    public <O extends ObjectType> boolean isAncestor(
            PrismObject<O> object, String descendantOrgOid) {
        return false;
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid, @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull OperationResult parentResult) {
        return null;
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            RepoModifyOptions options, @NotNull OperationResult parentResult) {
        return null;
    }

    @NotNull
    @Override
    public <T extends ObjectType> ModifyObjectResult<T> modifyObject(
            @NotNull Class<T> type, @NotNull String oid,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
            ModificationPrecondition<T> precondition, RepoModifyOptions options,
            @NotNull OperationResult parentResult) {
        return null;
    }

    @NotNull
    @Override
    public <T extends ObjectType> DeleteObjectResult deleteObject(
            Class<T> type, String oid, OperationResult parentResult) {
        return null;
    }

    @Override
    public long advanceSequence(String oid, OperationResult parentResult) {
        return 0;
    }

    @Override
    public void returnUnusedValuesToSequence(
            String oid, Collection<Long> unusedValues, OperationResult parentResult) {

    }

    @Override
    public @NotNull <T extends ObjectType> Collection<Long> allocateContainerIdentifiers(
            @NotNull Class<T> type, @NotNull String oid, int howMany, @NotNull OperationResult result) {
        return Set.of();
    }

    @Override
    public @NotNull RepositoryDiag getRepositoryDiag() {
        return new RepositoryDiag();
    }

    @Override
    public @NotNull String getRepositoryType() {
        return "mock";
    }

    @Override
    public boolean supports(@NotNull Class<? extends ObjectType> type) {
        return false;
    }

    @Override
    public void repositorySelfTest(OperationResult parentResult) {

    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {

    }

    @Override
    public RepositoryQueryDiagResponse executeQueryDiagnostics(
            RepositoryQueryDiagRequest request, OperationResult result) {
        return null;
    }

    @Override
    public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {

    }

    @Override
    public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
        return null;
    }

    @Override
    public void postInit(OperationResult result) {

    }

    @Override
    public ConflictWatcher createAndRegisterConflictWatcher(@NotNull String oid) {
        return null;
    }

    @Override
    public void unregisterConflictWatcher(ConflictWatcher watcher) {

    }

    @Override
    public boolean hasConflict(ConflictWatcher watcher, OperationResult result) {
        return false;
    }

    @Override
    public <T extends ObjectType> void addDiagnosticInformation(Class<T> type,
            String oid, DiagnosticInformationType information, OperationResult parentResult) {

    }

    @Override
    public PerformanceMonitor getPerformanceMonitor() {
        return null;
    }

}
