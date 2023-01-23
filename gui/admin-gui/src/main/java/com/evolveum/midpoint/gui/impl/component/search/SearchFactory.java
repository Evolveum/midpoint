/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.*;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

public class SearchFactory<C extends Containerable> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFactory.class);
    private static final String DOT_CLASS = SearchFactory.class.getName() + ".";
    private static final String LOAD_SYSTEM_CONFIGURATION = DOT_CLASS + "loadSystemConfiguration";

    private Class<C> type;
    private CompiledObjectCollectionView collectionView;
    private ModelServiceLocator modelServiceLocator;

    private String nameSearch;

    private boolean isPreview;

    private boolean isViewForDashboard;

    private Map<ItemPath, ItemDefinition<?>> allSearchableItems;

    private SearchContext additionalSearchContext;

    public SearchFactory(Class<C> type) {
        this.type = type;
    }

    public SearchFactory<C> type(Class<C> type) {
        this.type = type;
        return this;
    }

    public SearchFactory<C> collectionView(CompiledObjectCollectionView collectionView) {
        this.collectionView = collectionView;
        return this;
    }
    public SearchFactory<C> modelServiceLocator(ModelServiceLocator modelServiceLocator) {
        this.modelServiceLocator = modelServiceLocator;
        return this;
    }
    public SearchFactory<C> nameSearch(String nameSearch) {
        this.nameSearch = nameSearch;
        return this;
    }
    public SearchFactory isPreview(boolean isPreview) {
        this.isPreview = isPreview;
        return this;
    }

    public SearchFactory<C> isViewForDashboard(boolean isViewForDashboard) {
        this.isViewForDashboard = isViewForDashboard;
        return this;
    }

    public SearchFactory<C> additionalSearchContext(SearchContext additionalSearchContext) {
        this.additionalSearchContext = additionalSearchContext;
        return this;
    }

    public Search<C> createSearch() {
        PredefinedSearchableItems predefinedSearchableItems = new PredefinedSearchableItems(type, modelServiceLocator);
        if (additionalSearchContext != null) {
            predefinedSearchableItems = predefinedSearchableItems.resourceObjectDefinition(additionalSearchContext.getResourceObjectDefinition())
                    .assignmentTargetType(additionalSearchContext.getAssignmentTargetType())
                    .panelType(additionalSearchContext.getPanelType())
                    .containerDefinition(additionalSearchContext.getDefinitionOverride());
        }
        allSearchableItems =  predefinedSearchableItems.createAvailableSearchItems();
        SearchBoxConfigurationType mergedConfig = getMergedConfiguration();

        SearchConfigurationWrapper<C> basicSearchWrapper = createBasicSearchWrapper(mergedConfig);

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


        return search;
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
        if (PredefinedSearchableItems.PanelType.ASSOCIABLE_SHADOW.equals(additionalSearchContext.getPanelType())) {
            AssociationSearchItemWrapper wrapper = new AssociationSearchItemWrapper(additionalSearchContext.getResourceObjectDefinition());
            search.getItems().add(0, wrapper);
        }
    }

    private SearchBoxConfigurationType getMergedConfiguration() {
        SearchBoxConfigurationType configuredSearchBox = getConfiguredSearchBox();
        SearchBoxConfigurationType defaultSearchBoxConfig = new SearchBoxConfigurationBuilder()
                .type(type)
                .availableDefinitions(allSearchableItems)
                .additionalSearchContext(additionalSearchContext)
                .modelServiceLocator(modelServiceLocator)
                .create();

        return SearchConfigurationMerger.mergeConfigurations(defaultSearchBoxConfig, configuredSearchBox, modelServiceLocator);
    }

    private Search<C> createSearch(SearchBoxConfigurationType mergedConfig, SearchConfigurationWrapper<C> basicSearchWrapper) {
        AxiomQueryWrapper<C> axiomWrapper = new AxiomQueryWrapper<>();
        AdvancedQueryWrapper advancedQueryWrapper = new AdvancedQueryWrapper();
        FulltextQueryWrapper fulltextQueryWrapper = new FulltextQueryWrapper();

        ObjectTypeSearchItemWrapper<C> objectTypeSearchItemWrapper = new ObjectTypeSearchItemWrapper<>(mergedConfig.getObjectTypeConfiguration());
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

        search.setSearchMode(getDefaultSearchMode(mergedConfig, type));
        search.setAllowedModeList(mergedConfig.getAllowedMode());
        if (collectionView !=  null) {
            search.setCollectionViewName(collectionView.getViewIdentifier());
            if (collectionView.getCollection() != null && collectionView.getCollection().getCollectionRef() != null) {
                search.setCollectionRefOid(collectionView.getCollection().getCollectionRef().getOid());
            }
        }

        return search;
    }


    private SearchConfigurationWrapper<C> createBasicSearchWrapper(SearchBoxConfigurationType configuredSearchBox) {
        SearchConfigurationWrapper<C> basicSearchWrapper = createDefaultSearchBoxConfigurationWrapper(configuredSearchBox);
        basicSearchWrapper.setAllowToConfigureSearchItems(isAllowToConfigureSearchItems(configuredSearchBox));

        if (isViewForDashboard) {
            basicSearchWrapper.getItemsList().add(new ObjectCollectionSearchItemWrapper(collectionView));
        }

        initCollectionViewList(basicSearchWrapper);
        sortItems(basicSearchWrapper);
        return basicSearchWrapper;
    }

    private void initCollectionViewList(SearchConfigurationWrapper<C> basicSearchWrapper) {
        if (collectionView == null || !collectionView.isDefaultView()) {
            return;
        }

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

    private void sortItems(SearchConfigurationWrapper<C> basicSearchWrapper) {
        basicSearchWrapper.getItemsList().sort((i1, i2) -> String.CASE_INSENSITIVE_ORDER.compare(
                StringUtils.isEmpty(i1.getName()) ? "" : PageBase.createStringResourceStatic(i1.getName()).getString(),
                StringUtils.isEmpty(i2.getName()) ? "" : PageBase.createStringResourceStatic(i2.getName()).getString()));

        basicSearchWrapper.getItemsList().sort(Comparator.comparing(i -> i instanceof PropertySearchItemWrapper));
    }

    private SearchBoxModeType getDefaultSearchMode(SearchBoxConfigurationType config, Class<C> type) {
        List<SearchBoxModeType> allowedModes = config.getAllowedMode();
        if (isFullTextSearchEnabled(type) && allowedModes.contains(SearchBoxModeType.FULLTEXT)) {
            return SearchBoxModeType.FULLTEXT;
        }
        if (allowedModes.size() == 1) {
            return allowedModes.get(0);
        }
        return config.getDefaultMode();
    }

    private SearchBoxConfigurationType getConfiguredSearchBox() {
        if (collectionView == null) {
            return null;
        }

        //TODO legacy support for members?
        return collectionView.getSearchBoxConfiguration();
    }

    public void createAbstractRoleSearchItemWrapperList(SearchConfigurationWrapper<C> searchConfigWrapper, SearchBoxConfigurationType config) {
        AbstractRoleSearchItemWrapper roleSearchWrapper = new AbstractRoleSearchItemWrapper(config);
        if (roleSearchWrapper.isNotEmpty()){
            searchConfigWrapper.getItemsList().add(roleSearchWrapper);
        }

    }

    public SearchConfigurationWrapper<C> createDefaultSearchBoxConfigurationWrapper(SearchBoxConfigurationType mergedConfig) {
        SearchConfigurationWrapper<C> searchConfigWrapper = new SearchConfigurationWrapper<>();
        SearchItemsType searchItems = mergedConfig.getSearchItems();
        for (SearchItemType searchItem : searchItems.getSearchItem()) {
            searchConfigWrapper.getItemsList().add(SearchConfigurationWrapperFactory.createPropertySearchItemWrapper(type, allSearchableItems, searchItem, modelServiceLocator));
        }

        return searchConfigWrapper;
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
