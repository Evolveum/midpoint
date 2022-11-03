/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SearchFactory {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFactory.class);
    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String LOAD_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "loadAdminGuiConfiguration";
    protected static final String OPERATION_LOAD_FULLTEXT_SEARCH_CONFIGURATION = DOT_CLASS + "loadFullTextSearchConfiguration";

    public static <C extends Containerable> Search<C> createMemberSearch(Class<C> type, List<QName> supportedTypes, List<QName> supportedRelations,
            QName abstractRoleType, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultOrgMembersSearchBoxConfiguration(type, supportedTypes, supportedRelations, modelServiceLocator);

        SearchBoxConfigurationType mergedConfig = getMergedConfiguration(defaultSearchBox, collectionView);
        SearchConfigurationWrapper<C> basicSearchWrapper = createBasicSearchWrapper(type, collectionView, mergedConfig, modelServiceLocator);

        if (abstractRoleType != null) {
            createAbstractRoleSearchItemWrapperList(abstractRoleType, basicSearchWrapper, mergedConfig);
        }
        return createSearch(type, mergedConfig, collectionView, basicSearchWrapper, modelServiceLocator);

    }
    public static <C extends Containerable> Search<C> createSearch(Class<C> type, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultSearchBoxConfiguration(type, Arrays.asList(ObjectType.F_EXTENSION), null, modelServiceLocator);
        return createSearch(type,  defaultSearchBox, collectionView, modelServiceLocator);
    }

    private static <C extends Containerable> Search<C> createSearch(Class<C> type, SearchBoxConfigurationType defaultSearchBoxConfig, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType mergedConfig = getMergedConfiguration(defaultSearchBoxConfig, collectionView);

        SearchConfigurationWrapper<C> basicSearchWrapper = createBasicSearchWrapper(type, collectionView, mergedConfig, modelServiceLocator);
        return createSearch(type, mergedConfig, collectionView, basicSearchWrapper, modelServiceLocator);
    }

    private static SearchBoxConfigurationType getMergedConfiguration(SearchBoxConfigurationType defaultSearchBoxConfig, CompiledObjectCollectionView collectionView) {
        SearchBoxConfigurationType configuredSearchBox = getConfiguredSearchBox(collectionView);
        SearchBoxConfigurationType mergedConfig = SearchConfigurationMerger.mergeConfigurations(defaultSearchBoxConfig, configuredSearchBox);
        return mergedConfig;
    }

    private static <C extends Containerable> Search<C> createSearch(Class<C> type, SearchBoxConfigurationType mergedConfig, CompiledObjectCollectionView collectionView, SearchConfigurationWrapper<C> basicSearchWrapper, ModelServiceLocator modelServiceLocator) {
        AxiomQueryWrapper<C> axiomWrapper = new AxiomQueryWrapper<>();
        AdvancedQueryWrapper advancedQueryWrapper = new AdvancedQueryWrapper();
        FulltextQueryWrapper fulltextQueryWrapper = new FulltextQueryWrapper();

        Search<C> search = new Search<>(type, mergedConfig);
        search.setTypeClass(type);

        if (mergedConfig.getObjectTypeConfiguration() != null) {
            search.setAllowedTypeList(mergedConfig.getObjectTypeConfiguration()
                    .getSupportedTypes());
        }

        search.setAdvancedQueryWrapper(advancedQueryWrapper);
        search.setAxiomQueryWrapper(axiomWrapper);
        search.setFulltextQueryWrapper(fulltextQueryWrapper);
        search.setSearchConfigurationWrapper(basicSearchWrapper);
        search.setSearchMode(getDefaultSearchMode(mergedConfig, modelServiceLocator, type));
        search.setAllowedModeList(createAllowedModeList(type, modelServiceLocator));
        if (collectionView !=  null) {
            search.setCollectionViewName(collectionView.getViewIdentifier());
            if (collectionView.getCollection() != null && collectionView.getCollection().getCollectionRef() != null) {
                search.setCollectionRefOid(collectionView.getCollection().getCollectionRef().getOid());
            }
        }

        return search;
    }

