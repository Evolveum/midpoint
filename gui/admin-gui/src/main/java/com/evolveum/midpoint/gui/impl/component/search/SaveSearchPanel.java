/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.string.StringValue;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

public class SaveSearchPanel<C extends Containerable> extends BasePanel<ObjectQuery> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SaveSearchPanel.class);
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_SEARCH_NAME = "searchName";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    private Class<C> type;
    IModel<String> feedbackMessageModel = Model.of();
    IModel<String> queryNameModel = Model.of();
    public SaveSearchPanel(String id, IModel<ObjectQuery> queryModel, Class<C> type) {
        super(id, queryModel);
        this.type = type;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        Label feedbackMessage = new Label(ID_FEEDBACK, feedbackMessageModel);
        feedbackMessage.setOutputMarkupId(true);
        feedbackMessage.add(new VisibleBehaviour(() -> feedbackMessageModel.getObject() != null && StringUtils.isNotEmpty(feedbackMessageModel.getObject())));
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
                    ajaxRequestTarget.add(SaveSearchPanel.this);
                    return;
                }
                saveCustomQuery();
                getPageBase().hideMainPopup(ajaxRequestTarget);
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

    private void saveCustomQuery() {
        ObjectQuery query = getModelObject();
        SearchItemType searchItemType = new SearchItemType();
        searchItemType.setDisplayName(new PolyStringType(queryNameModel.getObject()));
        try {
            searchItemType.setFilter(getPageBase().getQueryConverter().createSearchFilterType(query.getFilter()));
        } catch (SchemaException e) {
            LOGGER.error("Unable to create search filter from query: ", query, e.getLocalizedMessage());
            return;
        }
        saveSearchItemToAdminConfig(searchItemType);
    }

    private void saveSearchItemToAdminConfig(SearchItemType searchItemType) {
        StringValue collectionViewParameter = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
        String collectionViewName = collectionViewParameter != null ? collectionViewParameter.toString() : null;
        FocusType principalFocus = getPageBase().getPrincipalFocus();
        if (!(principalFocus instanceof UserType)) {
            return;
        }
        AdminGuiConfigurationType adminGui = ((UserType) principalFocus).getAdminGuiConfiguration();
        if (adminGui == null) {
            adminGui = new AdminGuiConfigurationType();
            ((UserType) principalFocus).setAdminGuiConfiguration(adminGui);
        }
        GuiObjectListViewsType views = adminGui.getObjectCollectionViews();
        if (views == null) {
            views = new GuiObjectListViewsType();
            adminGui.objectCollectionViews(views);
        }
        GuiObjectListViewType view = null;
        if (StringUtils.isNotEmpty(collectionViewName)) {
            view = findViewByName(views.getObjectCollectionView(), collectionViewName);
        } else {
            view = findViewByType(views.getObjectCollectionView());
        }
        if (view == null) {
            view = new GuiObjectListViewType();
            view.setType(WebComponentUtil.containerClassToQName(PrismContext.get(), type));
            views.objectCollectionView(view);
        }
        SearchBoxConfigurationType searchConfig = view.getSearchBoxConfiguration();
        if (searchConfig == null) {
            searchConfig = new SearchBoxConfigurationType();
            view.searchBoxConfiguration(searchConfig);
        }
        SearchItemsType searchItems = searchConfig.getSearchItems();
        if (searchItems == null) {
            searchItems = new SearchItemsType();
            searchConfig.searchItems(searchItems);
        }
        List<SearchItemType> searchItemList = searchItems.getSearchItem();
        if (searchItemList == null) {
            searchItems.createSearchItemList();
            searchItemList = searchItems.getSearchItem();
        }
//        searchItemList.add(searchItemType);

        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        OperationResult result = new OperationResult("save search to user");
        try {
            modifications.add(PrismContext.get().deltaFor(UserType.class)
                    .item(UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS,
                            GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW, view.getId(), GuiObjectListViewType.F_SEARCH_BOX_CONFIGURATION,
                            SearchBoxConfigurationType.F_SEARCH_ITEMS, SearchItemsType.F_SEARCH_ITEM).add(searchItemType).asItemDelta());
            getPageBase().getRepositoryService().modifyObject(UserType.class, principalFocus.getOid(), modifications, result);
        } catch (SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException e) {
            LOGGER.error("Unable to save a filter to user, ", e.getLocalizedMessage());
        }
    }

    private GuiObjectListViewType findViewByName(List<GuiObjectListViewType> views, String viewName) {
        if (StringUtils.isEmpty(viewName)) {
            return null;
        }
        for (GuiObjectListViewType view : views) {
            if (view.getIdentifier().equals(viewName)) {
                return view;
            }
        }
        return null;
    }

    private GuiObjectListViewType findViewByType(List<GuiObjectListViewType> views) {
        for (GuiObjectListViewType view : views) {
            if (view.getType().equals(WebComponentUtil.containerClassToQName(PrismContext.get(), type))) {
                return view;
            }
        }
        return null;
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
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return getPageBase().createStringResource("SaveSearchPanel.saveSearch");
    }

}
