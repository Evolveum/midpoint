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
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class SearchFactory {

    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String LOAD_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "loadAdminGuiConfiguration";

    public static <C extends Containerable> Search<C> createMemberSearch(Class<C> type, List<QName> supportedTypes, List<QName> supportedRelations,
            QName abstractRoleType, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultOrgMembersSearchBoxConfiguration(type, supportedTypes, supportedRelations, modelServiceLocator);
        return createSearch(type, abstractRoleType, defaultSearchBox, collectionView, modelServiceLocator);
    }
    public static <C extends Containerable> Search<C> createSearch(Class<C> type, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultSearchBoxConfiguration(type, Arrays.asList(ObjectType.F_EXTENSION), null, modelServiceLocator);
        return createSearch(type, null, defaultSearchBox, collectionView, modelServiceLocator);
    }

    private static <C extends Containerable> Search<C> createSearch(Class<C> type, QName abstractRoleType, SearchBoxConfigurationType defaultSearchBoxConfig, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType configuredSearchBox = getConfiguredSearchBox(collectionView);
        SearchBoxConfigurationType mergedConfig = SearchConfigurationMerger.mergeConfigurations(defaultSearchBoxConfig, configuredSearchBox);

        SearchConfigurationWrapper<C> basicSearchWrapper = createBasicSearchWrapper(type, collectionView, mergedConfig, modelServiceLocator);
        if (abstractRoleType != null) {
            createAbstractRoleSearchItemWrapperList(abstractRoleType, basicSearchWrapper, mergedConfig);
        }
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

        return search;
    }

    public static <C extends Containerable> Search<C> createAssignmnetSearch(Class<C> type, QName targetType, CompiledObjectCollectionView collectionView, ModelServiceLocator modelServiceLocator) {


        var targetExtensionPath = ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(targetType), ObjectType.F_EXTENSION);
        SearchBoxConfigurationType defaultSearchBox = SearchBoxConfigurationUtil.getDefaultSearchBoxConfiguration(type, Arrays.asList(ObjectType.F_EXTENSION, targetExtensionPath), null, modelServiceLocator);
        SearchBoxConfigurationType configuredSearchBox = getConfiguredSearchBox(collectionView);
        SearchBoxConfigurationType mergedConfig = SearchConfigurationMerger.mergeConfigurations(defaultSearchBox, configuredSearchBox);

        SearchConfigurationWrapper<C> basicSearchWrapper = createBasicSearchWrapper(type, collectionView, mergedConfig, modelServiceLocator);

        //TODO
//        if (targetType == null) {
//            SearchFactory.addSearchRefWrapper(containerDef, ItemPath.create(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
//            SearchFactory.addSearchRefWrapper(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
//            SearchFactory.addSearchPropertyWrapper(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs,
//                    "AssignmentPanel.search.policyRule.name", getPageBase());
//            SearchFactory.addSearchRefWrapper(containerDef,
//                    ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
//                            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
//        }
//        if (ResourceType.COMPLEX_TYPE.equals(targetType)) {
//            SearchFactory.addSearchRefWrapper(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
//        }
//        if (PolicyRuleType.COMPLEX_TYPE.equals(targetType)) {
//            SearchFactory.addSearchPropertyWrapper(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
//                    defs, getPageBase());
//            SearchFactory.addSearchPropertyWrapper(containerDef, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
//                    defs, getPageBase());
//            SearchFactory.addSearchPropertyWrapper(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME),
//                    defs, "AssignmentPanel.search.policyRule.name", getPageBase());
//            SearchFactory.addSearchRefWrapper(containerDef,
//                    ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
//                            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
//
//            defs.addAll(SearchFactory.createSearchableExtensionWrapperList(containerDef, getPageBase()));
//        }
//        addSearchPropertyWrapper(AssignmentType.class, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), modelServiceLocator);
//        addSearchPropertyWrapper(AssignmentType.class, ItemPath.create(AssignmentType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
//                 modelServiceLocator));


        AxiomQueryWrapper<C> axiomWrapper = new AxiomQueryWrapper<>();
        AdvancedQueryWrapper advancedQueryWrapper = new AdvancedQueryWrapper();
        FulltextQueryWrapper fulltextQueryWrapper = new FulltextQueryWrapper();



        Search<C> search = new Search<>(type, mergedConfig);
        search.setTypeClass(type);
        search.setAdvancedQueryWrapper(advancedQueryWrapper);
        search.setAxiomQueryWrapper(axiomWrapper);
        search.setFulltextQueryWrapper(fulltextQueryWrapper);
        search.setSearchConfigurationWrapper(basicSearchWrapper);
        search.setSearchMode(getDefaultSearchMode(mergedConfig, modelServiceLocator, type));
        search.setAllowedModeList(createAllowedModeList(type, modelServiceLocator));
        search.setCollectionViewName(collectionView.getViewIdentifier());

//        if (view != null
//                && view.getCollection() != null
//                && view.getCollection().getCollectionRef() != null
//                && QNameUtil.match(ObjectCollectionType.COMPLEX_TYPE, view.getCollection().getCollectionRef().getType())) {
//            searchWrapper.setCollectionRefOid(view.getCollection().getCollectionRef().getOid());
//        }
        if (collectionView != null && collectionView.getCollection() != null && collectionView.getCollection().getCollectionRef() != null) {
            search.setCollectionRefOid(collectionView.getCollection().getCollectionRef().getOid());
        }

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

    public static <C extends Containerable> Search<ShadowType> createProjectionsTabSearch(ModelServiceLocator modelServiceLocator) {
        Search<ShadowType> search = createSearch(ShadowType.class, modelServiceLocator);
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
        return search;
    }

    private static <C extends Containerable> List<SearchBoxModeType> createAllowedModeList(Class<C> type, ModelServiceLocator modelServiceLocator) {
        List<SearchBoxModeType> allowedModeTypes = Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY);
        if (isFullTextSearchEnabled(modelServiceLocator, (Class<? extends ObjectType>) type)) {
            allowedModeTypes.add(SearchBoxModeType.FULLTEXT);
        }
        return allowedModeTypes;
    }

    public static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            SearchItemType item, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        ItemPath itemPath = null;
        ItemDefinition<?> itemDef = null;
        if (item.getPath() != null) {
            PrismContainerDefinition<C> def;
            if (ObjectType.class.isAssignableFrom(type) && modelServiceLocator != null) {
                def = PredefinedSearchableItems.findObjectDefinition((Class<? extends ObjectType>) type, coordinates, modelServiceLocator);
            } else {
                def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
            }
            itemDef = def.findItemDefinition(item.getPath().getItemPath());
        }
        if (item.getPath() != null) {
            itemPath = item.getPath().getItemPath();
        }
        if (itemDef == null && !hasParameter(item) && item.getFilter() == null) {
            return null;
        }
        List<DisplayableValue<?>> availableValues = getSearchItemAvailableValues(item, itemDef, modelServiceLocator);
        QName valueTypeName = getSearchItemValueTypeName(item, itemDef);
        LookupTableType lookupTable = getSearchItemLookupTable(item, itemDef, modelServiceLocator);

        PropertySearchItemWrapper<?> searchItemWrapper =
                createPropertySearchItemWrapper(
                        type, itemDef, itemPath, valueTypeName, availableValues, lookupTable, item.getFilter());

        searchItemWrapper.setVisible(BooleanUtils.isTrue(item.isVisibleByDefault()) || hasParameter(item));
        searchItemWrapper.setValueTypeName(valueTypeName);

        searchItemWrapper.setName(getSearchItemName(item, itemDef));
        searchItemWrapper.setHelp(getSearchItemHelp(item, itemDef));

        if (hasParameter(item)) {
            searchItemWrapper.setParameterName(item.getParameter().getName());
            if (item.getParameter().getType() != null) {
                searchItemWrapper.setParameterValueType(PrismContext.get().getSchemaRegistry().determineClassForType(item.getParameter().getType()));
            }
            if (searchItemWrapper instanceof DateSearchItemWrapper) {
                ((DateSearchItemWrapper) searchItemWrapper).setInterval(false);
            }
        }

        if (item.isVisibleByDefault() != null) {
            searchItemWrapper.setVisible(item.isVisibleByDefault());
        }
        if (item.getFilter() != null) {
            searchItemWrapper.setPredefinedFilter(item.getFilter());
            searchItemWrapper.setVisible(true);
            searchItemWrapper.setApplyFilter(true);
            searchItemWrapper.setFilterExpression(item.getFilterExpression());
        }
        return searchItemWrapper;
    }

    private static String getSearchItemName(SearchItemType searchItem, ItemDefinition<?> itemDef) {
        String name = null;
        if (searchItem.getDisplayName() != null) {
            name = WebComponentUtil.getTranslatedPolyString(searchItem.getDisplayName());
        }
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(searchItem.getDisplay()));
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = WebComponentUtil.getItemDefinitionDisplayNameOrName(itemDef, null);
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        return hasParameter(searchItem) ? searchItem.getParameter().getName() : "";
    }

    private static String getSearchItemHelp(SearchItemType searchItem, ItemDefinition<?> itemDef) {
        String help = GuiDisplayTypeUtil.getHelp(searchItem.getDisplay());
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        if (itemDef !=null) {
            help = WebPrismUtil.getHelpText(itemDef);
            if (StringUtils.isNotBlank(help)) {
                Pattern pattern = Pattern.compile("<.+?>");
                Matcher m = pattern.matcher(help);
                help = m.replaceAll("");
            }
            if (StringUtils.isNotEmpty(help)) {
                return help;
            }
        }
        return hasParameter(searchItem) ? GuiDisplayTypeUtil.getHelp(searchItem.getParameter().getDisplay()) : "";
    }

    private static boolean hasParameter(SearchItemType searchItem) {
        return searchItem != null && searchItem.getParameter() != null;
    }

    private static List<DisplayableValue<?>> getSearchItemAvailableValues(SearchItemType searchItem, ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def instanceof PrismPropertyDefinition<?>) {
            return CollectionUtils.isNotEmpty(((PrismPropertyDefinition<?>)def).getAllowedValues()) ?
                    (List<DisplayableValue<?>>) ((PrismPropertyDefinition<?>)def).getAllowedValues()
                    : getAllowedValues(ItemPath.create(def.getItemName()));
        }
        if (hasParameter(searchItem)) {
            SearchFilterParameterType parameter = searchItem.getParameter();
            return WebComponentUtil.getAllowedValues(parameter.getAllowedValuesExpression(), modelServiceLocator);
        }
        return new ArrayList<>();
    }

    private static LookupTableType getSearchItemLookupTable(SearchItemType searchItem, ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(def, (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        if (hasParameter(searchItem) && searchItem.getParameter().getAllowedValuesLookupTable() != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(
                    searchItem.getParameter().getAllowedValuesLookupTable().asReferenceValue(), (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        return null;
    }

    private static QName getSearchItemValueTypeName(SearchItemType searchItem, ItemDefinition<?> def) {
        if (def != null) {
            return def.getTypeName();
        }
        if (hasParameter(searchItem)) {
            return searchItem.getParameter().getType();
        }
        return null;
    }

    private static <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            ItemDefinition<?> itemDef, ItemPath path, QName valueTypeName, List<DisplayableValue<?>> availableValues,
            LookupTableType lookupTable, SearchFilterType predefinedFilter) {

        if (CollectionUtils.isNotEmpty(availableValues)) {
            return new ChoicesSearchItemWrapper(path, availableValues);
        }
        if (lookupTable != null) {
            return new AutoCompleteSearchItemWrapper(path, lookupTable);
        }
        if (itemDef instanceof PrismReferenceDefinition) {
            ReferenceSearchItemWrapper itemWrapper = new ReferenceSearchItemWrapper((PrismReferenceDefinition)itemDef, type);
            itemWrapper.setName(WebComponentUtil.getItemDefinitionDisplayNameOrName(itemDef, null));
            return itemWrapper;
        }
        if (path != null) {
            if (ShadowType.F_OBJECT_CLASS.equivalent(path)) {
                return new ObjectClassSearchItemWrapper();
            } else if (ShadowType.F_DEAD.equivalent(path)) {
                DeadShadowSearchItemWrapper deadWrapper = new DeadShadowSearchItemWrapper(Arrays.asList(new SearchValue<>(true), new SearchValue<>(false)));
                deadWrapper.setValue(new SearchValue(false));
                return deadWrapper;
            }
        }
        if (valueTypeName != null) {
            if (DOMUtil.XSD_BOOLEAN.equals(valueTypeName)) {
                List<DisplayableValue<Boolean>> list = new ArrayList<>();
                list.add(new SearchValue<>(Boolean.TRUE, "Boolean.TRUE"));
                list.add(new SearchValue<>(Boolean.FALSE, "Boolean.FALSE"));
                return new ChoicesSearchItemWrapper(path, list);
            } else if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, valueTypeName)) {
                return new ItemPathSearchItemWrapper(path);
            } else if (QNameUtil.match(valueTypeName, DOMUtil.XSD_DATETIME)) {
                return new DateSearchItemWrapper(path);
            }
        }
        if (itemDef != null && itemDef.getValueEnumerationRef() != null) {
            return new TextSearchItemWrapper(
                    path,
                    itemDef,
                    itemDef.getValueEnumerationRef().getOid(),
                    itemDef.getValueEnumerationRef().getTargetType());
        }

        if (path != null) {
            return new TextSearchItemWrapper(path, itemDef);
        }
        return new TextSearchItemWrapper();
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

//        if (config.getRelationConfiguration() != null) {
//            RelationSearchItemConfigurationType relationConfiguration = config.getRelationConfiguration();
//            RelationSearchItemWrapper relation = new RelationSearchItemWrapper(relationConfiguration);
//            if (relationConfiguration.getDisplay() != null) {
//                relation.setName(WebComponentUtil.getTranslatedPolyString(relationConfiguration.getDisplay().getLabel()));
//                relation.setHelp(WebComponentUtil.getTranslatedPolyString(relationConfiguration.getDisplay().getHelp()));
//            }
//            if (relationConfiguration.getVisibility() != null) {
//                relation.setVisible(WebComponentUtil.getElementVisibility(relationConfiguration.getVisibility()));
//            }
//            if (relationConfiguration.getSupportedRelations() != null) {
//                relation.
//            }
//            searchConfigWrapper.getItemsList().add(relation);
//        }
//
//        if (config.getIndirectConfiguration() != null) {
//            IndirectSearchItemWrapper indirect = new IndirectSearchItemWrapper(searchConfigWrapper);
//            if (config.getIndirectConfiguration().getDisplay() != null) {
//                indirect.setName(WebComponentUtil.getTranslatedPolyString(config.getIndirectConfiguration().getDisplay().getLabel()));
//                indirect.setHelp(WebComponentUtil.getTranslatedPolyString(config.getIndirectConfiguration().getDisplay().getHelp()));
//            }
//            if (config.getIndirectConfiguration().getVisibility() != null) {
//                indirect.setVisible(WebComponentUtil.getElementVisibility(config.getIndirectConfiguration().getVisibility()));
//            }
//            searchConfigWrapper.getItemsList().add(indirect);
//        }
//
//        if (config.getScopeConfiguration() != null) {
//            ScopeSearchItemWrapper scope = new ScopeSearchItemWrapper(searchConfigWrapper);
//            if (config.getScopeConfiguration().getDisplay() != null) {
//                scope.setName(WebComponentUtil.getTranslatedPolyString(config.getScopeConfiguration().getDisplay().getLabel()));
//                scope.setHelp(WebComponentUtil.getTranslatedPolyString(config.getScopeConfiguration().getDisplay().getHelp()));
//            }
//            if (config.getScopeConfiguration().getVisibility() != null) {
//                scope.setVisible(WebComponentUtil.getElementVisibility(config.getScopeConfiguration().getVisibility()));
//            }
//            searchConfigWrapper.getItemsList().add(scope);
//        }
//        if (config.getProjectConfiguration() != null) {
//            ProjectSearchItemWrapper project = new ProjectSearchItemWrapper(searchConfigWrapper);
//            if (config.getProjectConfiguration().getDisplay() != null) {
//                project.setName(WebComponentUtil.getTranslatedPolyString(config.getProjectConfiguration().getDisplay().getLabel()));
//                project.setHelp(WebComponentUtil.getTranslatedPolyString(config.getProjectConfiguration().getDisplay().getHelp()));
//            }
//            if (config.getProjectConfiguration().getVisibility() != null) {
//                project.setVisible(WebComponentUtil.getElementVisibility(config.getProjectConfiguration().getVisibility()));
//            }
//            searchConfigWrapper.getItemsList().add(project);
//        }
//        if (config.getTenantConfiguration() != null) {
//            TenantSearchItemWrapper tenant = new TenantSearchItemWrapper(searchConfigWrapper);
//            if (config.getTenantConfiguration().getDisplay() != null) {
//                tenant.setName(WebComponentUtil.getTranslatedPolyString(config.getTenantConfiguration().getDisplay().getLabel()));
//                tenant.setHelp(WebComponentUtil.getTranslatedPolyString(config.getTenantConfiguration().getDisplay().getHelp()));
//            }
//            if (config.getTenantConfiguration().getVisibility() != null) {
//                tenant.setVisible(WebComponentUtil.getElementVisibility(config.getTenantConfiguration().getVisibility()));
//            }
//            searchConfigWrapper.getItemsList().add(tenant);
//        }
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
            searchConfigWrapper.getItemsList().add(createPropertySearchItemWrapper(type, searchItem,  null, modelServiceLocator));
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

    private static List<DisplayableValue<?>> getAllowedValues(ItemPath path) {
        if (AuditEventRecordType.F_CHANNEL.equivalent(path)) {
            List<DisplayableValue<?>> list = new ArrayList<>();
            for (GuiChannel channel : GuiChannel.values()) {
                list.add(new SearchValue<>(channel.getUri(), channel.getLocalizationKey()));
            }
            return list;
        }
        return null;
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

    public static <C extends Containerable> List<SearchItemDefinition> getAvailableDefinitions(
            PrismContainerDefinition<C> objectDef, List<ItemPath> availableItemPath, boolean useDefsFromSuperclass, ModelServiceLocator modelServiceLocator) {
        List<SearchItemDefinition> definitions = new ArrayList<>();

        if (objectDef == null) {
            return definitions;
        }

        definitions.addAll(createExtensionDefinitionList(objectDef));

        Class<C> typeClass = objectDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = CollectionUtils.isEmpty(availableItemPath) ? PredefinedSearchableItems.getAvailableSearchableItems(typeClass, modelServiceLocator) : availableItemPath;
            if (paths != null) {
                for (ItemPath path : paths) {
                    ItemDefinition<?> def = objectDef.findItemDefinition(path);
                    if (def != null) {
                        SearchItemDefinition searchItemDef = new SearchItemDefinition(path, def, getAllowedValues(path));
                        definitions.add(searchItemDef);
                    }
                }
            }

            if (!useDefsFromSuperclass) {
                break;
            }

            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return definitions;
    }
    @Deprecated
    public static <C extends Containerable> List<SearchItemDefinition> createExtensionDefinitionList(
            PrismContainerDefinition<C> objectDef) {

        List<SearchItemDefinition> searchItemDefinitions = new ArrayList<>();

        ItemPath extensionPath = ObjectType.F_EXTENSION;

        PrismContainerDefinition ext = objectDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext == null) {
            return searchItemDefinitions;
        }

        //noinspection unchecked
        for (ItemDefinition<?> def : (List<ItemDefinition<?>>) ext.getDefinitions()) {
            ItemPath itemPath = ItemPath.create(extensionPath, def.getItemName());

            if (!isIndexed(def)) {
                continue;
            }

            if (def instanceof PrismPropertyDefinition) {
                addSearchPropertyDef(objectDef, itemPath, searchItemDefinitions);
            }

            if (def instanceof PrismReferenceDefinition) {
                addSearchRefDef(objectDef, itemPath, searchItemDefinitions, null, null);
            }

            if (!(def instanceof PrismPropertyDefinition)
                    && !(def instanceof PrismReferenceDefinition)) {
                continue;
            }
        }

        return searchItemDefinitions;
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

    @Deprecated
    public static <C extends Containerable> void addSearchPropertyDef(
            PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs) {
        addSearchPropertyDef(containerDef, path, defs, null);
    }

    @Deprecated
    public static <C extends Containerable> void addSearchPropertyDef(
            PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs, String key) {
        PrismPropertyDefinition propDef = containerDef.findPropertyDefinition(path);
        if (propDef == null) {
            return;
        }
        SearchItemDefinition searchItem = new SearchItemDefinition(path, propDef, getAllowedValues(path));
        if (key != null) {
            PolyStringType displayName = new PolyStringType(propDef.getItemName().getLocalPart());
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setFallback(propDef.getItemName().getLocalPart());
            translation.setKey(key);
            displayName.setTranslation(translation);
            searchItem.setDisplayName(displayName);
        }
        defs.add(searchItem);
    }

    public static <C extends Containerable> void addSearchRefWrapper(PrismContainerDefinition<C> containerDef, ItemPath path,
            List<? super AbstractSearchItemWrapper> defs, AreaCategoryType category, PageBase pageBase) {
        PrismReferenceDefinition refDef = containerDef.findReferenceDefinition(path);
        if (refDef == null) {
            return;
        }
        ReferenceSearchItemWrapper item = (ReferenceSearchItemWrapper) createPropertySearchItemWrapper(containerDef.getCompileTimeClass(),
                createSearchItem(containerDef.getCompileTimeClass(), path), null, pageBase);
        if (pageBase == null) {
            item.getAvailableValues().addAll(Collections.singletonList(WebComponentUtil.getDefaultRelationOrFail()));
            return;
        }
        item.getAvailableValues().addAll(Collections.singletonList(WebComponentUtil.getCategoryRelationChoices(category, pageBase)));
        defs.add(item);
    }

    public static <C extends Containerable> void addShadowAttributeSearchItemWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs, ModelServiceLocator modelServiceLocator) {
        addSearchPropertyWrapper(containerDef, path, defs, null, modelServiceLocator);
    }

    public static <C extends Containerable> void addSearchPropertyWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs, ModelServiceLocator modelServiceLocator) {
        addSearchPropertyWrapper(containerDef, path, defs, null, modelServiceLocator);
    }

    public static <C extends Containerable> void addSearchPropertyWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs, String key, ModelServiceLocator modelServiceLocator) {
        PrismPropertyDefinition propDef = containerDef.findPropertyDefinition(path);
        if (propDef == null) {
            return;
        }
        PropertySearchItemWrapper item = createPropertySearchItemWrapper(containerDef.getCompileTimeClass(),
                createSearchItem(containerDef.getCompileTimeClass(), propDef.getItemName()), null, modelServiceLocator);
        if (key != null) {
            PolyStringType displayName = new PolyStringType(propDef.getItemName().getLocalPart());
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setFallback(propDef.getItemName().getLocalPart());
            translation.setKey(key);
            displayName.setTranslation(translation);
            item.setName(WebComponentUtil.getTranslatedPolyString(displayName));
        }
        defs.add(item);
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
