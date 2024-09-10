/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.util;

import static com.evolveum.midpoint.model.test.util.SynchronizationRequest.*;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestSpringBeans;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingProfileType;

/**
 * Easily finds a shadow on a resource (with or without fetching).
 *
 * @see SynchronizationRequest
 */
@Experimental
public class ShadowFindRequest {

    @NotNull private final AbstractModelIntegrationTest test;
    @NotNull private final String resourceOid;
    private ResourceType resource;
    @NotNull private final AccountsScope accountsScope;
    @NotNull private final AccountsSpecification accountsSpecification;
    private final boolean noFetch;

    private ShadowFindRequest(
            @NotNull ShadowFindRequest.ShadowFindRequestBuilder builder) {
        this.test = builder.test;
        this.resource = builder.resource;
        if (resource != null) {
            this.resourceOid = Objects.requireNonNull(resource.getOid(), "No resource OID");
        } else {
            this.resourceOid = Objects.requireNonNull(builder.resourceOid, "No resource OID");
        }
        this.accountsScope = Objects.requireNonNull(builder.accountsScope, "No accounts scope");
        this.accountsSpecification = builder.getAccountsSpecification();
        this.noFetch = builder.noFetch;
    }

    public @NotNull AbstractShadow findRequired(Task task, OperationResult result) throws CommonException, IOException {
        ObjectQuery query = createResourceObjectQuery(task, result);
        var shadows = test.getProvisioningService().searchShadows(query, createGetOperationOptions(), task, result);
        if (shadows.size() > 1) {
            throw new AssertionError("Expected to find exactly one shadow, found " + shadows.size() + ": " + shadows);
        } else if (shadows.isEmpty()) {
            throw new AssertionError("Expected to find exactly one shadow, found none");
        } else {
            return shadows.get(0);
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> createGetOperationOptions() {
        return GetOperationOptionsBuilder.create()
                .noFetch(noFetch)
                .build();
    }

    private ObjectQuery createResourceObjectQuery(Task task, OperationResult result)
            throws CommonException {
        return accountsSpecification.updateQuery(
                        accountsScope.startQuery(getResource(task, result)))
                .build();
    }

    private Resource getResource(Task task, OperationResult result) throws CommonException {
        if (resource != null) {
            return Resource.of(resource);
        } else {
            return Resource.of(
                    getProvisioningService()
                            .getObject(ResourceType.class, resourceOid, null, task, result)
                            .asObjectable());
        }
    }

    private static ProvisioningService getProvisioningService() {
        return TestSpringBeans.getBean(ProvisioningService.class);
    }

    @SuppressWarnings("unused")
    public static final class ShadowFindRequestBuilder {
        @NotNull private final AbstractModelIntegrationTest test;
        private String resourceOid;
        private ResourceType resource;
        private AccountsScope accountsScope = new ObjectTypeScope(ResourceObjectTypeIdentification.defaultAccount());
        private QName namingAttribute;
        private String nameValue;
        private boolean processingAllAccounts;
        private TracingProfileType tracingProfile;
        private boolean noFetch;

        public ShadowFindRequestBuilder(@NotNull AbstractModelIntegrationTest test) {
            this.test = test;
        }

        public ShadowFindRequestBuilder withResourceOid(String resourceOid) {
            this.resourceOid = resourceOid;
            return this;
        }

        public ShadowFindRequestBuilder withResource(ResourceType resource) {
            this.resource = resource;
            return this;
        }

        public ShadowFindRequestBuilder withDefaultAccountType() {
            return withTypeIdentification(
                    ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT));
        }

        public ShadowFindRequestBuilder withTypeIdentification(
                @NotNull ResourceObjectTypeIdentification typeIdentification) {
            this.accountsScope = new ObjectTypeScope(typeIdentification);
            return this;
        }

        public ShadowFindRequestBuilder withWholeObjectClass(@NotNull QName objectClassName) {
            this.accountsScope = new ObjectClassScope(objectClassName);
            return this;
        }

        public ShadowFindRequestBuilder withNamingAttribute(String localName) {
            return withNamingAttribute(new QName(MidPointConstants.NS_RI, localName));
        }

        public ShadowFindRequestBuilder withNamingAttribute(QName namingAttribute) {
            this.namingAttribute = namingAttribute;
            return this;
        }

        public ShadowFindRequestBuilder withNameValue(String nameValue) {
            this.nameValue = nameValue;
            return this;
        }

        public ShadowFindRequestBuilder withProcessingAllAccounts() {
            this.processingAllAccounts = true;
            return this;
        }

        public ShadowFindRequestBuilder withNoFetch() {
            this.noFetch = true;
            return this;
        }

        public @NotNull AbstractShadow findRequired(Task task, OperationResult result) throws CommonException, IOException {
            return build().findRequired(task, result);
        }

        public ShadowFindRequest build() {
            return new ShadowFindRequest(this);
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
}
