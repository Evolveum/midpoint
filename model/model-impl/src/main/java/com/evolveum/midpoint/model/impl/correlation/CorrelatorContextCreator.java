/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import static com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil.identify;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;

import com.evolveum.midpoint.model.api.indexing.IndexingConfiguration;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.identities.IdentitiesManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.lens.identities.IndexingConfigurationImpl;
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ObjectTemplateTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link CorrelatorContext} instances.
 *
 * TODO rework merging!!!
 */
public class CorrelatorContextCreator {

    /** What items are allowed in the configuration that contains `using` clause - i.e. that points to another config. */
    private static final Collection<ItemName> ALLOWED_ITEMS_FOR_USING = List.of(
            AbstractCorrelatorType.F_USING,
            AbstractCorrelatorType.F_DISPLAY_NAME,
            AbstractCorrelatorType.F_DESCRIPTION,
            AbstractCorrelatorType.F_DOCUMENTATION);

    /** These items are _not_ merged when extending the correlators. */
    private static final Collection<ItemName> NOT_MERGED_WHEN_EXTENDING = List.of(
            AbstractCorrelatorType.F_USING, // forbidden anyway
            AbstractCorrelatorType.F_NAME,
            AbstractCorrelatorType.F_DISPLAY_NAME,
            AbstractCorrelatorType.F_DESCRIPTION,
            AbstractCorrelatorType.F_DOCUMENTATION);

    /**
     * The original configuration we start with.
     */
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
    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;
    @NotNull private final IndexingConfiguration indexingConfiguration;

    /** The system configuration. We use it to look for global correlator definitions. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    /** The resulting merged configuration. It is cloned to avoid any immutability issues. */
    private AbstractCorrelatorType merged;

    private CorrelatorContextCreator(
            @NotNull CorrelatorConfiguration originalConfiguration,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @NotNull IndexingConfiguration indexingConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        this.originalConfiguration = originalConfiguration;
        this.originalConfigurationBean = originalConfiguration.getConfigurationBean();
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.identityManagementConfiguration = identityManagementConfiguration;
        this.indexingConfiguration = indexingConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    static CorrelatorContext<?> createRootContext(@NotNull FullCorrelationContext fullContext, ModelBeans beans)
            throws ConfigurationException, SchemaException {
        return createRootContext(
                fullContext.getCorrelationDefinitionBean(),
                fullContext.objectTemplate,
                fullContext.systemConfiguration,
                beans);
    }

    static CorrelatorContext<?> createRootContext(
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration,
            @NotNull ModelBeans beans)
            throws ConfigurationException, SchemaException {
        CompositeCorrelatorType correlators;
        CompositeCorrelatorType specificCorrelators = correlationDefinitionBean.getCorrelators();
        if (specificCorrelators != null) {
            correlators = specificCorrelators;
        } else {
            correlators = ObjectTemplateTypeUtil.getCorrelators(objectTemplate);
        }
        return new CorrelatorContextCreator(
                getConfiguration(correlators),
                correlationDefinitionBean,
                IdentitiesManager.createIdentityConfiguration(objectTemplate),
                IndexingConfigurationImpl.of(objectTemplate, beans),
                systemConfiguration)
                .create();
    }

    public static CorrelatorContext<?> createChildContext(
            @NotNull CorrelatorConfiguration childConfiguration,
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @NotNull IndexingConfiguration indexingConfiguration,
            @Nullable SystemConfigurationType systemConfiguration)
            throws ConfigurationException, SchemaException {
        return new CorrelatorContextCreator(
                childConfiguration,
                correlationDefinitionBean,
                identityManagementConfiguration,
                indexingConfiguration,
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
                    identityManagementConfiguration,
                    indexingConfiguration,
                    systemConfiguration);
        }

        createMergedConfiguration();

        return new CorrelatorContext<>(
                new CorrelatorConfiguration.TypedCorrelationConfiguration(merged),
                originalConfigurationBean,
                correlationDefinitionBean,
                identityManagementConfiguration,
                indexingConfiguration,
                systemConfiguration);
    }

