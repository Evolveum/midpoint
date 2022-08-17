/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;

import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Overall context in which the correlator works.
 *
 * Differs from {@link CorrelationContext} in that the latter covers only a single correlation operation.
 * The former covers the whole life of a correlator, and operations other than correlation.
 */
public class CorrelatorContext<C extends AbstractCorrelatorType> implements DebugDumpable {

    private static final double DEFAULT_OWNER = 1.0;
    private static final double DEFAULT_CANDIDATE = 0.0;

    /** The final (combined) configuration bean for this correlator. */
    @NotNull private final C configurationBean;

    /** The configuration wrapping the final (combined) {@link #configurationBean}. */
    @NotNull private final CorrelatorConfiguration configuration;

    /** The original configuration bean. Used to resolve child configurations. */
    @NotNull private final AbstractCorrelatorType originalConfigurationBean;

    /** Complete correlation definition. Used to access things outside of specific correlator configuration. */
    @NotNull private final CorrelationDefinitionType correlationDefinitionBean;

    /** TODO */
    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;

    /** TODO */
    @NotNull private final IndexingConfiguration indexingConfiguration;

    /** System configuration, used to look for correlator configurations. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    public CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @NotNull AbstractCorrelatorType originalConfigurationBean,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @NotNull IndexingConfiguration indexingConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        //noinspection unchecked
        this.configurationBean = (C) configuration.getConfigurationBean();
        this.configuration = configuration;
        this.originalConfigurationBean = originalConfigurationBean;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.identityManagementConfiguration = identityManagementConfiguration;
        this.indexingConfiguration = indexingConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    public @NotNull C getConfigurationBean() {
        return configurationBean;
    }

    public @NotNull CorrelatorConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * TODO
     */
    public @NotNull Map<String, ItemCorrelationType> getItemDefinitionsMap() {
        return new HashMap<>(); // TODO extract from the object template
    }

    public @NotNull AbstractCorrelatorType getOriginalConfigurationBean() {
        return originalConfigurationBean;
    }

    public @NotNull CorrelationDefinitionType getCorrelationDefinitionBean() {
        return correlationDefinitionBean;
    }

    public @Nullable SystemConfigurationType getSystemConfiguration() {
        return systemConfiguration;
    }

    public @NotNull IdentityManagementConfiguration getIdentityManagementConfiguration() {
        return identityManagementConfiguration;
    }

    public @NotNull IndexingConfiguration getIndexingConfiguration() {
        return indexingConfiguration;
    }

    public double getOwnerThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = correlationDefinitionBean.getThresholds();
        Double owner = thresholds != null ? thresholds.getOwner() : null;
        return Objects.requireNonNullElse(owner, DEFAULT_OWNER);
    }

    public double getCandidateThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = correlationDefinitionBean.getThresholds();
        Double candidate = thresholds != null ? thresholds.getCandidate() : null;
        return Objects.requireNonNullElse(candidate, DEFAULT_CANDIDATE);
    }


    @Override
    public String debugDump(int indent) {
        // Temporary: this config bean is the core of the context; other things need not be so urgently dumped
        // (maybe they might be - in some shortened form).
        return configurationBean.debugDump(indent);
    }

    public Object dumpXmlLazily() {
        return DebugUtil.lazy(this::dumpXml);
    }

    private String dumpXml() {
        try {
            return PrismContext.get().xmlSerializer().serializeRealValue(
                    configurationBean,
                    new ItemName("a" + configurationBean.getClass().getSimpleName()));
        } catch (SchemaException e) {
            return e.getMessage();
        }
    }

    // TODO improve
    @Override
    public String toString() {
        return "CorrelatorContext{" +
                "configurationBean=" + configurationBean +
                '}';
    }
}
