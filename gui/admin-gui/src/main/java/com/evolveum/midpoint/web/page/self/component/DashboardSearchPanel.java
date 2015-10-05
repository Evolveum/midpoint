package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.admin.users.dto.UsersDto;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Kate on 23.09.2015.
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
            "User", "Role", "Organization"});
    private static final int USER_INDEX = 0;
    private static final int ROLE_INDEX = 1;
    private static final int ORG_INDEX = 2;

    private String selected = "Google";

    public DashboardSearchPanel(String id) {
        this(id, null);
    }

    public DashboardSearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        final Form searchForm = new Form(ID_SEARCH_FORM);
        add(searchForm);
        searchForm.setOutputMarkupId(true);



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
                return SEARCH_TYPES.get(0);
            }
        });
        final AjaxSubmitLink searchButton = new AjaxSubmitLink(ID_SEARCH_BUTTON) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                String searchType = buttonLabel.getDefaultModel().getObject().toString();
                String searchText = searchInput.getValue();
                if (searchText != null && !searchText.trim().equals("")) {
                    performSearch(searchType, searchText);
                }
            }
        };

        searchButton.add(buttonLabel);
        searchForm.add(searchButton);

        final WebMarkupContainer list = new WebMarkupContainer(ID_SEARCH_TYPES);
        final RepeatingView searchTypes = new RepeatingView(ID_SEARCH_TYPE_ITEM);
        for (final String searchTypeItem : SEARCH_TYPES) {
            final AjaxSubmitLink searchTypeLink = new AjaxSubmitLink(searchTypes.newChildId()) {
                @Override
                public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    if (searchInput.getValue() != null && !searchInput.getValue().trim().equals("")) {
                        performSearch(searchTypeItem, searchInput.getValue());
                    }
//                    else {
//                            TODO  make invisible the list of items
//                    }
                }

                @Override
                protected void onComponentTag(final ComponentTag tag) {
                    super.onComponentTag(tag);
                    tag.put("value", searchTypeItem);
                }

            };
            searchTypeLink.add(new Label(ID_LINK_LABEL, new Model<String>() {
                public String getObject() {
                    return searchTypeItem;
                }
            }));
            searchTypes.add(searchTypeLink);
        }
        list.add(searchTypes);
        searchForm.add(list);
    }

    private void performSearch(String searchType, String text) {
        if (SEARCH_TYPES.indexOf(searchType) == USER_INDEX) {
            setResponsePage(new PageUsers(UsersDto.SearchType.NAME, text));
        } else if (SEARCH_TYPES.indexOf(searchType) == ROLE_INDEX) {
            setResponsePage(new PageRoles(text));
        } else if (SEARCH_TYPES.indexOf(searchType) == ORG_INDEX) {

        }

    }

    protected IModel<String> createSearchTextModel() {
        return (IModel) getModel();
    }
}
