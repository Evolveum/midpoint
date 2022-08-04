/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlation;

import com.evolveum.midpoint.model.api.identities.IdentityManagementConfiguration;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.correlator.CorrelatorConfiguration;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.impl.correlator.FullCorrelationContext;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * Creates {@link CorrelatorContext} instances.
 *
 * TODO double check handling of PCV IDs when merging
 *  (fortunately, the object is not stored anywhere, so conflicts are most probably harmless)
 */
public class CorrelatorContextCreator {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelatorContextCreator.class);

    /** What items are allowed in the configuration that contains `using` clause - i.e. that points to another config. */
    private static final Collection<ItemName> ALLOWED_ITEMS_FOR_USING = List.of(
            AbstractCorrelatorType.F_USING,
            AbstractCorrelatorType.F_AUTHORITY,
            AbstractCorrelatorType.F_ORDER,
            AbstractCorrelatorType.F_DISPLAY_NAME,
            AbstractCorrelatorType.F_DEFINITIONS,
            AbstractCorrelatorType.F_DESCRIPTION,
            AbstractCorrelatorType.F_DOCUMENTATION);

    /** These items are _not_ merged when extending the correlators. */
    private static final Collection<ItemName> NOT_MERGED_WHEN_EXTENDING = List.of(
            AbstractCorrelatorType.F_USING, // forbidden anyway
            AbstractCorrelatorType.F_AUTHORITY,
            AbstractCorrelatorType.F_ORDER,
            AbstractCorrelatorType.F_NAME,
            AbstractCorrelatorType.F_DISPLAY_NAME,
            AbstractCorrelatorType.F_DESCRIPTION,
            AbstractCorrelatorType.F_DOCUMENTATION,
            AbstractCorrelatorType.F_DEFINITIONS // these are merged in a different way
    );

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

    /** Used to find additional definitions. */
    @Nullable private final AbstractCorrelatorType parentOriginalConfigurationBean;

    /** The correlation definition. We just pass this to the context. */
    @Nullable private final CorrelationDefinitionType correlationDefinitionBean;

    /** TODO */
    @NotNull private final IdentityManagementConfiguration identityManagementConfiguration;

    /** The system configuration. We use it to look for global correlator definitions. */
    @Nullable private final SystemConfigurationType systemConfiguration;

    /** Correlators that we should take definitions from. */
    @NotNull private final Queue<AbstractCorrelatorType> toMergeDefinitionsQueue = new LinkedList<>();

    /** Correlators we have merged the definitions from. */
    @NotNull private final List<AbstractCorrelatorType> definitionsMergedFrom = new ArrayList<>();

    /** Source (not cloned) for {@link #merged}. Present in this form to allow reference comparisons. */
    private AbstractCorrelatorType mergedBeforeClone;

    /** The resulting merged configuration. It is cloned to avoid any immutability issues. */
    private AbstractCorrelatorType merged;

    private CorrelatorContextCreator(
            @NotNull CorrelatorConfiguration originalConfiguration,
            @Nullable AbstractCorrelatorType parentOriginalConfigurationBean,
            @Nullable CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @Nullable SystemConfigurationType systemConfiguration) {
        this.originalConfiguration = originalConfiguration;
        this.originalConfigurationBean = originalConfiguration.getConfigurationBean();
        this.parentOriginalConfigurationBean = parentOriginalConfigurationBean;
        this.correlationDefinitionBean = correlationDefinitionBean;
        this.identityManagementConfiguration = identityManagementConfiguration;
        this.systemConfiguration = systemConfiguration;
    }

    static CorrelatorContext<?> createRootContext(@NotNull FullCorrelationContext fullContext)
            throws ConfigurationException, SchemaException {
        return createRootContext(
                fullContext.getCorrelationDefinitionBean(),
                fullContext.objectTemplate,
                fullContext.systemConfiguration);
    }

    static CorrelatorContext<?> createRootContext(
            @NotNull CorrelationDefinitionType correlationDefinitionBean,
            @Nullable ObjectTemplateType objectTemplate,
            @Nullable SystemConfigurationType systemConfiguration)
            throws ConfigurationException, SchemaException {
        CompositeCorrelatorType correlators;
        CompositeCorrelatorType specificCorrelators = correlationDefinitionBean.getCorrelators();
        if (specificCorrelators != null) {
            correlators = specificCorrelators;
        } else {
            correlators = getTemplateCorrelators(objectTemplate);
        }
        return new CorrelatorContextCreator(
                getConfiguration(correlators),
                null,
                correlationDefinitionBean,
                IdentityManagementConfiguration.of(objectTemplate),
                systemConfiguration)
                .create();
    }

    private static CompositeCorrelatorType getTemplateCorrelators(ObjectTemplateType objectTemplate) {
        if (objectTemplate == null) {
            return null;
        }
        IdentityDataHandlingType identityHandling = objectTemplate.getIdentity();
        if (identityHandling == null) {
            return null;
        }
        return identityHandling.getCorrelators();
    }

    public static CorrelatorContext<?> createChildContext(
            @NotNull CorrelatorConfiguration childConfiguration,
            @NotNull AbstractCorrelatorType parentOriginalConfigurationBean,
            @Nullable CorrelationDefinitionType correlationDefinitionBean,
            @NotNull IdentityManagementConfiguration identityManagementConfiguration,
            @Nullable SystemConfigurationType systemConfiguration)
            throws ConfigurationException, SchemaException {
        return new CorrelatorContextCreator(
                childConfiguration,
                parentOriginalConfigurationBean,
                correlationDefinitionBean,
                identityManagementConfiguration,
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
                    systemConfiguration);
        }

        createMergedConfiguration();

        return new CorrelatorContext<>(
                new CorrelatorConfiguration.TypedCorrelationConfiguration(merged),
                originalConfigurationBean,
                correlationDefinitionBean,
                identityManagementConfiguration,
                systemConfiguration);
    }

    private void createMergedConfiguration() throws ConfigurationException, SchemaException {
        toMergeDefinitionsQueue.add(originalConfigurationBean);
        if (parentOriginalConfigurationBean != null) {
            toMergeDefinitionsQueue.add(parentOriginalConfigurationBean);
        }

        mergedBeforeClone = evaluateInitialUsingStep();
        merged = mergedBeforeClone.clone();

        goThroughExtensionChain();

        while (!toMergeDefinitionsQueue.isEmpty()) {
            mergeDefinitionsFromQueueHead();
        }
    }

    private @NotNull AbstractCorrelatorType evaluateInitialUsingStep() throws ConfigurationException {
        String firstUsing = originalConfigurationBean.getUsing();
        if (firstUsing != null) {
            configCheck(originalConfigurationBean.getExtending() == null,
                    "Both 'extending' and 'using' cannot be set at once: %s", identify(originalConfigurationBean));
            AbstractCorrelatorType referenced = resolveReference(firstUsing);
            checkCompatibilityForTheUsingClause(originalConfigurationBean, referenced);
            toMergeDefinitionsQueue.add(referenced);
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
            toMergeDefinitionsQueue.add(extending);
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

    private void mergeDefinitionsFromQueueHead() throws ConfigurationException {
        AbstractCorrelatorType current = toMergeDefinitionsQueue.remove();
        LOGGER.trace("Getting ancestors for {}", identifyLazily(current));
        while (current != null) {
            if (containsByIdentity(definitionsMergedFrom, current)) {
                LOGGER.trace("Definition already merged from {}", identifyLazily(current));
                break;
            }
            mergeDefinitionsFrom(current);
            definitionsMergedFrom.add(current);
            current = getParentCorrelatorBean(current);
            LOGGER.trace("Parent = {}", identifyLazily(current));
        }
    }

    private void mergeDefinitionsFrom(@NotNull AbstractCorrelatorType config) throws ConfigurationException {
        if (config == mergedBeforeClone) {
            return; // No need to merge from myself
        }
        LOGGER.trace("Merging definitions from {}", identifyLazily(config));
        CorrelatorDefinitionsType definitions = config.getDefinitions();
        if (definitions == null) {
            return;
        }
        CorrelationItemsDefinitionType items = definitions.getItems();
        if (items != null) {
            for (CorrelationItemDefinitionType item : items.getItem()) {
                mergeItem(item);
            }
        }
    }

    @NotNull
    private CorrelatorDefinitionsType createOrFindMergedDefinitions() {
        CorrelatorDefinitionsType definitions = merged.getDefinitions();
        if (definitions != null) {
            return definitions;
        }
        CorrelatorDefinitionsType newDefinitions = new CorrelatorDefinitionsType();
        merged.setDefinitions(newDefinitions);
        return newDefinitions;
    }

    private void mergeItem(@NotNull CorrelationItemDefinitionType newItem) throws ConfigurationException {
        CorrelatorDefinitionsType definitions = createOrFindMergedDefinitions();
        if (definitions.getItems() == null) {
            definitions.setItems(new CorrelationItemsDefinitionType());
        }
        List<CorrelationItemDefinitionType> existingItemList = definitions.getItems().getItem();
        CorrelationItemDefinitionType existingItem = findItem(existingItemList, newItem);
        if (existingItem == null) {
            existingItemList.add(newItem.cloneWithoutId());
        } else {
            mergeItem(existingItem, newItem);
        }
    }

    private void mergeItem(
            @NotNull CorrelationItemDefinitionType existingItem,
            @NotNull CorrelationItemDefinitionType newItem) {
        if (existingItem.getPath() == null && newItem.getPath() != null) {
            existingItem.setPath(newItem.getPath().clone());
        }
    }

    private CorrelationItemDefinitionType findItem(
            List<CorrelationItemDefinitionType> items, CorrelationItemDefinitionType newItem) throws ConfigurationException {
        String newItemName = getName(newItem);
        List<CorrelationItemDefinitionType> matching = items.stream()
                .filter(item -> CorrelationItemDefinitionUtil.getName(item).equals(newItemName))
                .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                matching,
                () -> new ConfigurationException("Multiple items named '" + newItemName + "' found: " + matching));
    }

    private <T> boolean containsByIdentity(Collection<T> objects, T object) {
        return objects.stream()
                .anyMatch(o -> o == object);
    }

    /**
     * Gets the parent correlator bean - in the prism sense.
     *
     * Does not support untyped parents.
     *
     * Temporary code.
     */
    private AbstractCorrelatorType getParentCorrelatorBean(AbstractCorrelatorType configBean) {
        PrismContainerable<?> parentContainer = configBean.asPrismContainerValue().getParent();
        if (!(parentContainer instanceof PrismContainer<?>)) {
            return null;
        }
        PrismContainerValue<?> parentPcv = ((PrismContainer<?>) parentContainer).getParent();
        if (parentPcv == null) {
            return null;
        }
        if (parentPcv.getCompileTimeClass() == null) {
            // Not a typed correlator configuration, so let's exit to avoid exceptions in asContainerable call below.
            return null;
        }
        Containerable parentContainerable = parentPcv.asContainerable();
        if (parentContainerable instanceof AbstractCorrelatorType) {
            return ((AbstractCorrelatorType) parentContainerable);
        } else {
            return null;
        }
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

        Collection<CorrelatorConfiguration> configurations = CorrelatorConfiguration.getConfigurations(composite);

        if (configurations.isEmpty()) {
            if (composite.getExtending() == null && composite.getUsing() == null) {
                throw new IllegalArgumentException("No correlator configurations in " + identify(composite));
            }
        }

        if (configurations.size() == 1) {
            CorrelatorConfiguration configuration = configurations.iterator().next();
            if (canBeStandalone(configuration)) {
                return configuration;
            }
        }

        // This is the default composite correlator.
        return new CorrelatorConfiguration.TypedCorrelationConfiguration(composite);
    }

    /**
     * Currently, a configuration that is not non-authoritative can be run as standalone - without wrapping
     * in composite correlator.
     */
    private static boolean canBeStandalone(CorrelatorConfiguration configuration) {
        return configuration.getAuthority() != CorrelatorAuthorityLevelType.NON_AUTHORITATIVE;
    }
}
