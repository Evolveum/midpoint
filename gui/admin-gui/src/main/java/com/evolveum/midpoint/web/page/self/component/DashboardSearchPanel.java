/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * Created by honchar.
 */
public class DashboardSearchPanel extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DashboardSearchPanel.class);

    private static final String ID_SEARCH_INPUT = "searchInput";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_SEARCH_TYPE_ITEM = "searchTypeItem";
    private static final String ID_SEARCH_TYPES = "searchTypes";
    private static final String ID_SEARCH_FORM = "searchForm";

    private final Map<SearchType, IModel<String>> searchTypes = new HashMap<>();

    private SearchType selectedSearchType = SearchType.USERS;

    private enum SearchType {
        USERS, RESOURCES, TASKS;
    }

    public DashboardSearchPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    protected void initLayout() {
        final Form<?> searchForm = new com.evolveum.midpoint.web.component.form.Form<>(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL)) {
            searchTypes.put(SearchType.USERS, createStringResource("PageDashboard.search.users"));
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL)) {
            searchTypes.put(SearchType.RESOURCES, createStringResource("PageDashboard.search.resources"));
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL)) {
            searchTypes.put(SearchType.TASKS, createStringResource("PageDashboard.search.tasks"));
        }

        TextField<String> searchInput = new TextField<>(ID_SEARCH_INPUT, Model.of(""));
        searchInput.add(new VisibleBehaviour(() -> !searchTypes.isEmpty()));
        searchInput.setOutputMarkupId(true);
        searchInput.setOutputMarkupPlaceholderTag(true);
        searchForm.add(searchInput);

        final AjaxSubmitLink searchButton = new AjaxSubmitLink(ID_SEARCH_BUTTON) {

            private static final long serialVersionUID = 1L;

            public IModel<?> getBody() {
                return searchTypes.get(selectedSearchType);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                performSearch(getSearchText());
            }
        };
        searchButton.setOutputMarkupId(true);
        searchButton.setOutputMarkupPlaceholderTag(true);
        searchForm.add(searchButton);
        searchForm.setDefaultButton(searchButton);

        ListView<SearchType> li = new ListView<SearchType>(ID_SEARCH_TYPES,
                new ListModel<SearchType>(new ArrayList<>(searchTypes.keySet()))) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<SearchType> item) {
                final AjaxLink<String> searchTypeLink = new AjaxLink<String>(ID_SEARCH_TYPE_ITEM) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public IModel<String> getBody() {
                        return searchTypes.get(item.getModelObject());
                    }

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        selectedSearchType = item.getModelObject();
                        target.add(DashboardSearchPanel.this.get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_BUTTON)));
                    }


                };
                searchTypeLink.setOutputMarkupId(true);
                item.add(searchTypeLink);
            }
        };
        li.setOutputMarkupId(true);
        searchForm.add(li);


    }

 private  String getSearchText() {
     TextField<String> searchInput =  (TextField<String>) get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_INPUT));
     if (searchInput == null) {
         LOGGER.error("cannot find search input component");
         return null;
     }

     return searchInput.getModelObject();
 }


    private void performSearch(String text) {

        PageParameters params = null;
        if (StringUtils.isNotBlank(text)) {
            params = new PageParameters();
            params.add(PageBase.PARAMETER_SEARCH_BY_NAME, text);
        }
        switch (selectedSearchType) {
            case USERS:
                setResponsePage(PageUsers.class, params);
                break;
            case RESOURCES:
                setResponsePage(PageResources.class, params);
                break;
            case TASKS:
                setResponsePage(PageTasks.class, params);
                break;
            default:
                setResponsePage(PageUsers.class, params);
        }

    }

}
