/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class SearchBoxConfigurationUtil {

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

    public static <C extends Containerable> SearchBoxConfigurationType getDefaultSearchBoxConfiguration(Class<C> type, Collection<ItemPath> extensionPaths, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBoxConfig = createDefaultSearchBoxConfig();
        SearchItemsType searchItemsType = createSearchItemsForType(type, extensionPaths, coordinates, modelServiceLocator);
        defaultSearchBoxConfig.setSearchItems(searchItemsType);
        return defaultSearchBoxConfig;
    }

    public static SearchBoxConfigurationType getDefaultAssignmentSearchBoxConfiguration(QName assignmentTargetType, PrismContainerDefinition<AssignmentType> definitionOverride, Collection<ItemPath> extensionPaths, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType defaultSearchBoxConfig = createDefaultSearchBoxConfig();

        SearchItemsType searchItemsType = createSearchItemsForAssignments(assignmentTargetType, definitionOverride, extensionPaths, null, modelServiceLocator);
        defaultSearchBoxConfig.setSearchItems(searchItemsType);
        return defaultSearchBoxConfig;
    }

    private static <C extends Containerable> SearchItemsType createSearchItemsForType(Class<C> type, Collection<ItemPath> extensionPaths, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        Map<ItemPath, ItemDefinition<?>> availableDefs = PredefinedSearchableItems.getAvailableSearchItems(type, extensionPaths, coordinates, modelServiceLocator);//getSearchableDefinitionMap(def, modelServiceLocator);

        List<SearchItemType> searchItems = createSearchItemList(type, availableDefs);
        SearchItemsType searchItemsType = new SearchItemsType();
        searchItemsType.getSearchItem().addAll(searchItems);
        return searchItemsType;
    }

    private static <C extends Containerable> SearchItemsType createSearchItemsForAssignments(QName assignmentTargetType, PrismContainerDefinition<AssignmentType> definitionOverride, Collection<ItemPath> extensionPaths, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
        Map<ItemPath, ItemDefinition<?>> availableDefs = PredefinedSearchableItems.getAvailableAssignmentSearchItems(assignmentTargetType, definitionOverride, extensionPaths, modelServiceLocator);//getSearchableDefinitionMap(def, modelServiceLocator);

        List<SearchItemType> searchItems = createSearchItemList(AssignmentType.class, availableDefs);
        SearchItemsType searchItemsType = new SearchItemsType();
        searchItemsType.getSearchItem().addAll(searchItems);
        return searchItemsType;
    }

    private static <C extends Containerable> List<SearchItemType> createSearchItemList(Class<C> type, Map<ItemPath, ItemDefinition<?>> availableDefinitions) {
        List<SearchItemType> searchItems = new ArrayList<>();
//        List<ItemPath> fixedItems = collectFixedItems(type);
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

    public static <C extends Containerable> SearchBoxConfigurationType getDefaultOrgMembersSearchBoxConfiguration(Class<C> defaultType,
            QName abstractRoleType,
            List<QName> supportedTypes, List<QName> supportedRelations, ModelServiceLocator modelServiceLocator) {
        SearchBoxConfigurationType searchBoxConfig = createDefaultSearchBoxConfig();

        searchBoxConfig.setObjectTypeConfiguration(createObjectTypeSearchItemConfiguration(defaultType, supportedTypes));

        RelationSearchItemConfigurationType relationSearchItem = new RelationSearchItemConfigurationType();
        relationSearchItem.getSupportedRelations().addAll(supportedRelations);
        relationSearchItem.setDefaultValue(getDefaultRelationAllowAny(supportedRelations));
        relationSearchItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        searchBoxConfig.setRelationConfiguration(relationSearchItem);

        if (QNameUtil.match(OrgType.COMPLEX_TYPE, abstractRoleType)) {
            ScopeSearchItemConfigurationType scopeSearchItem = new ScopeSearchItemConfigurationType();
            scopeSearchItem.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
            scopeSearchItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
            searchBoxConfig.setScopeConfiguration(scopeSearchItem);
        }

        IndirectSearchItemConfigurationType indirectItem = new IndirectSearchItemConfigurationType();
        indirectItem.setIndirect(false);
        indirectItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        searchBoxConfig.setIndirectConfiguration(indirectItem);

        //TODO tenant and project only for roles
//        defaultScope = searchBoxConfig.getScopeConfiguration() != null ? searchBoxConfig.getScopeConfiguration().getDefaultValue()
//                : searchBoxConfig.getDefaultScope();
//        if (searchBoxConfig.getRelationConfiguration() != null) {
//            defaultRelation = searchBoxConfig.getRelationConfiguration().getDefaultValue() != null ?
//                    searchBoxConfig.getRelationConfiguration().getDefaultValue() : RelationTypes.MEMBER.getRelation();
//            searchBoxConfig.getRelationConfiguration().getSupportedRelations()
//                    .forEach(relation -> supportedRelations.add(relation));
//        }
//        if (searchBoxConfig.getIndirectConfiguration() != null && searchBoxConfig.getIndirectConfiguration().isIndirect() != null) {
//            indirect = searchBoxConfig.getIndirectConfiguration().isIndirect();
//        }
//        if  (searchBoxConfig.getProjectConfiguration() != null) {
//            //todo
//        }
//        if (searchBoxConfig.getTenantConfiguration() != null) {
//            //todo
//        }

        SearchItemsType searchItemsType = createSearchItemsForType(defaultType, Arrays.asList(ObjectType.F_EXTENSION), null, modelServiceLocator);
        searchBoxConfig.searchItems(searchItemsType);
        return searchBoxConfig;
    }

    public static <C extends Containerable> ObjectTypeSearchItemConfigurationType createObjectTypeSearchItemConfiguration(Class<C> defaultType, List<QName> supportedTypes) {
        ObjectTypeSearchItemConfigurationType objectTypeItem = new ObjectTypeSearchItemConfigurationType();
        objectTypeItem.setDefaultValue(WebComponentUtil.containerClassToQName(PrismContext.get(), defaultType));
        objectTypeItem.getSupportedTypes().addAll(supportedTypes);
        objectTypeItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return objectTypeItem;
    }

    private static QName getDefaultRelationAllowAny(List<QName> availableRelationList) {
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        return PrismConstants.Q_ANY;
    }

    private static SearchBoxConfigurationType createDefaultSearchBoxConfig() {
        SearchBoxConfigurationType searchBoxConfig = new SearchBoxConfigurationType();
        searchBoxConfig.getAllowedMode().addAll(Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
        searchBoxConfig.setDefaultMode(SearchBoxModeType.BASIC);
        return searchBoxConfig;
    }

    private static <C extends Containerable> List<ItemPath> collectFixedItems(Class<C> type)  {
        List<ItemPath> fixedItems = new ArrayList<>();
        while (type != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(type)) {
            List<ItemPath> paths = FIXED_SEARCH_ITEMS.get(type);
            if (paths != null) {
                fixedItems.addAll(paths);
            }
            type = (Class<C>) type.getSuperclass();
        }
        return fixedItems;
    }
}
