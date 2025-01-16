/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.*;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SearchBuilder<C extends Serializable> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchBuilder.class);
    private static final String DOT_CLASS = SearchBuilder.class.getName() + ".";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";

    private Class<C> type;
    private CompiledObjectCollectionView collectionView;
    private ModelServiceLocator modelServiceLocator;

    private String nameSearch;

    private boolean isPreview;

    private boolean isViewForDashboard;

    private boolean isFullTextSearchEnabled = true;

    private boolean typeChanged = false;

    private PathKeyedMap<ItemDefinition<?>> allSearchableItems;

    private SearchContext additionalSearchContext;

    public SearchBuilder(Class<C> type) {
        this.type = type;
    }

    public SearchBuilder<C> type(Class<C> type) {
        this.type = type;
        return this;
    }

    public SearchBuilder<C> collectionView(CompiledObjectCollectionView collectionView) {
        this.collectionView = collectionView;
        return this;
    }

    public SearchBuilder<C> modelServiceLocator(ModelServiceLocator modelServiceLocator) {
        this.modelServiceLocator = modelServiceLocator;
        return this;
    }

    public SearchBuilder<C> nameSearch(String nameSearch) {
        this.nameSearch = nameSearch;
        return this;
    }

    public SearchBuilder isPreview(boolean isPreview) {
        this.isPreview = isPreview;
        return this;
    }

    public SearchBuilder<C> isViewForDashboard(boolean isViewForDashboard) {
        this.isViewForDashboard = isViewForDashboard;
        return this;
    }

    public SearchBuilder<C> additionalSearchContext(SearchContext additionalSearchContext) {
        this.additionalSearchContext = additionalSearchContext;
        return this;
    }

    public SearchBuilder<C> setFullTextSearchEnabled(boolean fullTextSearchEnabled) {
        isFullTextSearchEnabled = fullTextSearchEnabled;
        return this;
    }

    public SearchBuilder<C> setTypeChanged(boolean typeChanged) {
        this.typeChanged = typeChanged;
        return this;
    }

    public Search<C> build() {
        SearchableItemsDefinitions searchableItemsDefinitions =
                new SearchableItemsDefinitions(type, modelServiceLocator)
                        .additionalSearchContext(additionalSearchContext);
        allSearchableItems = searchableItemsDefinitions.createAvailableSearchItems();
        SearchBoxConfigurationType mergedConfig = getMergedConfiguration();

        BasicQueryWrapper basicSearchWrapper = createBasicSearchWrapper(mergedConfig);

        createAbstractRoleSearchItemWrapperList(basicSearchWrapper, mergedConfig);

        Search<C> search = createSearch(mergedConfig, basicSearchWrapper);

        initSearchByNameIfNeeded(search);

        initAssociationWrapperIfNeeded(search);

        // technically, it should not be here. but for now, we can live with it
        initTimestampForAudit(search);

//            if (storage != null && view.getPaging() != null) {
//                ObjectPaging paging = ObjectQueryUtil.convertToObjectPaging(view.getPaging(), getPrismContext());
//                if (storage.getPaging() == null) {
//                    storage.setPaging(paging);
//                }
//                if (getTableId() != null && paging.getMaxSize() != null
//                        && !getPageBase().getSessionStorage().getUserProfile().isExistPagingSize(getTableId())) {
//                    getPageBase().getSessionStorage().getUserProfile().setPagingSize(getTableId(), paging.getMaxSize());
//                }
//            }

        initProcessedObjectState(search);

        return search;
    }

    private void initProcessedObjectState(Search search) {
        if (!(SimulationResultProcessedObjectType.class.equals(type))) {
            return;
        }

        if (additionalSearchContext == null) {
            return;
        }

        ObjectProcessingStateType state = additionalSearchContext.getObjectProcessingState();
        if (state == null) {
            return;
        }

        ChoicesSearchItemWrapper<ObjectProcessingStateType> item = (ChoicesSearchItemWrapper<ObjectProcessingStateType>)
                search.findPropertySearchItem(SimulationResultProcessedObjectType.F_STATE);
        if (item == null) {
            return;
        }

        DisplayableValue<ObjectProcessingStateType> value = item.getAvailableValues().stream().filter(d -> state.equals(d.getValue())).findFirst().orElse(null);
        if (value != null) {
            item.setValue(value);
        }
    }

    private void initSearchByNameIfNeeded(Search<C> search) {
        if (!isNameSearchRequested(search)) {
            return;
        }

        if (SearchBoxModeType.FULLTEXT.equals(search.getSearchMode())) {
            search.setFullText(nameSearch);
            return;
        }

        PropertySearchItemWrapper<String> nameItem = search.findPropertySearchItem(ObjectType.F_NAME);
        if (nameItem == null) {
            return;
        }

        nameItem.setValue(new SearchValue<>(nameSearch));
    }

    private boolean isNameSearchRequested(Search<C> search) {
        return nameSearch != null && !search.searchByNameEquals(nameSearch);
    }

    private void initTimestampForAudit(Search<C> search) {
        if (!(AuditEventRecordType.class.equals(type))) {
            return;
        }
        DateSearchItemWrapper timestampItem = (DateSearchItemWrapper) search.findPropertySearchItem(AuditEventRecordType.F_TIMESTAMP);
        if (timestampItem != null && timestampItem.getSingleDate() == null && timestampItem.getIntervalSecondDate() == null
                && !isViewForDashboard && !isPreview) {
            Date todayDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
            timestampItem.setSingleDate(MiscUtil.asXMLGregorianCalendar(todayDate));
        }
    }

    private void initAssociationWrapperIfNeeded(Search<C> search) {
        if (additionalSearchContext == null) {
            return;
        }
        if (CollectionPanelType.ASSOCIABLE_SHADOW.equals(additionalSearchContext.getPanelType())) {
            AssociationSearchItemWrapper wrapper = new AssociationSearchItemWrapper(additionalSearchContext.getResourceObjectDefinition());
            search.getItems().add(0, wrapper);
        }
    }

    private SearchBoxConfigurationType getMergedConfiguration() {
        SearchBoxConfigurationType configuredSearchBox = getConfiguredSearchBox();
        resolveRealSearchType(configuredSearchBox);
        SearchBoxConfigurationType defaultSearchBoxConfig = new SearchBoxConfigurationBuilder()
                .type(type)
                .availableDefinitions(allSearchableItems)
                .additionalSearchContext(additionalSearchContext)
                .modelServiceLocator(modelServiceLocator)
                .fullTextSearchEnabled(isFullTextSearchEnabled())
                .create();

        return SearchConfigurationMerger.mergeConfigurations(defaultSearchBoxConfig, configuredSearchBox, modelServiceLocator);
    }

    private void resolveRealSearchType(SearchBoxConfigurationType configuredSearchBox) {
        if (configuredSearchBox == null) {
            return;
        }
        //if the search is loaded because of the changed type, the new selected type value
        //should be used for search instead of configured default type value
        if (typeChanged) {
            QName searchType = WebComponentUtil.anyClassToQName(modelServiceLocator.getPrismContext(), type);
            configuredSearchBox.setDefaultObjectType(searchType);
            if (configuredSearchBox.getObjectTypeConfiguration() != null) {
                configuredSearchBox.getObjectTypeConfiguration().setDefaultValue(searchType);
            }
        }
    }

    public ItemDefinition<?> getDefinitionOverride() {
        return additionalSearchContext == null ? null : additionalSearchContext.getDefinitionOverride();
    }

    private Search<C> createSearch(SearchBoxConfigurationType mergedConfig, BasicQueryWrapper basicSearchWrapper) {
        AxiomQueryWrapper axiomWrapper = new AxiomQueryWrapper(getDefinitionOverride());
        AdvancedQueryWrapper advancedQueryWrapper = new AdvancedQueryWrapper(null);
        FulltextQueryWrapper fulltextQueryWrapper = new FulltextQueryWrapper(null);
        if (AssignmentType.class.equals(type)) {
            fulltextQueryWrapper = new AssignmentFulltextQueryWrapper(null);
        }

        ObjectTypeSearchItemWrapper objectTypeSearchItemWrapper = new ObjectTypeSearchItemWrapper(mergedConfig.getObjectTypeConfiguration());
        objectTypeSearchItemWrapper.setAllowAllTypesSearch(isAllowedAllTypesSearch());
        objectTypeSearchItemWrapper.setValueForNull(getValueRepresentingAllTypes());
        Search<C> search = new Search<>(objectTypeSearchItemWrapper, mergedConfig);

        OidSearchItemWrapper oidSearchItemWrapper = new OidSearchItemWrapper();
        oidSearchItemWrapper.setValue(new SearchValue<>());
        search.setOidSearchItemWrapper(oidSearchItemWrapper);
        search.setAdvancedQueryWrapper(advancedQueryWrapper);
        search.setAxiomQueryWrapper(axiomWrapper);
        search.setFulltextQueryWrapper(fulltextQueryWrapper);
        search.setSearchConfigurationWrapper(basicSearchWrapper);

        search.setSearchMode(mergedConfig.getDefaultMode());
        search.setAllowedModeList(mergedConfig.getAllowedMode());
        if (collectionView != null) {
            search.setCollectionViewName(collectionView.getViewIdentifier());
            if (collectionView.getCollection() != null && collectionView.getCollection().getCollectionRef() != null) {
                search.setCollectionRefOid(collectionView.getCollection().getCollectionRef().getOid());
            }
            if (collectionView.getFilter() != null) {
                search.setCollectionFilter(collectionView.getFilter());
            }
        }

        return search;
    }

    private BasicQueryWrapper createBasicSearchWrapper(SearchBoxConfigurationType configuredSearchBox) {
        BasicQueryWrapper basicSearchWrapper = createDefaultSearchBoxConfigurationWrapper(configuredSearchBox);
        basicSearchWrapper.setAllowToConfigureSearchItems(isAllowToConfigureSearchItems(configuredSearchBox));

        if (isViewForDashboard) {
            basicSearchWrapper.getItemsList().add(new ObjectCollectionSearchItemWrapper(collectionView));
        }

        initCollectionViewList(basicSearchWrapper);
        sortItems(basicSearchWrapper);
        return basicSearchWrapper;
    }

    private void initCollectionViewList(BasicQueryWrapper basicSearchWrapper) {
        if (collectionView == null || !collectionView.isDefaultView()) {
            return;
        }

        List<CompiledObjectCollectionView> views = modelServiceLocator.getCompiledGuiProfile()
                .findAllApplicableObjectCollectionViews(WebComponentUtil.anyClassToQName(PrismContext.get(), type))
                .stream()
                .filter(v -> v.getFilter() != null)     //todo should we check also collectionRef?
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(views)) {
            ObjectCollectionListSearchItemWrapper viewListItem = new ObjectCollectionListSearchItemWrapper(type,
                    views);
            viewListItem.setVisible(true);
            basicSearchWrapper.getItemsList().add(viewListItem);
        }

    }

    private void sortItems(BasicQueryWrapper basicSearchWrapper) {
        basicSearchWrapper.getItemsList().sort((i1, i2) -> String.CASE_INSENSITIVE_ORDER.compare(
                StringUtils.isEmpty(i1.getName().getObject()) ? "" : PageBase.createStringResourceStatic(i1.getName().getObject()).getString(),
                StringUtils.isEmpty(i2.getName().getObject()) ? "" : PageBase.createStringResourceStatic(i2.getName().getObject()).getString()));

        basicSearchWrapper.getItemsList().sort(Comparator.comparing(i -> i instanceof PropertySearchItemWrapper));
    }

    private SearchBoxConfigurationType getConfiguredSearchBox() {
        if (collectionView == null) {
            return null;
        }

        //TODO legacy support for members?
        return collectionView.getSearchBoxConfiguration();
    }

    public void createAbstractRoleSearchItemWrapperList(BasicQueryWrapper searchConfigWrapper, SearchBoxConfigurationType config) {
        AbstractRoleSearchItemWrapper roleSearchWrapper = new AbstractRoleSearchItemWrapper(config);
        if (roleSearchWrapper.isNotEmpty()) {
            searchConfigWrapper.getItemsList().add(roleSearchWrapper);
        }

    }

    public BasicQueryWrapper createDefaultSearchBoxConfigurationWrapper(SearchBoxConfigurationType mergedConfig) {
        BasicQueryWrapper searchConfigWrapper = new BasicQueryWrapper();
        SearchItemsType searchItems = mergedConfig.getSearchItems();
        if (searchItems == null) {
            return searchConfigWrapper;
        }

        for (SearchItemType searchItem : searchItems.getSearchItem()) {
            searchConfigWrapper.getItemsList().add(SearchConfigurationWrapperFactory.createPropertySearchItemWrapper(
                    type, allSearchableItems, searchItem, additionalSearchContext, modelServiceLocator));
        }

        return searchConfigWrapper;
    }

    private boolean isFullTextSearchEnabled() {
        if (!isFullTextSearchEnabled) {
            return false;
        }
        //This is not entirely correct. however, there is no clever options for now for handling fullText for assignments
        // The main problem is the isFullTextSearchEnabled(type), since the fulltext for assignment is not actually for
        // AssignmentType, but for its targetRef. So, fulltext for AbstractRoleType has to be specified
        if (AssignmentType.class.isAssignableFrom(type)) {
            return isFullTextSearchEnabled((Class<C>) AbstractRoleType.class);
        }
        return isFullTextSearchEnabled(type);
    }

    private boolean isFullTextSearchEnabled(Class<C> type) {
        OperationResult result = new OperationResult(LOAD_SYSTEM_CONFIGURATION);
        try {
            return FullTextSearchUtil.isEnabledFor(modelServiceLocator.getModelInteractionService().getSystemConfiguration(result)
                    .getFullTextSearch(), type);
        } catch (SchemaException | ObjectNotFoundException ex) {
            throw new SystemException(ex);
        }
    }

    private boolean isAllowToConfigureSearchItems(SearchBoxConfigurationType searchBoxConfigurationType) {
        if (searchBoxConfigurationType == null || searchBoxConfigurationType.isAllowToConfigureSearchItems() == null) {
            return true; //todo should be set to false
        }
        return searchBoxConfigurationType.isAllowToConfigureSearchItems();
    }

    private boolean isAllowedAllTypesSearch() {
        if (additionalSearchContext == null || additionalSearchContext.getPanelType() == null) {
            return false;
        }
        return additionalSearchContext.getPanelType().isAllowAllTypeSearch();
    }

    private QName getValueRepresentingAllTypes() {
        if (additionalSearchContext == null || additionalSearchContext.getPanelType() == null) {
            return null;
        }
        return additionalSearchContext.getPanelType().getTypeForNull();
    }
}
