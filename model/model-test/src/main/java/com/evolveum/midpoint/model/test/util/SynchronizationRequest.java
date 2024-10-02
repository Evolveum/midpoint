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
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;

import com.evolveum.midpoint.schema.util.GetOperationOptionsUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.test.TestSimulationResult;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.expression.ExpressionTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestSpringBeans;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

/**
 * Causes a single or multiple accounts be synchronized (imported or reconciled):
 *
 * - typically on background, using one-time task created for this,
 * - or alternatively on foreground, using the appropriate model method (for single account import only).
 *
 * Why a special class? To make clients' life easier and avoid many method variants.
 * (Regarding what parameters it needs to specify.)
 *
 * TODO remove the builder, it's unnecessary complication
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
    private final Collection<String> tracingAccounts;
    @NotNull private final TaskExecutionMode taskExecutionMode;
    private final ShadowClassificationModeType classificationMode;
    private final Consumer<TaskType> taskCustomizer; // only for background execution
    private final boolean noFetchWhenSynchronizing; // limited support for now

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
        this.tracingAccounts = builder.tracingAccounts;
        this.taskExecutionMode = Objects.requireNonNullElse(builder.taskExecutionMode, TaskExecutionMode.PRODUCTION);
        this.classificationMode = builder.classificationMode;
        this.taskCustomizer = builder.taskCustomizer;
        this.noFetchWhenSynchronizing = builder.noFetchWhenSynchronizing;
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
                .queryApplication(ResourceObjectSetQueryApplicationModeType.REPLACE)
                .searchOptions(GetOperationOptionsUtil.optionsToOptionsBeanNullable(createGetOperationOptions()));
        if (synchronizationStyle == IMPORT) {
            work = new WorkDefinitionsType()
                    ._import(new ImportWorkDefinitionType()
                            .resourceObjects(resourceObjectSet));
        } else if (synchronizationStyle == SHADOW_RECLASSIFICATION) {
            work = new WorkDefinitionsType()
                    .shadowReclassification(new ShadowReclassificationWorkDefinitionType()
                            .resourceObjects(resourceObjectSet));
        } else {
            work = new WorkDefinitionsType()
                    .reconciliation(new ReconciliationWorkDefinitionType()
                            .resourceObjects(resourceObjectSet));
        }
        var reporting = new ActivityReportingDefinitionType();
        ActivityDefinitionType activityDefinition = new ActivityDefinitionType()
                .work(work)
                .executionMode(
                        getBackgroundTaskExecutionMode())
                .execution(new ActivityExecutionModeDefinitionType()
                        .configurationToUse(
                                taskExecutionMode.toConfigurationSpecification()))
                .reporting(reporting);
        if (test.isNativeRepository() && !taskExecutionMode.isFullyPersistent()) {
            reporting.simulationResult(new ActivitySimulationResultDefinitionType());
        }
        if (tracingProfile != null) {
            ActivityTracingDefinitionType tracing = new ActivityTracingDefinitionType()
                    .tracingProfile(tracingProfile);
            if (tracingAccounts != null) {
                String script = String.format(
                        "[%s].contains(item?.name?.orig)",
                        tracingAccounts.stream()
                                .map(s -> "'" + s + "'")
                                .collect(Collectors.joining(",")));
                tracing.getBeforeItemCondition().add(new BeforeItemConditionType()
                        .expression(ExpressionTypeUtil.forGroovyCode(script)));
            }
            reporting.tracing(tracing);
        }
        TaskType syncTask = new TaskType()
                .name(synchronizationStyle.taskName)
                .executionState(TaskExecutionStateType.RUNNABLE)
                .activity(activityDefinition);
        if (taskCustomizer != null) {
            taskCustomizer.accept(syncTask);
        }
        String taskOid = test.addObjectSilently(syncTask, task, result);
        test.waitForTaskCloseOrSuspend(taskOid, timeout);

        if (assertSuccess) {
            test.assertTask(taskOid, "after")
                    .assertClosed()
                    .assertSuccess();
        }
        return taskOid;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createGetOperationOptions() {
        return GetOperationOptionsBuilder.create()
                .shadowClassificationMode(classificationMode)
                .noFetch(noFetchWhenSynchronizing)
                .build();
    }

    private @NotNull ExecutionModeType getBackgroundTaskExecutionMode() {
        if (taskExecutionMode.isFullyPersistent()) {
            return ExecutionModeType.FULL;
        } else if (taskExecutionMode.isPersistentAtShadowLevelButNotFully()) {
            return ExecutionModeType.PREVIEW;
        } else if (taskExecutionMode.isNothingPersistent()) {
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
                        createGetOperationOptions(),
                        task,
                        result);
        stateCheck(!shadows.isEmpty(), "No shadows: %s", accountsSpecification.describeQuery());
        TaskExecutionMode oldMode = task.setExecutionMode(taskExecutionMode);
        try {
            for (var shadow : shadows) {
                var shadowOid = shadow.getOid();
                if (tracingProfile != null) {
                    test.traced(
                            tracingProfile,
                            () -> executeImportOnForeground(result, shadowOid));
                } else {
                    executeImportOnForeground(result, shadowOid);
                }
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
            @Nullable SimulationDefinitionType simulationDefinition, Task task, OperationResult result) throws CommonException {
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
        private Collection<String> tracingAccounts;
        private Task task;
        private TaskExecutionMode taskExecutionMode;
        private ShadowClassificationModeType classificationMode;
        private Consumer<TaskType> taskCustomizer;
        private boolean noFetchWhenSynchronizing;

        public SynchronizationRequestBuilder(@NotNull AbstractModelIntegrationTest test) {
            this.test = test;
        }

        public SynchronizationRequestBuilder withResourceOid(String resourceOid) {
            this.resourceOid = resourceOid;
            return this;
        }

        public SynchronizationRequestBuilder withDefaultAccountType() {
            return withTypeIdentification(
                    ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT));
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

        public SynchronizationRequestBuilder withUsingShadowReclassification() {
            return withSynchronizationStyle(SHADOW_RECLASSIFICATION);
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

        public SynchronizationRequestBuilder withTracingAccounts(String... names) {
            if (tracingProfile == null) {
                withTracing();
            }
            this.tracingAccounts = List.of(names);
            return this;
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

        public SynchronizationRequestBuilder withClassificationMode(ShadowClassificationModeType classificationMode) {
            this.classificationMode = classificationMode;
            return this;
        }

        /** Only for background execution. */
        public SynchronizationRequestBuilder withTaskCustomizer(Consumer<TaskType> taskCustomizer) {
            this.taskCustomizer = taskCustomizer;
            return this;
        }

        public SynchronizationRequestBuilder withNoFetchWhenSynchronizing() {
            this.noFetchWhenSynchronizing = true;
            return this;
        }

        public SynchronizationRequest build() {
            return new SynchronizationRequest(this);
        }

        /** Executes on background (in a task). */
        public String execute(OperationResult result) throws CommonException, IOException {
            return build().execute(result);
        }

        /** Beware, ImportFromResourceLauncher that is used re-reads the shadow from the resource. */
        public void executeOnForeground(OperationResult result) throws CommonException, IOException {
            build().executeOnForeground(result);
        }

        public TestSimulationResult executeOnForegroundSimulated(
                @Nullable SimulationDefinitionType simulationDefinition, Task task, OperationResult result)
                throws CommonException {
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
        IMPORT("import"), RECONCILIATION("reconciliation"), SHADOW_RECLASSIFICATION("shadowReclassification");

        private final String taskName;

        SynchronizationStyle(String taskName) {
            this.taskName = taskName;
        }
    }
}
