/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.axiom.concepts.Lazy;
import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.PRIMARY_CORRELATION_ITEM_TARGET;

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

    /** System configuration, used to look for correlator configurations. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    // TODO
    @NotNull private final Lazy<Map<String, ItemRoute>> targetPlacesLazy = Lazy.from(this::computeTargetPlaces);

    // TODO
    @NotNull private final Lazy<Map<String, CorrelationItemDefinitionType>> itemDefinitionsLazy =
            Lazy.from(this::createItemDefinitionsMap);

    public CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @NotNull AbstractCorrelatorType originalConfigurationBean,
            @Nullable CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        //noinspection unchecked
        this.configurationBean = (C) configuration.getConfigurationBean();
        this.configuration = configuration;
        this.originalConfigurationBean = originalConfigurationBean;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.identityManagementConfiguration = identityManagementConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    public @NotNull C getConfigurationBean() {
        return configurationBean;
    }

    public @NotNull CorrelatorConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the path to the "source place" in the object being correlated.
     */
    public @NotNull ItemRoute getSourcePlaceRoute() {
        CorrelationPlacesDefinitionType placesDefinition = getPlacesDefinition();
        CorrelationItemSourceDefinitionType source = placesDefinition != null ? placesDefinition.getSource() : null;
        if (source == null) {
            return ItemRoute.EMPTY;
        } else {
            return ItemRoute.fromBeans(
                    source.getPath(),
                    source.getRoute());
        }
    }

    public @NotNull Map<String, ItemRoute> getTargetPlaces() {
        return targetPlacesLazy.get();
    }

    public @NotNull ItemRoute getTargetPlaceRoute(@Nullable String qualifier) {
        return Objects.requireNonNullElse(
                getTargetPlaces().get(qualifier),
                ItemRoute.EMPTY);
    }

    private @NotNull Map<String, ItemRoute> computeTargetPlaces() {
        CorrelationPlacesDefinitionType placesDefinition = getPlacesDefinition();
        if (placesDefinition == null) {
            return Map.of();
        }
        Map<String, ItemRoute> map = new HashMap<>();
        for (CorrelationItemTargetDefinitionType targetBean : placesDefinition.getTarget()) {
            map.put(
                    Objects.requireNonNullElse(targetBean.getQualifier(), PRIMARY_CORRELATION_ITEM_TARGET),
                    ItemRoute.fromBeans(
                            targetBean.getPath(),
                            targetBean.getRoute()));
        }
        return map;
    }

    private @Nullable CorrelationPlacesDefinitionType getPlacesDefinition() {
        return configurationBean.getDefinitions() != null ?
                configurationBean.getDefinitions().getPlaces() : null;
    }

    /**
     * Returns the named item definition.
     */
    public @NotNull CorrelationItemDefinitionType getNamedItemDefinition(String ref) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                itemDefinitionsLazy.get().get(ref),
                () -> new ConfigurationException("No item named '" + ref + "' exists"));
    }

    /**
     * Returns all relevant named item definitions - from this context and all its parents.
     */
    public @NotNull Map<String, CorrelationItemDefinitionType> getItemDefinitionsMap() {
        return itemDefinitionsLazy.get();
    }

    private @NotNull Map<String, CorrelationItemDefinitionType> createItemDefinitionsMap() {
        Map<String, CorrelationItemDefinitionType> defMap = new HashMap<>();
        getLocalItemsDefinitionCollection().forEach(
                def -> {
                    String name = CorrelationItemDefinitionUtil.getName(def);
                    if (!defMap.containsKey(name)) {
                        defMap.put(name, def);
                    }
                }
        );
        return defMap;
    }

    private List<CorrelationItemDefinitionType> getLocalItemsDefinitionCollection() {
        CorrelatorDefinitionsType definitions = configurationBean.getDefinitions();
        return definitions != null && definitions.getItems() != null ?
                definitions.getItems().getItem() : List.of();
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
}
