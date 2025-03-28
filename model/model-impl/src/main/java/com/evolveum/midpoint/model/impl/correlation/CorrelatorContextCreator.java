/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil.identify;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.CorrelatorDiscriminator;
import com.evolveum.midpoint.model.api.correlation.TemplateCorrelationConfiguration;

import com.evolveum.midpoint.schema.merger.correlator.CorrelatorMergeOperation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.schema.util.ObjectTemplateTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link CorrelatorContext} instances.
 */
public class CorrelatorContextCreator {

    /** The original configuration we start with. */
    @NotNull private final CorrelatorConfiguration originalConfiguration;

    /**
     * The configuration bean we start with. It should be embedded in its context, i.e. its prism parents should be reachable.
     *
     * This is _not_ the original "correlators" bean, though. It is the selected individual correlator.
     * See {@link #getConfiguration(CompositeCorrelatorType)}.
     */
    @NotNull private final AbstractCorrelatorType originalConfigurationBean;

    /** The correlation definition. We just pass this to the context. */
    @NotNull private final CorrelationDefinitionType correlationDefinitionBean;

    /** TODO */
    @NotNull private final TemplateCorrelationConfiguration templateCorrelationConfiguration;

    /** The system configuration. We use it to look for global correlator definitions. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    private CorrelatorContextCreator(
            @NotNull CorrelatorConfiguration originalConfiguration,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull TemplateCorrelationConfiguration templateCorrelationConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        this.originalConfiguration = originalConfiguration;
        this.originalConfigurationBean = originalConfiguration.getConfigurationBean();
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.templateCorrelationConfiguration = templateCorrelationConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    //TODO change signature to composite correlator + thresholds
    public static CorrelatorContext<?> createRootContext(
            @Nullable CorrelationDefinitionType correlationDefinitionBean,
            @NotNull CorrelatorDiscriminator correlatorDiscriminator,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration)
            throws ConfigurationException, SchemaException {
        CompositeCorrelatorType correlators;
        CompositeCorrelatorType specificCorrelators = correlationDefinitionBean == null ? null : correlationDefinitionBean.getCorrelators();
        if (specificCorrelators != null) {
            correlators = specificCorrelators;
        } else {
            correlators = ObjectTemplateTypeUtil.getCorrelators(objectTemplate, correlatorDiscriminator);
        }
        return new CorrelatorContextCreator(
                getConfiguration(correlators),
                Objects.requireNonNullElseGet(correlationDefinitionBean, () -> new CorrelationDefinitionType()),
                TemplateCorrelationConfigurationImpl.of(objectTemplate),
                systemConfiguration)
                .create();
    }

    public static CorrelatorContext<?> createChildContext(
            @NotNull CorrelatorConfiguration childConfiguration,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull TemplateCorrelationConfiguration templateCorrelationConfiguration,
            @Nullable SystemConfigurationType systemConfiguration)
            throws ConfigurationException, SchemaException {
        return new CorrelatorContextCreator(
                childConfiguration,
                correlationDefinitionBean,
                templateCorrelationConfiguration,
                systemConfiguration)
                .create();
    }

    private CorrelatorContext<?> create() throws ConfigurationException, SchemaException {
        if (originalConfiguration.isUntyped()) {
            // We don't support merging for untyped configurations (yet).
            return new CorrelatorContext<>(
                    originalConfiguration,
                    originalConfigurationBean,
                    correlationDefinitionBean,
                    templateCorrelationConfiguration,
                    systemConfiguration);
        }

        AbstractCorrelatorType mergedConfig = resolveSuperReferences(originalConfigurationBean, new HashSet<>());

        return new CorrelatorContext<>(
                new CorrelatorConfiguration.TypedCorrelationConfiguration(mergedConfig),
                originalConfigurationBean,
                correlationDefinitionBean,
                templateCorrelationConfiguration,
                systemConfiguration);
    }

    private AbstractCorrelatorType resolveSuperReferences(AbstractCorrelatorType current, Set<String> seen)
            throws ConfigurationException, SchemaException {
        SuperCorrelatorReferenceType superDefinition = current.getSuper();
        if (superDefinition == null) {
            return current;
        }
        String ref = MiscUtil.configNonNull(
                superDefinition.getRef(),
                () -> "No reference in 'super-correlator' definition in " + current);
        if (!seen.add(ref)) {
            throw new ConfigurationException("There is a cycle in the correlator hierarchy: " + seen);
        }
        AbstractCorrelatorType superConfigRaw = getByReference(ref);
        AbstractCorrelatorType superConfigResolved = resolveSuperReferences(superConfigRaw, seen);
        AbstractCorrelatorType currentCloned = current.clone();
        new CorrelatorMergeOperation(currentCloned, superConfigResolved)
                .execute();
        return currentCloned;
    }

    private @NotNull AbstractCorrelatorType getByReference(@NotNull String name) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                getByReferenceInternal(name),
                () -> new ConfigurationException("No correlator configuration named '" + name + "' was found"));
    }

    private AbstractCorrelatorType getByReferenceInternal(@NotNull String name) throws ConfigurationException {
        if (systemConfiguration == null) {
            return null;
        }
        SystemConfigurationCorrelationType correlation = systemConfiguration.getCorrelation();
        if (correlation == null) {
            return null;
        }
        CompositeCorrelatorType correlators = correlation.getCorrelators();
        if (correlators == null) {
            return null;
        }
        List<CorrelatorConfiguration> matching = CorrelatorConfiguration.getConfigurationsDeeply(correlators).stream()
                .filter(cfg -> name.equals(cfg.getConfigurationBean().getName()))
                .collect(Collectors.toList());
        CorrelatorConfiguration singleMatching = MiscUtil.extractSingleton(
                matching,
                () -> new ConfigurationException(
                        "Ambiguous correlator name: '%s': %d configurations found: %s".formatted(
                                name, matching.size(), CorrelatorConfiguration.identify(matching))));
        return singleMatching != null ? singleMatching.getConfigurationBean() : null;
    }

    /**
     * Returns exactly one {@link CorrelatorConfiguration} from given "correlators" structure.
     * It may be a single correlator, or the whole structure - if it should be interpreted as a composite correlator.
     *
     * This is a bit unfortunate consequence of trying to be user-friendly with the typed "correlators" structure.
     * It may be changed in the future.
     *
     * @throws IllegalArgumentException If there are no configurations.
     */
    private static @NotNull CorrelatorConfiguration getConfiguration(@Nullable CompositeCorrelatorType composite) {
        if (composite == null) {
            return CorrelatorConfiguration.none();
        }
        Collection<CorrelatorConfiguration> configurations = CorrelatorConfiguration.getChildConfigurations(composite);

        if (configurations.isEmpty()) {
            if (composite.getSuper() == null) {
                throw new IllegalArgumentException("No correlator configurations in " + identify(composite));
            }
        }

        if (configurations.size() == 1) {
            var single = configurations.iterator().next();
            if (!single.hasCompositionItem()) {
                return single;
            } else {
                // There's a "composition" item in the correlator, so the correlator has to be treated as part of the composite
                // correlator configuration, even if it's the solo child there.
            }
        }

        // This is the default composite correlator.
        return new CorrelatorConfiguration.TypedCorrelationConfiguration(composite);
    }
}
