/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class SaveSearchPanel<C extends Containerable> extends BasePanel<Search<C>> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SaveSearchPanel.class);
    private static final String ID_FEEDBACK_MESSAGE = "feedbackMessage";
    private static final String ID_SEARCH_NAME = "searchName";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    private final Class<C> type;
    IModel<String> feedbackMessageModel = Model.of();
    IModel<String> queryNameModel = Model.of();
    private final String defaultCollectionViewIdentifier;

    public SaveSearchPanel(String id, IModel<Search<C>> searchModel, Class<C> type, String defaultCollectionViewIdentifier) {
        super(id, searchModel);
        this.type = type;
        this.defaultCollectionViewIdentifier = defaultCollectionViewIdentifier;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MessagePanel<?> feedbackMessage = new MessagePanel<>(ID_FEEDBACK_MESSAGE, MessagePanel.MessagePanelType.WARN, feedbackMessageModel);
        feedbackMessage.add(new VisibleBehaviour(() -> feedbackMessageModel.getObject() != null && StringUtils.isNotEmpty(feedbackMessageModel.getObject())));
        feedbackMessage.setOutputMarkupId(true);
        add(feedbackMessage);

        TextField<String> nameField = new TextField<>(ID_SEARCH_NAME, queryNameModel);
        nameField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        nameField.setOutputMarkupId(true);
        add(nameField);

        WebMarkupContainer buttonsPanel = new WebMarkupContainer(ID_BUTTONS_PANEL);
        buttonsPanel.setOutputMarkupId(true);
        add(buttonsPanel);

        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                if (StringUtils.isEmpty(queryNameModel.getObject())) {
                    feedbackMessageModel = createStringResource("SaveSearchPanel.enterQueryNameWarning");
                    ajaxRequestTarget.add(feedbackMessage);
                    return;
                }
                saveCustomQuery(ajaxRequestTarget);
                getPageBase().hideMainPopup(ajaxRequestTarget);
                saveSearchFilterPerformed(ajaxRequestTarget);
            }
        };
        saveButton.setOutputMarkupId(true);
        buttonsPanel.add(saveButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("Button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        cancelButton.setOutputMarkupId(true);
        buttonsPanel.add(cancelButton);
    }

    protected void saveSearchFilterPerformed(AjaxRequestTarget target) {
    }

    private void saveCustomQuery(AjaxRequestTarget ajaxRequestTarget) {
        Search<C> search = getModelObject();
        AvailableFilterType availableFilter = new AvailableFilterType();
        availableFilter.setDisplay(new DisplayType().label(queryNameModel.getObject()));
        availableFilter.setSearchMode(getModelObject().getSearchMode());
        SearchItemType searchItem = null;
        if (SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())) {
            availableFilter.getSearchItem().addAll(getAvailableFilterSearchItems(type, search.getItems(), search.getSearchMode()));
        } else {
            if (SearchBoxModeType.AXIOM_QUERY.equals(getModelObject().getSearchMode())) {
                searchItem = createAxiomSearchItem();
            } else if (SearchBoxModeType.ADVANCED.equals(getModelObject().getSearchMode())) {
                searchItem = createAdvancedSearchItem();
            } else if (SearchBoxModeType.FULLTEXT.equals(getModelObject().getSearchMode())) {
                searchItem = createFulltextSearchItem();
            } else if (SearchBoxModeType.OID.equals(getModelObject().getSearchMode())) {
                searchItem = createOidSearchItem(getModelObject().findOidSearchItemWrapper());
            }
            if (searchItem != null) {
                availableFilter.getSearchItem().add(searchItem);
            }
        }
        if (CollectionUtils.isEmpty(availableFilter.getSearchItem())) {
            ajaxRequestTarget.add(getPageBase().getFeedbackPanel());
            return;
        }
        saveSearchItemToAdminConfig(availableFilter, ajaxRequestTarget);
    }

    private List<SearchItemType> getAvailableFilterSearchItems(Class<C> typeClass, List<AbstractSearchItemWrapper> items, SearchBoxModeType mode) {
        List<SearchItemType> searchItems = new ArrayList<>();
        for (AbstractSearchItemWrapper item : items) {
            if (!item.isApplyFilter(mode)) {
                continue;
            }
            ObjectFilter filter = item.createFilter(typeClass, getPageBase(), null);
            if (filter != null) {
                SearchItemType searchItem = new SearchItemType();
                if (item instanceof PropertySearchItemWrapper) {
                    searchItem.setPath(new ItemPathType(((PropertySearchItemWrapper) item).getPath()));
                }
                searchItem.setDisplay(new DisplayType().label(item.getName()).help(item.getHelp()));
                try {
                    searchItem.setFilter(getPageBase().getQueryConverter().createSearchFilterType(filter));
                } catch (SchemaException e) {
                    LOGGER.error("Unable to create search filter from query: {}, {}", filter, e.getLocalizedMessage());
                }
                searchItem.setVisibleByDefault(true);
                searchItems.add(searchItem);
                //todo do later non property items - oid, type...
            }
        }
        return searchItems;
    }

    private SearchItemType createAxiomSearchItem() {
        try {
            SearchItemType axiomSearchItem = new SearchItemType();
            ObjectFilter axiomFilter = PrismContext.get()
                    .createQueryParser(PrismContext.get().getSchemaRegistry().staticNamespaceContext().allPrefixes())
                    .parseFilter(getModelObject().getTypeClass(), getModelObject().getDslQuery());
            axiomSearchItem.setFilter(PrismContext.get().getQueryConverter().createSearchFilterType(axiomFilter));
            return axiomSearchItem;
        } catch (SchemaException e) {
            LOGGER.error("Unable to parse axiom filter from query: {}, {}", getModelObject().getDslQuery(), e.getLocalizedMessage());
            getPageBase().error("Unable to parse axiom filter from query: " + getModelObject().getDslQuery());
        }
        return null;
    }

    private SearchItemType createAdvancedSearchItem() {
        try {
            SearchItemType advancedSearchItem = new SearchItemType();

            SearchFilterType search = PrismContext.get().parserFor(getModelObject().getAdvancedQuery()).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
            ObjectFilter advancedFilter = PrismContext.get().getQueryConverter().parseFilter(search, getModelObject().getTypeClass());
            advancedSearchItem.setFilter(PrismContext.get().getQueryConverter().createSearchFilterType(advancedFilter));
            return advancedSearchItem;
        } catch (Exception e) {
            LOGGER.error("Unable to parse advanced filter from query: {}, {}", getModelObject().getAdvancedQuery(), e.getLocalizedMessage());
            getPageBase().error("Unable to parse advanced filter from query: " + getModelObject().getAdvancedQuery());
        }
        return null;
    }

    private SearchItemType createFulltextSearchItem() {
        try {
            SearchItemType fulltextSearchItem = new SearchItemType();
            ObjectFilter filter = PrismContext.get().queryFor(getModelObject().getTypeClass())
                    .fullText(getModelObject().getFullText())
                    .buildFilter();
            fulltextSearchItem.setFilter(PrismContext.get().getQueryConverter().createSearchFilterType(filter));
            return fulltextSearchItem;
        } catch (SchemaException e) {
            LOGGER.error("Unable to create fulltext filter from query: {}, {}", getModelObject().getFullText(), e.getLocalizedMessage());
            getPageBase().error("Unable to parse fulltext filter from query: " + getModelObject().getFullText());
        }
        return null;
    }

    private SearchItemType createOidSearchItem(OidSearchItemWrapper oidSearchItemWrapper) {
        try {
            SearchItemType oidSearchItem = new SearchItemType();
            ObjectFilter filter = oidSearchItemWrapper.createFilter(getModelObject().getTypeClass(), getPageBase(), null);
            oidSearchItem.setFilter(PrismContext.get().getQueryConverter().createSearchFilterType(filter));
            return oidSearchItem;
        } catch (SchemaException e) {
            LOGGER.error("Unable to parse oid filter from query: {}, {}", oidSearchItemWrapper.getValue().getValue(), e.getLocalizedMessage());
            getPageBase().error("Unable to parse oid filter from query: " + oidSearchItemWrapper.getValue().getValue());
        }
        return null;
    }

    private void saveSearchItemToAdminConfig(AvailableFilterType availableFilter, AjaxRequestTarget ajaxRequestTarget) {
        FocusType principalFocus = getPageBase().getPrincipalFocus();
        boolean viewExists = true;
        boolean addItemToPath = true;
        List<ItemName> path = new ArrayList<>();
        Object valueToAdd = null;
        if (!(principalFocus instanceof UserType)) {
            return;
        }
        AdminGuiConfigurationType adminGui = ((UserType) principalFocus).getAdminGuiConfiguration();
        if (adminGui == null) {
            viewExists = false;
            adminGui = new AdminGuiConfigurationType();
            valueToAdd = adminGui;
            addItemToPath = false;
        }
        path.add(UserType.F_ADMIN_GUI_CONFIGURATION);

        GuiObjectListViewsType views = adminGui.getObjectCollectionViews();
        if (addItemToPath) {
            path.add(AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS);
        }
        if (views == null) {
            viewExists = false;
            views = new GuiObjectListViewsType();
            if (valueToAdd != null) {
                adminGui.objectCollectionViews(views);
            } else {
                valueToAdd = views;
            }
            addItemToPath = false;
        }

        StringValue collectionViewParameter = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
        String viewName = collectionViewParameter == null || collectionViewParameter.isNull() ? defaultCollectionViewIdentifier
                : collectionViewParameter.toString();
        GuiObjectListViewType objectListView = null;
        for (GuiObjectListViewType listView : views.getObjectCollectionView()) {
            if (viewName.equals(listView.getIdentifier())) {
                objectListView = listView;
            }
        }
        if (addItemToPath) {
            path.add(GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW);
        }
        if (objectListView == null) {
            viewExists = false;
            objectListView = new GuiObjectListViewType();
            objectListView.setType(WebComponentUtil.containerClassToQName(PrismContext.get(), type));
            if (valueToAdd != null) {
                views.getObjectCollectionView().add(objectListView);
            } else {
                valueToAdd = objectListView;
            }
            addItemToPath = false;
        }
        if (objectListView.getIdentifier() == null) {
            StringValue viewIdentifier = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
            objectListView.setIdentifier(viewIdentifier == null || viewIdentifier.isNull() || viewIdentifier.isNull() ?
                    defaultCollectionViewIdentifier : viewIdentifier.toString());
        }
        SearchBoxConfigurationType searchConfig = objectListView.getSearchBoxConfiguration();
        if (addItemToPath) {
            path.add(GuiObjectListViewType.F_SEARCH_BOX_CONFIGURATION);
            path.add(SearchBoxConfigurationType.F_AVAILABLE_FILTER);
        }
        if (searchConfig == null) {
            searchConfig = new SearchBoxConfigurationType();
            if (valueToAdd != null) {
                objectListView.setSearchBoxConfiguration(searchConfig);
            } else {
                valueToAdd = availableFilter;
            }
        }
        if (searchConfig.getAvailableFilter() == null) {
            searchConfig.beginAvailableFilter();
        }

        OperationResult result = new OperationResult("save search to user");
        try {
            ObjectDelta<UserType> userDelta;
            if (!viewExists) {
                searchConfig.getAvailableFilter().add(availableFilter);
                userDelta = getPrismContext().deltaFor(UserType.class)
                        .item(path.toArray(ItemName[]::new))
                        .add(valueToAdd).asObjectDelta(principalFocus.getOid());
            } else {
                Object[] viewPath = new Object[] { UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS,
                        GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW, objectListView.getId(), GuiObjectListViewType.F_SEARCH_BOX_CONFIGURATION,
                        SearchBoxConfigurationType.F_AVAILABLE_FILTER };
                userDelta = getPrismContext().deltaFor(UserType.class)
                        .item(viewPath)
                        .add(availableFilter).asObjectDelta(principalFocus.getOid());
            }
            WebModelServiceUtils.save(userDelta, result, getPageBase().createSimpleTask("task"), getPageBase());
        } catch (Exception e) {
            LOGGER.error("Unable to save a filter to user, {}", e.getLocalizedMessage());
            error("Unable to save a filter to user, " + e.getLocalizedMessage());
            ajaxRequestTarget.add(getPageBase().getFeedbackPanel());
            return;
        }
        result.recomputeStatus();
        getPageBase().showResult(result);
        ajaxRequestTarget.add(getPageBase().getFeedbackPanel());
    }

    @Override
    public int getWidth() {
        return 500;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return getPageBase().createStringResource("SaveSearchPanel.saveSearch");
    }
}
