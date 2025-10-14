package com.evolveum.midpoint.gui.impl.component.data.provider.suggestion;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationStatusInfoUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.SerializableBiFunction;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Centralized utility for building {@link SuggestionsModelDto} for various Smart Integration suggestion types.
 * <p>
 * Each factory method prepares a Wicket {@link LoadableModel} combining base container values
 * with Smart-generated suggestions, and provides a resolver function for {@link StatusInfo}.
 */
public final class StatusAwareDataFactory {

    private static final String CLASS_DOT = StatusAwareDataFactory.class.getName() + ".";
    private static final String OP_LOAD_SUGGESTIONS = CLASS_DOT + "loadSuggestions";

    private StatusAwareDataFactory() {
        // Utility class; prevent instantiation
    }

    /**
     * Generic helper that loads Smart suggestions and merges them with base container values.
     */
    private static <C extends Containerable, S> @NotNull SuggestionsModelDto<C> buildSuggestionsModel(
            @NotNull Component component,
            @NotNull IModel<Boolean> toggleModel,
            @NotNull String resourceOid,
            @NotNull SerializableSupplier<List<PrismContainerValueWrapper<C>>> baseValuesSupplier,
            @NotNull SerializableBiFunction<PageBase, Task, SmartIntegrationStatusInfoUtils.SuggestionProviderResult<C, S>> loader) {

        final Map<PrismContainerValueWrapper<C>, StatusInfo<S>> suggestionsIndex = new HashMap<>();

        IModel<List<PrismContainerValueWrapper<C>>> model = new LoadableModel<>() {
            @Override
            protected @NotNull List<PrismContainerValueWrapper<C>> load() {
                List<PrismContainerValueWrapper<C>> values = new ArrayList<>(baseValuesSupplier.get());
                suggestionsIndex.clear();

                if (Boolean.TRUE.equals(toggleModel.getObject())) {
                    PageBase page = (PageBase) component.getPage();
                    Task task = page.createSimpleTask(OP_LOAD_SUGGESTIONS);
                    var res = loader.apply(page, task);
                    values.addAll(res.wrappers());
                    suggestionsIndex.putAll(res.suggestionByWrapper());
                }

                return values;
            }
        };

        SuggestionsModelDto<C> dto = new SuggestionsModelDto<>(model, resourceOid);
        dto.suggestionResolver = suggestionsIndex::get;
        return dto;
    }

    /**
     * Creates a model combining association definitions with Smart association suggestions.
     */
    public static @NotNull SuggestionsModelDto<ShadowAssociationTypeDefinitionType> createAssociationModel(
            @NotNull Component component,
            @NotNull IModel<Boolean> toggleModel,
            @NotNull PrismContainerWrapperModel<ResourceType, ShadowAssociationTypeDefinitionType> baseWrapper,
            @NotNull String resourceOid) {

        return buildSuggestionsModel(
                component,
                toggleModel,
                resourceOid,
                () -> Optional.ofNullable(baseWrapper.getObject().getValues()).orElse(List.of()),
                (page, task) -> SmartIntegrationStatusInfoUtils.loadAssociationTypeSuggestionWrappers(
                        page, resourceOid, task, task.getResult())
        );
    }

    /**
     * Creates a model combining object type definitions with Smart object type suggestions.
     */
    public static @NotNull SuggestionsModelDto<ResourceObjectTypeDefinitionType> createObjectTypeModel(
            @NotNull Component component,
            @NotNull IModel<Boolean> toggleModel,
            @NotNull PrismContainerWrapperModel<ResourceType, ResourceObjectTypeDefinitionType> baseWrapper,
            @NotNull String resourceOid) {

        return buildSuggestionsModel(
                component,
                toggleModel,
                resourceOid,
                () -> Optional.ofNullable(baseWrapper.getObject().getValues()).orElse(List.of()),
                (page, task) -> SmartIntegrationStatusInfoUtils.loadObjectTypeSuggestionWrappers(
                        page, resourceOid, task, task.getResult())
        );
    }

    /**
     * Creates a model merging correlation definitions with Smart correlation suggestions.
     */
    public static @NotNull SuggestionsModelDto<ItemsSubCorrelatorType> createCorrelationModel(
            @NotNull Component component,
            @NotNull IModel<Boolean> toggleModel,
            @NotNull IModel<PrismContainerWrapper<ItemsSubCorrelatorType>> containerModel,
            @NotNull PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper,
            @NotNull String resourceOid) {

        return buildSuggestionsModel(
                component,
                toggleModel,
                resourceOid,
                () -> Optional.ofNullable(containerModel.getObject().getValues()).orElse(List.of()),
                (page, task) -> SmartIntegrationStatusInfoUtils.loadCorrelationSuggestionWrappers(
                        page, resourceOid, parentWrapper.getRealValue(), false, task, task.getResult())
        );
    }

