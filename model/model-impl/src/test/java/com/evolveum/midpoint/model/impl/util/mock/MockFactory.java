/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util.mock;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

@SuppressWarnings({ "ConstantConditions" })
public class MockFactory {

    public static ProvisioningService createProvisioningService() {
        return new ProvisioningService() {
            @Override
            public @NotNull <T extends ObjectType> PrismObject<T> getObject(
                    @NotNull Class<T> type,
                    @NotNull String oid,
                    @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T extends ObjectType> String addObject(
                    @NotNull PrismObject<T> object,
                    @Nullable OperationProvisioningScriptsType scripts,
                    @Nullable ProvisioningOperationOptions options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public @NotNull SynchronizationResult synchronize(
                    @NotNull ResourceOperationCoordinates coordinates,
                    LiveSyncOptions options,
                    @NotNull LiveSyncTokenStorage tokenStorage,
                    @NotNull LiveSyncEventHandler handler,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return new SynchronizationResult();
            }

            @Override
            public void processAsynchronousUpdates(
                    @NotNull ResourceOperationCoordinates coordinates,
                    @NotNull AsyncUpdateEventHandler handler,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
            }

            @NotNull
            @Override
            public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(
                    @NotNull Class<T> type,
                    @Nullable ObjectQuery query,
                    @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return new SearchResultList<>(new ArrayList<>(0));
            }

            @Override
            public <T extends ObjectType> Integer countObjects(
                    @NotNull Class<T> type,
                    @Nullable ObjectQuery query,
                    @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(
                    @NotNull Class<T> type,
                    @Nullable ObjectQuery query,
                    @Nullable Collection<SelectorOptions<GetOperationOptions>> options,
                    @NotNull ResultHandler<T> handler,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public <T extends ObjectType> String modifyObject(
                    @NotNull Class<T> type,
                    @NotNull String oid,
                    @NotNull Collection<? extends ItemDelta<?, ?>> modifications,
                    @Nullable OperationProvisioningScriptsType scripts,
                    @Nullable ProvisioningOperationOptions options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public <T extends ObjectType> PrismObject<T> deleteObject(
                    Class<T> type,
                    String oid,
                    ProvisioningOperationOptions option,
                    OperationProvisioningScriptsType scripts,
                    Task task,
                    OperationResult parentResult) {
                return null;
            }

            @Override
            public Object executeScript(
                    String resourceOid, ProvisioningScriptType script, Task task, OperationResult parentResult) {
                return null;
            }

            @Override
            public @NotNull OperationResult testResource(
                    @NotNull String resourceOid,
                    @Nullable ResourceTestOptions options,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public @NotNull OperationResult testResource(
                    @NotNull PrismObject<ResourceType> resource,
                    @Nullable ResourceTestOptions options,
                    @NotNull Task task,
                    OperationResult parentResult) {
                return null;
            }

            @Override
            public @NotNull DiscoveredConfiguration discoverConfiguration(
                    @NotNull PrismObject<ResourceType> resource,
                    @NotNull OperationResult parentResult) {
                return DiscoveredConfiguration.empty();
            }

            @Override
            public @Nullable ResourceSchema fetchSchema(
                    @NotNull PrismObject<ResourceType> resource, @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) {
                return null;
            }

            @Override
            public List<ConnectorOperationalStatus> getConnectorOperationalStatus(
                    String resourceOid, Task task, OperationResult parentResult) {
                return null;
            }

            @Override
            public void refreshShadow(
                    PrismObject<ShadowType> shadow,
                    ProvisioningOperationOptions options,
                    Task task,
                    OperationResult parentResult) {
            }

            @Override
            public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult) {
            }

            @Override
            public <T extends ObjectType> void applyDefinition(
                    ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult) {

            }

            @Override
            public <T extends ObjectType> void applyDefinition(PrismObject<T> object, Task task, OperationResult parentResult) {
            }

            @Override
            public void determineShadowState(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult) {
            }

            @Override
            public <T extends ObjectType> void applyDefinition(
                    Class<T> type, ObjectQuery query, Task task, OperationResult parentResult) {
            }

            @Override
            public void provisioningSelfTest(OperationResult parentTestResult, Task task) {

            }

            @Override
            public ProvisioningDiag getProvisioningDiag() {
                return null;
            }

            @Override
            public void postInit(OperationResult parentResult) {
            }

            @Override
            public ConstraintsCheckingResult checkConstraints(
                    ResourceObjectDefinition objectTypeDefinition,
                    PrismObject<ShadowType> shadowObject,
                    PrismObject<ShadowType> shadowObjectOld,
                    ResourceType resource,
                    String shadowOid,
                    ConstraintViolationConfirmer constraintViolationConfirmer,
                    ConstraintsCheckingStrategyType strategy,
                    @NotNull Task task,
                    @NotNull OperationResult parentResult) {
                return null;
            }

            @Override
            public void enterConstraintsCheckerCache() {
            }

            @Override
            public void exitConstraintsCheckerCache() {
            }

            @Override
            public <O extends ObjectType, T> ItemComparisonResult compare(
                    Class<O> type, String oid, ItemPath path, T expectedValue, Task task, OperationResult result) {
                return null;
            }

            @Override
            public void shutdown() {
            }

            @Override
            public SystemConfigurationType getSystemConfiguration() {
                return null;
            }

            @Override
            public void setSynchronizationSorterEvaluator(SynchronizationSorterEvaluator evaluator) {
            }

            @Override
            public @NotNull ResourceObjectClassification classifyResourceObject(
                    @NotNull ShadowType combinedObject,
                    @NotNull ResourceType resource,
                    @Nullable ObjectSynchronizationDiscriminatorType existingSorterResult,
                    @NotNull Task task,
                    @NotNull OperationResult result) {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable String generateShadowTag(
                    @NotNull ShadowType combinedObject,
                    @NotNull ResourceType resource,
                    @NotNull ResourceObjectDefinition definition,
                    @NotNull Task task,
                    @NotNull OperationResult result) {
                return null;
            }

            @Override
            public void expandConfigurationObject(
                    @NotNull PrismObject<? extends ObjectType> configurationObject,
                    @NotNull Task task,
                    @NotNull OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException {
            }

            @Override
            public @NotNull CapabilityCollectionType getNativeCapabilities(@NotNull String connOid, OperationResult result) {
                return new CapabilityCollectionType();
            }
        };
    }

    public static RepositoryService createRepositoryService() {
        return new RepositoryService() {
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
                    @Nullable ObjectQuery query, @Nullable Collection<SelectorOptions<GetOperationOptions>> options, @NotNull OperationResult parentResult) {
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
            public RepositoryDiag getRepositoryDiag() {
                return null;
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
        };
    }

    public static EventDispatcher createChangeNotificationDispatcher() {
        return new EventDispatcher() {
            @Override
            public void notify(ShadowDeathEvent event, Task task, OperationResult result) {

            }

            @Override
            public void registerListener(ResourceObjectChangeListener listener) {

            }

            @Override
            public void registerListener(ResourceOperationListener listener) {

            }

            @Override
            public void registerListener(ExternalResourceEventListener listener) {

            }

            @Override
            public void registerListener(ShadowDeathListener listener) {

            }

            @Override
            public void unregisterListener(ResourceObjectChangeListener listener) {

            }

            @Override
            public void unregisterListener(ResourceOperationListener listener) {

            }

            @Override
            public void unregisterListener(ExternalResourceEventListener listener) {

            }

            @Override
            public void unregisterListener(ShadowDeathListener listener) {

            }

            @Override
            public void notifyEvent(ExternalResourceEvent event, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifyChange(@NotNull ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifySuccess(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifyFailure(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifyInProgress(@NotNull ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public String getName() {
                return null;
            }
        };
    }
}
