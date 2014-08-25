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
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyStrictnessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDependencyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
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
public class ResourceDependencyEditor extends SimplePanel{

    private static final String ID_CONTAINER = "protectedContainer";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_DEPENDENCY_LINK = "dependencyLink";
    private static final String ID_DEPENDENCY_LINK_NAME = "dependencyLinkName";
    private static final String ID_DEPENDENCY_BODY = "dependencyBodyContainer";
    private static final String ID_ORDER = "order";
    private static final String ID_STRICTNESS = "strictness";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_REF_LABEL = "resourceRefLabel";
    private static final String ID_REF_BUTTON = "resourceRefEditButton";
    private static final String ID_ADD_BUTTON = "addButton";

    public ResourceDependencyEditor(String id, IModel<List<ResourceObjectTypeDependencyType>> model){
        super(id, model);
    }

    @Override
    public IModel<List<ResourceObjectTypeDependencyType>> getModel(){
        IModel<List<ResourceObjectTypeDependencyType>> model = super.getModel();

        if(model.getObject() == null){
            model.setObject(new ArrayList<ResourceObjectTypeDependencyType>());
        }

        return model;
    }

    @Override
    protected void initLayout(){
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ListView repeater = new ListView<ResourceObjectTypeDependencyType>(ID_REPEATER, getModel()) {

            @Override
            protected void populateItem(final ListItem<ResourceObjectTypeDependencyType> item) {
                WebMarkupContainer linkContainer = new WebMarkupContainer(ID_DEPENDENCY_LINK);
                linkContainer.setOutputMarkupId(true);
                linkContainer.add(new AttributeModifier("href", createCollapseItemId(item, true)));
                item.add(linkContainer);

                Label linkLabel = new Label(ID_DEPENDENCY_LINK_NAME, new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return "Dependency#" + item.getIndex();
                    }
                });
                linkContainer.add(linkLabel);

                WebMarkupContainer dependencyBody = new WebMarkupContainer(ID_DEPENDENCY_BODY);
                dependencyBody.setOutputMarkupId(true);
                dependencyBody.setMarkupId(createCollapseItemId(item, false).getObject());
                dependencyBody.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if(item.getIndex() == 0){
                            return "panel-collapse collapse in";
                        } else {
                            return "panel-collapse collapse";
                        }
                    }
                }));
                item.add(dependencyBody);

                TextField order = new TextField<>(ID_ORDER, new PropertyModel<Integer>(item.getModelObject(), "order"));
                dependencyBody.add(order);

                DropDownChoice strictness = new DropDownChoice<>(ID_STRICTNESS,
                        new PropertyModel<ResourceObjectTypeDependencyStrictnessType>(item.getModelObject(), "strictness"),
                        WebMiscUtil.createReadonlyModelFromEnum(ResourceObjectTypeDependencyStrictnessType.class),
                        new EnumChoiceRenderer<ResourceObjectTypeDependencyStrictnessType>(this));
                dependencyBody.add(strictness);

                DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                        new PropertyModel<ShadowKindType>(item.getModelObject(), "kind"),
                        WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                        new EnumChoiceRenderer<ShadowKindType>(this));
                dependencyBody.add(kind);

                TextField intent = new TextField<>(ID_INTENT, new PropertyModel<String>(item.getModelObject(), "intent"));
                dependencyBody.add(intent);

                TextField resourceRefLabel = new TextField<>(ID_REF_LABEL, new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        //TODO - load resource ref name from oid in reference and display it
                        if(item.getModelObject().getResourceRef() != null){
                            return item.getModelObject().getResourceRef().getOid();
                        } else {
                            return null;
                        }
                    }
                });
                dependencyBody.add(resourceRefLabel);

                AjaxLink refButton = new AjaxLink(ID_REF_BUTTON) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        resourceRefEditPerformed(target, item);
                    }
                };
                dependencyBody.add(refButton);
            }
        };
        repeater.setOutputMarkupId(true);
        container.add(repeater);

        AjaxLink add = new AjaxLink(ID_ADD_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addDependencyPerformed(target);
            }
        };
        add(add);
    }

    private WebMarkupContainer getMainContainer(){
        return (WebMarkupContainer) get(ID_CONTAINER);
    }

    private IModel<String> createCollapseItemId(final ListItem<ResourceObjectTypeDependencyType> item, final boolean appendSelector){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(appendSelector){
                    sb.append("#");
                }

                sb.append("collapse").append(item.getId());

                return sb.toString();
            }
        };
    }

    private void addDependencyPerformed(AjaxRequestTarget target){
        getModel().getObject().add(new ResourceObjectTypeDependencyType());
        target.add(getMainContainer());
    }

    private void resourceRefEditPerformed(AjaxRequestTarget target, ListItem<ResourceObjectTypeDependencyType> item){
        //TODO - implement this
    }
}
