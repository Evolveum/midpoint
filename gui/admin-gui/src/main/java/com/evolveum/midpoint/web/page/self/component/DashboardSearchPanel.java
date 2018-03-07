package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class DashboardSearchPanel extends BasePanel<T> {

    private final String ID_SEARCH_INPUT = "searchInput";
    private final String ID_SEARCH_BUTTON = "searchButton";
    private final String ID_SEARCH_TYPE_ITEM = "searchTypeItem";
    private final String ID_SEARCH_TYPES = "searchTypes";
    private final String ID_BUTTON_LABEL = "buttonLabel";
    private final String ID_SEARCH_FORM = "searchForm";
    private List<String> SEARCH_TYPES;
    private final int USER_INDEX = 0;
    private final int RESOURCE_INDEX = 1;
    private final int TASK_INDEX = 2;
    private int selectedSearchType = 0;

    public DashboardSearchPanel(String id) {
        super(id);
        SEARCH_TYPES = Arrays.asList(
                createStringResource("PageDashboard.search.users").getString(),
                createStringResource("PageDashboard.search.resources").getString(),
                createStringResource("PageDashboard.search.tasks").getString()
        );

        initLayout();
    }

    protected void initLayout() {
        final Form searchForm = new com.evolveum.midpoint.web.component.form.Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);

        final List<String> accessibleSearchTypes = new ArrayList<>();
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL)) {
            accessibleSearchTypes.add(SEARCH_TYPES.get(USER_INDEX));
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL)) {
            accessibleSearchTypes.add(SEARCH_TYPES.get(RESOURCE_INDEX));
        }
        if (WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL)) {
            accessibleSearchTypes.add(SEARCH_TYPES.get(TASK_INDEX));
        }
        if (accessibleSearchTypes.size() == 0) {
            searchForm.setVisible(false);
        } else {
            final TextField searchInput = new TextField(ID_SEARCH_INPUT, Model.of("")) {
                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("placeholder", createStringResource("PageDashboard.search.input").getString());
                }
            };
            searchForm.add(searchInput);
            final Label buttonLabel = new Label(ID_BUTTON_LABEL, new Model<String>() {
                public String getObject() {
                    return accessibleSearchTypes.get(selectedSearchType);
                }
            });
            buttonLabel.setOutputMarkupId(true);

            final AjaxSubmitLink searchButton = new AjaxSubmitLink(ID_SEARCH_BUTTON) {
                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    String searchType = buttonLabel.getDefaultModel().getObject().toString();
                    String searchText = searchInput.getValue();
                    performSearch(searchType, searchText == null ? "" : searchText);
                }
            };
            searchButton.setOutputMarkupId(true);
            searchButton.add(buttonLabel);
            searchForm.add(searchButton);
            searchForm.setDefaultButton(searchButton);

            ListView<String> li = new ListView<String>(ID_SEARCH_TYPES, new IModel<List<String>>() {
                @Override
                public void detach() {
                }

                @Override
                public List<String> getObject() {
                    return accessibleSearchTypes;
                }

                @Override
                public void setObject(List<String> list) {
                }
            }) {

                @Override
                protected void populateItem(final ListItem<String> item) {
                    final AjaxLink searchTypeLink = new AjaxLink(ID_SEARCH_TYPE_ITEM) {
                        @Override
                        public IModel<?> getBody() {
                            return new Model<>(item.getModel().getObject());
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            selectedSearchType = accessibleSearchTypes.indexOf(item.getModelObject());
                            target.add(DashboardSearchPanel.this.get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_BUTTON)));
                        }

                        @Override
                        protected void onComponentTag(final ComponentTag tag) {
                            super.onComponentTag(tag);
                            tag.put("value", item.getModelObject());
                        }

                    };
                    searchTypeLink.setOutputMarkupId(true);
                    item.add(searchTypeLink);
                }
            };
            li.setOutputMarkupId(true);
            searchForm.add(li);


        }
    }

    private void performSearch(String searchType, String text) {
        if (SEARCH_TYPES.indexOf(searchType) == USER_INDEX) {
            setResponsePage(new PageUsers(UsersDto.SearchType.NAME, text));
        } else if (SEARCH_TYPES.indexOf(searchType) == RESOURCE_INDEX) {
            setResponsePage(new PageResources(text));
        } else if (SEARCH_TYPES.indexOf(searchType) == TASK_INDEX) {
            setResponsePage(new PageTasks(text));
        }

    }

}
