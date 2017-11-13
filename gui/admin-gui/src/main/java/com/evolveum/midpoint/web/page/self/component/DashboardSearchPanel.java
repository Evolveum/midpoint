package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.util.SimplePanel;
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
public class DashboardSearchPanel extends SimplePanel<T> {

    private static final String ID_SEARCH_INPUT = "searchInput";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_LINK_LABEL = "linkLabel";
    private static final String ID_SEARCH_TYPE_ITEM = "searchTypeItem";
    private static final String ID_SEARCH_TYPES = "searchTypes";
    private static final String ID_BUTTON_LABEL = "buttonLabel";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final List<String> SEARCH_TYPES = Arrays.asList(new String[]{
            "User", "Resource", "Task"});
    private static final int USER_INDEX = 0;
    private static final int RESOURCE_INDEX = 1;
    private static final int TASK_INDEX = 2;
    private int selectedSearchType = 0;

    public DashboardSearchPanel(String id) {
        this(id, null);
    }

    public DashboardSearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
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
                    tag.put("placeholder", createStringResource("SearchPanel.searchByName").getString());
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

//            final WebMarkupContainer list = new WebMarkupContainer(ID_SEARCH_TYPES);
//            list.setOutputMarkupId(true);


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
            })
            {

                    @Override
                    protected void populateItem(final ListItem<String> item) {
                    final AjaxLink searchTypeLink = new AjaxLink(ID_SEARCH_TYPE_ITEM) {
                        @Override
                        public IModel<?> getBody() {
                            return new Model<String>(item.getModel().getObject());
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


//            final RepeatingView searchTypes = new RepeatingView(ID_SEARCH_TYPE_ITEM);
//            searchTypes.setOutputMarkupId(true);
//            if (accessibleSearchTypes.size() > 1) {
//                for (int i = 0; i < accessibleSearchTypes.size(); i++) {
//                    if (i == selectedSearchType){
//                        continue;
//                    }
//                    final String searchTypeItem = accessibleSearchTypes.get(i);
//                    final AjaxSubmitLink searchTypeLink = new AjaxSubmitLink(searchTypes.newChildId()) {
//                        @Override
//                        public void onSubmit(AjaxRequestTarget target, Form<?> form) {
//                                selectedSearchType = accessibleSearchTypes.indexOf(searchTypeItem);
//                                target.add(DashboardSearchPanel.this.get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_BUTTON)));
//                                target.add(DashboardSearchPanel.this.get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_TYPES)));
////                                target.add(DashboardSearchPanel.this.get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_TYPES, ID_SEARCH_TYPE_ITEM)));
//                        }
//
//                        @Override
//                        protected void onComponentTag(final ComponentTag tag) {
//                            super.onComponentTag(tag);
//                            tag.put("value", searchTypeItem);
//                        }
//
//                    };
//                    searchTypeLink.setOutputMarkupId(true);
//                    Label linkLabel = new Label(ID_LINK_LABEL, new Model<String>() {
//                        public String getObject() {
//                            return searchTypeItem;
//                        }
//                    });
//                    linkLabel.setOutputMarkupId(true);
//                    searchTypeLink.add(linkLabel);
//                    searchTypes.add(searchTypeLink);
//                }
//            }
//            list.add(searchTypes);
//            if (accessibleSearchTypes.size() == 1){
//                list.setVisible(false);
//            }
//            searchForm.add(list);
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

    protected IModel<String> createSearchTextModel() {
        return (IModel) getModel();
    }
}
