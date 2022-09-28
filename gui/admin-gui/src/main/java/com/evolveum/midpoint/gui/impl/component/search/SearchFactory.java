/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.web.component.search.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

public class SearchFactory {

    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String LOAD_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "loadAdminGuiConfiguration";

    private static final Map<Class<?>, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();

    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME),
                ItemPath.create(ObjectType.F_LIFECYCLE_STATE),
                ItemPath.create(ObjectType.F_SUBTYPE),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP),
                ItemPath.create(ObjectType.F_METADATA, MetadataType.F_MODIFY_TIMESTAMP)
        ));
        SEARCHABLE_OBJECTS.put(FocusType.class, Arrays.asList(
                ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_FROM),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_VALID_TO)
        ));
        SEARCHABLE_OBJECTS.put(UserType.class, Arrays.asList(
                ItemPath.create(UserType.F_TITLE),
                ItemPath.create(UserType.F_GIVEN_NAME),
                ItemPath.create(UserType.F_FAMILY_NAME),
                ItemPath.create(UserType.F_FULL_NAME),
                ItemPath.create(UserType.F_ADDITIONAL_NAME),
                ItemPath.create(UserType.F_COST_CENTER),
                ItemPath.create(UserType.F_EMAIL_ADDRESS),
                ItemPath.create(UserType.F_TELEPHONE_NUMBER),
                ItemPath.create(UserType.F_EMPLOYEE_NUMBER),
                ItemPath.create(UserType.F_ORGANIZATIONAL_UNIT),
                ItemPath.create(UserType.F_LOCALITY)
        ));
        SEARCHABLE_OBJECTS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME)
        ));
        SEARCHABLE_OBJECTS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME),
                ItemPath.create(ServiceType.F_URL)
        ));
        SEARCHABLE_OBJECTS.put(ConnectorHostType.class, Arrays.asList(
                ItemPath.create(ConnectorHostType.F_HOSTNAME)
        ));
        SEARCHABLE_OBJECTS.put(ConnectorType.class, Arrays.asList(
                ItemPath.create(ConnectorType.F_CONNECTOR_BUNDLE),
                ItemPath.create(ConnectorType.F_CONNECTOR_VERSION),
                ItemPath.create(ConnectorType.F_CONNECTOR_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(AbstractRoleType.class, Arrays.asList(
                ItemPath.create(AbstractRoleType.F_IDENTIFIER),
                ItemPath.create(AbstractRoleType.F_REQUESTABLE)
        ));
        SEARCHABLE_OBJECTS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_DISPLAY_NAME),
                ItemPath.create(OrgType.F_COST_CENTER),
                ItemPath.create(OrgType.F_TENANT),
                ItemPath.create(OrgType.F_PARENT_ORG_REF),
                ItemPath.create(OrgType.F_LOCALITY)
        ));
//        SEARCHABLE_OBJECTS.put(GenericObjectType.class, Arrays.asList(
//                ItemPath.create(GenericObjectType.F_OBJECT_TYPE)
//        ));
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
        ));
