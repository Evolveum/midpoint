/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;

import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Overall context in which the correlator works.
 *
 * Differs from {@link CorrelationContext} in that the latter covers only a single correlation operation.
 * The former covers the whole life of a correlator, and operations other than correlation.
 */
public class CorrelatorContext<C extends AbstractCorrelatorType> implements DebugDumpable {

    /** The final (combined) configuration bean for this correlator. */
    @NotNull private final C configurationBean;

    /** The configuration wrapping the final (combined) {@link #configurationBean}. */
    @NotNull private final CorrelatorConfiguration configuration;

    /** The original configuration bean. Used to resolve child configurations. */
    @NotNull private final AbstractCorrelatorType originalConfigurationBean;

    /** Complete correlation definition. Used to access things outside of specific correlator configuration. */
    @Nullable private final CorrelationDefinitionType correlationDefinitionBean;

    /** TODO */
    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;

    /** TODO */
    @NotNull private final IndexingConfiguration indexingConfiguration;

    /** System configuration, used to look for correlator configurations. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    public CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @NotNull AbstractCorrelatorType originalConfigurationBean,
            @Nullable CorrelationDefinitionType correlationDefinitionBean,
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

    public @Nullable CorrelationDefinitionType getCorrelationDefinitionBean() {
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
            return PrismContext.get().xmlSerializer().serializeRealValue(configurationBean);
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
