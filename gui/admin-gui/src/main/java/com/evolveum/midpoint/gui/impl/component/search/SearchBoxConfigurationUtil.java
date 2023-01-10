/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class SearchBoxConfigurationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBoxConfigurationUtil.class);
    private static final String DOT_CLASS = SearchBoxConfigurationUtil.class.getName() + ".";
    private static final String LOAD_OBJECT_DEFINITION = DOT_CLASS + "loadObjectDefinition";

    private final Class<? extends Containerable> type;
    private ResourceShadowCoordinates coordinates;
    private ModelServiceLocator modelServiceLocator;
    private ShadowSearchType shadowSearchType;
    private QName assignmentTargetType;
    private PrismContainerDefinition<? extends Containerable> containerDefinition;
    private QName abstractRoleType;
    private List<QName> supportedTypes;
    private List<QName> supportedRelations;

    private ResourceObjectDefinition resourceObjectDefinition;


    public SearchBoxConfigurationUtil(Class<? extends Containerable> type) {
        this.type = type;
    }

    public SearchBoxConfigurationUtil shadowCoordinates(ResourceShadowCoordinates coordinates) {
        this.coordinates = coordinates;
        return this;
    }

    public SearchBoxConfigurationUtil modelServiceLocator(ModelServiceLocator modelServiceLocator) {
        this.modelServiceLocator = modelServiceLocator;
        return this;
    }

    public SearchBoxConfigurationUtil shadowSearchType(ShadowSearchType shadowSearchType) {
        this.shadowSearchType = shadowSearchType;
        return this;
    }

    public SearchBoxConfigurationUtil assignmentTargetType(QName assignmentTargetType) {
        this.assignmentTargetType = assignmentTargetType;
        return this;
    }

    public SearchBoxConfigurationUtil containerDefinition(PrismContainerDefinition<? extends Containerable> containerDefinition) {
        this.containerDefinition = containerDefinition;
        return this;
    }

    public SearchBoxConfigurationUtil abstractRoleType(QName abstractRoleType) {
        this.abstractRoleType = abstractRoleType;
        return this;
    }

    public SearchBoxConfigurationUtil supportedTypes(List<QName> supportedTypes) {
        this.supportedTypes = supportedTypes;
        return this;
    }

    public SearchBoxConfigurationUtil supportedRelations(List<QName> supportedRelations) {
        this.supportedRelations = supportedRelations;
        return this;
    }

    public SearchBoxConfigurationUtil resourceObjectDefinition(ResourceObjectDefinition resourceObjectDefinition) {
        this.resourceObjectDefinition = resourceObjectDefinition;
        return this;
    }

    public SearchBoxConfigurationType create() {
        SearchBoxConfigurationType defaultSearchBoxConfig = createDefaultSearchBoxConfig();
        SearchItemsType searchItemsType = createSearchItemsForType();
        defaultSearchBoxConfig.setSearchItems(searchItemsType);
        return defaultSearchBoxConfig;
    }

    public enum ShadowSearchType {
        PROJECTIONS,
        REPOSITORY,
        RESOURCE
    }