//        SEARCHABLE_OBJECTS.put(ReportType.class, Arrays.asList(
//                ItemPath.create(ReportType.F_NAME)
//        ));
        SEARCHABLE_OBJECTS.put(ShadowType.class, Arrays.asList(
                ItemPath.create(ShadowType.F_OBJECT_CLASS),
                ItemPath.create(ShadowType.F_RESOURCE_REF),
                ItemPath.create(ShadowType.F_DEAD),
                ItemPath.create(ShadowType.F_INTENT),
                ItemPath.create(ShadowType.F_EXISTS),
                ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION)
        ));
        SEARCHABLE_OBJECTS.put(TaskType.class, Arrays.asList(
                ItemPath.create(TaskType.F_TASK_IDENTIFIER),
                ItemPath.create(TaskType.F_NODE),
                ItemPath.create(TaskType.F_CATEGORY),
                ItemPath.create(TaskType.F_RESULT_STATUS),
                ItemPath.create(TaskType.F_EXECUTION_STATE),
                ItemPath.create(TaskType.F_HANDLER_URI),
                ItemPath.create(TaskType.F_OBJECT_REF)

        ));

        SEARCHABLE_OBJECTS.put(AssignmentType.class, Arrays.asList(
                ItemPath.create(AssignmentType.F_TARGET_REF),
                ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF),
                ItemPath.create(AssignmentType.F_TENANT_REF),
                ItemPath.create(AssignmentType.F_ORG_REF)
        ));

        SEARCHABLE_OBJECTS.put(CaseWorkItemType.class, Arrays.asList(
                ItemPath.create(CaseWorkItemType.F_ASSIGNEE_REF),
                ItemPath.create(CaseWorkItemType.F_ORIGINAL_ASSIGNEE_REF),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_STATE),
                ItemPath.create(PrismConstants.T_PARENT, CaseType.F_OBJECT_REF),
                ItemPath.create(CaseWorkItemType.F_PERFORMER_REF)
        ));

        SEARCHABLE_OBJECTS.put(CaseType.class, Arrays.asList(
                ItemPath.create(CaseType.F_STATE),
                ItemPath.create(CaseType.F_PARENT_REF),
                ItemPath.create(CaseType.F_REQUESTOR_REF),
                ItemPath.create(CaseType.F_TARGET_REF),
                ItemPath.create(CaseType.F_TASK_REF),
                ItemPath.create(CaseType.F_OBJECT_REF)
        ));

        SEARCHABLE_OBJECTS.put(ObjectPolicyConfigurationType.class, Arrays.asList(
                ItemPath.create(ObjectPolicyConfigurationType.F_SUBTYPE),
                ItemPath.create(ObjectPolicyConfigurationType.F_OBJECT_TEMPLATE_REF)
        ));

        SEARCHABLE_OBJECTS.put(AuditEventRecordType.class, Arrays.asList(
                ItemPath.create(AuditEventRecordType.F_TIMESTAMP),
                ItemPath.create(AuditEventRecordType.F_INITIATOR_REF),
                ItemPath.create(AuditEventRecordType.F_EVENT_STAGE),
                ItemPath.create(AuditEventRecordType.F_EVENT_TYPE),
                ItemPath.create(AuditEventRecordType.F_TARGET_REF),
                ItemPath.create(AuditEventRecordType.F_TARGET_OWNER_REF),
                ItemPath.create(AuditEventRecordType.F_CHANGED_ITEM),
                ItemPath.create(AuditEventRecordType.F_OUTCOME),
                ItemPath.create(AuditEventRecordType.F_CHANNEL),
                ItemPath.create(AuditEventRecordType.F_HOST_IDENTIFIER),
                ItemPath.create(AuditEventRecordType.F_REQUEST_IDENTIFIER),
                ItemPath.create(AuditEventRecordType.F_REFERENCE),
                ItemPath.create(AuditEventRecordType.F_TASK_IDENTIFIER)
        ));
    }

    private static final Map<Class<?>, List<ItemPath>> FIXED_SEARCH_ITEMS = new HashMap<>();
    static {
        FIXED_SEARCH_ITEMS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME))
        );
        FIXED_SEARCH_ITEMS.put(UserType.class, Arrays.asList(
                ItemPath.create(UserType.F_GIVEN_NAME),
                ItemPath.create(UserType.F_FAMILY_NAME)
        ));
        FIXED_SEARCH_ITEMS.put(AbstractRoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_DISPLAY_NAME)
        ));
        FIXED_SEARCH_ITEMS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_IDENTIFIER)
        ));
        FIXED_SEARCH_ITEMS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_IDENTIFIER)
        ));
        FIXED_SEARCH_ITEMS.put(OrgType.class, Arrays.asList(
                ItemPath.create(OrgType.F_PARENT_ORG_REF)
        ));
        FIXED_SEARCH_ITEMS.put(AuditEventRecordType.class, Arrays.asList(
                ItemPath.create(AuditEventRecordType.F_TIMESTAMP)
        ));
        FIXED_SEARCH_ITEMS.put(ShadowType.class, Arrays.asList(
                ItemPath.create(ShadowType.F_RESOURCE_REF),
                ItemPath.create(ShadowType.F_OBJECT_CLASS)
        ));
    }

    public static <C extends Containerable> com.evolveum.midpoint.web.component.search.Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ModelServiceLocator modelServiceLocator) {
        return createContainerSearch(type, null, modelServiceLocator, false);
    }

    public static <C extends Containerable> com.evolveum.midpoint.web.component.search.Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ItemPath defaultSearchItem,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {
        return createContainerSearch(type, defaultSearchItem, null, modelServiceLocator, useObjectCollection);
    }

    public static <C extends Containerable> com.evolveum.midpoint.web.component.search.Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ItemPath defaultSearchItem, List<SearchItemDefinition> defaultAvailableDefs,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {
        PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(type.getTypeClass());
        return createContainerSearch(type, containerDef, defaultSearchItem, defaultAvailableDefs, modelServiceLocator, useObjectCollection);
    }

    @Deprecated
    public static <C extends Containerable> com.evolveum.midpoint.web.component.search.Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, PrismContainerDefinition<C> containerDef, ItemPath defaultSearchItem, List<SearchItemDefinition> defaultAvailableDefs,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {

        List<SearchItemDefinition> availableDefs = defaultAvailableDefs;
        if (CollectionUtils.isEmpty(defaultAvailableDefs)) {
            availableDefs = getAvailableDefinitions(containerDef, null, true, modelServiceLocator);
        }

        com.evolveum.midpoint.web.component.search.Search<C> search = new com.evolveum.midpoint.web.component.search.Search<>(type, availableDefs);

        List<SearchItemDefinition> configuredSearchItemDefs = null;
        if (useObjectCollection) {
            configuredSearchItemDefs = getConfiguredSearchItemDefinitions(availableDefs, modelServiceLocator, containerDef.getTypeName(), null, Search.PanelType.DEFAULT);
        }
        if (!CollectionUtils.isEmpty(configuredSearchItemDefs)) {
            processSearchItemDefFromCompiledView(configuredSearchItemDefs, search, containerDef);
        } else if (defaultSearchItem != null) {
            ItemDefinition defaultItemDef = containerDef.findItemDefinition(defaultSearchItem);
            if (defaultItemDef != null) {
                search.addItem(defaultItemDef);
            }
        }
        return search;
    }

    public static <C extends Containerable> com.evolveum.midpoint.web.component.search.Search<C> createSearchForReport(Class<C> type, List<SearchFilterParameterType> parameters, ModelServiceLocator modelServiceLocator) {
        ContainerTypeSearchItem<C> typeItem = new ContainerTypeSearchItem<>(new SearchValue<>(type, ""));
        com.evolveum.midpoint.web.component.search.Search<C> search = new com.evolveum.midpoint.web.component.search.Search<>(typeItem, new ArrayList<>(), false, SearchBoxModeType.BASIC, Collections.singletonList(SearchBoxModeType.BASIC), false);

        SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
        PrismContainerDefinition objDef = registry.findContainerDefinitionByCompileTimeClass(type);

        final List<SearchItemDefinition> configuredSearchItemDefs = new ArrayList<>();
        parameters.forEach(parameter -> {
            SearchItemType searchItemType = new SearchItemType();
            searchItemType.setParameter(parameter);
            searchItemType.setVisibleByDefault(true);
            if (parameter.getDisplay() != null) {
                if (parameter.getDisplay().getLabel() != null) {
                    searchItemType.setDisplayName(parameter.getDisplay().getLabel());
                } else {
                    searchItemType.setDisplayName(new PolyStringType(parameter.getName()));
                }
                if (parameter.getDisplay().getHelp() != null) {
                    searchItemType.setDescription(
                            modelServiceLocator.getLocalizationService().translate(parameter.getDisplay().getHelp().toPolyString()));
                }
            }
            configuredSearchItemDefs.add(new SearchItemDefinition(searchItemType));
        });
        processSearchItemDefFromCompiledView(configuredSearchItemDefs, search, objDef);
        search.setCanConfigure(false);
        return search;
    }

    private static void processSearchItemDefFromCompiledView(List<SearchItemDefinition> configuredSearchItemDefs, com.evolveum.midpoint.web.component.search.Search search, PrismContainerDefinition containerDef) {
        configuredSearchItemDefs.forEach(searchItemDef -> {
            search.addItemToAllDefinitions(searchItemDef);
            if (searchItemDef.isVisibleByDefault()) {
                SearchItem item = null;
                if (searchItemDef.getPath() != null) {
                    ItemDefinition def = containerDef.findItemDefinition(searchItemDef.getPath());
                    item = search.addItem(def);
                    ((PropertySearchItem) item).setDisplayName(searchItemDef.getDisplayName());
                } else if (searchItemDef.getPredefinedFilter() != null) {
                    item = search.addItem(searchItemDef.getPredefinedFilter());
                }
                if (item != null) {
                    item.setFixed(true);
                    item.setDefinition(searchItemDef);
                }
            }
        });
    }

    public static <C extends Containerable> Search<C> createSearch(Class<C> type, ModelServiceLocator modelServiceLocator) {
        return createSearch(type, null, modelServiceLocator);
    }

    public static <C extends Containerable> Search<C> createSearch(Class<C> type, String collectionViewName,  ModelServiceLocator modelServiceLocator) {
        SearchConfigurationWrapper<C> searchConfigWrapper = createDefaultSearchBoxConfigurationWrapper(type, null, modelServiceLocator);
        SearchBoxConfigurationType config = getSearchBoxConfiguration(modelServiceLocator,
                WebComponentUtil.containerClassToQName(PrismContext.get(), type), collectionViewName, Search.PanelType.DEFAULT);
        if (config != null) {
            SearchConfigurationWrapper<C> preconfiguredSearchConfigWrapper = new SearchConfigurationWrapper<C>(type, config, modelServiceLocator);
            preconfiguredSearchConfigWrapper.getItemsList().forEach(item -> item.setVisible(true));
            searchConfigWrapper = combineSearchBoxConfiguration(searchConfigWrapper, preconfiguredSearchConfigWrapper, true);
        }
        searchConfigWrapper.setCollectionViewName(collectionViewName);
        return createSearch(searchConfigWrapper, null, modelServiceLocator, Search.PanelType.DEFAULT, false);
    }

    /**
     * use this method to create a search
     * @param searchConfig
     * @param modelServiceLocator
     * @param <C>
     * @return
     */
    public static <C extends Containerable> Search<C> createSearch(
            SearchConfigurationWrapper<C> searchConfig, ModelServiceLocator modelServiceLocator) {
        return createSearch(searchConfig, null, modelServiceLocator, Search.PanelType.DEFAULT, true);
    }

    public static <C extends Containerable> Search<C> createSearch(
            SearchConfigurationWrapper<C> searchConfig, boolean mergeWithDefaultSearchWrapper, ModelServiceLocator modelServiceLocator) {
        return createSearch(searchConfig, null, modelServiceLocator, Search.PanelType.DEFAULT, mergeWithDefaultSearchWrapper);
    }

    public static <C extends Containerable> Search<C> createSearch(
            SearchConfigurationWrapper<C> searchConfig, ModelServiceLocator modelServiceLocator, boolean combineWithDefaultConfig) {
        return createSearch(searchConfig, null, modelServiceLocator, Search.PanelType.DEFAULT, true);
    }

   public static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createMemberPanelSearch(
           SearchConfigurationWrapper<C> searchConfig, ModelServiceLocator modelServiceLocator) {
        return createSearch(searchConfig, null, modelServiceLocator, Search.PanelType.MEMBER_PANEL, true);
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

    private static <C extends Containerable> Search<C> createSearch(
            SearchConfigurationWrapper<C> searchConfigurationWrapper, ResourceShadowCoordinates coordinates,
            ModelServiceLocator modelServiceLocator, Search.PanelType panelType, boolean mergeWithDefaultSearchWrapper) {
        SearchConfigurationWrapper<C> searchConfWrapper;
        if (mergeWithDefaultSearchWrapper) {
            SearchConfigurationWrapper<C> defaultWrapper = createDefaultSearchBoxConfigurationWrapper(
                    searchConfigurationWrapper.getTypeClass(), coordinates, modelServiceLocator);
            searchConfWrapper = combineSearchBoxConfiguration(defaultWrapper, searchConfigurationWrapper);
            if (!searchConfWrapper.getAllowedModeList().contains(searchConfWrapper.getDefaultSearchBoxMode())) {
                if (searchConfWrapper.getAllowedModeList().contains(SearchBoxModeType.BASIC)) {
                    searchConfWrapper.setDefaultSearchBoxMode(SearchBoxModeType.BASIC);
                } else {
                    searchConfWrapper.setDefaultSearchBoxMode(searchConfWrapper.getAllowedModeList().get(0));
                }
            }
        } else {
            searchConfWrapper = searchConfigurationWrapper;
        }
        if (CollectionUtils.isNotEmpty(searchConfWrapper.getAllowedTypeList()) && !objectTypeSearchItemWrapperExists(searchConfWrapper.getItemsList())) {
            ObjectTypeSearchItemWrapper typeItem = new ObjectTypeSearchItemWrapper(searchConfWrapper.getAllowedTypeList(),
                    WebComponentUtil.containerClassToQName(PrismContext.get(), searchConfWrapper.getTypeClass()));
            typeItem.setAllowAllTypesSearch(searchConfWrapper.isAllowAllTypeSearch());
            searchConfWrapper.getItemsList().add(typeItem);
        }
        if (searchConfWrapper.getAllowedModeList().contains(SearchBoxModeType.OID)) {
            OidSearchItemWrapper oidWrapper = new OidSearchItemWrapper();
            oidWrapper.setVisible(true);
            searchConfWrapper.getItemsList().add(oidWrapper);
        }
        if (StringUtils.isEmpty(searchConfWrapper.getCollectionViewName())) {
            //todo we need to get saved searches here for the specified type
            List<CompiledObjectCollectionView> views = modelServiceLocator.getCompiledGuiProfile()
                    .findAllApplicableObjectCollectionViews(WebComponentUtil.containerClassToQName(PrismContext.get(), searchConfWrapper.getTypeClass()))
                    .stream()
                    .filter(v -> v.getFilter() != null)     //todo should we check also collectionRef?
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(views)) {
                ObjectCollectionListSearchItemWrapper<C> viewListItem = new ObjectCollectionListSearchItemWrapper<>(searchConfWrapper.getTypeClass(),
                        views);
                viewListItem.setVisible(true);
                searchConfWrapper.getItemsList().add(viewListItem);
            }
        }
        searchConfWrapper.getItemsList().sort((i1, i2) -> String.CASE_INSENSITIVE_ORDER.compare(
                StringUtils.isEmpty(i1.getName()) ? "" : PageBase.createStringResourceStatic(i1.getName()).getString(),
                StringUtils.isEmpty(i2.getName()) ? "" : PageBase.createStringResourceStatic(i2.getName()).getString()));

        searchConfWrapper.getItemsList().sort(Comparator.comparing(i -> i instanceof PropertySearchItemWrapper));

        Search<C> search = new Search<>(searchConfWrapper);
        QName typeQname = WebComponentUtil.containerClassToQName(PrismContext.get(), searchConfigurationWrapper.getTypeClass());
        searchConfigurationWrapper.setAllowToConfigureSearchItems(
                isAllowToConfigureSearchItems(modelServiceLocator, typeQname, searchConfigurationWrapper.getCollectionViewName(), panelType));
        return search;
    }

    public static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            SearchItemType item, ItemDefinition<?> itemDef, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        ItemPath itemPath = null;
        if (itemDef == null && item.getPath() != null) {
            PrismContainerDefinition<C> def;
            if (ObjectType.class.isAssignableFrom(type) && modelServiceLocator != null) {
                def = findObjectDefinition((Class<? extends ObjectType>) type, coordinates, modelServiceLocator);
            } else {
                def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
            }
            itemDef = def.findItemDefinition(item.getPath().getItemPath());
        }
        if (item.getPath() != null) {
            itemPath = item.getPath().getItemPath();
        }
        PropertySearchItemWrapper searchItemWrapper = null;
        if (itemDef == null && !hasParameter(item) && item.getFilter() == null) {
            return searchItemWrapper;
        }
        List<DisplayableValue<?>> availableValues = getSearchItemAvailableValues(item, itemDef, modelServiceLocator);
        QName valueTypeName = getSearchItemValueTypeName(item, itemDef);
        LookupTableType lookupTable = getSearchItemLookupTable(item, itemDef, modelServiceLocator);

        searchItemWrapper = createPropertySearchItemWrapper(type, itemDef, itemPath, valueTypeName,
                availableValues, lookupTable, item.getFilter());

        boolean isFixedItem = false;
        if (itemPath != null) {
            isFixedItem = isFixedItem(type, itemPath);
        }
        searchItemWrapper.setVisible(isFixedItem || hasParameter(item));
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
        name = WebComponentUtil.getTranslatedPolyString(WebComponentUtil.getLabel(searchItem.getDisplay()));
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
        String help = WebComponentUtil.getHelp(searchItem.getDisplay());
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
        return hasParameter(searchItem) ? WebComponentUtil.getHelp(searchItem.getParameter().getDisplay()) : "";
    }

    private static boolean hasParameter(SearchItemType searchItem) {
        return searchItem != null && searchItem.getParameter() != null;
    }

    private static boolean hasParameter(AbstractSearchItemWrapper searchItem) {
        return searchItem != null && StringUtils.isNotEmpty(searchItem.getParameterName());
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

    private static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
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
            return new TextSearchItemWrapper(path, itemDef.getValueEnumerationRef().getOid(), itemDef.getValueEnumerationRef().getTargetType());
        }
        if (path != null) {
            if (ShadowType.F_OBJECT_CLASS.equivalent(path)) {
                return new ObjectClassSearchItemWrapper();
            } else if (ShadowType.F_DEAD.equivalent(path)) {
                return new DeadShadowSearchItemWrapper(Arrays.asList(new SearchValue<>(true), new SearchValue<>(false)));
            } else {
                return new TextSearchItemWrapper(path);
            }
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
    public static void createAbstractRoleSearchItemWrapperList(SearchConfigurationWrapper searchConfigWrapper, SearchBoxConfigurationType config) {
        if (config.getObjectTypeConfiguration() != null) {
            searchConfigWrapper.getItemsList().add(new ObjectTypeSearchItemWrapper(config.getObjectTypeConfiguration()));
        }
        if (config.getRelationConfiguration() != null) {
            RelationSearchItemWrapper relation = new RelationSearchItemWrapper(searchConfigWrapper);
            if (config.getRelationConfiguration().getDisplay() != null) {
                relation.setName(WebComponentUtil.getTranslatedPolyString(config.getRelationConfiguration().getDisplay().getLabel()));
                relation.setHelp(WebComponentUtil.getTranslatedPolyString(config.getRelationConfiguration().getDisplay().getHelp()));
            }
            searchConfigWrapper.getItemsList().add(relation);
        }

        if (config.getIndirectConfiguration() != null) {
            IndirectSearchItemWrapper indirect = new IndirectSearchItemWrapper(searchConfigWrapper);
            if (config.getIndirectConfiguration().getDisplay() != null) {
                indirect.setName(WebComponentUtil.getTranslatedPolyString(config.getIndirectConfiguration().getDisplay().getLabel()));
                indirect.setHelp(WebComponentUtil.getTranslatedPolyString(config.getIndirectConfiguration().getDisplay().getHelp()));
            }
            searchConfigWrapper.getItemsList().add(indirect);
        }

        if (config.getScopeConfiguration() != null) {
            ScopeSearchItemWrapper scope = new ScopeSearchItemWrapper(searchConfigWrapper);
            if (config.getScopeConfiguration().getDisplay() != null) {
                scope.setName(WebComponentUtil.getTranslatedPolyString(config.getScopeConfiguration().getDisplay().getLabel()));
                scope.setHelp(WebComponentUtil.getTranslatedPolyString(config.getScopeConfiguration().getDisplay().getHelp()));
            }
            searchConfigWrapper.getItemsList().add(scope);
        }
        if (config.getProjectConfiguration() != null) {
            ProjectSearchItemWrapper project = new ProjectSearchItemWrapper(searchConfigWrapper);
            if (config.getProjectConfiguration().getDisplay() != null) {
                project.setName(WebComponentUtil.getTranslatedPolyString(config.getProjectConfiguration().getDisplay().getLabel()));
                project.setHelp(WebComponentUtil.getTranslatedPolyString(config.getProjectConfiguration().getDisplay().getHelp()));
            }
            searchConfigWrapper.getItemsList().add(project);
        }
        if (config.getTenantConfiguration() != null) {
            TenantSearchItemWrapper tenant = new TenantSearchItemWrapper(searchConfigWrapper);
            if (config.getTenantConfiguration().getDisplay() != null) {
                tenant.setName(WebComponentUtil.getTranslatedPolyString(config.getTenantConfiguration().getDisplay().getLabel()));
                tenant.setHelp(WebComponentUtil.getTranslatedPolyString(config.getTenantConfiguration().getDisplay().getHelp()));
            }
            searchConfigWrapper.getItemsList().add(tenant);
        }
    }

    private static SearchConfigurationWrapper combineSearchBoxConfiguration(SearchConfigurationWrapper config, SearchConfigurationWrapper customConfig) {
        return combineSearchBoxConfiguration(config, customConfig, false);
    }

    private static SearchConfigurationWrapper combineSearchBoxConfiguration(SearchConfigurationWrapper config,
            SearchConfigurationWrapper customConfig, boolean replaceSearchItems) {
        if (config == null) {
            return customConfig;
        }
        if (customConfig == null) {
            return config;
        }
        if (CollectionUtils.isNotEmpty(customConfig.getAllowedModeList())) {
            config.getAllowedModeList().clear();
            config.getAllowedModeList().addAll(customConfig.getAllowedModeList());
        }
        if (StringUtils.isNotEmpty(customConfig.getCollectionRefOid())) {
            config.setCollectionRefOid(customConfig.getCollectionRefOid());
        }
        if (StringUtils.isNotEmpty(customConfig.getCollectionViewName())) {
            config.setCollectionViewName(customConfig.getCollectionViewName());
        }
        if (customConfig.getDefaultSearchBoxMode() != null  && config.getAllowedModeList().contains(customConfig.getDefaultScope())) {
            config.setDefaultSearchBoxMode(customConfig.getDefaultSearchBoxMode());
        }
        if (customConfig.getDefaultScope() != null) {
            config.setDefaultScope(customConfig.getDefaultScope());
        }
        if (customConfig.getTypeClass() != null) {
            config.setTypeClass(customConfig.getTypeClass());
        }
        if (CollectionUtils.isNotEmpty(customConfig.getAllowedTypeList())) {
            config.getAllowedTypeList().clear();
            config.getAllowedTypeList().addAll(customConfig.getAllowedTypeList());
        }
        if (customConfig.getDefaultRelation() != null) {
            config.setDefaultRelation(customConfig.getDefaultRelation());
        }
        if (CollectionUtils.isNotEmpty(customConfig.getSupportedRelations())) {
            config.getSupportedRelations().clear();
            config.getSupportedRelations().addAll(customConfig.getSupportedRelations());
        }

        if (CollectionUtils.isNotEmpty(customConfig.getItemsList())) {
            if (replaceSearchItems) {
                config.getItemsList().clear();
                config.getItemsList().addAll(customConfig.getItemsList());
            } else {
                customConfig.getItemsList().forEach(item -> {
                    addOrReplaceSearchItemWrapper(config, (AbstractSearchItemWrapper) item);
                });
            }
        }
        config.setAllowToConfigureSearchItems(customConfig.isAllowToConfigureSearchItems());
        return config;
    }

    private static void addOrReplaceSearchItemWrapper(SearchConfigurationWrapper config, AbstractSearchItemWrapper customItem) {
        List<AbstractSearchItemWrapper> items = config.getItemsList();
        boolean execute = false;
        if (customItem instanceof PropertySearchItemWrapper) {
            Iterator<AbstractSearchItemWrapper> itemsIterator = items.iterator();
            while (itemsIterator.hasNext()) {
                AbstractSearchItemWrapper item = itemsIterator.next();
                if (!hasParameter(item) && item instanceof PropertySearchItemWrapper &&
                        ((PropertySearchItemWrapper<?>) item).getPath().equivalent(((PropertySearchItemWrapper<?>) customItem).getPath())) {
                    execute = true;
                } else if (item instanceof AbstractRoleSearchItemWrapper && customItem.getClass().equals(item.getClass())) {
                    execute = true;
                }
                if (execute) {
                    itemsIterator.remove();
                    items.add(customItem);
                    break;
                }
            }
            if (!execute) {
                items.add(customItem);
            }
            return;
        }

        for (AbstractSearchItemWrapper item : items) {
            if (item.getClass().equals(customItem.getClass())) {
                items.remove(item);
                items.add(customItem);
                execute = true;
                break;
            }
        }
        if (!execute) {
            items.add(customItem);
        }
    }

    public static ScopeSearchItemConfigurationType combineScopeSearchItem(ScopeSearchItemConfigurationType scope1, ScopeSearchItemConfigurationType scope2) {
        ScopeSearchItemConfigurationType scopeConfig = combineCustomUserInterfaceFeatureType(scope1, scope2);
        if (scopeConfig != scope2) {
            if (scope2 != null && scope2.getDefaultValue() != null) {
                scopeConfig.setDefaultValue(scope2.getDefaultValue());
            } else if (scope1 != null) {
                scopeConfig.setDefaultValue(scope1.getDefaultValue());
            }
        }
        return scopeConfig;
    }

    private static <F extends UserInterfaceFeatureType> F combineCustomUserInterfaceFeatureType(F feature, F customFeature) {
        if (feature == null) {
            return customFeature;
        }
        if (customFeature == null) {
            return feature;
        }
        if (StringUtils.isNotEmpty(customFeature.getDescription())) {
            feature.description(customFeature.getDescription());
        }
        if (StringUtils.isNotEmpty(customFeature.getDocumentation())) {
            feature.documentation(customFeature.getDocumentation());
        }
        feature.setDisplay(WebComponentUtil.combineDisplay(feature.getDisplay(), customFeature.getDisplay()));
        if (customFeature.getVisibility() != null) {
            feature.setVisibility(customFeature.getVisibility());
        }
        if (customFeature.getDisplayOrder() != null) {
            feature.setDisplayOrder(customFeature.getDisplayOrder());
        }
        if (customFeature.getApplicableForOperation() != null) {
            feature.setApplicableForOperation(customFeature.getApplicableForOperation());
        }
        return feature;
    }

    private static SearchItemsType combineSearchItems(SearchItemsType searchItems, SearchItemsType customSearchItems) {
        if (searchItems == null || CollectionUtils.isEmpty(searchItems.getSearchItem())) {
            return customSearchItems;
        }
        if (customSearchItems == null || CollectionUtils.isEmpty(customSearchItems.getSearchItem())) {
            return searchItems;
        }
        customSearchItems.getSearchItem().forEach(customItem -> {
            SearchItemType item = findSearchItemByPath(searchItems.getSearchItem(), customItem.getPath());
            if (item != null) {
                combineSearchItem(item, customItem);
            } else {
                searchItems.getSearchItem().add(customItem.clone());
            }
        });
        return searchItems;
    }

    private static SearchItemType findSearchItemByPath(List<SearchItemType> itemList, ItemPathType path) {
        if (path == null) {
            return null;
        }
        for (SearchItemType item : itemList) {
            if (path.equivalent(item.getPath())) {
                return item;
            }
        }
        return null;
    }

    private static SearchItemType combineSearchItem(SearchItemType item, SearchItemType customItem) {
        if (item == null) {
             return customItem;
        }
        if (customItem == null) {
            return item;
        }
        if (customItem.getPath() != null) {
            item.setPath(customItem.getPath());
        }
        if (customItem.getFilter() != null) {
            item.setFilter(customItem.getFilter());
        }
        if (customItem.getFilterExpression() != null) {
            item.setFilterExpression(customItem.getFilterExpression());
        }
        if (customItem.getDescription() != null) {
            item.setDescription(customItem.getDescription());
        }
        if (customItem.getDisplayName() != null) {
            item.setDisplayName(customItem.getDisplayName());
        }
        if (customItem.getParameter() != null) {
            item.setParameter(customItem.getParameter());
        }
        if (customItem.isVisibleByDefault() != null) {
            item.setVisibleByDefault(customItem.isVisibleByDefault());
        }
        return item;
    }

    public static <C extends Containerable> SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(Class<C> type,
            ModelServiceLocator modelServiceLocator) {
        return createDefaultSearchBoxConfigurationWrapper(type, null, modelServiceLocator);
    }

    public static <C extends Containerable> SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(
            Class<C> type, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        SearchConfigurationWrapper searchConfigWrapper = new SearchConfigurationWrapper(type, modelServiceLocator);
        PrismContainerDefinition<C> def = null;
        if (ObjectType.class.isAssignableFrom(type)) {
            def = findObjectDefinition((Class<? extends ObjectType>) type, coordinates, modelServiceLocator);
        } else {
            def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
        }

        Map<ItemPath, ItemDefinition<?>> availableDefs = getSearchableDefinitionMap(def, modelServiceLocator);

        availableDefs.keySet().forEach(path -> {
            searchConfigWrapper.getItemsList().add(createPropertySearchItemWrapper(type, new SearchItemType().path(new ItemPathType(path)),
                    availableDefs.get(path), null, modelServiceLocator));
        });
        if (ObjectType.class.isAssignableFrom(type)) {
            searchConfigWrapper.setTypeClass(type);
        }
        searchConfigWrapper
                .addAllowedMode(SearchBoxModeType.BASIC)
                .addAllowedMode(SearchBoxModeType.ADVANCED)
                .addAllowedMode(SearchBoxModeType.AXIOM_QUERY);
        searchConfigWrapper.setDefaultSearchBoxMode(SearchBoxModeType.BASIC);

        if (ObjectType.class.isAssignableFrom(type) && isFullTextSearchEnabled(modelServiceLocator, (Class<? extends ObjectType>) type)) {
            searchConfigWrapper.addAllowedMode(SearchBoxModeType.FULLTEXT);
            searchConfigWrapper.setDefaultSearchBoxMode(SearchBoxModeType.FULLTEXT);
        }
        return searchConfigWrapper;
    }

    public static <T extends ObjectType> PrismObjectDefinition findObjectDefinition(
            Class<T> type, ResourceShadowCoordinates coordinates,
            ModelServiceLocator modelServiceLocator) {

        Task task = modelServiceLocator.createSimpleTask(LOAD_OBJECT_DEFINITION);
        OperationResult result = task.getResult();
        try {
            if (Modifier.isAbstract(type.getModifiers())) {
                SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
                return registry.findObjectDefinitionByCompileTimeClass(type);
            }
            PrismObject empty = modelServiceLocator.getPrismContext().createObject(type);

            if (ShadowType.class.equals(type)) {
                return modelServiceLocator.getModelInteractionService().getEditShadowDefinition(
                        coordinates, AuthorizationPhaseType.REQUEST, task, result);
            } else {
                return modelServiceLocator.getModelInteractionService().getEditObjectDefinition(
                        empty, AuthorizationPhaseType.REQUEST, task, result);
            }
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException | ExpressionEvaluationException | CommunicationException | SecurityViolationException ex) {
            result.recordFatalError(ex.getMessage());
            throw new SystemException(ex);
        }
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

    public static <C extends Containerable> List<SearchItemDefinition> getAvailableDefinitions(
            PrismContainerDefinition<C> objectDef, List<ItemPath> availableItemPath, boolean useDefsFromSuperclass, ModelServiceLocator modelServiceLocator) {
        List<SearchItemDefinition> definitions = new ArrayList<>();

        if (objectDef == null) {
            return definitions;
        }

        definitions.addAll(createExtensionDefinitionList(objectDef));

        Class<C> typeClass = objectDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = CollectionUtils.isEmpty(availableItemPath) ? getAvailableSearchableItems(typeClass, modelServiceLocator) : availableItemPath;
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

    public static List<ItemPath> getAvailableSearchableItems(Class<?> typeClass, ModelServiceLocator modelServiceLocator) {
        List<ItemPath> items = SEARCHABLE_OBJECTS.get(typeClass);
        if (AuditEventRecordType.class.equals(typeClass)) {
            SystemConfigurationType systemConfigurationType;
            try {
                systemConfigurationType = modelServiceLocator.getModelInteractionService()
                        .getSystemConfiguration(new OperationResult("load_system_config"));
            } catch (SchemaException | ObjectNotFoundException e) {
                throw new SystemException(e);
            }
            if (systemConfigurationType != null && systemConfigurationType.getAudit() != null
                    && systemConfigurationType.getAudit().getEventRecording() != null &&
                    Boolean.TRUE.equals(systemConfigurationType.getAudit().getEventRecording().isRecordResourceOids())) {
                ArrayList<ItemPath> auditItems = new ArrayList<>(items);
                auditItems.add(ItemPath.create(AuditEventRecordType.F_RESOURCE_OID));
                items = auditItems;
            }
        }
        return items;
    }

    private static <T extends ObjectType> boolean isFullTextSearchEnabled(ModelServiceLocator modelServiceLocator, Class<T> type) {
        OperationResult result = new OperationResult(LOAD_SYSTEM_CONFIGURATION);
        try {
            return FullTextSearchUtil.isEnabledFor(modelServiceLocator.getModelInteractionService().getSystemConfiguration(result)
                    .getFullTextSearch(), type);
        } catch (SchemaException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    private static boolean isAllowToConfigureSearchItems(ModelServiceLocator modelServiceLocator, QName type,
            String collectionViewName, Search.PanelType panelType) {
        SearchBoxConfigurationType searchConfig = getSearchBoxConfiguration(modelServiceLocator, type, collectionViewName, panelType);
        if (searchConfig == null || searchConfig.isAllowToConfigureSearchItems() == null) {
            return true; //todo should be set to false
        }
        return searchConfig.isAllowToConfigureSearchItems();
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

    public static <C extends Containerable> List<AbstractSearchItemWrapper> createSearchableExtensionWrapperList(
            PrismContainerDefinition<C> objectDef, ModelServiceLocator modelServiceLocator) {
        return createSearchableExtensionWrapperList(objectDef, modelServiceLocator, ObjectType.F_EXTENSION);
    }

    public static <C extends Containerable> List<AbstractSearchItemWrapper> createSearchableExtensionWrapperList(
            PrismContainerDefinition<C> objectDef, ModelServiceLocator modelServiceLocator, ItemPath extensionPath) {
        List<AbstractSearchItemWrapper> searchItemWrappers = new ArrayList<>();
        PrismContainerDefinition ext = objectDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext == null) {
            return searchItemWrappers;
        }
        if (ext != null && ext.getDefinitions() != null) {
            List<ItemDefinition<?>> defs = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(def -> (def instanceof PrismReferenceDefinition || def instanceof PrismPropertyDefinition)
                            && isIndexed(def)).collect(Collectors.toList());
            List<SearchItemType> searchItems = new ArrayList<>();
            defs.forEach(def -> searchItems.add(new SearchItemType()
                    .path(new ItemPathType(ItemPath.create(extensionPath, def.getItemName())))
                    .displayName(WebComponentUtil.getItemDefinitionDisplayNameOrName(def, null))));
            searchItems.forEach(searchItem -> searchItemWrappers.add(createPropertySearchItemWrapper(objectDef.getCompileTimeClass(),
                    searchItem, null, null, modelServiceLocator)));
        }
        return searchItemWrappers;
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
            List<ItemPath> paths = getAvailableSearchableItems(typeClass, modelServiceLocator);
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

    public static <C extends Containerable> boolean isFixedItem(Class<C> typeClass, ItemPath path) {

        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            if (FIXED_SEARCH_ITEMS.get(typeClass) != null &&
                    ItemPathCollectionsUtil.containsEquivalent(FIXED_SEARCH_ITEMS.get(typeClass), path)) {
                return true;
            }
            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return false;
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
                new SearchItemType().path(new ItemPathType(path)), refDef, null, pageBase);
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
                new SearchItemType().path(new ItemPathType(propDef.getItemName())), propDef, null, modelServiceLocator);
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

    public static ScopeSearchItemConfigurationType createScopeSearchItem() {
        //todo set default value here?
        return new ScopeSearchItemConfigurationType();
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
