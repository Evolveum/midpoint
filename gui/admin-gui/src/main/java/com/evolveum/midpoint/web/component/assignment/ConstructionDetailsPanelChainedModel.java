/**
 * Copyright (c) 2015-2018 Evolveum
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
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.*;
import sun.security.provider.SHA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
public class ConstructionDetailsPanelChainedModel extends BasePanel<ConstructionType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ConstructionDetailsPanelChainedModel.class);
    private static final String DOT_CLASS = ConstructionDetailsPanelChainedModel.class.getName() + ".";
    private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

    private static final String ID_FORM = "constructionForm";
    private static final String ID_KIND_FIELD = "kindField";
    private static final String ID_INTENT_FIELD = "intentField";

    private IModel<String> intentChoicesModel;
    private LoadableDetachableModel<PrismObject<ResourceType>> resourceModel;
    private IModel<ShadowKindType> kindModel;

    public ConstructionDetailsPanelChainedModel(String id, IModel<ConstructionType> constructionModel) {
        super(id, constructionModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    private void initModels() {
        resourceModel = new LoadableDetachableModel<PrismObject<ResourceType>>() {
            @Override
            protected PrismObject<ResourceType> load() {
                ObjectReferenceType resourceRef = getModelObject().getResourceRef();
                Task loadResourceTask = getPageBase().createSimpleTask(OPERATION_LOAD_RESOURCE);
                OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
                PrismObject<ResourceType> resource = WebModelServiceUtils.loadObject(resourceRef, getPageBase(), loadResourceTask, result);
                result.computeStatusIfUnknown();
                if (!result.isAcceptable()) {
                    LOGGER.error("Cannot find resource referenced from construction. {}", result.getMessage());
                    result.recordPartialError("Could not find resource referenced from construction.");
                    return null;
                }
                return resource;
            }
        };
        kindModel = new IModel<ShadowKindType>() {
            @Override
            public ShadowKindType getObject() {
                return getModelObject().getKind();
            }

            @Override
            public void setObject(ShadowKindType shadowKindType) {
                getModelObject().setKind(shadowKindType);
            }

            @Override
            public void detach() {

            }
        };
        intentChoicesModel = new ChainingModel<String>(kindModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {

                List<String> availableIntentValues = new ArrayList<>();
                try {
                    if (resourceModel.getObject() == null) {
                        return availableIntentValues.toString();
                    }
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceModel.getObject());
                    if (refinedSchema != null) {
                        ShadowKindType kind = ((IModel<ShadowKindType>) super.getChainedModel()).getObject();
                        List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
                        for (RefinedObjectClassDefinition def : definitions) {
                            if (def.getIntent() != null) {
                                availableIntentValues.add(def.getIntent());
                            }
                        }
                    }
                } catch (SchemaException ex) {
                    LOGGER.error("Cannot get refined resource schema for resource {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
                }

                return availableIntentValues.toString();
            }

            @Override
            public void setObject(String o){
              super.setObject(o);
            }
        };
    }

    private void initLayout() {
        CompoundPropertyModel constrModel = new CompoundPropertyModel(getModel()){
            @Override
                    public Object getObject(){
                Object o = super.getObject();
                return o;
            }

            @Override
            public void setObject(Object o){
                        super.setObject(o);
            }
        };
        Form<ConstructionType> form = new Form<ConstructionType>(ID_FORM, constrModel);
        form.setOutputMarkupId(true);

        DropDownChoice kindChoice = new DropDownChoice<>("kind", Model.ofList(Arrays.asList(ShadowKindType.values())));
        kindChoice.setOutputMarkupId(true);
        kindChoice.add(new EmptyOnBlurAjaxFormUpdatingBehaviour(){
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
//                ajaxRequestTarget.add(form);
            }
        });
//        kindChoice.add(new AjaxEventBehavior("blur") {
//            @Override
//            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
//                ajaxRequestTarget.add(form);
//            }
//        });
        form.add(kindChoice);
        DropDownChoice intentDropdown = new DropDownChoice<>("intent", new IModel<List<String>>() {
            @Override
            public List<String> getObject() {
                List<String> availableIntentValues = new ArrayList<>();
                try {
                    if (resourceModel.getObject() == null) {
                        return availableIntentValues;
                    }
                    RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceModel.getObject());
                    if (refinedSchema != null) {
                        ConstructionType m = (ConstructionType) constrModel.getObject();
                        ShadowKindType kind = m.getKind();
                        List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
                        for (RefinedObjectClassDefinition def : definitions) {
                            if (def.getIntent() != null) {
                                availableIntentValues.add(def.getIntent());
                            }
                        }
                    }
                } catch (SchemaException ex) {
                    LOGGER.error("Cannot get refined resource schema for resource {}. {}", resourceModel.getObject().getName().getOrig(), ex.getLocalizedMessage());
                }

                return availableIntentValues;
            }
            @Override
            public void setObject(List<String> o) {
                //
            }

            @Override
            public void detach(){

            }
            });

        intentDropdown.setOutputMarkupId(true);
        form.add(intentDropdown);

                add(form);


//        DropDownChoice kindDropDown = new DropDownChoice<ShadowKindType>(ID_KIND_FIELD, kindModel, Model.ofList(Arrays.asList(ShadowKindType.values()))){
//            @Override
//            protected void onSelectionChanged(ShadowKindType newSelection) {
//                if (newSelection == null){
//                    ConstructionDetailsPanelChainedModel.this.getModelObject().setKind(null);
//                    return;
//                }
//                if (newSelection instanceof ShadowKindType){
//                    ConstructionDetailsPanelChainedModel.this.getModelObject().setKind((ShadowKindType) newSelection);
//                }
//            }
//        };
//        kindDropDown.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
//
//        kindDropDown.setOutputMarkupId(true);
//        add(kindDropDown);
//
//        TextField intentDropDown = new TextField(ID_INTENT_FIELD, intentChoicesModel);
//        DropDownChoicePanel intentDropDown = new DropDownChoicePanel(ID_INTENT_FIELD,
//                PropertyModel.of(getModel(), ConstructionType.F_INTENT.getLocalPart()), intentChoicesModel);
//        intentDropDown.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
//        intentDropDown.setOutputMarkupId(true);
//        add(intentDropDown);

    }

}
