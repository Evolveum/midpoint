/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.update;

import com.evolveum.midpoint.provisioning.ucf.api.ConfigurationItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@SuppressWarnings({ "WeakerAccess", "unused" })
public class ConnectorConfiguration {

    private AsyncUpdateSourcesType sources;
    private ExpressionType transformExpression;
    private AsyncUpdateErrorHandlingActionType errorHandlingAction;
    private ProcessTracingConfigurationType processTracingConfiguration;

    @ConfigurationItem
    public AsyncUpdateSourcesType getSources() {
        return sources;
    }

    public void setSources(AsyncUpdateSourcesType sources) {
        this.sources = sources;
    }

    @ConfigurationItem
    public ExpressionType getTransformExpression() {
        return transformExpression;
    }

    public void setTransformExpression(ExpressionType transformExpression) {
        this.transformExpression = transformExpression;
    }

    @ConfigurationItem
    public AsyncUpdateErrorHandlingActionType getErrorHandlingAction() {
        return errorHandlingAction;
    }

    public void setErrorHandlingAction(AsyncUpdateErrorHandlingActionType errorHandlingAction) {
        this.errorHandlingAction = errorHandlingAction;
    }

    @ConfigurationItem
    public ProcessTracingConfigurationType getProcessTracingConfiguration() {
        return processTracingConfiguration;
    }

    public void setProcessTracingConfiguration(
            ProcessTracingConfigurationType processTracingConfiguration) {
        this.processTracingConfiguration = processTracingConfiguration;
    }

    public void validate() {
        if (getAllSources().isEmpty()) {
            throw new IllegalStateException("No asynchronous update sources were configured");
        }
    }

    @NotNull
    List<AsyncUpdateSourceType> getAllSources() {
        List<AsyncUpdateSourceType> allSources = new ArrayList<>();
        if (sources != null) {
            allSources.addAll(sources.getJms());
            allSources.addAll(sources.getAmqp091());
            allSources.addAll(sources.getOther());
        }
        return allSources;
    }

    boolean hasSourcesChanged(ConnectorConfiguration other) {
        // we can consider weaker comparison here in the future
        return other == null || !Objects.equals(other.sources, sources);
    }
}
