/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A catalogue of all bulk actions.
 */
public enum BulkAction {

    ASSIGN("assign", SchemaConstantsGenerated.SC_ASSIGN, AssignActionExpressionType.class),
    EXECUTE_SCRIPT("execute-script", SchemaConstantsGenerated.SC_EXECUTE, ExecuteScriptActionExpressionType.class),
    EVALUATE_EXPRESSION("evaluate-expression", SchemaConstantsGenerated.SC_EVALUATE_EXPRESSION, EvaluateExpressionActionExpressionType.class),
    LOG("log", SchemaConstantsGenerated.SC_LOG, LogActionExpressionType.class),
    MODIFY("modify", SchemaConstantsGenerated.SC_MODIFY, ModifyActionExpressionType.class),
    RECOMPUTE("recompute", SchemaConstantsGenerated.SC_RECOMPUTE, RecomputeActionExpressionType.class),
    ADD("add", SchemaConstantsGenerated.SC_ADD, AddActionExpressionType.class),
    APPLY_DEFINITION("apply-definition", SchemaConstantsGenerated.SC_APPLY_DEFINITION, ApplyDefinitionActionExpressionType.class),
    DELETE("delete", SchemaConstantsGenerated.SC_DELETE, DeleteActionExpressionType.class),
    UNASSIGN("unassign", SchemaConstantsGenerated.SC_UNASSIGN, UnassignActionExpressionType.class),
    PURGE_SCHEMA("purge-schema", SchemaConstantsGenerated.SC_PURGE_SCHEMA, PurgeSchemaActionExpressionType.class),
    ENABLE("enable", SchemaConstantsGenerated.SC_ENABLE, EnableActionExpressionType.class),
    DISABLE("disable", SchemaConstantsGenerated.SC_DISABLE, DisableActionExpressionType.class),
    RESUME("resume", SchemaConstantsGenerated.SC_RESUME_TASK, ResumeTaskActionExpressionType.class),
    GENERATE_VALUE("generate-value", SchemaConstantsGenerated.SC_GENERATE_VALUE, GenerateValueActionExpressionType.class),
    DISCOVER_CONNECTORS("discover-connectors"),
    RESOLVE("resolve", SchemaConstantsGenerated.SC_RESOLVE_REFERENCE, ResolveReferenceActionExpressionType.class),
    NOTIFY("notify", SchemaConstantsGenerated.SC_NOTIFY, NotifyActionExpressionType.class),
    REENCRYPT("reencrypt"),
    VALIDATE("validate"),
    TEST_RESOURCE("test-resource", SchemaConstantsGenerated.SC_TEST_RESOURCE, TestResourceActionExpressionType.class),
    SEARCH("search", SchemaConstantsGenerated.SC_SEARCH, SearchExpressionType.class);

    @NotNull private final String name;
    @Nullable private final ItemName beanName;
    @Nullable private final Class<? extends AbstractActionExpressionType> beanClass;

    BulkAction(@NotNull String name, @NotNull ItemName beanName, @NotNull Class<? extends AbstractActionExpressionType> beanClass) {
        this.name = name;
        this.beanName = beanName;
        this.beanClass = beanClass;
    }

    BulkAction(@NotNull String name) {
        this.name = name;
        this.beanName = null;
        this.beanClass = null;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getBeanLocalName() {
        return beanName != null ? beanName.getLocalPart() : null;
    }

    public @Nullable Class<? extends AbstractActionExpressionType> getBeanClass() {
        return beanClass;
    }

    public @NotNull String getAuthorizationUrl() {
        return AuthorizationConstants.NS_AUTHORIZATION_BULK + "#" + name;
    }
}
