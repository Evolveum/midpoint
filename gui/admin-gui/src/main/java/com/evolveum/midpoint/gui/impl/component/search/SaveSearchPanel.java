/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.login.PageRegistrationConfirmation;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
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

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
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
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class SaveSearchPanel<C extends Containerable> extends BasePanel<Search<C>> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SaveSearchPanel.class);
    private static final String ID_FEEDBACK_MESSAGE = "feedbackMessage";
    private static final String ID_SEARCH_NAME = "searchName";
    private static final String ID_BUTTONS_PANEL = "buttonsPanel";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    private Class<C> type;
    IModel<String> feedbackMessageModel = Model.of();
    IModel<String> queryNameModel = Model.of();
    public SaveSearchPanel(String id, IModel<Search<C>> searchModel, Class<C> type) {
        super(id, searchModel);
        this.type = type;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MessagePanel feedbackMessage = new MessagePanel(ID_FEEDBACK_MESSAGE, MessagePanel.MessagePanelType.WARN, feedbackMessageModel);
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
        Search<C> search = getModelObject();
        AvailableFilterType availableFilter = new AvailableFilterType();
        availableFilter.setDisplay(new DisplayType().label(queryNameModel.getObject()));
        availableFilter.setSearchMode(getModelObject().getSearchMode());
        if (SearchBoxModeType.BASIC.equals(getModelObject().getSearchMode())) {
            availableFilter.getSearchItem().addAll(getAvailableFilterSearchItems(type, search.getItems(), search.getSearchMode()));
        }
        saveSearchItemToAdminConfig(availableFilter);
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
                    LOGGER.error("Unable to create search filter from query: ", filter, e.getLocalizedMessage());
                }
                searchItem.setVisibleByDefault(true);
                searchItems.add(searchItem);
                //todo do later non property items - oid, type...
            }
        }
        return searchItems;
    }

    private void saveSearchItemToAdminConfig(AvailableFilterType availableFilter) {
        FocusType principalFocus = getPageBase().getPrincipalFocus();
        boolean newObjectListView = WebComponentUtil.getPrincipalUserObjectListView(getPageBase(), principalFocus, type, false) == null;
        GuiObjectListViewType view = WebComponentUtil.getPrincipalUserObjectListView(getPageBase(), principalFocus, type, true);
        if (view == null) {
            view = new GuiObjectListViewType();
            view.setType(WebComponentUtil.containerClassToQName(PrismContext.get(), type));
            //view.setIdentifier(); //todo set collection view identifier
            ((UserType)principalFocus).getAdminGuiConfiguration().getObjectCollectionViews().objectCollectionView(view);
        }
        SearchBoxConfigurationType searchConfig = view.getSearchBoxConfiguration();
        if (searchConfig == null) {
            searchConfig = new SearchBoxConfigurationType();
            view.searchBoxConfiguration(searchConfig);
        }
        if (searchConfig.getAvailableFilter() == null) {
            searchConfig.beginAvailableFilter();
        }

        OperationResult result = new OperationResult("save search to user");
        try {
            Object[] path;
            ObjectDelta<UserType> userDelta = null;
            if (newObjectListView) {
                searchConfig.getAvailableFilter().add(availableFilter);
                path = new Object[]{UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS,
                        GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW};
                userDelta = getPrismContext().deltaFor(UserType.class)
                        .item(path)
                        .add(view).asObjectDelta(principalFocus.getOid());
            } else {
                path = new Object[]{UserType.F_ADMIN_GUI_CONFIGURATION, AdminGuiConfigurationType.F_OBJECT_COLLECTION_VIEWS,
                        GuiObjectListViewsType.F_OBJECT_COLLECTION_VIEW, view.getId(), GuiObjectListViewType.F_SEARCH_BOX_CONFIGURATION,
                        SearchBoxConfigurationType.F_AVAILABLE_FILTER};
                userDelta = getPrismContext().deltaFor(UserType.class)
                        .item(path)
                        .add(availableFilter).asObjectDelta(principalFocus.getOid());
            }
            WebModelServiceUtils.save(userDelta, result, getPageBase().createSimpleTask("task"), getPageBase());
        } catch (Exception e) {
            LOGGER.error("Unable to save a filter to user, ", e.getLocalizedMessage());
        }
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