    private void createMergedConfiguration() throws ConfigurationException, SchemaException {
        merged = evaluateInitialUsingStep().clone();
        goThroughExtensionChain();
    }

    private @NotNull AbstractCorrelatorType evaluateInitialUsingStep() throws ConfigurationException {
        String firstUsing = originalConfigurationBean.getUsing();
        if (firstUsing != null) {
            configCheck(originalConfigurationBean.getExtending() == null,
                    "Both 'extending' and 'using' cannot be set at once: %s", identify(originalConfigurationBean));
            AbstractCorrelatorType referenced = resolveReference(firstUsing);
            checkCompatibilityForTheUsingClause(originalConfigurationBean, referenced);
            return referenced;
        } else {
            return originalConfigurationBean;
        }
    }

    private void goThroughExtensionChain() throws ConfigurationException, SchemaException {
        AbstractCorrelatorType current = merged;
        for (;;) {
            configCheck(current.getUsing() == null,
                    "The 'using' property cannot be set at referenced correlator: %s", identify(current));
            String extendingRef = current.getExtending();
            if (extendingRef == null) {
                break;
            }
            AbstractCorrelatorType extending = resolveReference(extendingRef);
            mergeFromConfigBeingExtended(extending);
            current = extending;
        }
    }

    private void mergeFromConfigBeingExtended(@NotNull AbstractCorrelatorType beingExtended) throws SchemaException {
        //noinspection unchecked
        PrismContainerValue<AbstractCorrelatorType> mergedPcv = merged.asPrismContainerValue();
        //noinspection unchecked
        PrismContainerValue<AbstractCorrelatorType> beingExtendedPcv = beingExtended.asPrismContainerValue();

        for (Item<?, ?> itemToMove : beingExtendedPcv.getItems()) {
            if (!QNameUtil.contains(NOT_MERGED_WHEN_EXTENDING, itemToMove.getElementName())) {
                Item<?, ?> itemToMoveCloned = itemToMove.clone();
                if (itemToMove.isSingleValueByDefinition()) {
                    mergedPcv.addReplaceExisting(itemToMoveCloned);
                } else {
                    mergedPcv.merge(itemToMoveCloned);
                }
            }
        }
    }

    private void checkCompatibilityForTheUsingClause(
            @NotNull AbstractCorrelatorType original, @NotNull AbstractCorrelatorType referenced) throws ConfigurationException {
        //noinspection unchecked
        Collection<QName> unsupportedItemNames = ((Collection<QName>) original.asPrismContainerValue().getItemNames())
                .stream()
                .filter(itemName -> !QNameUtil.contains(ALLOWED_ITEMS_FOR_USING, itemName))
                .collect(Collectors.toList());
        if (!unsupportedItemNames.isEmpty()) {
            throw new ConfigurationException("Correlator " + identify(original) + " that references " + identify(referenced)
                    + " via 'using' clause contains unsupported configuration items: " + unsupportedItemNames);
        }
        if (original.getClass().equals(referenced.getClass())) {
            // Configuration bean classes are compatible
        } else if (original instanceof CompositeCorrelatorType) {
            // This is also OK
        } else {
            // But this may be misinterpreted by the user
            throw new ConfigurationException("Unsupported combination of correlators bound by 'using' clause: "
                    + identify(original) + " references " + identify(referenced));
        }
    }

    private @NotNull AbstractCorrelatorType resolveReference(@NotNull String name) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                resolveReferenceInternal(name),
                () -> new ConfigurationException("No correlator configuration named '" + name + "' was found"));
    }

    private AbstractCorrelatorType resolveReferenceInternal(@NotNull String name) throws ConfigurationException {
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
                () -> new ConfigurationException("Ambiguous correlator name: '" + name + "': "
                        + matching.size() + " configurations found: " + CorrelatorConfiguration.identify(matching)));
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
            if (composite.getExtending() == null && composite.getUsing() == null) {
                throw new IllegalArgumentException("No correlator configurations in " + identify(composite));
            }
        }

        if (configurations.size() == 1) {
            return configurations.iterator().next();
        }

        // This is the default composite correlator.
        return new CorrelatorConfiguration.TypedCorrelationConfiguration(composite);
    }
}
