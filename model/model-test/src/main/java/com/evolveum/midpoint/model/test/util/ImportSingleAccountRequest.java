/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.util;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.Resource;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.test.TestSpringBeans;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Objects;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.test.AbstractIntegrationTest.DEFAULT_SHORT_TASK_WAIT_TIMEOUT;

/**
 * Causes a single account be imported on background - using one-time task created for this.
 *
 * Why a special class? To make clients' life easier and avoid many method variants.
 * (Regarding what parameters it needs to specify.)
 */
@Experimental
public class ImportSingleAccountRequest {

    @NotNull private final AbstractModelIntegrationTest test;
    @NotNull private final String resourceOid;
    @NotNull private final ResourceObjectTypeIdentification typeIdentification;
    @NotNull private final QName namingAttribute;
    @NotNull private final String nameValue;
    private final long timeout;
    private final boolean assertSuccess;
    private final Task task;
    private final TracingProfileType tracingProfile;

    private ImportSingleAccountRequest(
            @NotNull ImportSingleAccountRequestBuilder builder) {
        this.test = builder.test;
        this.resourceOid = Objects.requireNonNull(builder.resourceOid, "No resource OID");
        this.typeIdentification = Objects.requireNonNull(builder.typeIdentification, "No type");
        this.namingAttribute = Objects.requireNonNull(builder.namingAttribute, "No naming attribute");
        this.nameValue = Objects.requireNonNull(builder.nameValue, "No 'name' attribute value");
        this.timeout = builder.timeout;
        this.assertSuccess = builder.assertSuccess;
        this.task = Objects.requireNonNullElseGet(builder.task, test::getTestTask);
        this.tracingProfile = builder.tracingProfile;
    }

    public String execute(OperationResult result) throws CommonException, PreconditionViolationException {
        ResourceAttributeDefinition<?> namingAttrDef = Resource.of(
                        TestSpringBeans.getBean(ProvisioningService.class)
                                .getObject(ResourceType.class, resourceOid, null, task, result)
                                .asObjectable())
                .getCompleteSchemaRequired()
                .findObjectDefinitionRequired(typeIdentification)
                .findAttributeDefinitionRequired(namingAttribute);
        PrismContext prismContext = PrismContext.get();
        ObjectQuery query =
                prismContext.queryFor(ShadowType.class)
                        .item(
                                ItemPath.create(ShadowType.F_ATTRIBUTES, namingAttribute),
                                namingAttrDef)
                        .eq(nameValue)
                        .build();
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
                                                .query(prismContext.getQueryConverter().createQueryType(query))
                                                .queryApplication(ResourceObjectSetQueryApplicationModeType.APPEND)))));
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

    @SuppressWarnings("unused")
    public static final class ImportSingleAccountRequestBuilder {
        @NotNull private final AbstractModelIntegrationTest test;
        private String resourceOid;
        private ResourceObjectTypeIdentification typeIdentification = ResourceObjectTypeIdentification.defaultAccount();
        private QName namingAttribute = ICFS_NAME;
        private String nameValue;
        private long timeout = DEFAULT_SHORT_TASK_WAIT_TIMEOUT;
        private boolean assertSuccess = true;
        private TracingProfileType tracingProfile;
        private Task task;

        public ImportSingleAccountRequestBuilder(@NotNull AbstractModelIntegrationTest test) {
            this.test = test;
        }

        public ImportSingleAccountRequestBuilder withResourceOid(String resourceOid) {
            this.resourceOid = resourceOid;
            return this;
        }

        public ImportSingleAccountRequestBuilder withTypeIdentification(ResourceObjectTypeIdentification typeIdentification) {
            this.typeIdentification = typeIdentification;
            return this;
        }

        public ImportSingleAccountRequestBuilder withNamingAttribute(QName namingAttribute) {
            this.namingAttribute = namingAttribute;
            return this;
        }

        public ImportSingleAccountRequestBuilder withNameValue(String nameValue) {
            this.nameValue = nameValue;
            return this;
        }

        public ImportSingleAccountRequestBuilder withTimeout(long timeout) {
            this.timeout = timeout;
            return this;
        }

        public ImportSingleAccountRequestBuilder withAssertSuccess(boolean assertSuccess) {
            this.assertSuccess = assertSuccess;
            return this;
        }

        public ImportSingleAccountRequestBuilder withTask(Task task) {
            this.task = task;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public ImportSingleAccountRequestBuilder withTracingProfile(TracingProfileType tracingProfile) {
            this.tracingProfile = tracingProfile;
            return this;
        }

        public ImportSingleAccountRequestBuilder traced() {
            return withTracingProfile(
                    test.createModelLoggingTracingProfile());
        }

        public ImportSingleAccountRequest build() {
            return new ImportSingleAccountRequest(this);
        }

        public String execute(OperationResult result) throws CommonException, PreconditionViolationException {
            return build().execute(result);
        }
    }
}
