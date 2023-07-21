/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.util.*;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.impl.query.ValueFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.VariableItemPathSegment;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

public class SearchBoxConfigurationBuilder {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBoxConfigurationBuilder.class);
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
        FIXED_SEARCH_ITEMS.put(SimulationResultProcessedObjectType.class, Arrays.asList(
                SimulationResultProcessedObjectType.F_STATE
        ));
    }

    private Class<?> type;
    private CollectionPanelType collectionPanelType;
    private ModelServiceLocator modelServiceLocator;
    private Map<ItemPath, ItemDefinition<?>> availableDefinitions;
    private SearchContext additionalSearchContext;

    public SearchBoxConfigurationBuilder type(Class<?> type) {
        this.type = type;
        return this;
    }

    public SearchBoxConfigurationBuilder additionalSearchContext(SearchContext additionalSearchContext) {
        this.additionalSearchContext = additionalSearchContext;
        if (additionalSearchContext != null) {
            this.collectionPanelType = additionalSearchContext.getPanelType();
        }
        return this;
    }

    public SearchBoxConfigurationBuilder modelServiceLocator(ModelServiceLocator modelServiceLocator) {
        this.modelServiceLocator = modelServiceLocator;
        return this;
    }

    public SearchBoxConfigurationBuilder availableDefinitions(Map<ItemPath, ItemDefinition<?>> availableDefinitions) {
        this.availableDefinitions = availableDefinitions;
        return this;
    }
    public SearchBoxConfigurationType create() {
        SearchBoxConfigurationType defaultSearchBoxConfig = createDefaultSearchBoxConfig();

        defaultSearchBoxConfig.setObjectTypeConfiguration(createObjectTypeSearchItemConfiguration());

        addMemberSearchConfiguration(defaultSearchBoxConfig);

        SearchItemsType searchItemsType = createSearchItemsForType();
        defaultSearchBoxConfig.setSearchItems(searchItemsType);
        return defaultSearchBoxConfig;
    }

    private SearchBoxConfigurationType createDefaultSearchBoxConfig() {
        SearchBoxConfigurationType searchBoxConfig = new SearchBoxConfigurationType();
        if (additionalSearchContext != null && additionalSearchContext.getAvailableSearchBoxModes() != null) {
            searchBoxConfig.getAllowedMode().addAll(additionalSearchContext.getAvailableSearchBoxModes());
        } else {
            searchBoxConfig.getAllowedMode().addAll(Arrays.asList(SearchBoxModeType.BASIC, SearchBoxModeType.ADVANCED, SearchBoxModeType.AXIOM_QUERY));
        }
        if (searchBoxConfig.getAllowedMode().size() == 1) {
            searchBoxConfig.setDefaultMode(searchBoxConfig.getAllowedMode().iterator().next());
        } else {
            searchBoxConfig.setDefaultMode(SearchBoxModeType.BASIC);
        }
        return searchBoxConfig;
    }

    private SearchItemsType createSearchItemsForType() {
        List<SearchItemType> searchItems = createSearchItemList();
        SearchItemsType searchItemsType = new SearchItemsType();
        searchItemsType.getSearchItem().addAll(searchItems);
        return searchItemsType;
    }

    private List<SearchItemType> createSearchItemList() {
        List<SearchItemType> searchItems = new ArrayList<>();

        if (getReportCollection() != null) {
            return createReportSearchItems(getReportCollection());
        }

        for (ItemPath path : availableDefinitions.keySet()) {
            SearchItemType searchItem = new SearchItemType();
            searchItem.setPath(new ItemPathType(path));
            if (isFixedItem(type, path) || isDeadItemForProjections(path) ) {
                searchItem.setVisibleByDefault(true);
            }
            if (synchronizationSituationForRepoSadow(path)) {
                searchItem.setVisibleByDefault(true);
            }
            searchItems.add(searchItem);
        }

        return searchItems;
    }

    private ObjectCollectionReportEngineConfigurationType getReportCollection(){
        if (additionalSearchContext == null) {
            return null;
        }
        return additionalSearchContext.getReportCollection();
    }

    private boolean isDeadItemForProjections(ItemPath path) {
        if (CollectionPanelType.PROJECTION_SHADOW != collectionPanelType) {
            return false;
        }
        return ItemPath.create(ShadowType.F_DEAD).equivalent(path);
    }

    private boolean synchronizationSituationForRepoSadow(ItemPath path) {
        if (CollectionPanelType.REPO_SHADOW != collectionPanelType) {
            return false;
        }
        return ItemPath.create(ShadowType.F_SYNCHRONIZATION_SITUATION).equivalent(path);
    }

    private List<SearchItemType> createReportSearchItems(ObjectCollectionReportEngineConfigurationType reportCollection) {
        List<SearchFilterParameterType> parameters = reportCollection.getParameter();
        List<SearchItemType> searchItems = parameters
                .stream()
                .map(parameter -> createSearchItem(parameter))
                .collect(Collectors.toList());

        if (reportCollection.getCollection() != null) {
            SearchFilterType filter = reportCollection.getCollection().getFilter();
            if (filter != null) {
                try {
                    ObjectFilter parsedFilter = PrismContext.get().getQueryConverter().parseFilter(filter, type);
                    if (parsedFilter instanceof AndFilter) {
                        List<ObjectFilter> conditions = ((AndFilter) parsedFilter).getConditions();
                        conditions.forEach(condition -> processFilterToSearchItem(searchItems, condition));
                    }
                } catch (Exception e) {
                    LOGGER.debug("Unable to parse filter, {} ", filter);
                }
            }
        }
        return searchItems;
    }

    private SearchItemType createSearchItem(SearchFilterParameterType parameter) {
        SearchItemType searchItemType = new SearchItemType();
        searchItemType.setParameter(parameter);
        searchItemType.setVisibleByDefault(true);
        DisplayType displayType = getSearchItemDisplayName(parameter);
        searchItemType.setDisplay(displayType);
        searchItemType.setDescription(GuiDisplayTypeUtil.getHelp(displayType));
        return searchItemType;
    }

    private DisplayType getSearchItemDisplayName(SearchFilterParameterType parameter) {
        if (parameter == null || parameter.getDisplay() == null) {
            return new DisplayType();
        }
        DisplayType displayType = parameter.getDisplay();
        PolyStringType searchItemDisplayType = GuiDisplayTypeUtil.getLabel(displayType);
        if (searchItemDisplayType == null) {
            displayType.setLabel(new PolyStringType(parameter.getName()));

        }
        return displayType;
    }

    private void processFilterToSearchItem(List<SearchItemType> searchItems, ObjectFilter filter) {
        if (!(filter instanceof ValueFilterImpl && ((ValueFilterImpl<?, ?>) filter).getExpression() != null)) {
            return;
        }
        List<JAXBElement<?>> pathElement = findAllEvaluators((ValueFilter<?, ?>) filter);
        if (pathElement.isEmpty()) {
            return;
        }
        ItemPathType path = (ItemPathType) pathElement.get(0).getValue();
        ItemPath itemPath = path.getItemPath();
        if (itemPath.startsWithVariable()) {
            VariableItemPathSegment variablePath = (VariableItemPathSegment) itemPath.first();
            SearchItemType searchItem = getSearchItemByParameterName(searchItems, variablePath.getName().toString());
            if (searchItem != null) {
                searchItem.setPath(new ItemPathType(((ValueFilterImpl<?, ?>) filter).getPath()));
            }
        }
    }

    private List<JAXBElement<?>> findAllEvaluators(ValueFilter<?,?> filter) {
        ExpressionWrapper expression = filter.getExpression();
        ExpressionType expressionType = (ExpressionType) expression.getExpression();
        return ExpressionUtil.findAllEvaluatorsByName(expressionType, SchemaConstantsGenerated.C_PATH);
    }

    private SearchItemType getSearchItemByParameterName(List<SearchItemType> searchItemList, String parameterName) {
        Optional<SearchItemType> searchItemType = searchItemList.stream().filter(item -> item.getParameter() != null &&
                StringUtils.isNotEmpty(item.getParameter().getName()) && item.getParameter().getName().equals(parameterName)).findFirst();
        if (!searchItemType.isEmpty()) {
            return searchItemType.get();
        }
        return null;
    }

    private ObjectTypeSearchItemConfigurationType createObjectTypeSearchItemConfiguration() {
        ObjectTypeSearchItemConfigurationType objectTypeItem = new ObjectTypeSearchItemConfigurationType();
        objectTypeItem.setDefaultValue(WebComponentUtil.anyClassToQName(PrismContext.get(), type));
        objectTypeItem.getSupportedTypes().addAll(getSupportedObjectTypes(collectionPanelType));
        objectTypeItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return objectTypeItem;
    }

    private void addMemberSearchConfiguration(SearchBoxConfigurationType searchBoxConfig) {
        if (!isMemberPanel()) {
            return;
        }
        searchBoxConfig.setRelationConfiguration(createRelationSearchItemConfigurationType());
        searchBoxConfig.setScopeConfiguration(createScopeSearchItemConfigurationType());
        searchBoxConfig.setIndirectConfiguration(createIndirectSearchItemConfigurationType());
        searchBoxConfig.setTenantConfiguration(createParameterSearchItem("Tenant", "abstractRoleMemberPanel.tenant"));
        searchBoxConfig.setProjectConfiguration(createParameterSearchItem("Project/Org", "abstractRoleMemberPanel.project"));

    }

    private boolean isMemberPanel() {
        return collectionPanelType != null && collectionPanelType.isMemberPanel();
    }

    private RelationSearchItemConfigurationType createRelationSearchItemConfigurationType() {
        List<QName> supportedRelations = getSupportedRelations();
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
        if (!isOrgMemberPanel() || isGovernanceCards()) {
            return null;
        }
        ScopeSearchItemConfigurationType scopeSearchItem = new ScopeSearchItemConfigurationType();
        scopeSearchItem.setDefaultValue(SearchBoxScopeType.ONE_LEVEL);
        scopeSearchItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return scopeSearchItem;
    }

    private IndirectSearchItemConfigurationType createIndirectSearchItemConfigurationType() {
        if (isGovernanceCards()) {
            return null;
        }
        IndirectSearchItemConfigurationType indirectItem = new IndirectSearchItemConfigurationType();
        indirectItem.setIndirect(false);
        indirectItem.setVisibility(UserInterfaceElementVisibilityType.VISIBLE);
        return indirectItem;
    }

    private UserInterfaceFeatureType createParameterSearchItem(String label, String labelKey) {
        if (!isRoleMemberPanel() || isGovernanceCards()) {
            return null;
        }
        UserInterfaceFeatureType parameterSearchItem = new UserInterfaceFeatureType();
        DisplayType tenantDisplay = GuiDisplayTypeUtil.createDisplayTypeWith(label, labelKey, null);
        parameterSearchItem.setDisplay(tenantDisplay);
        return parameterSearchItem;
    }

    private boolean isGovernanceCards() {
        return CollectionPanelType.CARDS_GOVERNANCE == collectionPanelType;
    }
    private boolean isOrgMemberPanel() {
        return collectionPanelType == CollectionPanelType.ORG_MEMBER_MEMBER
                || collectionPanelType == CollectionPanelType.ORG_MEMBER_GOVERNANCE
                || collectionPanelType == CollectionPanelType.MEMBER_ORGANIZATION;
    }

    private boolean isRoleMemberPanel() {
        return collectionPanelType == CollectionPanelType.ROLE_MEMBER_MEMBER
                || collectionPanelType == CollectionPanelType.ROLE_MEMBER_GOVERNANCE;
    }
    private static QName getDefaultRelationAllowAny(List<QName> availableRelationList) {
        if (availableRelationList != null && availableRelationList.size() == 1) {
            return availableRelationList.get(0);
        }
        return PrismConstants.Q_ANY;
    }

    public List<QName> getSupportedRelations() {
        if (collectionPanelType == null) {
            return null;
        }

        switch (collectionPanelType) {
            case MEMBER_ORGANIZATION:
                return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ORGANIZATION, modelServiceLocator);
            case ROLE_MEMBER_GOVERNANCE:
            case SERVICE_MEMBER_GOVERNANCE:
            case ORG_MEMBER_GOVERNANCE:
            case CARDS_GOVERNANCE:
            case ARCHETYPE_MEMBER_GOVERNANCE:
                return getSupportedGovernanceTabRelations(modelServiceLocator);
            case ARCHETYPE_MEMBER_MEMBER:
                return Arrays.asList(SchemaConstants.ORG_DEFAULT);
            case ROLE_MEMBER_MEMBER:
            case SERVICE_MEMBER_MEMBER:
            case ORG_MEMBER_MEMBER:
            case MEMBER_WIZARD:
                return getSupportedMembersTabRelations();
        }
        return null;
    }

    private List<QName> getSupportedMembersTabRelations() {
        List<QName> relations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, modelServiceLocator);
        List<QName> governance = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, modelServiceLocator);
        governance.forEach(relations::remove);
        return relations;
    }

    private static List<QName> getSupportedGovernanceTabRelations(ModelServiceLocator modelServiceLocator) {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.GOVERNANCE, modelServiceLocator);
    }

    public static List<QName> getSupportedObjectTypes(CollectionPanelType collectionPanelType) {
        if (collectionPanelType == null) {
            return new ArrayList<>();
        }
        switch (collectionPanelType) {
            case ROLE_MEMBER_MEMBER:
            case ROLE_MEMBER_GOVERNANCE:
            case SERVICE_MEMBER_MEMBER:
            case SERVICE_MEMBER_GOVERNANCE:
            case ORG_MEMBER_GOVERNANCE:
            case CARDS_GOVERNANCE:
            case ARCHETYPE_MEMBER_GOVERNANCE:
                return WebComponentUtil.createFocusTypeList();
            case ORG_MEMBER_MEMBER:
            case MEMBER_ORGANIZATION:
            case ARCHETYPE_MEMBER_MEMBER:
                List<QName> supportedObjectTypes = WebComponentUtil.createAssignmentHolderTypeQnamesList();
                supportedObjectTypes.remove(AssignmentHolderType.COMPLEX_TYPE);
                return supportedObjectTypes;
            case DEBUG:
                return WebComponentUtil.createObjectTypesList().stream()
                        .map(type -> type.getTypeQName()).collect(Collectors.toList());
            case ASSIGNABLE:
                return Arrays.asList(
                        AbstractRoleType.COMPLEX_TYPE,
                        OrgType.COMPLEX_TYPE,
                        ArchetypeType.COMPLEX_TYPE,
                        RoleType.COMPLEX_TYPE,
                        ServiceType.COMPLEX_TYPE);
            case MEMBER_WIZARD:
                return Arrays.asList(UserType.COMPLEX_TYPE);
        }
        return new ArrayList<>();
    }

    public static <C> boolean isFixedItem(Class<C> typeClass, ItemPath path) {

        while (typeClass != null && !com.evolveum.prism.xml.ns._public.types_3.ObjectType.class.equals(typeClass)) {
            if (FIXED_SEARCH_ITEMS.get(typeClass) != null &&
                    ItemPathCollectionsUtil.containsEquivalent(FIXED_SEARCH_ITEMS.get(typeClass), path)) {
                return true;
            }
            typeClass = (Class<C>) typeClass.getSuperclass();
        }

        return false;
    }

}
