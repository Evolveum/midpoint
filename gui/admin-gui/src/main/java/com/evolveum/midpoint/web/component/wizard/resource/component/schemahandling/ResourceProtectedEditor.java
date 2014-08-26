/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectPatternType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceProtectedEditor extends SimplePanel{

    private static final String ID_CONTAINER = "protectedContainer";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_ACCOUNT_LINK = "accountLink";
    private static final String ID_ACCOUNT_NAME = "accountLinkName";
    private static final String ID_ACCOUNT_BODY = "accountBodyContainer";
    private static final String ID_NAME = "name";
    private static final String ID_UID = "uid";
    private static final String ID_FILTER_DESCRIPTION = "filterDescription";
    private static final String ID_FILTER_CLAUSE = "filterClause";
    private static final String ID_BUTTON_ADD = "addButton";

    public ResourceProtectedEditor(String id, IModel<List<ResourceObjectPatternType>> model){
        super(id, model);
    }

    @Override
    public IModel<List<ResourceObjectPatternType>> getModel(){
        IModel<List<ResourceObjectPatternType>> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new ArrayList<ResourceObjectPatternType>());
        }

        return model;
    }

    @Override
    protected void initLayout(){
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ListView repeater = new ListView<ResourceObjectPatternType>(ID_REPEATER, getModel()){

            @Override
            protected void populateItem(final ListItem<ResourceObjectPatternType> item){
                WebMarkupContainer linkCont = new WebMarkupContainer(ID_ACCOUNT_LINK);
                linkCont.setOutputMarkupId(true);
                linkCont.add(new AttributeModifier("href", createCollapseItemId(item, true)));
                item.add(linkCont);

                Label accountLabel = new Label(ID_ACCOUNT_NAME, new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        //TODO - what should be the label of this?
                        return "Account#" + item.getIndex();
                    }
                });
                linkCont.add(accountLabel);

                WebMarkupContainer accountBody = new WebMarkupContainer(ID_ACCOUNT_BODY);
                accountBody.setOutputMarkupId(true);
                accountBody.setMarkupId(createCollapseItemId(item, false).getObject());
                accountBody.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(item.getIndex() == 0){
                            return "panel-collapse collapse in";
                        } else {
                            return "panel-collapse collapse";
                        }
                    }
                }));
                item.add(accountBody);

                //TODO - maybe add some validator and auto-complete functionality?
                TextField name = new TextField<>(ID_NAME, new PropertyModel<String>(item.getModelObject(), "name"));
                accountBody.add(name);

                //TODO - maybe add some validator and auto-complete functionality?
                TextField uid = new TextField<>(ID_UID, new PropertyModel<String>(item.getModelObject(), "uid"));
                accountBody.add(uid);

                TextArea filterDescription = new TextArea<>(ID_FILTER_DESCRIPTION,
                        new PropertyModel<String>(item.getModelObject(), "filter.description"));
                accountBody.add(filterDescription);

                //TODO - what is this? How should we edit this?
                TextField filterClause = new TextField<>(ID_FILTER_CLAUSE,
                        new PropertyModel<String>(item.getModelObject(), "filter.filterClauseXNode"));
                accountBody.add(filterClause);
            }
        };
        repeater.setOutputMarkupId(true);
        container.add(repeater);

        AjaxLink add = new AjaxLink(ID_BUTTON_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addProtectedAccountPerformed(target);
            }
        };
        add(add);
    }

    private WebMarkupContainer getMainContainer(){
        return (WebMarkupContainer) get(ID_CONTAINER);
    }

    private IModel<String> createCollapseItemId(final ListItem<ResourceObjectPatternType> item,final boolean includeSelector){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(includeSelector){
                    sb.append("#");
                }

                sb.append("collapse").append(item.getId());

                return sb.toString();
            }
        };
    }

    private void addProtectedAccountPerformed(AjaxRequestTarget target){
        getModel().getObject().add(new ResourceObjectPatternType());
        target.add(getMainContainer());
    }
}
