/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.TemplateCorrelationConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationConfidenceThresholdsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

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
    @NotNull private final TemplateCorrelationConfiguration templateCorrelationConfiguration;

    /** System configuration, used to look for correlator configurations. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    public CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @NotNull AbstractCorrelatorType originalConfigurationBean,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull TemplateCorrelationConfiguration templateCorrelationConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        //noinspection unchecked
        this.configurationBean = (C) configuration.getConfigurationBean();
        this.configuration = configuration;
        this.originalConfigurationBean = originalConfigurationBean;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.templateCorrelationConfiguration = templateCorrelationConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    public @NotNull C getConfigurationBean() {
        return configurationBean;
    }

    public @NotNull CorrelatorConfiguration getConfiguration() {
        return configuration;
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

    public @NotNull TemplateCorrelationConfiguration getTemplateCorrelationConfiguration() {
        return templateCorrelationConfiguration;
    }

    public double getDefiniteThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = correlationDefinitionBean.getThresholds();
        Double definite = thresholds != null ? thresholds.getDefinite() : null;
        return Objects.requireNonNullElse(definite, DEFAULT_OWNER);
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