//    private <T extends ObjectType> List<SearchItemType> createAttributeSearchItemWrappers(ResourceShadowCoordinates coordinates) {
//
//        List<SearchItemType> itemsList = new ArrayList<>();
//
//        ResourceObjectDefinition ocDef = null;
//        try {
//
//            if (coordinates.getKind() != null) {
//
//                ocDef = coordinates.getDefinitionByKind();
//
//            } else if (coordinates.getObjectClass() != null) {
//                ocDef = coordinates.getDefinitionByObjectClass();
//
//            }
//        } catch (SchemaException | ConfigurationException e) {
//            //todo logging
//            return itemsList;
//        }
//
//        if (ocDef == null) {
//            return itemsList;
//        }
//
//        for (ResourceAttributeDefinition def : ocDef.getAttributeDefinitions()) {
//            itemsList.add(new SearchItemType().path(new ItemPathType(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)))));
////            itemsList.add(SearchFactory.createPropertySearchItemWrapper(ShadowType.class,
////                    new SearchItemType().path(new ItemPathType(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)))), //TODO visible by default
////                    def, null, getPageBase()));
//        }
//
//        return itemsList;
//    }

    private SearchItemsType createSearchItemsForType() {
        List<SearchItemType> searchItems = createSearchItemList();
        SearchItemsType searchItemsType = new SearchItemsType();
        searchItemsType.getSearchItem().addAll(searchItems);
        return searchItemsType;
    }

    private List<SearchItemType> createSearchItemList() {
        Map<ItemPath, ItemDefinition<?>> availableDefinitions = createAvailableSearchItems();
        List<SearchItemType> searchItems = new ArrayList<>();
        for (ItemPath path : availableDefinitions.keySet()) {
            SearchItemType searchItem = new SearchItemType();
            searchItem.setPath(new ItemPathType(path));
            if (PredefinedSearchableItems.isFixedItem(type, path)) {
                searchItem.setVisibleByDefault(true);
            }
            searchItems.add(searchItem);
        }
        return searchItems;
    }

    private ObjectTypeSearchItemConfigurationType createObjectTypeSearchItemConfiguration() {
        if (CollectionUtils.isEmpty(supportedTypes)) {
            return null;
        }
        ObjectTypeSearchItemConfigurationType objectTypeItem = new ObjectTypeSearchItemConfigurationType();
        objectTypeItem.setDefaultValue(WebComponentUtil.containerClassToQName(PrismContext.get(), type));
        objectTypeItem.getSupportedTypes().addAll(supportedTypes);
        objectTypeItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return objectTypeItem;
    }

    private RelationSearchItemConfigurationType createRelationSearchItemConfigurationType() {
        if (CollectionUtils.isEmpty(supportedRelations)) {
            return null;
        }
        RelationSearchItemConfigurationType relationSearchItem = new RelationSearchItemConfigurationType();
        relationSearchItem.getSupportedRelations().addAll(supportedRelations);
        relationSearchItem.setDefaultValue(getDefaultRelationAllowAny(supportedRelations));
        relationSearchItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return relationSearchItem;
    }

    private ScopeSearchItemConfigurationType createScopeSearchItemConfigurationType() {
        ScopeSearchItemConfigurationType scopeSearchItem = new ScopeSearchItemConfigurationType();
        scopeSearchItem.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
        scopeSearchItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return scopeSearchItem;
    }

    private IndirectSearchItemConfigurationType createIndirectSearchItemConfigurationType() {
        IndirectSearchItemConfigurationType indirectItem = new IndirectSearchItemConfigurationType();
        indirectItem.setIndirect(false);
        indirectItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return indirectItem;
    }

    private UserInterfaceFeatureType createParameterSearchItem(String label, String labelKey) {
        UserInterfaceFeatureType parameterSearchItem = new UserInterfaceFeatureType();
        DisplayType tenantDisplay = GuiDisplayTypeUtil.createDisplayTypeWith(label, labelKey, null);
        parameterSearchItem.setDisplay(tenantDisplay);
        return parameterSearchItem;
    }
    private static QName getDefaultRelationAllowAny(List<QName> availableRelationList) {
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        return PrismConstants.Q_ANY;
    }

    private SearchBoxConfigurationType createDefaultSearchBoxConfig() {
        SearchBoxConfigurationType searchBoxConfig = new SearchBoxConfigurationType();
        searchBoxConfig.getAllowedMode().addAll(Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
        searchBoxConfig.setDefaultMode(SearchBoxModeType.BASIC);

        searchBoxConfig.setObjectTypeConfiguration(createObjectTypeSearchItemConfiguration());

        addMemberSearchConfiguration(searchBoxConfig);

        return searchBoxConfig;
    }

    private void addMemberSearchConfiguration(SearchBoxConfigurationType searchBoxConfig) {
        if (abstractRoleType == null) {
            return;
        }
        searchBoxConfig.setRelationConfiguration(createRelationSearchItemConfigurationType());

        if (QNameUtil.match(OrgType.COMPLEX_TYPE, abstractRoleType)) {
            searchBoxConfig.setScopeConfiguration(createScopeSearchItemConfigurationType());
        }

        searchBoxConfig.setIndirectConfiguration(createIndirectSearchItemConfigurationType());

        if (QNameUtil.match(RoleType.COMPLEX_TYPE, abstractRoleType)) {
            searchBoxConfig.setTenantConfiguration(createParameterSearchItem("Tenant", "abstractRoleMemberPanel.tenant"));

            searchBoxConfig.setProjectConfiguration(createParameterSearchItem("Project/Org", "abstractRoleMemberPanel.project"));
        }
    }

    private List<ItemPath> getAvailableSearchableItems(Class<? extends Containerable> typeClass) {
        List<ItemPath> items = PredefinedSearchableItems.getSearchableItemsFor(typeClass, shadowSearchType);//SEARCHABLE_OBJECTS.get(typeClass);
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



    public Map<ItemPath, ItemDefinition<?>> createAvailableSearchItems() {

        Collection<ItemPath> extensionPaths = createExtensionPaths();

        Map<ItemPath, ItemDefinition<?>> searchableDefinitions = new HashMap<>();

        PrismContainerDefinition<? extends Containerable> containerDef = getDefinition();
        collectExtensionDefinitions(containerDef, extensionPaths, searchableDefinitions);

        collectionNonExtensionDefinitions(containerDef, searchableDefinitions, isUseSuperclassDefinition());

        collectAttributesDefinitions(searchableDefinitions);

        return searchableDefinitions;

    }

    private boolean isUseSuperclassDefinition() {
        return !ShadowType.class.equals(type) || ShadowSearchType.RESOURCE != shadowSearchType;

    }

    private PrismContainerDefinition<? extends Containerable> getDefinition() {
        if (containerDefinition != null) {
            return containerDefinition;
        }
        if (ObjectType.class.isAssignableFrom(type)) {
            return containerDefinition = findObjectDefinition();
        }

        return containerDefinition = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
    }

    private Collection<ItemPath> createExtensionPaths() {
        List<ItemPath> extensionPaths = new ArrayList<>();
        extensionPaths.add(ObjectType.F_EXTENSION);
        if (AssignmentType.class.equals(type)) {
            extensionPaths.add(ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(assignmentTargetType), ObjectType.F_EXTENSION));
        }
        return extensionPaths;
    }

    public PrismObjectDefinition findObjectDefinition() {

        Task task = modelServiceLocator.createSimpleTask(LOAD_OBJECT_DEFINITION);
        OperationResult result = task.getResult();
        try {
            if (Modifier.isAbstract(type.getModifiers())) {
                SchemaRegistry registry = modelServiceLocator.getPrismContext().getSchemaRegistry();
                return registry.findObjectDefinitionByCompileTimeClass((Class<? extends ObjectType>)type);
            }
            PrismObject empty = modelServiceLocator.getPrismContext().createObject((Class<? extends ObjectType>)type);

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
    private <C extends Containerable> void collectExtensionDefinitions(PrismContainerDefinition<C> containerDef, Collection<ItemPath> extensionPaths, Map<ItemPath, ItemDefinition<?>> searchableItems) {
        if (containerDef == null) {
            return;
        }

        if (ShadowSearchType.RESOURCE == shadowSearchType) {
            return;
        }

        for (ItemPath path : extensionPaths) {

            PrismContainerDefinition ext = containerDef.findContainerDefinition(path);
            if (ext == null) {
                LOGGER.trace("No extension defined, shipping collecting extension search items");
                return;
            }
            Map<ItemPath, ItemDefinition<?>> extensionItems = ((List<ItemDefinition<?>>) ext.getDefinitions()).stream()
                    .filter(SearchBoxConfigurationUtil::isNotContainerAndIsIndexed)
                    .collect(Collectors.toMap(d -> ItemPath.create(path, d.getItemName()), d -> d));
            searchableItems.putAll(extensionItems);
        }
    }

    private <C extends Containerable> void collectionNonExtensionDefinitions(PrismContainerDefinition<C> containerDef, Map<ItemPath, ItemDefinition<?>> searchableDefinitions, boolean useDefsFromSuperclass) {
        if (ShadowSearchType.RESOURCE == shadowSearchType) {
            return;
        }
        Class<C> typeClass = containerDef.getCompileTimeClass();
        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            List<ItemPath> paths = getAvailableSearchableItems(typeClass);

            if (paths == null) {
                if (!useDefsFromSuperclass) {
                    break;
                }
                typeClass = (Class<C>) typeClass.getSuperclass();
                continue;
            }

            for (ItemPath path : paths) {
                ItemDefinition<?> def = containerDef.findItemDefinition(path);
                if (def != null) {
                    searchableDefinitions.put(path, def);
                }
            }

            if (!useDefsFromSuperclass) {
                break;
            }

            typeClass = (Class<C>) typeClass.getSuperclass();
        }
    }

    private void collectAttributesDefinitions(Map<ItemPath, ItemDefinition<?>> searchableDefinitions) {
        if (resourceObjectDefinition == null) {
            return;
        }

        if (ShadowSearchType.RESOURCE != shadowSearchType) {
            return;
        }

        for (ResourceAttributeDefinition def : resourceObjectDefinition.getAttributeDefinitions()) {
            searchableDefinitions.put(ItemPath.create(ShadowType.F_ATTRIBUTES, getAttributeName(def)), def);
        }
    }

    private ItemName getAttributeName(ResourceAttributeDefinition def) {
        return def.getItemName();
    }

    private static boolean isNotContainerAndIsIndexed(ItemDefinition<?> def) {
        if (def instanceof PrismContainerDefinition) {
            return false;
        }
        return isIndexed(def);
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

        return indexed;
    }

}