//    protected PrismContainerDefinition<C> getTypeDefinitionForSearch() {
//        return getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(getType());
//    }

    public static Search<AssignmentType> createAssignmnetSearch(QName targetType, boolean isRepoSearchEnabled, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {

        PrismContainerDefinition<AssignmentType> definitionOverwrite;
        PrismContainerDefinition<AssignmentType> orig = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
        if (targetType == null) {
            definitionOverwrite = orig;
        } else {
            // We have more concrete assignment type, we should replace targetRef definition
            // with one with concrete assignment type.
            definitionOverwrite = modelServiceLocator.getModelInteractionService().assignmentTypeDefinitionWithConcreteTargetRefType(orig, targetType);
        }

        var targetExtensionPath = ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(targetType), ObjectType.F_EXTENSION);
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultAssignmentSearchBoxConfiguration(targetType, definitionOverwrite, Arrays.asList(ObjectType.F_EXTENSION, targetExtensionPath), modelServiceLocator);

        SearchBoxConfigurationType mergedConfig = getMergedConfiguration(defaultSearchBox, collectionView);

        SearchConfigurationWrapper<AssignmentType> basicSearchWrapper = createBasicSearchWrapper(AssignmentType.class, collectionView, mergedConfig, modelServiceLocator);
//        addAssignmentTargetSpecificItems(targetType, definitionOverwrite, basicSearchWrapper, modelServiceLocator);

        Search<AssignmentType> search = createSearch(AssignmentType.class, mergedConfig, collectionView, basicSearchWrapper, modelServiceLocator);

        search.setContainerDefinition(definitionOverwrite);
        if (isRepoSearchEnabled) {
            OperationResult result = new OperationResult(OPERATION_LOAD_FULLTEXT_SEARCH_CONFIGURATION);
            try {
                FullTextSearchConfigurationType config = modelServiceLocator.getModelInteractionService().getSystemConfiguration(result).getFullTextSearch();
                if (config != null && FullTextSearchUtil.isEnabledFor(config, Collections.singletonList(targetType == null ? AbstractRoleType.COMPLEX_TYPE : targetType))) {
                    defaultSearchBox.getAllowedMode().add(SearchBoxModeType.FULLTEXT);
                    defaultSearchBox.setDefaultMode(SearchBoxModeType.FULLTEXT);
                }
            } catch (Exception e) {
                LOGGER.debug("Unable to load full text search configuration from system configuration, {}", e.getMessage());
            }
        }
        //TODo allowed mode list
        search.setAllowedModeList(createAllowedModeList(AssignmentType.class, modelServiceLocator));

        return search;
    }

    //TODO REVIEW NEEDED!!!!
    private static <C extends Containerable> SearchConfigurationWrapper<C> createBasicSearchWrapper(Class<C> type,
            CompiledObjectCollectionView collectionView, SearchBoxConfigurationType configuredSearchBox, ModelServiceLocator modelServiceLocator) {
        SearchConfigurationWrapper<C> basicSearchWrapper = createDefaultSearchBoxConfigurationWrapper(type, configuredSearchBox, modelServiceLocator);
        basicSearchWrapper.setAllowToConfigureSearchItems(isAllowToConfigureSearchItems(configuredSearchBox));


        if (collectionView == null) {
            //todo we need to get saved searches here for the specified type
            List<CompiledObjectCollectionView> views = modelServiceLocator.getCompiledGuiProfile()
                    .findAllApplicableObjectCollectionViews(WebComponentUtil.containerClassToQName(PrismContext.get(), type))
                    .stream()
                    .filter(v -> v.getFilter() != null)     //todo should we check also collectionRef?
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(views)) {
                ObjectCollectionListSearchItemWrapper<C> viewListItem = new ObjectCollectionListSearchItemWrapper<>(type,
                        views);
                viewListItem.setVisible(true);
                basicSearchWrapper.getItemsList().add(viewListItem);
            }
        }
        basicSearchWrapper.getItemsList().sort((i1, i2) -> String.CASE_INSENSITIVE_ORDER.compare(
                StringUtils.isEmpty(i1.getName()) ? "" : PageBase.createStringResourceStatic(i1.getName()).getString(),
                StringUtils.isEmpty(i2.getName()) ? "" : PageBase.createStringResourceStatic(i2.getName()).getString()));

        basicSearchWrapper.getItemsList().sort(Comparator.comparing(i -> i instanceof PropertySearchItemWrapper));
        return basicSearchWrapper;
    }

    private static <C extends Containerable> SearchBoxModeType getDefaultSearchMode(SearchBoxConfigurationType config, ModelServiceLocator modelServiceLocator, Class<C> type) {
        if (isFullTextSearchEnabled(modelServiceLocator, type)) {
            return SearchBoxModeType.FULLTEXT;
        }
        if (config == null || config.getDefaultMode() == null) {
            return SearchBoxModeType.BASIC;
        }

        return config.getDefaultMode();
    }

    private static SearchBoxConfigurationType getConfiguredSearchBox(CompiledObjectCollectionView collectionView) {
        if (collectionView == null) {
            return null;
        }

        //TODO legacy support for members?
        return collectionView.getSearchBoxConfiguration();
    }

    public static <C extends Containerable> Search<C> createSearch(Class<C> type, ModelServiceLocator modelServiceLocator) {
        return createSearch(type, null, modelServiceLocator);
    }

    public static <C extends Containerable> Search<ShadowType> createProjectionsTabSearch(CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        Search<ShadowType> search = createSearch(ShadowType.class, collectionView, modelServiceLocator);
        search.getItems().forEach(item -> {
            if (!(item instanceof PropertySearchItemWrapper)) {
                return;
            }
            //on the projections tab we have by default just DEAD search item, therefore we hide all others
            item.setVisible(false);
            if (((PropertySearchItemWrapper) item).getPath() != null
                    && ShadowType.F_DEAD.equivalent(((PropertySearchItemWrapper<?>) item).getPath())) {
                item.setVisible(true);
                item.setCanConfigure(false);
            }
        });

//        List<? super AbstractSearchItemWrapper> defs = new ArrayList<>();
//
//        addSearchRefWrapper(containerDef, ShadowType.F_RESOURCE_REF, defs, AreaCategoryType.ADMINISTRATION, modelServiceLocator);
//        addSearchPropertyWrapper(containerDef, ShadowType.F_NAME, defs, modelServiceLocator);
//        addSearchPropertyWrapper(containerDef, ShadowType.F_INTENT, defs, modelServiceLocator);
//        addSearchPropertyWrapper(containerDef, ShadowType.F_KIND, defs, modelServiceLocator);

        return search;
    }

    private static <C extends Containerable> List<SearchBoxModeType> createAllowedModeList(Class<C> type, ModelServiceLocator modelServiceLocator) {
        List<SearchBoxModeType> allowedModeTypes = Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY);
        if (isFullTextSearchEnabled(modelServiceLocator, (Class<? extends ObjectType>) type)) {
            allowedModeTypes.add(SearchBoxModeType.FULLTEXT);
        }
        return allowedModeTypes;
    }

    private static boolean objectTypeSearchItemWrapperExists(List<AbstractSearchItemWrapper> items) {
        for (AbstractSearchItemWrapper item : items) {
            if (item instanceof ObjectTypeSearchItemWrapper) {
                return true;
            }
        }
        return false;
    }
    public static void createAbstractRoleSearchItemWrapperList(QName abstractRoleType, SearchConfigurationWrapper searchConfigWrapper, SearchBoxConfigurationType config) {
        AbstractRoleSearchItemWrapper roleSearchWrapper = new AbstractRoleSearchItemWrapper(abstractRoleType, config);
        if (roleSearchWrapper.isNotEmpty()){
            searchConfigWrapper.getItemsList().add(roleSearchWrapper);
        }

    }

    public static <C extends Containerable> SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(Class<C> type,
            ModelServiceLocator modelServiceLocator) {
        return createDefaultSearchBoxConfigurationWrapper(type, null, modelServiceLocator);
    }

    public static <C extends Containerable> SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(Class<C> type,
            SearchBoxConfigurationType mergedConfig, ModelServiceLocator modelServiceLocator) {
        return createDefaultSearchBoxConfigurationWrapper(type, mergedConfig, null, modelServiceLocator);
    }

    public static <C extends Containerable> SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(
            Class<C> type, @NotNull SearchBoxConfigurationType mergedConfig, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        SearchConfigurationWrapper searchConfigWrapper = new SearchConfigurationWrapper();
        SearchItemsType searchItems = mergedConfig.getSearchItems();
        for (SearchItemType searchItem : searchItems.getSearchItem()) {
            searchConfigWrapper.getItemsList().add(SearchConfigurationWrapperFactory.createPropertySearchItemWrapper(type, searchItem,  coordinates, modelServiceLocator));
        }
        return searchConfigWrapper;
    }

    private static <C extends Containerable> SearchItemType createSearchItem(Class<C> type, ItemPath path) {
        SearchItemType searchItemType = new SearchItemType().path(new ItemPathType(path));
        searchItemType.setVisibleByDefault(PredefinedSearchableItems.isFixedItem(type, path));
        return searchItemType;
    }

    public static List<SearchItemDefinition> getConfiguredSearchItemDefinitions(List<SearchItemDefinition> availableDefinitions,
            ModelServiceLocator modelServiceLocator, QName type, String collectionViewName, Search.PanelType panelType) {
        SearchBoxConfigurationType searchConfig = getSearchBoxConfiguration(modelServiceLocator, type, collectionViewName, panelType);
        if (searchConfig == null) {
            return null;
        }
        SearchItemsType configuredSearchItems = searchConfig.getSearchItems();
        if (configuredSearchItems == null || CollectionUtils.isEmpty(configuredSearchItems.getSearchItem())) {
            return null;
        }
        List<SearchItemDefinition> configuredSearchItemList = new ArrayList<>();
        configuredSearchItems.getSearchItem().forEach(searchItem -> {
            for (SearchItemDefinition def : availableDefinitions) {
                ItemPathType searchItemPath = new ItemPathType(def.getPath());
                if (searchItem.getPath() != null && searchItem.getPath().equivalent(searchItemPath)) {
                    def.setDisplayName(searchItem.getDisplayName());
                    configuredSearchItemList.add(def);
                    return;
                }
            }
        });
        configuredSearchItems.getSearchItem().forEach(searchItem -> {
            if (searchItem.getFilter() != null || searchItem.getFilterExpression() != null) {
                configuredSearchItemList.add(new SearchItemDefinition(searchItem));
                return;
            }
        });
        return configuredSearchItemList;
    }

    private static <T extends Containerable> boolean isFullTextSearchEnabled(ModelServiceLocator modelServiceLocator, Class<T> type) {
        OperationResult result = new OperationResult(LOAD_SYSTEM_CONFIGURATION);
        try {
            return FullTextSearchUtil.isEnabledFor(modelServiceLocator.getModelInteractionService().getSystemConfiguration(result)
                    .getFullTextSearch(), type);
        } catch (SchemaException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    private static boolean isAllowToConfigureSearchItems(SearchBoxConfigurationType searchBoxConfigurationType) {
        if (searchBoxConfigurationType == null || searchBoxConfigurationType.isAllowToConfigureSearchItems() == null) {
            return true; //todo should be set to false
        }
        return searchBoxConfigurationType.isAllowToConfigureSearchItems();
    }

    private static SearchBoxConfigurationType getSearchBoxConfiguration(ModelServiceLocator modelServiceLocator,
            QName type, String collectionViewName, Search.PanelType panelType) {
        OperationResult result = new OperationResult(LOAD_ADMIN_GUI_CONFIGURATION);
        try {
            CompiledGuiProfile guiConfig = modelServiceLocator.getModelInteractionService().getCompiledGuiProfile(null, result);
            CompiledObjectCollectionView view = guiConfig.findObjectCollectionView(type, collectionViewName);
            if (view != null) {
                if (Search.PanelType.MEMBER_PANEL.equals(panelType) && view.getAdditionalPanels() != null
                        && view.getAdditionalPanels().getMemberPanel() != null) {
                    return view.getAdditionalPanels().getMemberPanel().getSearchBoxConfiguration();
                }
                return view.getSearchBoxConfiguration();
            }
            return null;
        } catch (SchemaException | ObjectNotFoundException | CommunicationException
                | ConfigurationException | SecurityViolationException | ExpressionEvaluationException ex) {
            throw new SystemException(ex);
        }
    }

    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getSearchableDefinitionMap(
            PrismContainerDefinition<C> containerDef, ModelServiceLocator modelServiceLocator) {
        return getSearchableDefinitionMap(containerDef, modelServiceLocator, true);
    }

    /**
         *
         * @param containerDef
         * @param useDefsFromSuperclass leave it here for a while; seems that it is always true
         * @param <C>
         * @return
         */
    public static <C extends Containerable> Map<ItemPath, ItemDefinition<?>> getSearchableDefinitionMap(
            PrismContainerDefinition<C> containerDef, ModelServiceLocator modelServiceLocator, boolean useDefsFromSuperclass) {

        Map<ItemPath, ItemDefinition<?>> searchableDefinitions = new HashMap<>();

        if (containerDef == null) {
            return searchableDefinitions;
        }
        PrismContainerDefinition ext = containerDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext != null && ext.getDefinitions() != null) {
            List<ItemDefinition<?>> defs = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(def -> (def instanceof PrismReferenceDefinition || def instanceof PrismPropertyDefinition)
                            && isIndexed(def)).collect(Collectors.toList());
            defs.forEach(def -> searchableDefinitions.put(ItemPath.create(ObjectType.F_EXTENSION, def.getItemName()), def));
        }
        Class<C> typeClass = containerDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = PredefinedSearchableItems.getAvailableSearchableItems(typeClass, modelServiceLocator);
            if (paths != null) {
                for (ItemPath path : paths) {
                    ItemDefinition<?> def = containerDef.findItemDefinition(path);
                    if (def != null) {
                        searchableDefinitions.put(path, def);
                    }
                }
            }
            if (!useDefsFromSuperclass) {
                break;
            }

            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return searchableDefinitions;
    }



    @Deprecated
    public static <C extends Containerable> void addSearchRefDef(
            PrismContainerDefinition<C> containerDef, ItemPath path,
            List<SearchItemDefinition> defs, AreaCategoryType category, PageBase pageBase) {
        PrismReferenceDefinition refDef = containerDef.findReferenceDefinition(path);
        if (refDef == null) {
            return;
        }
        if (pageBase == null) {
            defs.add(new SearchItemDefinition(path, refDef,
                    Collections.singletonList(WebComponentUtil.getDefaultRelationOrFail())));
            return;
        }
        defs.add(new SearchItemDefinition(path, refDef,
                WebComponentUtil.getCategoryRelationChoices(category, pageBase)));
    }

    public static <C extends Containerable> void addShadowAttributeSearchItemWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs, ModelServiceLocator modelServiceLocator) {
//        addSearchPropertyWrapper(containerDef, path, defs, null, modelServiceLocator);
    }


    private static boolean isIndexed(ItemDefinition def) {
        if (!(def instanceof PrismPropertyDefinition)) {
            return true;
        }

        PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) def;
        Boolean indexed = propertyDef.isIndexed();
        if (indexed == null) {
            return true;
        }

        return indexed.booleanValue();
    }
}
