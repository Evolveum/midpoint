package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.self.dto.LinkDto;
import org.apache.poi.ss.formula.functions.T;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Kate on 23.09.2015.
 */
public class SearchPanel extends SimplePanel<T> {

    private static final String ID_SEARCH_INPUT = "searchInput";
    private static final String ID_SEARCH_BUTTON = "searchButton";
    private static final String ID_LINK_LABEL = "linkLabel";
    private static final String ID_SEARCH_TYPE_ITEM = "searchTypeItem";
    private static final String ID_SEARCH_TYPES = "searchTypes";
    private static final String ID_BUTTON_LABEL = "buttonLabel";
    private static final List<String> SEARCH_TYPES = Arrays.asList(new String[]{
            "User", "Role", "Organization"});
    private String selected = "Google";

    public SearchPanel(String id) {
        this(id, null);
    }

    public SearchPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        TextField searchInput = new TextField(ID_SEARCH_INPUT) {
            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("placeholder", createStringResource("SearchPanel.searchByName").getString());
            }
        };
        add(searchInput);
        final AjaxLink searchButton = new AjaxLink(ID_SEARCH_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target){

            }
//            @Override
//            protected void onComponentTag(final ComponentTag tag) {
//                super.onComponentTag(tag);
//                tag.put("value", createStringResource("SearchPanel.searchButton").getString());
//            }

        };
        final int index = 0;
        final Model<Integer> typeIndex = new Model<Integer>(){
            public Integer getObject() {
                return index;
            }
        };
        final Label buttonLabel = new Label(ID_BUTTON_LABEL, new Model<String>(){
            public String getObject() {
                return SEARCH_TYPES.get(typeIndex.getObject());
            }
        });
        searchButton.add(buttonLabel);
        add(searchButton);

        final WebMarkupContainer list = new WebMarkupContainer(ID_SEARCH_TYPES);
        final RepeatingView searchTypes = new RepeatingView(ID_SEARCH_TYPE_ITEM);
        for (final String searchTypeItem : SEARCH_TYPES) {
            final AjaxLink searchTypeLink = new AjaxLink(searchTypes.newChildId()) {
                @Override
                public void onClick(AjaxRequestTarget target) {
                    buttonLabel.setDefaultModel(new Model<String>() {
                        public String getObject() {
                            return searchTypeItem;
                        }
                    });
                    target.add(searchButton);
                    //TODO make list items invisible after some of them is selected
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

        add(list);
//Link l =  new Link (ID_ROLE_SEARCH, createStringResource("SearchPanel.roleSearch")){
//    public void onClick(){
//
//    }
//};
//        add(l);


    }


}
