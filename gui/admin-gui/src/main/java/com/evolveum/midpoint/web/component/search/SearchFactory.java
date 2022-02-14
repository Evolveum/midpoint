/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

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
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
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
        FIXED_SEARCH_ITEMS.put(ShadowType.class, Arrays.asList(
                ItemPath.create(ShadowType.F_RESOURCE_REF),
                ItemPath.create(ShadowType.F_OBJECT_CLASS)
        ));
    }

    public static <C extends Containerable> Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ModelServiceLocator modelServiceLocator) {
        return createContainerSearch(type, null, modelServiceLocator, false);
    }

    public static <C extends Containerable> Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ItemPath defaultSearchItem,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {
        return createContainerSearch(type, defaultSearchItem, null, modelServiceLocator, useObjectCollection);
    }

    public static <C extends Containerable> Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, ItemPath defaultSearchItem, List<SearchItemDefinition> defaultAvailableDefs,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {
        PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(type.getTypeClass());
        return createContainerSearch(type, containerDef, defaultSearchItem, defaultAvailableDefs, modelServiceLocator, useObjectCollection);
    }

    @Deprecated
    public static <C extends Containerable> Search<C> createContainerSearch(ContainerTypeSearchItem<C> type, PrismContainerDefinition<C> containerDef, ItemPath defaultSearchItem, List<SearchItemDefinition> defaultAvailableDefs,
            ModelServiceLocator modelServiceLocator, boolean useObjectCollection) {

        List<SearchItemDefinition> availableDefs = defaultAvailableDefs;
        if (CollectionUtils.isEmpty(defaultAvailableDefs)) {
            availableDefs = getAvailableDefinitions(containerDef, null, true, modelServiceLocator);
        }

        Search<C> search = new Search<>(type, availableDefs);

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

    public static <T extends ObjectType> Search<? extends T> createSearch(Class<? extends T> type, ModelServiceLocator modelServiceLocator) {
        @NotNull ObjectTypes objectTypes = ObjectTypes.getObjectType(type);
        return createSearch(new ContainerTypeSearchItem<>(new SearchValue<>(type, "ObjectType." + objectTypes.getTypeQName().getLocalPart())), null, null,
                null, modelServiceLocator, null, true, true, Search.PanelType.DEFAULT);
    }

    public static <T extends ObjectType> Search createSearch(ContainerTypeSearchItem<T> type, ModelServiceLocator modelServiceLocator, boolean isOidSearchEnabled) {
        return createSearch(type, null, null, null, modelServiceLocator, null, true,
                true, Search.PanelType.DEFAULT, isOidSearchEnabled);
    }

    public static <T extends ObjectType> Search<T> createSearch(
            ContainerTypeSearchItem<T> type, String collectionViewName, List<ItemPath> fixedSearchItems, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator, List<ItemPath> availableItemPath, boolean useDefsFromSuperclass, boolean useObjectCollection, Search.PanelType panelType) {
        return createSearch(type, collectionViewName, fixedSearchItems, discriminator, modelServiceLocator, availableItemPath, useDefsFromSuperclass, useObjectCollection,
                panelType, false);
    }

    private static <T extends ObjectType> Search<T> createSearch(
            ContainerTypeSearchItem<T> type, String collectionViewName, List<ItemPath> fixedSearchItems, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator, List<ItemPath> availableItemPath, boolean useDefsFromSuperclass, boolean useObjectCollection, Search.PanelType panelType,
            boolean isOidSearchEnabled) {

        PrismObjectDefinition<?> objectDef = findObjectDefinition(type.getTypeClass(), discriminator, modelServiceLocator);
        List<SearchItemDefinition> availableDefs = getAvailableDefinitions(objectDef, availableItemPath, useDefsFromSuperclass, modelServiceLocator);
        boolean isFullTextSearchEnabled = isFullTextSearchEnabled(modelServiceLocator, type.getTypeClass());

        QName qNametype = WebComponentUtil.classToQName(modelServiceLocator.getPrismContext(), type.getTypeClass());
        SearchBoxConfigurationType searchBox = getSearchBoxConfiguration(modelServiceLocator, qNametype, collectionViewName, panelType);
        SearchBoxModeType searchMode = null;
        List<SearchBoxModeType> allowedSearchModes = new ArrayList<>();
        if (searchBox != null) {
            searchMode = searchBox.getDefaultMode();
            allowedSearchModes = searchBox.getAllowedMode();
        }
        Search<T> search = new Search<>(type, availableDefs, isFullTextSearchEnabled, searchMode, allowedSearchModes, isOidSearchEnabled);

        SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
        PrismObjectDefinition<?> objDef = registry.findObjectDefinitionByCompileTimeClass(type.getTypeClass());

        List<SearchItemDefinition> configuredSearchItemDefs = null;
        if (useObjectCollection) {
            configuredSearchItemDefs = getConfiguredSearchItemDefinitions(availableDefs, modelServiceLocator, qNametype, collectionViewName, panelType);
        }
        if (useObjectCollection && !CollectionUtils.isEmpty(configuredSearchItemDefs)) {
            processSearchItemDefFromCompiledView(configuredSearchItemDefs, search, objDef);
        } else {
            if (CollectionUtils.isEmpty(fixedSearchItems)) {
                fixedSearchItems = new ArrayList<>();
                fixedSearchItems.add(ObjectType.F_NAME);
            }
            fixedSearchItems.forEach(searchItemPath -> {
                ItemDefinition def = objDef.findItemDefinition(searchItemPath);
                SearchItem item = search.addItem(def);
                if (item != null) {
                    item.setFixed(true);
                }
            });
        }
        search.setCanConfigure(isAllowToConfigureSearchItems(modelServiceLocator, qNametype, collectionViewName, panelType));
        return search;
    }

    public static <C extends Containerable> Search<C> createSearchForReport(Class<C> type, List<SearchFilterParameterType> parameters, ModelServiceLocator modelServiceLocator) {
        ContainerTypeSearchItem<C> typeItem = new ContainerTypeSearchItem<>(new SearchValue<>(type, ""));
        Search<C> search = new Search<>(typeItem, new ArrayList<>(), false, SearchBoxModeType.BASIC, Collections.singletonList(SearchBoxModeType.BASIC), false);

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

    private static void processSearchItemDefFromCompiledView(List<SearchItemDefinition> configuredSearchItemDefs, Search search, PrismContainerDefinition containerDef) {
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

    public static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createSearchNew(Class<C> type, ModelServiceLocator modelServiceLocator) {
        return createSearch(type, null, modelServiceLocator);
    }

    public static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createSearch(Class<C> type, String collectionViewName,  ModelServiceLocator modelServiceLocator) {
        SearchConfigurationWrapper<C> searchConfigWrapper = new SearchConfigurationWrapper(type,
                createDefaultSearchBoxConfiguration(type, null, modelServiceLocator), collectionViewName);
        return createSearch(searchConfigWrapper, modelServiceLocator);
    }

    /**
     * use this method to create a search
     * @param searchConfig
     * @param modelServiceLocator
     * @param <C>
     * @return
     */
    public static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createSearch(
            SearchConfigurationWrapper<C> searchConfig, ModelServiceLocator modelServiceLocator) {
        return createSearchNew(searchConfig, null, modelServiceLocator, Search.PanelType.DEFAULT);
    }

   public static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createMemberPanelSearch(
           SearchConfigurationWrapper<C> searchConfig, ModelServiceLocator modelServiceLocator) {
        return createSearchNew(searchConfig, null, modelServiceLocator, Search.PanelType.MEMBER_PANEL);
    }

    private static <C extends Containerable> com.evolveum.midpoint.gui.impl.component.search.Search<C> createSearchNew(
            SearchConfigurationWrapper<C> searchConfigurationWrapper, ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator, Search.PanelType panelType) {
        SearchBoxConfigurationType searchBoxConfig = createDefaultSearchBoxConfiguration(searchConfigurationWrapper.getTypeClass(), discriminator, modelServiceLocator);
        searchBoxConfig = combineSearchBoxConfiguration(searchBoxConfig, searchConfigurationWrapper.getConfig());

        if (searchConfigurationWrapper.getTypeClass().isAssignableFrom(ObjectType.class)) {
            QName typeQname = WebComponentUtil.classToQName(PrismContext.get(), (Class<? extends ObjectType>) searchConfigurationWrapper.getTypeClass());
            SearchBoxConfigurationType configuredSearchBoxConfig = getSearchBoxConfiguration(modelServiceLocator,
                    typeQname, searchConfigurationWrapper.getCollectionViewName(), panelType);
            searchBoxConfig = combineSearchBoxConfiguration(searchBoxConfig, configuredSearchBoxConfig);
        }
        if (Search.PanelType.MEMBER_PANEL.equals(panelType)) {
            //todo add additional panel config here
        }
        searchConfigurationWrapper.setConfig(searchBoxConfig);
        searchConfigurationWrapper.setSearchBoxMode(searchBoxConfig.getDefaultMode());
        createSearchItemWrapperList(searchConfigurationWrapper.getTypeClass(), searchConfigurationWrapper, discriminator, modelServiceLocator);
        if (searchBoxConfig.isAllowToConfigureSearchItems() != null && !searchBoxConfig.isAllowToConfigureSearchItems()) {
            searchConfigurationWrapper.getItemsList().forEach(item -> item.setCanConfigure(false));
        }
        com.evolveum.midpoint.gui.impl.component.search.Search search =
                new com.evolveum.midpoint.gui.impl.component.search.Search(Model.of(searchConfigurationWrapper));
        if (searchConfigurationWrapper.getTypeClass().isAssignableFrom(ObjectType.class)) {
            QName typeQname = WebComponentUtil.classToQName(PrismContext.get(), (Class<? extends ObjectType>) searchConfigurationWrapper.getTypeClass());
            search.getConfig().setAllowToConfigureSearchItems(
                    isAllowToConfigureSearchItems(modelServiceLocator, typeQname, searchConfigurationWrapper.getCollectionViewName(), panelType));
        }
        return search;
    }

    private static <C extends Containerable> void createSearchItemWrapperList(Class<C> type, SearchConfigurationWrapper searchConfigWrapper,
            ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        if (searchConfigWrapper.getConfig() == null) {
            return;
        }
        SearchBoxConfigurationType config = searchConfigWrapper.getConfig();
        if (config.getObjectTypeConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new ObjectTypeSearchItemWrapper(config.getObjectTypeConfiguration(), searchConfigWrapper.isAllowAllTypeSearch()));
        }
        if (config.getSearchItems() != null && config.getSearchItems().getSearchItem() != null) {
            config.getSearchItems().getSearchItem().forEach(item -> {
                if (item.getPath() != null) {
                    PropertySearchItemWrapper property = createPropertySearchItemWrapper(type, item, discriminator, modelServiceLocator);
                    if (property != null) {
                        searchConfigWrapper.addSearchItem(property);
                    }
                } else if (item.getFilter() != null || item.getParameter() != null) {
                    searchConfigWrapper.addSearchItem(new FilterSearchItemWrapper(item, searchConfigWrapper.getTypeClass()));
                }
            });
        }
        if (config.getAllowedMode() != null && config.getAllowedMode().contains(SearchBoxModeType.OID)) {
            OidSearchItemWrapper oidWrapper = new OidSearchItemWrapper();
            searchConfigWrapper.addSearchItem(oidWrapper);
        }
        if (config.getScopeConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new ScopeSearchItemWrapper(searchConfigWrapper));
        }
        if (config.getRelationConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new RelationSearchItemWrapper(searchConfigWrapper));
        }
        if (config.getIndirectConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new IndirectSearchItemWrapper(searchConfigWrapper));
        }
        if (config.getProjectConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new ProjectSearchItemWrapper(searchConfigWrapper));
        }
        if (config.getTenantConfiguration() != null) {
            searchConfigWrapper.addSearchItem(new TenantSearchItemWrapper(searchConfigWrapper));
        }
        //todo what to do with defaultScope and defaultObjectType?

    }

    private static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            SearchItemType item, ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        return createPropertySearchItemWrapper(type, item, null, discriminator, modelServiceLocator);
    }

    private static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            SearchItemType item, List<DisplayableValue> availableValues, ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        PrismContainerDefinition<C> def = null;
        if (type.isAssignableFrom(ObjectType.class)) {
            def = findObjectDefinition((Class<? extends ObjectType>) type, discriminator, modelServiceLocator);
        } else {
            def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
        }
        ItemDefinition<?> itemDef = def.findItemDefinition(item.getPath().getItemPath());
        if (itemDef == null) {
            return null;
        }
        return createPropertySearchItemWrapper(type, itemDef, item, availableValues);
    }

    public static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            ItemDefinition<?> itemDef, SearchItemType item, List<DisplayableValue> availableValues) {
        PropertySearchItemWrapper itemWrapper = null;
        if (itemDef instanceof PrismReferenceDefinition) {
            itemWrapper = new ReferenceSearchItemWrapper(item, (PrismReferenceDefinition)itemDef, type);
            if (item.isVisibleByDefault() != null && item.isVisibleByDefault().booleanValue()) {
                itemWrapper.setVisible(true);
            }
            return itemWrapper;
        }
        PrismPropertyDefinition propertyDef = (PrismPropertyDefinition) itemDef;
        if (CollectionUtils.isNotEmpty(propertyDef.getAllowedValues())) {
            itemWrapper = new ChoicesSearchItemWrapper(item, (List<DisplayableValue>)propertyDef.getAllowedValues());
        } else if (CollectionUtils.isNotEmpty(availableValues)) {
            itemWrapper = new ChoicesSearchItemWrapper(item, availableValues);
        } else if (DOMUtil.XSD_BOOLEAN.equals(propertyDef.getTypeName())) {
            List<DisplayableValue<Boolean>> list = new ArrayList<>();
            list.add(new SearchValue<>(Boolean.TRUE, "Boolean.TRUE"));
            list.add(new SearchValue<>(Boolean.FALSE, "Boolean.FALSE"));
            itemWrapper = new ChoicesSearchItemWrapper(item, list);
        } else if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, propertyDef.getTypeName())) {
            itemWrapper = new ItemPathSearchItemWrapper(item);
        } else if (QNameUtil.match(itemDef.getTypeName(), DOMUtil.XSD_DATETIME)) {
            itemWrapper = new DateSearchItemWrapper(item);
        } else if (ShadowType.F_OBJECT_CLASS.equivalent(item.getPath().getItemPath())) {
            itemWrapper = new ObjectClassSearchItemWrapper(item);
        } else {
            itemWrapper = new TextSearchItemWrapper(item, itemDef);
        }
        if (item.isVisibleByDefault() != null && item.isVisibleByDefault().booleanValue()) {
            itemWrapper.setVisible(true);
        }
        itemWrapper.setItemDef(itemDef);
        return itemWrapper;
    }

    private static List<AbstractRoleSearchItemWrapper> createAbstractRoleSearchItemWrapperList(SearchConfigurationWrapper searchConfig) {
        List<AbstractRoleSearchItemWrapper> wrappers = new ArrayList<>();
        wrappers.add(new IndirectSearchItemWrapper(searchConfig));
        wrappers.add(new ScopeSearchItemWrapper(searchConfig));
        wrappers.add(new TenantSearchItemWrapper(searchConfig));
        wrappers.add(new ProjectSearchItemWrapper(searchConfig));
        wrappers.add(new RelationSearchItemWrapper(searchConfig));
        return wrappers;
    }

    private static SearchBoxConfigurationType combineSearchBoxConfiguration(SearchBoxConfigurationType config, SearchBoxConfigurationType customConfig) {
        if (config == null) {
            return customConfig;
        }
        if (customConfig == null) {
            return config;
        }
        if (customConfig.getDefaultMode() != null) {
            config.setDefaultMode(customConfig.getDefaultMode());
        }
        if (CollectionUtils.isNotEmpty(customConfig.getAllowedMode())) {
            if (config.getAllowedMode() == null) {
                config.createAllowedModeList();
            }
            config.getAllowedMode().clear();
            config.getAllowedMode().addAll(customConfig.getAllowedMode());
        }
        config.setScopeConfiguration(combineScopeSearchItem(config.getScopeConfiguration(), customConfig.getScopeConfiguration()));
        if (customConfig.getObjectTypeConfiguration() != null) {
            ObjectTypeSearchItemConfigurationType objectTypeConfig = combineCustomUserInterfaceFeatureType(config.getObjectTypeConfiguration(), customConfig.getObjectTypeConfiguration());
            if (objectTypeConfig != customConfig.getObjectTypeConfiguration()) {
                if (customConfig.getObjectTypeConfiguration().getDefaultValue() != null) {
                    objectTypeConfig.setDefaultValue(customConfig.getObjectTypeConfiguration().getDefaultValue());
                } else if (config.getObjectTypeConfiguration() != null) {
                    objectTypeConfig.setDefaultValue(config.getObjectTypeConfiguration().getDefaultValue());
                }
                if (CollectionUtils.isNotEmpty(customConfig.getObjectTypeConfiguration().getSupportedTypes())) {
                    objectTypeConfig.createSupportedTypesList().addAll(customConfig.getObjectTypeConfiguration().getSupportedTypes());
                } else if (config.getObjectTypeConfiguration() != null) {
                    objectTypeConfig.createSupportedTypesList().addAll(config.getObjectTypeConfiguration().getSupportedTypes());
                }
            }
            config.setObjectTypeConfiguration(objectTypeConfig);
        }
        if (customConfig.getRelationConfiguration() != null) {
            RelationSearchItemConfigurationType relationConfig = combineCustomUserInterfaceFeatureType(config.getRelationConfiguration(),
                    customConfig.getRelationConfiguration());
            if (relationConfig != customConfig.getRelationConfiguration()) {
                if (customConfig.getRelationConfiguration().getDefaultValue() != null) {
                    relationConfig.setDefaultValue(customConfig.getRelationConfiguration().getDefaultValue());
                } else if (config.getRelationConfiguration() != null) {
                    relationConfig.setDefaultValue(config.getRelationConfiguration().getDefaultValue());
                }
                if (CollectionUtils.isNotEmpty(customConfig.getRelationConfiguration().getSupportedRelations())) {
                    relationConfig.createSupportedRelationsList().addAll(customConfig.getRelationConfiguration().getSupportedRelations());
                } else if (config.getRelationConfiguration() != null) {
                    relationConfig.createSupportedRelationsList().addAll(config.getRelationConfiguration().getSupportedRelations());
                }
            }
            config.setRelationConfiguration(relationConfig);
        }
        if (customConfig.getIndirectConfiguration() != null) {
            IndirectSearchItemConfigurationType indirectConfig = combineCustomUserInterfaceFeatureType(config.getIndirectConfiguration(),
                    customConfig.getIndirectConfiguration());
            if (indirectConfig != config.getIndirectConfiguration()) {
                if (customConfig.getIndirectConfiguration().isIndirect() != null) {
                    indirectConfig.setIndirect(customConfig.getIndirectConfiguration().isIndirect());
                } else if (config.getIndirectConfiguration() != null) {
                    indirectConfig.setIndirect(config.getIndirectConfiguration().isIndirect());
                }
            }
            config.setIndirectConfiguration(indirectConfig);
        }
        if (customConfig.getProjectConfiguration() != null) {
            config.setProjectConfiguration(combineCustomUserInterfaceFeatureType(config.getProjectConfiguration(),
                    customConfig.getProjectConfiguration()));
        }
        if (customConfig.getTenantConfiguration() != null) {
            config.setTenantConfiguration(combineCustomUserInterfaceFeatureType(config.getTenantConfiguration(),
                    customConfig.getTenantConfiguration()));
        }
        if (customConfig.getDefaultScope() != null) {
            config.setDefaultScope(customConfig.getDefaultScope());
        }
        if (customConfig.getDefaultObjectType() != null) {
            config.setDefaultObjectType(customConfig.getDefaultObjectType());
        }
        if (customConfig.getSearchItems() != null && CollectionUtils.isNotEmpty(customConfig.getSearchItems().getSearchItem())) {
            config.setSearchItems(combineSearchItems(config.getSearchItems(), customConfig.getSearchItems()));
            config.setDefaultObjectType(customConfig.getDefaultObjectType());
        }
        if (customConfig.isAllowToConfigureSearchItems() != null) {
            config.setAllowToConfigureSearchItems(customConfig.isAllowToConfigureSearchItems());
        }

        return config;
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

    public static <C extends Containerable> SearchBoxConfigurationType createDefaultSearchBoxConfiguration(Class<C> type,
            ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType config = new SearchBoxConfigurationType();
        PrismContainerDefinition<C> def = null;
        if (type.isAssignableFrom(ObjectType.class)) {
            def = findObjectDefinition((Class<? extends ObjectType>) type, discriminator, modelServiceLocator);
        } else {
            def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
        }

        List<ItemDefinition<?>> availableDefs = getSearchableDefinitionList(def, modelServiceLocator);

        SearchItemsType searchItems = new SearchItemsType();
        availableDefs.forEach(item -> {
            SearchItemType searchItem = new SearchItemType();
            searchItem
                    .path(new ItemPathType(item.getItemName()))
                    .visibleByDefault(isFixedItem(type, item.getItemName()))
                    .displayName(WebComponentUtil.getItemDefinitionDisplayNameOrName(item, null))
                    .description(item.getHelp());

            if (type.isAssignableFrom(ObjectType.class)) {
                config.defaultObjectType(WebComponentUtil.classToQName(PrismContext.get(), (Class<? extends ObjectType>) type));
            }
            searchItems.getSearchItem().add(searchItem);
        });
        config.searchItems(searchItems)
                .defaultMode(SearchBoxModeType.BASIC)
                .allowedMode(SearchBoxModeType.BASIC)
                .allowedMode(SearchBoxModeType.ADVANCED);

        if (type.isAssignableFrom(ObjectType.class) && isFullTextSearchEnabled(modelServiceLocator, (Class<? extends ObjectType>) type)) {
            config.allowedMode(SearchBoxModeType.FULLTEXT);
        }
        return config;
    }

    public static <T extends ObjectType> PrismObjectDefinition findObjectDefinition(
            Class<T> type, ResourceShadowDiscriminator discriminator,
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
                return modelServiceLocator.getModelInteractionService().getEditShadowDefinition(discriminator,
                        AuthorizationPhaseType.REQUEST, task, result);
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

    private static List<DisplayableValue> getAllowedValues(ItemPath path) {
        if (AuditEventRecordType.F_CHANNEL.equivalent(path)) {
            List<DisplayableValue> list = new ArrayList<>();
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
            PrismContainerDefinition<C> objectDef) {

        List<AbstractSearchItemWrapper> searchItemWrappers = new ArrayList<>();

        ItemPath extensionPath = ObjectType.F_EXTENSION;

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
                    searchItem, null, null)));
        }
        return searchItemWrappers;
    }

    public static <C extends Containerable> List<ItemDefinition<?>> getSearchableDefinitionList(
            PrismContainerDefinition<C> containerDef, ModelServiceLocator modelServiceLocator) {
        return getSearchableDefinitionList(containerDef, modelServiceLocator, true);
    }

    /**
         *
         * @param containerDef
         * @param useDefsFromSuperclass leave it here for a while; seems that it is always true
         * @param <C>
         * @return
         */
    public static <C extends Containerable> List<ItemDefinition<?>> getSearchableDefinitionList(
            PrismContainerDefinition<C> containerDef, ModelServiceLocator modelServiceLocator, boolean useDefsFromSuperclass) {

        List<ItemDefinition<?>> searchableDefinitions = new ArrayList<>();

        if (containerDef == null) {
            return searchableDefinitions;
        }
        PrismContainerDefinition ext = containerDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext != null && ext.getDefinitions() != null) {
            searchableDefinitions.addAll(((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(def -> (def instanceof PrismReferenceDefinition || def instanceof PrismPropertyDefinition)
                            && isIndexed(def)).collect(Collectors.toList()));
        }
        Class<C> typeClass = containerDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = getAvailableSearchableItems(typeClass, modelServiceLocator);
            if (paths != null) {
                for (ItemPath path : paths) {
                    ItemDefinition<?> def = containerDef.findItemDefinition(path);
                    if (def != null) {
                        searchableDefinitions.add(def);
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
        SearchItemType searchItem = new SearchItemType()
                .path(new ItemPathType(path))
                .displayName(WebComponentUtil.getItemDefinitionDisplayNameOrName(refDef, null));
        if (pageBase == null) {
            defs.add(new ReferenceSearchItemWrapper(searchItem, refDef,
                    Collections.singletonList(WebComponentUtil.getDefaultRelationOrFail()), containerDef.getCompileTimeClass()));
            return;
        }
        defs.add(new ReferenceSearchItemWrapper(searchItem, refDef,
                WebComponentUtil.getCategoryRelationChoices(category, pageBase), containerDef.getCompileTimeClass()));
    }

    public static <C extends Containerable> void addShadowAttributeSearchItemWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs) {
        addSearchPropertyWrapper(containerDef, path, defs, null);
    }

    public static <C extends Containerable> void addSearchPropertyWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs) {
        addSearchPropertyWrapper(containerDef, path, defs, null);
    }

    public static <C extends Containerable> void addSearchPropertyWrapper(PrismContainerDefinition<C> containerDef,
            ItemPath path, List<? super AbstractSearchItemWrapper> defs, String key) {
        PrismPropertyDefinition propDef = containerDef.findPropertyDefinition(path);
        if (propDef == null) {
            return;
        }
        PolyStringType displayName = new PolyStringType(propDef.getItemName().getLocalPart());
        if (key != null) {
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setFallback(propDef.getItemName().getLocalPart());
            translation.setKey(key);
            displayName.setTranslation(translation);
        }

        SearchItemType searchItem = new SearchItemType()
                .displayName(displayName)
                .path(new ItemPathType(path));

        defs.add(createPropertySearchItemWrapper(containerDef.getCompileTimeClass(), searchItem, getAllowedValues(path), null, null));
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
