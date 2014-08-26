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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  @author shood
 * */
public class ResourceDependencyEditor extends SimplePanel{

    private static enum ChangeState{
        SKIP, FIRST, LAST
    }

    private static final Trace LOGGER = TraceManager.getTrace(ResourceDependencyEditor.class);

    private static final String DOT_CLASS = ResourceDependencyEditor.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCES = DOT_CLASS + "createResourceList";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private static final String ID_CONTAINER = "protectedContainer";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_DEPENDENCY_LINK = "dependencyLink";
    private static final String ID_DEPENDENCY_LINK_NAME = "dependencyLinkName";
    private static final String ID_DEPENDENCY_BODY = "dependencyBodyContainer";
    private static final String ID_ORDER = "order";
    private static final String ID_STRICTNESS = "strictness";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_REF = "resourceRef";
    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_DELETE_BUTTON = "deleteDependency";

    private ChangeState changeState = ChangeState.FIRST;
    private Map<String, String> resourceMap = new HashMap<>();

    public ResourceDependencyEditor(String id, IModel<List<ResourceObjectTypeDependencyType>> model){
        super(id, model);
    }

    public List<ResourceObjectTypeDependencyType> getDependencyList(){
        return getModel().getObject();
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

                Label linkLabel = new Label(ID_DEPENDENCY_LINK_NAME, createDependencyLabelModel(item));
                linkContainer.add(linkLabel);

                AjaxLink delete = new AjaxLink(ID_DELETE_BUTTON) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteDependencyPerformed(target, item);
                    }
                };
                item.add(delete);

                WebMarkupContainer dependencyBody = new WebMarkupContainer(ID_DEPENDENCY_BODY);
                dependencyBody.setOutputMarkupId(true);
                dependencyBody.setMarkupId(createCollapseItemId(item, false).getObject());

                if(changeState != ChangeState.SKIP){
                    dependencyBody.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

                        @Override
                        public String getObject() {
                            if(changeState == ChangeState.FIRST && item.getIndex() == 0){
                                return "panel-collapse collapse in";
                            } else if(changeState == ChangeState.LAST && item.getIndex() == (getModelObject().size()-1)){
                                return "panel-collapse collapse in";
                            } else {
                                return "panel-collapse collapse";
                            }
                        }
                    }));
                }

                item.add(dependencyBody);

                TextField order = new TextField<>(ID_ORDER, new PropertyModel<Integer>(item.getModelObject(), "order"));
                order.add(prepareAjaxOnComponentTagUpdateBehavior());
                dependencyBody.add(order);

                DropDownChoice strictness = new DropDownChoice<>(ID_STRICTNESS,
                        new PropertyModel<ResourceObjectTypeDependencyStrictnessType>(item.getModelObject(), "strictness"),
                        WebMiscUtil.createReadonlyModelFromEnum(ResourceObjectTypeDependencyStrictnessType.class),
                        new EnumChoiceRenderer<ResourceObjectTypeDependencyStrictnessType>(this));
                strictness.add(prepareAjaxOnComponentTagUpdateBehavior());
                dependencyBody.add(strictness);

                DropDownChoice kind = new DropDownChoice<>(ID_KIND,
                        new PropertyModel<ShadowKindType>(item.getModelObject(), "kind"),
                        WebMiscUtil.createReadonlyModelFromEnum(ShadowKindType.class),
                        new EnumChoiceRenderer<ShadowKindType>(this));
                kind.add(prepareAjaxOnComponentTagUpdateBehavior());
                dependencyBody.add(kind);

                TextField intent = new TextField<>(ID_INTENT, new PropertyModel<String>(item.getModelObject(), "intent"));
                intent.add(prepareAjaxOnComponentTagUpdateBehavior());
                dependencyBody.add(intent);

                DropDownChoice resource = new DropDownChoice<>(ID_REF,
                        new PropertyModel<ObjectReferenceType>(item.getModelObject(), "resourceRef"),
                        new AbstractReadOnlyModel<List<ObjectReferenceType>>() {

                            @Override
                            public List<ObjectReferenceType> getObject() {
                                return createResourceList();
                            }
                        }, new IChoiceRenderer<ObjectReferenceType>() {

                    @Override
                    public Object getDisplayValue(ObjectReferenceType object) {
                        return createResourceReadLabel(object);
                    }

                    @Override
                    public String getIdValue(ObjectReferenceType object, int index) {
                        return Integer.toString(index);
                    }
                });
                resource.add(prepareAjaxOnComponentTagUpdateBehavior());
                dependencyBody.add(resource);
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

    private IModel<String> createDependencyLabelModel(final ListItem<ResourceObjectTypeDependencyType> item){
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();
                ResourceObjectTypeDependencyType dep = item.getModelObject();
                sb.append("#").append(item.getIndex()).append(":");

                if(dep.getResourceRef() != null){
                    sb.append(resourceMap.get(dep.getResourceRef().getOid()));
                }

                if(dep.getKind() != null){
                    sb.append(dep.getKind()).append(":");
                }

                if(dep.getIntent() != null){
                    sb.append(dep.getIntent()).append(":");
                }

                sb.append(":").append(dep.getOrder()).append(":");
                if(dep.getStrictness() != null){
                    sb.append(dep.getStrictness().toString());
                }

                return sb.toString();
            }
        };
    }

    private AjaxFormComponentUpdatingBehavior prepareAjaxOnComponentTagUpdateBehavior(){
        return new AjaxFormComponentUpdatingBehavior("onBlur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {}
        };
    }

    private List<ObjectReferenceType> createResourceList(){
        resourceMap.clear();
        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCES);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCES);
        List<PrismObject<ResourceType>> resources = null;
        List<ObjectReferenceType> references = new ArrayList<>();

        try {
            resources = getPageBase().getModelService().searchObjects(ResourceType.class, new ObjectQuery(), null, task, result);
            result.recomputeStatus();
        } catch (Exception e){
            result.recordFatalError("Couldn't get resource list.", e);
            LoggingUtils.logException(LOGGER, "Couldn't get resource list.", e);
        }

        // TODO - show error somehow
        // if(!result.isSuccess()){
        //    getPageBase().showResult(result);
        // }

        if(resources != null){
            ObjectReferenceType ref;
            for(PrismObject<ResourceType> r: resources){
                resourceMap.put(r.getOid(), WebMiscUtil.getName(r));
                ref = new ObjectReferenceType();
                ref.setType(ResourceType.COMPLEX_TYPE);
                ref.setOid(r.getOid());
                references.add(ref);
            }
        }

        return references;
    }

    private String createResourceReadLabel(ObjectReferenceType ref){
        return resourceMap.get(ref.getOid());
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
        ResourceObjectTypeDependencyType dependency = new ResourceObjectTypeDependencyType();
        changeState = ChangeState.LAST;
        getModel().getObject().add(dependency);
        target.add(getMainContainer());
    }

    private void deleteDependencyPerformed(AjaxRequestTarget target, ListItem<ResourceObjectTypeDependencyType> item){
        changeState = ChangeState.SKIP;
        getModel().getObject().remove(item.getModelObject());
        target.add(getMainContainer());
    }
}
