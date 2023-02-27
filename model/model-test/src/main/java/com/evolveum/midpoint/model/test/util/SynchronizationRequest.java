/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.util;

import static com.evolveum.midpoint.model.test.util.SynchronizationRequest.SynchronizationStyle.*;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.test.AbstractIntegrationTest.DEFAULT_SHORT_TASK_WAIT_TIMEOUT;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.schema.TaskExecutionMode;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestSpringBeans;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Causes a single or multiple accounts be synchronized (imported or reconciled):
 *
 * - typically on background, using one-time task created for this,
 * - or alternatively on foreground, using the appropriate model method (for single account import only).
 *
 * Why a special class? To make clients' life easier and avoid many method variants.
 * (Regarding what parameters it needs to specify.)
 *
 * Limitations:
 *
 * - only crude support for tracing on foreground (yet).
 */
@Experimental
public class SynchronizationRequest {

    @NotNull private final AbstractModelIntegrationTest test;
    @NotNull private final String resourceOid;
    @NotNull private final AccountsScope accountsScope;
    @NotNull private final AccountsSpecification accountsSpecification;
    @NotNull private final SynchronizationStyle synchronizationStyle;
    private final long timeout;
    private final boolean assertSuccess;
    private final Task task;
    private final TracingProfileType tracingProfile;
    @NotNull private final TaskExecutionMode taskExecutionMode;

    private SynchronizationRequest(
            @NotNull SynchronizationRequest.SynchronizationRequestBuilder builder) {
        this.test = builder.test;
        this.resourceOid = Objects.requireNonNull(builder.resourceOid, "No resource OID");
        this.accountsScope = Objects.requireNonNull(builder.accountsScope, "No accounts scope");
        this.accountsSpecification = builder.getAccountsSpecification();
        this.synchronizationStyle = Objects.requireNonNullElse(builder.synchronizationStyle, IMPORT);
        this.timeout = builder.timeout;
        this.assertSuccess = builder.assertSuccess;
        this.task = Objects.requireNonNullElseGet(builder.task, test::getTestTask);
        this.tracingProfile = builder.tracingProfile;
        this.taskExecutionMode = Objects.requireNonNullElse(builder.taskExecutionMode, TaskExecutionMode.PRODUCTION);
    }

    public String execute(OperationResult result) throws CommonException, IOException {
        ObjectQuery query = createResourceObjectQuery(result);
        WorkDefinitionsType work;
        ResourceObjectSetType resourceObjectSet = new ResourceObjectSetType()
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                .kind(accountsScope.getKind())
                .intent(accountsScope.getIntent())
                .objectclass(accountsScope.getObjectClassName())
                .query(PrismContext.get().getQueryConverter().createQueryType(query))
                .queryApplication(ResourceObjectSetQueryApplicationModeType.REPLACE);
        if (synchronizationStyle == IMPORT) {
            work = new WorkDefinitionsType()
                    ._import(new ImportWorkDefinitionType()
                            .resourceObjects(resourceObjectSet));
        } else {
            work = new WorkDefinitionsType()
                    .reconciliation(new ReconciliationWorkDefinitionType()
                            .resourceObjects(resourceObjectSet));
        }
        TaskType importTask = new TaskType()
                .name(synchronizationStyle.taskName)
                .executionState(TaskExecutionStateType.RUNNABLE)
                .activity(new ActivityDefinitionType()
                        .work(work)
                        .executionMode(
                                getBackgroundTaskExecutionMode())
                        .execution(new ActivityExecutionDefinitionType()
                                .configurationToUse(new ConfigurationSpecificationType()
                                                .productionConfiguration(
                                                        taskExecutionMode.isProductionConfiguration()))
                                .createSimulationResult(
                                        test.isNativeRepository() && !taskExecutionMode.isFullyPersistent())));
        String taskOid = test.addObject(importTask, task, result);
        if (tracingProfile != null) {
            test.traced(
                    tracingProfile,
                    () -> test.waitForTaskCloseOrSuspend(taskOid, timeout));
        } else {
            test.waitForTaskCloseOrSuspend(taskOid, timeout);
        }

        if (assertSuccess) {
            test.assertTask(taskOid, "after")
                    .assertClosed()
                    .assertSuccess();
        }
        return taskOid;
    }

    private @NotNull ExecutionModeType getBackgroundTaskExecutionMode() {
        if (taskExecutionMode.isFullyPersistent()) {
            return ExecutionModeType.FULL;
        } else if (taskExecutionMode.isShadowLevelPersistent()) {
            return ExecutionModeType.PREVIEW;
        } else if (taskExecutionMode.areShadowChangesSimulated()) {
            return ExecutionModeType.SHADOW_MANAGEMENT_PREVIEW;
        } else {
            throw new AssertionError(taskExecutionMode);
        }
    }

