/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import java.lang.reflect.Modifier;
import java.util.*;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.ItemPathTypeUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchConfigurationUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.cxf.common.util.CollectionUtils;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchFactory {

    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";
    private static final String LOAD_ADMIN_GUI_CONFIGURATION = DOT_CLASS + "loadAdminGuiConfiguration";

    private static final Map<Class, List<ItemPath>> SEARCHABLE_OBJECTS = new HashMap<>();

    static {
        SEARCHABLE_OBJECTS.put(ObjectType.class, Arrays.asList(
                ItemPath.create(ObjectType.F_NAME),
                ItemPath.create(ObjectType.F_LIFECYCLE_STATE),
                ItemPath.create(ObjectType.F_SUBTYPE)));
        SEARCHABLE_OBJECTS.put(FocusType.class, Arrays.asList(
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ItemPath.create(FocusType.F_ROLE_MEMBERSHIP_REF),
                ItemPath.create(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS)));
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
                ItemPath.create(UserType.F_EMPLOYEE_TYPE),
                ItemPath.create(UserType.F_ORGANIZATIONAL_UNIT),
                ItemPath.create(UserType.F_LOCALITY)));
        SEARCHABLE_OBJECTS.put(RoleType.class, Arrays.asList(
                ItemPath.create(RoleType.F_NAME),
                ItemPath.create(RoleType.F_DISPLAY_NAME),
                ItemPath.create(RoleType.F_ROLE_TYPE)));
        SEARCHABLE_OBJECTS.put(ServiceType.class, Arrays.asList(
                ItemPath.create(ServiceType.F_NAME),
                ItemPath.create(ServiceType.F_SERVICE_TYPE),
                ItemPath.create(ServiceType.F_URL)));
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
                ItemPath.create(OrgType.F_ORG_TYPE),
                ItemPath.create(OrgType.F_TENANT),
                ItemPath.create(OrgType.F_LOCALITY)
        ));
        SEARCHABLE_OBJECTS.put(GenericObjectType.class, Arrays.asList(
                ItemPath.create(GenericObjectType.F_OBJECT_TYPE)
        ));
        SEARCHABLE_OBJECTS.put(NodeType.class, Arrays.asList(
                ItemPath.create(NodeType.F_NODE_IDENTIFIER)
        ));
        SEARCHABLE_OBJECTS.put(ReportType.class, Arrays.asList(
                ItemPath.create(ReportType.F_NAME)
        ));
        SEARCHABLE_OBJECTS.put(ShadowType.class, Arrays.asList(
//                ItemPath.create(ShadowType.F_OBJECT_CLASS),
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
                ItemPath.create(TaskType.F_EXECUTION_STATUS),
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
    }

    public static <T extends ObjectType> Search createSearchForShadow(
            ResourceShadowDiscriminator discriminator, ModelServiceLocator modelServiceLocator) {
        return createSearch(ShadowType.class, discriminator, modelServiceLocator, true);
    }

    public static <C extends Containerable> Search createContainerSearch(Class<C> type, ModelServiceLocator modelServiceLocator) {

        PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
        List<SearchItemDefinition> availableDefs = getAvailableDefinitions(containerDef, true);

        Search search = new Search(type, availableDefs);
        return search;
    }

//    public static <C extends Containerable> Search createContainerSearch(Class<C> type, List<SearchItemDefinition> availableDefs, ModelServiceLocator modelServiceLocator) {
//
////        PrismContainerDefinition<C> containerDef = modelServiceLocator.getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
////        List<SearchItemDefinition> availableDefs = getAvailableDefinitions(containerDef, true);
//
//        Search search = new Search(type, availableDefs);
//        return search;
//    }

    public static <T extends ObjectType> Search createSearch(Class<T> type, ModelServiceLocator modelServiceLocator) {
        return createSearch(type, null, modelServiceLocator, true);
    }

    public static <T extends ObjectType> Search createSearch(
            Class<T> type, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator, boolean useDefsFromSuperclass) {
        return createSearch(type, null, discriminator,  modelServiceLocator, useDefsFromSuperclass);
    }

    public static <T extends ObjectType> Search createSearch(
            Class<T> type, String collectionViewName, ResourceShadowDiscriminator discriminator,
            ModelServiceLocator modelServiceLocator, boolean useDefsFromSuperclass) {

        PrismObjectDefinition objectDef = findObjectDefinition(type, discriminator, modelServiceLocator);
        List<SearchItemDefinition> availableDefs =  getAvailableDefinitions(objectDef, useDefsFromSuperclass);
        boolean isFullTextSearchEnabled = isFullTextSearchEnabled(modelServiceLocator, type);

        Search search = new Search(type, availableDefs, isFullTextSearchEnabled,
                getDefaultSearchType(modelServiceLocator, type, collectionViewName));

        SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();

        PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(type);
        SearchItemsType searchItemsConfig = getConfiguredSearchItems(modelServiceLocator, type, collectionViewName);
        List<SearchItemDefinition> configuredSearchItemDefs = getConfiguredSearchItemDefinitions(objectDef, useDefsFromSuperclass, searchItemsConfig);
        if (!CollectionUtils.isEmpty(configuredSearchItemDefs)){
            configuredSearchItemDefs.forEach(searchItemDef -> {
                PrismPropertyDefinition def = objDef.findPropertyDefinition(searchItemDef.getPath());
                SearchItem item = search.addItem(def);
                if (item != null) {
                    item.setFixed(true);
                }
            });
        } else {
            PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);
            SearchItem item = search.addItem(def);
            if (item != null) {
                item.setFixed(true);
            }
        }
        search.setShowMoreDialog(isAllowToConfigureSearchItems(modelServiceLocator, type, collectionViewName));
        return search;
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

    private static <C extends Containerable> List<SearchItemDefinition> getConfiguredSearchItemDefinitions(PrismContainerDefinition<C> objectDef,
            boolean useDefsFromSuperclass, SearchItemsType configuredSearchItems){
        List<SearchItemDefinition> availableDefinitions = getAvailableDefinitions(objectDef, useDefsFromSuperclass);
        if (configuredSearchItems == null || CollectionUtils.isEmpty(configuredSearchItems.getSearchItem())){
            return null;
        }
        List<SearchItemDefinition> configuredSearchItemList = new ArrayList<>();
        configuredSearchItems.getSearchItem().forEach(searchItem -> {
            availableDefinitions.forEach(def -> {
                ItemPathType searchItemPath = new ItemPathType(def.getPath());
                if (searchItem.getPath().equivalent(searchItemPath)){
                    configuredSearchItemList.add(def);
                    return;
                }
            });
        });
        return configuredSearchItemList;
    }

    public static <C extends Containerable> List<SearchItemDefinition> getAvailableDefinitions(
            PrismContainerDefinition<C> objectDef, boolean useDefsFromSuperclass) {
//        Map<ItemPath, ItemDefinition> map = new HashMap<>();
        List<SearchItemDefinition> definitions = new ArrayList<>();

        if (objectDef == null) {
            return definitions;
        }

        definitions.addAll(createExtensionDefinitionList(objectDef));

        Class<C> typeClass = objectDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = SEARCHABLE_OBJECTS.get(typeClass);
            if (paths != null) {
                for (ItemPath path : paths) {
                    ItemDefinition def = objectDef.findItemDefinition(path);
                    if (def != null) {
                        SearchItemDefinition searchItemDef = new SearchItemDefinition(path, def, null);
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

    private static <T extends ObjectType> boolean isFullTextSearchEnabled(ModelServiceLocator modelServiceLocator, Class<T> type) {
        OperationResult result = new OperationResult(LOAD_SYSTEM_CONFIGURATION);
        try {
            return FullTextSearchConfigurationUtil.isEnabledFor(modelServiceLocator.getModelInteractionService().getSystemConfiguration(result)
                    .getFullTextSearch(), type);
        } catch (SchemaException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    private static <T extends ObjectType> SearchBoxModeType getDefaultSearchType(ModelServiceLocator modelServiceLocator, Class<T> type, String collectionViewName) {
        SearchBoxConfigurationType searchConfig = getSearchBoxConfiguration(modelServiceLocator, type, collectionViewName);
        if (searchConfig == null){
            return null;
        }
        return searchConfig.getDefaultMode();
    }

    private static <T extends ObjectType> SearchItemsType getConfiguredSearchItems(ModelServiceLocator modelServiceLocator, Class<T> type, String collectionViewName){
        SearchBoxConfigurationType searchConfig = getSearchBoxConfiguration(modelServiceLocator, type, collectionViewName);
        if (searchConfig == null){
            return null;
        }
        return searchConfig.getSearchItems();
    }

    private static <T extends ObjectType> boolean isAllowToConfigureSearchItems(ModelServiceLocator modelServiceLocator, Class<T> type, String collectionViewName){
        SearchBoxConfigurationType searchConfig = getSearchBoxConfiguration(modelServiceLocator, type, collectionViewName);
        if (searchConfig == null || searchConfig.isAllowToConfigureSearchItems() == null){
            return false;
        }
        return searchConfig.isAllowToConfigureSearchItems();
    }

    private static <T extends ObjectType> SearchBoxConfigurationType getSearchBoxConfiguration(ModelServiceLocator modelServiceLocator,
            Class<T> type, String collectionViewName) {
        OperationResult result = new OperationResult(LOAD_ADMIN_GUI_CONFIGURATION);
        try {
            CompiledGuiProfile guiConfig = modelServiceLocator.getModelInteractionService().getCompiledGuiProfile(null, result);
            CompiledObjectCollectionView view = guiConfig.findObjectCollectionView(
                    WebComponentUtil.classToQName(modelServiceLocator.getPrismContext(), type), collectionViewName);
            if (view != null) {
                return view.getSearchBoxConfiguration();
            }
            return null;
        } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException ex) {
            throw new SystemException(ex);
        }
    }

    public static <C extends Containerable> List<SearchItemDefinition> createExtensionDefinitionList(
            PrismContainerDefinition<C> objectDef) {

//        Map<ItemPath, ItemDefinition> map = new HashMap<>();
        List<SearchItemDefinition> searchItemDefinitions = new ArrayList<>();

        ItemPath extensionPath = ObjectType.F_EXTENSION;

        PrismContainerDefinition ext = objectDef.findContainerDefinition(ObjectType.F_EXTENSION);
        if (ext == null) {
            return searchItemDefinitions;
        }

        for (ItemDefinition def : (List<ItemDefinition>) ext.getDefinitions()) {
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

    public static <C extends Containerable> void addSearchRefDef(PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs, AreaCategoryType category, PageBase pageBase) {
        PrismReferenceDefinition refDef = containerDef.findReferenceDefinition(path);
        if (refDef == null) {
            return;
        }
        if (pageBase == null) {
            defs.add(new SearchItemDefinition(path, refDef, Collections.singletonList(WebComponentUtil.getDefaultRelationOrFail())));
            return;
        }
        defs.add(new SearchItemDefinition(path, refDef, WebComponentUtil.getCategoryRelationChoices(category, pageBase)));
    }

    public static <C extends Containerable> void addSearchPropertyDef(PrismContainerDefinition<C> containerDef, ItemPath path, List<SearchItemDefinition> defs) {
        PrismPropertyDefinition propDef = containerDef.findPropertyDefinition(path);
        if (propDef == null) {
            return;
        }
        defs.add(new SearchItemDefinition(path, propDef, null));
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
