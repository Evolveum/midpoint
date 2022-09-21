/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util.mock;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.*;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.repo.api.perf.PerformanceMonitor;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MockFactory {

    public static ProvisioningService createProvisioningService() {
        return new ProvisioningService() {
            @Override
            public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public <T extends ObjectType> String addObject(PrismObject<T> object, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public int synchronize(ResourceShadowDiscriminator shadowCoordinates, Task task, TaskPartitionDefinitionType taskPartition, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, PolicyViolationException, PreconditionViolationException {
                return 0;
            }

            @Override
            public String startListeningForAsyncUpdates(ResourceShadowDiscriminator shadowCoordinates, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public void stopListeningForAsyncUpdates(String listeningActivityHandle, Task task, OperationResult parentResult) {

            }

            @Override
            public AsyncUpdateListeningActivityInformationType getAsyncUpdatesListeningActivityInformation(String listeningActivityHandle, Task task, OperationResult parentResult) {
                return null;
            }

            @NotNull
            @Override
            public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return new SearchResultList(new ArrayList<>(0));
            }

            @Override
            public <T extends ObjectType> Integer countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<T> handler, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public <T extends ObjectType> String modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationProvisioningScriptsType scripts, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, PolicyViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public <T extends ObjectType> PrismObject<T> deleteObject(Class<T> type, String oid, ProvisioningOperationOptions option, OperationProvisioningScriptsType scripts, Task task, OperationResult parentResult) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public <T extends ObjectType> Object executeScript(String resourceOid, ProvisioningScriptType script, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public OperationResult testResource(String resourceOid, Task task) throws ObjectNotFoundException {
                return null;
            }

            @Override
            public Set<ConnectorType> discoverConnectors(ConnectorHostType hostType, OperationResult parentResult) throws CommunicationException {
                return null;
            }

            @Override
            public List<ConnectorOperationalStatus> getConnectorOperationalStatus(String resourceOid, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public List<PrismObject<? extends ShadowType>> listResourceObjects(String resourceOid, QName objectClass, ObjectPaging paging, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public void refreshShadow(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, SecurityViolationException, ExpressionEvaluationException {

            }

            @Override
            public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

            }

            @Override
            public <T extends ObjectType> void applyDefinition(ObjectDelta<T> delta, Objectable object, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

            }

            @Override
            public <T extends ObjectType> void applyDefinition(PrismObject<T> object, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

            }

            @Override
            public <T extends ObjectType> void applyDefinition(Class<T> type, ObjectQuery query, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

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
            public ConstraintsCheckingResult checkConstraints(RefinedObjectClassDefinition shadowDefinition, PrismObject<ShadowType> shadowObject, PrismObject<ShadowType> shadowObjectOld, ResourceType resourceType, String shadowOid, ResourceShadowDiscriminator resourceShadowDiscriminator, ConstraintViolationConfirmer constraintViolationConfirmer, ConstraintsCheckingStrategyType strategy, Task task, OperationResult parentResult) throws CommunicationException, ObjectAlreadyExistsException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public void enterConstraintsCheckerCache() {

            }

            @Override
            public void exitConstraintsCheckerCache() {

            }

            @Override
            public <O extends ObjectType, T> ItemComparisonResult compare(Class<O> type, String oid, ItemPath path, T expectedValue, Task task, OperationResult result) throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException, EncryptionException {
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
            public ShadowState determineShadowState(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
                return null;
            }

            @Override
            public TaskManager getTaskManager() {
                return null;
            }
        };
    }

    public static RepositoryService createRepositoryService() {
        return new RepositoryService() {
            @Override
            public <O extends ObjectType> PrismObject<O> getObject(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
                return null;
            }

            @Override
            public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
                return null;
            }

            @Override
            public <T extends Containerable> int countContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
                return 0;
            }

            @Override
            public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult parentResult) throws ObjectAlreadyExistsException, SchemaException {
                return null;
            }

            @NotNull
            @Override
            public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
                return new SearchResultList(new ArrayList<>(0));
            }

            @Override
            public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
                return new SearchResultList(new ArrayList<>(0));
            }

            @Override
            public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query, ResultHandler<T> handler, Collection<SelectorOptions<GetOperationOptions>> options, boolean strictlySequential, OperationResult parentResult) throws SchemaException {
                return null;
            }

            @Override
            public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {
                return 0;
            }

            @Override
            public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult parentResult) throws SchemaException {
                return 0;
            }

            @Override
            public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids) throws SchemaException {
                return false;
            }

            @Override
            public <O extends ObjectType> boolean isDescendant(PrismObject<O> object, String orgOid) throws SchemaException {
                return false;
            }

            @Override
            public <O extends ObjectType> boolean isAncestor(PrismObject<O> object, String oid) throws SchemaException {
                return false;
            }

            @NotNull
            @Override
            public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
                return null;
            }

            @NotNull
            @Override
            public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, RepoModifyOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
                return null;
            }

            @NotNull
            @Override
            public <T extends ObjectType> ModifyObjectResult<T> modifyObject(Class<T> type, String oid, Collection<? extends ItemDelta> modifications, ModificationPrecondition<T> precondition, RepoModifyOptions options, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, PreconditionViolationException {
                return null;
            }

            @NotNull
            @Override
            public <T extends ObjectType> DeleteObjectResult deleteObject(Class<T> type, String oid, OperationResult parentResult) throws ObjectNotFoundException {
                return null;
            }

            @Override
            public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult parentResult) throws ObjectNotFoundException {
                return null;
            }

            @Override
            public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) {
                return null;
            }

            @Override
            public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid, Class<T> resourceObjectShadowType, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
                return null;
            }

            @Override
            public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
                return 0;
            }

            @Override
            public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {

            }

            @Override
            public RepositoryDiag getRepositoryDiag() {
                return null;
            }

            @Override
            public void repositorySelfTest(OperationResult parentResult) {

            }

            @Override
            public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {

            }

            @Override
            public RepositoryQueryDiagResponse executeQueryDiagnostics(RepositoryQueryDiagRequest request, OperationResult result) {
                return null;
            }

            @Override
            public <O extends ObjectType> boolean selectorMatches(ObjectSelectorType objectSelector, PrismObject<O> object, ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
                return false;
            }

            @Override
            public void applyFullTextSearchConfiguration(FullTextSearchConfigurationType fullTextSearch) {

            }

            @Override
            public FullTextSearchConfigurationType getFullTextSearchConfiguration() {
                return null;
            }

            @Override
            public void postInit(OperationResult result) throws SchemaException {

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
            public <T extends ObjectType> void addDiagnosticInformation(Class<T> type, String oid, DiagnosticInformationType information, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

            }

            @Override
            public PerformanceMonitor getPerformanceMonitor() {
                return null;
            }
        };
    }

    public static ChangeNotificationDispatcher createChangeNotificationDispatcher() {
        return new ChangeNotificationDispatcher() {
            @Override
            public void registerNotificationListener(ResourceObjectChangeListener listener) {

            }

            @Override
            public void registerNotificationListener(ResourceOperationListener listener) {

            }

            @Override
            public void registerNotificationListener(ResourceEventListener listener) {

            }

            @Override
            public void unregisterNotificationListener(ResourceObjectChangeListener listener) {

            }

            @Override
            public void unregisterNotificationListener(ResourceOperationListener listener) {

            }

            @Override
            public void unregisterNotificationListener(ResourceEventListener listener) {

            }

            @Override
            public void notifyEvent(ResourceEventDescription eventDescription, Task task, OperationResult parentResult) throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ObjectNotFoundException, GenericConnectorException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException {

            }

            @Override
            public <F extends FocusType> void notifyChange(ResourceObjectShadowChangeDescription change, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifySuccess(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifyFailure(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public void notifyInProgress(ResourceOperationDescription operationDescription, Task task, OperationResult parentResult) {

            }

            @Override
            public String getName() {
                return null;
            }
        };
    }
}
