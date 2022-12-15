/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.util;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.test.AbstractIntegrationTest.DEFAULT_SHORT_TASK_WAIT_TIMEOUT;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.test.SimulationResult;
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
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
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
 * Causes a single or multiple accounts be imported:
 *
 * - typically on background, using one-time task created for this,
 * - or alternatively on foreground, using the appropriate model method (for single account only).
 *
 * Why a special class? To make clients' life easier and avoid many method variants.
 * (Regarding what parameters it needs to specify.)
 *
 * Limitations:
 *
 * - only crude support for tracing on foreground (yet).
 */
@Experimental
public class ImportAccountsRequest {

    @NotNull private final AbstractModelIntegrationTest test;
    @NotNull private final String resourceOid;
    @NotNull private final ResourceObjectTypeIdentification typeIdentification;
    @NotNull private final AccountsSpecification accountsSpecification;
    private final long timeout;
    private final boolean assertSuccess;
    private final Task task;
    private final TracingProfileType tracingProfile;
    @NotNull private final TaskExecutionMode taskExecutionMode;

    private ImportAccountsRequest(
            @NotNull ImportAccountsRequest.ImportAccountsRequestBuilder builder) {
        this.test = builder.test;
        this.resourceOid = Objects.requireNonNull(builder.resourceOid, "No resource OID");
        this.typeIdentification = Objects.requireNonNull(builder.typeIdentification, "No type");
        this.accountsSpecification = builder.getAccountsSpecification();
        this.timeout = builder.timeout;
        this.assertSuccess = builder.assertSuccess;
        this.task = Objects.requireNonNullElseGet(builder.task, test::getTestTask);
        this.tracingProfile = builder.tracingProfile;
        this.taskExecutionMode = Objects.requireNonNullElse(builder.taskExecutionMode, TaskExecutionMode.PRODUCTION);
    }

    public String execute(OperationResult result) throws CommonException {
        ObjectQuery query = createResourceObjectQuery(result);
        TaskType importTask = new TaskType()
                .name("import")
                .executionState(TaskExecutionStateType.RUNNABLE)
                .activity(new ActivityDefinitionType()
                        .work(new WorkDefinitionsType()
                                ._import(new ImportWorkDefinitionType()
                                        .resourceObjects(new ResourceObjectSetType()
                                                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE)
                                                .kind(typeIdentification.getKind())
                                                .intent(typeIdentification.getIntent())
                                                .query(PrismContext.get().getQueryConverter().createQueryType(query))
                                                .queryApplication(ResourceObjectSetQueryApplicationModeType.REPLACE))))
                        .executionMode(
                                getBackgroundTaskExecutionMode())
                        .execution(new ActivityExecutionDefinitionType()
                                .productionConfiguration(
                                        taskExecutionMode.isProductionConfiguration())
                                .createSimulationResult(
                                        test.isNativeRepository() && !taskExecutionMode.isPersistent())));
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
        if (taskExecutionMode.isPersistent()) {
            return ExecutionModeType.FULL;
        } else {
            return ExecutionModeType.PREVIEW;
        }
    }

    private ObjectQuery createResourceObjectQuery(OperationResult result)
            throws CommonException {
        return accountsSpecification.updateQuery(
                        getResource(result).queryFor(typeIdentification))
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

    public void executeOnForeground(OperationResult result) throws CommonException {
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

    public SimulationResult executeOnForegroundSimulated(
            SimulationResultType simulationConfiguration, Task task, OperationResult result) throws CommonException {
        stateCheck(!taskExecutionMode.isPersistent(), "No simulation? %s", taskExecutionMode);
        return test.executeInSimulationMode(
                taskExecutionMode,
                simulationConfiguration,
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

    @SuppressWarnings("unused")
    public static final class ImportAccountsRequestBuilder {
        @NotNull private final AbstractModelIntegrationTest test;
        private String resourceOid;
        private ResourceObjectTypeIdentification typeIdentification = ResourceObjectTypeIdentification.defaultAccount();
        private QName namingAttribute;
        private String nameValue;
        private boolean importingAllAccounts;
        private long timeout = DEFAULT_SHORT_TASK_WAIT_TIMEOUT;
        private boolean assertSuccess = true;
        private TracingProfileType tracingProfile;
        private Task task;
        private TaskExecutionMode taskExecutionMode;

        public ImportAccountsRequestBuilder(@NotNull AbstractModelIntegrationTest test) {
            this.test = test;
        }

        public ImportAccountsRequestBuilder withResourceOid(String resourceOid) {
            this.resourceOid = resourceOid;
            return this;
        }

        public ImportAccountsRequestBuilder withTypeIdentification(ResourceObjectTypeIdentification typeIdentification) {
            this.typeIdentification = typeIdentification;
            return this;
        }

        public ImportAccountsRequestBuilder withNamingAttribute(String localName) {
            return withNamingAttribute(new QName(MidPointConstants.NS_RI, localName));
        }

        public ImportAccountsRequestBuilder withNamingAttribute(QName namingAttribute) {
            this.namingAttribute = namingAttribute;
            return this;
        }

        public ImportAccountsRequestBuilder withNameValue(String nameValue) {
            this.nameValue = nameValue;
            return this;
        }

        public ImportAccountsRequestBuilder withImportingAllAccounts() {
            this.importingAllAccounts = true;
            return this;
        }

        public ImportAccountsRequestBuilder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ImportAccountsRequestBuilder withNotAssertingSuccess() {
            return withAssertSuccess(false);
        }

        public ImportAccountsRequestBuilder withAssertingSuccess() {
            return withAssertSuccess(true);
        }

        public ImportAccountsRequestBuilder withAssertSuccess(boolean assertSuccess) {
            this.assertSuccess = assertSuccess;
            return this;
        }

        public ImportAccountsRequestBuilder withTask(Task task) {
            this.task = task;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public ImportAccountsRequestBuilder withTracingProfile(TracingProfileType tracingProfile) {
            this.tracingProfile = tracingProfile;
            return this;
        }

        public ImportAccountsRequestBuilder withTracing() {
            return withTracingProfile(
                    test.createModelLoggingTracingProfile());
        }

        public ImportAccountsRequestBuilder simulatedDevelopment() {
            return withTaskExecutionMode(TaskExecutionMode.SIMULATED_DEVELOPMENT);
        }

        public ImportAccountsRequestBuilder simulatedProduction() {
            return withTaskExecutionMode(TaskExecutionMode.SIMULATED_PRODUCTION);
        }

        public ImportAccountsRequestBuilder withTaskExecutionMode(TaskExecutionMode taskExecutionMode) {
            this.taskExecutionMode = taskExecutionMode;
            return this;
        }

        public ImportAccountsRequest build() {
            return new ImportAccountsRequest(this);
        }

        public String execute(OperationResult result) throws CommonException {
            return build().execute(result);
        }

        public void executeOnForeground(OperationResult result) throws CommonException {
            build().executeOnForeground(result);
        }

        public SimulationResult executeOnForegroundSimulated(
                SimulationResultType simulationConfiguration, Task task, OperationResult result) throws CommonException {
            return build().executeOnForegroundSimulated(simulationConfiguration, task, result);
        }

        AccountsSpecification getAccountsSpecification() {
            if (nameValue != null) {
                return new SingleAccountSpecification(
                        Objects.requireNonNullElse(namingAttribute, ICFS_NAME),
                        nameValue);
            } else if (importingAllAccounts) {
                return new AllAccountsSpecification();
            } else {
                throw new IllegalStateException("Either 'allAccounts' or a specific name value must be provided");
            }
        }
    }
}