    /**
     * Creates a model combining mapping definitions with Smart mapping suggestions.
     * Includes initial sorting, filtering, and handling of accepted vs. normal mappings.
     */
    public static @NotNull SuggestionsModelDto<MappingType> createMappingModel(
            @NotNull Component component,
            @NotNull String resourceOid,
            @NotNull IModel<Boolean> switchSuggestionModel,
            @NotNull IModel<PrismContainerWrapper<MappingType>> containerModel,
            @NotNull PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> parentWrapper,
            @NotNull MappingDirection mappingDirection,
            @NotNull Set<PrismContainerValueWrapper<MappingType>> acceptedSuggestionsCache) {

        final Map<PrismContainerValueWrapper<MappingType>, StatusInfo<MappingsSuggestionType>> suggestionsIndex = new HashMap<>();

        IModel<List<PrismContainerValueWrapper<MappingType>>> valuesModel = new LoadableModel<>() {
            @Override
            protected @NotNull List<PrismContainerValueWrapper<MappingType>> load() {
                suggestionsIndex.clear();

                List<PrismContainerValueWrapper<MappingType>> suggestions = new ArrayList<>();
                List<PrismContainerValueWrapper<MappingType>> accepted = new ArrayList<>();
                List<PrismContainerValueWrapper<MappingType>> normal = new ArrayList<>();

                // Only load Smart suggestions for inbound mappings
                if (Boolean.TRUE.equals(switchSuggestionModel.getObject())
                        && mappingDirection == MappingDirection.INBOUND) {
                    loadSuggestions(suggestions);
                }

                PrismContainerWrapper<MappingType> container = containerModel.getObject();
                if (container != null && !container.getValues().isEmpty()) {
                    for (PrismContainerValueWrapper<MappingType> value : container.getValues()) {
                        if (acceptedSuggestionsCache.contains(value)) {
                            accepted.add(value);
                        } else {
                            normal.add(value);
                        }
                    }
                }

                return new ArrayList<>(initialSort(suggestions, accepted)) {{
                    addAll(normal);
                }};
            }

            private void loadSuggestions(@NotNull List<PrismContainerValueWrapper<MappingType>> suggestions) {
                PageBase page = (PageBase) component.getPage();
                Task task = page.createSimpleTask("Loading mapping suggestions");
                OperationResult result = task.getResult();

                ResourceObjectTypeDefinitionType rotDef = parentWrapper.getRealValue();

                var suggestionWrappers = SmartIntegrationStatusInfoUtils.loadMappingSuggestionWrappers(
                        page,
                        resourceOid,
                        rotDef,
                        mappingDirection,
                        false,
                        task,
                        result);

                suggestionWrappers.wrappers().forEach(WebPrismUtil::setReadOnlyRecursively);

                suggestions.addAll(suggestionWrappers.wrappers());
                suggestionsIndex.putAll(suggestionWrappers.suggestionByWrapper());
            }

            /**
             * Sorts suggestions and accepted ones by target path (case-insensitive).
             */
            private @NotNull List<PrismContainerValueWrapper<MappingType>> initialSort(
                    List<PrismContainerValueWrapper<MappingType>> suggestions,
                    List<PrismContainerValueWrapper<MappingType>> accepted) {

                List<PrismContainerValueWrapper<MappingType>> related = new ArrayList<>();
                related.addAll(suggestions);
                related.addAll(accepted);

                Comparator<PrismContainerValueWrapper<MappingType>> byTargetName =
                        Comparator.comparing(
                                v -> Optional.ofNullable(getTargetValue(v)).orElse(""),
                                String.CASE_INSENSITIVE_ORDER);

                related.sort(byTargetName);
                return related;
            }

            private String getTargetValue(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
                MappingType mapping = mappingWrapper.getRealValue();
                VariableBindingDefinitionType target = mapping != null ? mapping.getTarget() : null;
                ItemPathType path = target != null ? target.getPath() : null;
                return path != null ? path.toString() : "";
            }
        };

        SuggestionsModelDto<MappingType> dto = new SuggestionsModelDto<>(valuesModel, resourceOid);
        dto.suggestionResolver = suggestionsIndex::get;
        return dto;
    }

    public static class SuggestionsModelDto<C extends Containerable> {

        IModel<List<PrismContainerValueWrapper<C>>> model;
        SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> suggestionResolver;
        String resourceOid;

        public SuggestionsModelDto(IModel<List<PrismContainerValueWrapper<C>>> model, String resourceOid) {
            this.model = model;
            this.resourceOid = resourceOid;
        }

        public IModel<List<PrismContainerValueWrapper<C>>> getModel() {
            return model;
        }

        public String getResourceOid() {
            return resourceOid;
        }

        public SerializableFunction<PrismContainerValueWrapper<C>, StatusInfo<?>> getSuggestionResolver() {
            return suggestionResolver;
        }
    }
}