    private ObjectQuery createResourceObjectQuery(OperationResult result)
            throws CommonException {
        return accountsSpecification.updateQuery(
                        accountsScope.startQuery(getResource(result)))
                .build();
    }

    private Resource getResource(OperationResult result) throws CommonException {
        return Resource.of(
                getProvisioningService()
                        .getObject(ResourceType.class, resourceOid, null, task, result)
                        .asObjectable());
    }

    private static ProvisioningService getProvisioningService() {
        return TestSpringBeans.getBean(ProvisioningService.class);
    }

    @SuppressWarnings("WeakerAccess")
    public void executeOnForeground(OperationResult result) throws CommonException, IOException {
        List<PrismObject<ShadowType>> shadows =
                getProvisioningService().searchObjects(
                        ShadowType.class,
                        createResourceObjectQuery(result),
                        null,
                        task,
                        result);
        String shadowOid =
                MiscUtil.extractSingletonRequired(
                                shadows,
                                () -> new AssertionError("Multiple matching shadows: " + shadows),
                                () -> new AssertionError("No shadow" + accountsSpecification.describeQuery()))
                        .getOid();

        TaskExecutionMode oldMode = task.setExecutionMode(taskExecutionMode);
        try {
            if (tracingProfile != null) {
                test.traced(
                        tracingProfile,
                        () -> executeImportOnForeground(result, shadowOid));
            } else {
                executeImportOnForeground(result, shadowOid);
            }
        } finally {
            task.setExecutionMode(oldMode);
        }

        if (assertSuccess) {
            result.computeStatus();
            TestUtil.assertSuccess(result);
        }
    }

    private void executeImportOnForeground(OperationResult result, String shadowOid) throws CommonException {
        TestSpringBeans.getBean(ModelService.class)
                .importFromResource(shadowOid, task, result);
    }

    @SuppressWarnings("WeakerAccess")
    public TestSimulationResult executeOnForegroundSimulated(
            SimulationDefinitionType simulationDefinition, Task task, OperationResult result) throws CommonException {
        return test.executeWithSimulationResult(
                taskExecutionMode,
                simulationDefinition,
                task,
                result,
                (localSimResult) -> executeOnForeground(result));
    }

    interface AccountsSpecification {

        S_MatchingRuleEntry updateQuery(S_MatchingRuleEntry queryFor);

        String describeQuery();
    }

    static class SingleAccountSpecification implements AccountsSpecification {
        @NotNull private final QName namingAttribute;
        @NotNull private final String nameValue;

        SingleAccountSpecification(@NotNull QName namingAttribute, @NotNull String nameValue) {
            this.namingAttribute = namingAttribute;
            this.nameValue = nameValue;
        }

        @Override
        public S_MatchingRuleEntry updateQuery(S_MatchingRuleEntry queryFor) {
            return queryFor.and().item(ShadowType.F_ATTRIBUTES, namingAttribute).eq(nameValue);
        }

        @Override
        public String describeQuery() {
            return " having " + namingAttribute + " = " + nameValue;
        }
    }

    /** All accounts of given type. */
    static class AllAccountsSpecification implements AccountsSpecification {

        @Override
        public S_MatchingRuleEntry updateQuery(S_MatchingRuleEntry queryFor) {
            return queryFor;
        }

        @Override
        public String describeQuery() {
            return "";
        }
    }

    static abstract class AccountsScope {
        ShadowKindType getKind() {
            return null;
        }

        String getIntent() {
            return null;
        }

        QName getObjectClassName() {
            return null;
        }

        abstract S_MatchingRuleEntry startQuery(Resource resource) throws SchemaException, ConfigurationException;
    }

    static class ObjectTypeScope extends AccountsScope {
        @NotNull private final ResourceObjectTypeIdentification typeIdentification;

        ObjectTypeScope(@NotNull ResourceObjectTypeIdentification typeIdentification) {
            this.typeIdentification = typeIdentification;
        }

        @Override
        ShadowKindType getKind() {
            return typeIdentification.getKind();
        }

        @Override
        String getIntent() {
            return typeIdentification.getIntent();
        }

        @Override
        S_MatchingRuleEntry startQuery(Resource resource) throws SchemaException, ConfigurationException {
            return resource.queryFor(typeIdentification);
        }
    }

    static class ObjectClassScope extends AccountsScope {
        @NotNull private final QName objectClassName;

        ObjectClassScope(@NotNull QName objectClassName) {
            this.objectClassName = objectClassName;
        }

        @Override
        @NotNull QName getObjectClassName() {
            return objectClassName;
        }

        @Override
        S_MatchingRuleEntry startQuery(Resource resource) throws SchemaException, ConfigurationException {
            return resource.queryFor(objectClassName);
        }
    }

    @SuppressWarnings("unused")
    public static final class SynchronizationRequestBuilder {
        @NotNull private final AbstractModelIntegrationTest test;
        private String resourceOid;
        private AccountsScope accountsScope = new ObjectTypeScope(ResourceObjectTypeIdentification.defaultAccount());
        private QName namingAttribute;
        private String nameValue;
        private boolean processingAllAccounts;
        private SynchronizationStyle synchronizationStyle;
        private long timeout = DEFAULT_SHORT_TASK_WAIT_TIMEOUT;
        private boolean assertSuccess = true;
        private TracingProfileType tracingProfile;
        private Task task;
        private TaskExecutionMode taskExecutionMode;

        public SynchronizationRequestBuilder(@NotNull AbstractModelIntegrationTest test) {
            this.test = test;
        }

        public SynchronizationRequestBuilder withResourceOid(String resourceOid) {
            this.resourceOid = resourceOid;
            return this;
        }

        public SynchronizationRequestBuilder withTypeIdentification(
                @NotNull ResourceObjectTypeIdentification typeIdentification) {
            this.accountsScope = new ObjectTypeScope(typeIdentification);
            return this;
        }

        public SynchronizationRequestBuilder withWholeObjectClass(@NotNull QName objectClassName) {
            this.accountsScope = new ObjectClassScope(objectClassName);
            return this;
        }

        public SynchronizationRequestBuilder withNamingAttribute(String localName) {
            return withNamingAttribute(new QName(MidPointConstants.NS_RI, localName));
        }

        public SynchronizationRequestBuilder withNamingAttribute(QName namingAttribute) {
            this.namingAttribute = namingAttribute;
            return this;
        }

        public SynchronizationRequestBuilder withNameValue(String nameValue) {
            this.nameValue = nameValue;
            return this;
        }

        public SynchronizationRequestBuilder withProcessingAllAccounts() {
            this.processingAllAccounts = true;
            return this;
        }

        public SynchronizationRequestBuilder withUsingReconciliation() {
            return withSynchronizationStyle(RECONCILIATION);
        }

        @SuppressWarnings("SameParameterValue")
        SynchronizationRequestBuilder withSynchronizationStyle(SynchronizationStyle value) {
            this.synchronizationStyle = value;
            return this;
        }

        public SynchronizationRequestBuilder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public SynchronizationRequestBuilder withNotAssertingSuccess() {
            return withAssertSuccess(false);
        }

        /** Note: this is the default */
        public SynchronizationRequestBuilder withAssertingSuccess() {
            return withAssertSuccess(true);
        }

        @SuppressWarnings("WeakerAccess")
        public SynchronizationRequestBuilder withAssertSuccess(boolean assertSuccess) {
            this.assertSuccess = assertSuccess;
            return this;
        }

        public SynchronizationRequestBuilder withTask(Task task) {
            this.task = task;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public SynchronizationRequestBuilder withTracingProfile(TracingProfileType tracingProfile) {
            this.tracingProfile = tracingProfile;
            return this;
        }

        public SynchronizationRequestBuilder withTracing() {
            return withTracingProfile(
                    test.createModelLoggingTracingProfile());
        }

        public SynchronizationRequestBuilder simulatedDevelopment() {
            return withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT);
        }

        public SynchronizationRequestBuilder simulatedProduction() {
            return withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION);
        }

        public SynchronizationRequestBuilder withTaskExecutionMode(TaskExecutionMode taskExecutionMode) {
            this.taskExecutionMode = taskExecutionMode;
            return this;
        }

        public SynchronizationRequest build() {
            return new SynchronizationRequest(this);
        }

        public String execute(OperationResult result) throws CommonException, IOException {
            return build().execute(result);
        }

        public void executeOnForeground(OperationResult result) throws CommonException, IOException {
            build().executeOnForeground(result);
        }

        public TestSimulationResult executeOnForegroundSimulated(
                SimulationDefinitionType simulationDefinition, Task task, OperationResult result) throws CommonException {
            return build().executeOnForegroundSimulated(simulationDefinition, task, result);
        }

        AccountsSpecification getAccountsSpecification() {
            if (nameValue != null) {
                return new SingleAccountSpecification(
                        Objects.requireNonNullElse(namingAttribute, ICFS_NAME),
                        nameValue);
            } else if (processingAllAccounts) {
                return new AllAccountsSpecification();
            } else {
                throw new IllegalStateException("Either 'allAccounts' or a specific name value must be provided");
            }
        }
    }

    public enum SynchronizationStyle {
        IMPORT("import"), RECONCILIATION("reconciliation");

        private final String taskName;

        SynchronizationStyle(String taskName) {
            this.taskName = taskName;
        }
    }
}
