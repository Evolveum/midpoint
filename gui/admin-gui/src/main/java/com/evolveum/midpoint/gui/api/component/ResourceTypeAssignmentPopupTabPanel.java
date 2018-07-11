/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.RelationTypes;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by honchar
 */
public class ResourceTypeAssignmentPopupTabPanel extends AbstractAssignmentPopupTabPanel<ResourceType>{

    private static final long serialVersionUID = 1L;

    private static final String ID_KIND_CONTAINER = "kindContainer";
    private static final String ID_INTENT_CONTAINER = "intentContainer";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";

    private LoadableModel<List<String>> intentValues;
    private String intentValue;
    private ShadowKindType kindValue;

    private static final String DOT_CLASS = ResourceTypeAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeAssignmentPopupTabPanel.class);

    public ResourceTypeAssignmentPopupTabPanel(String id){
        super(id, ObjectTypes.RESOURCE);
    }

    @Override
    protected void initParametersPanel(){
        initModels();

        WebMarkupContainer kindContainer = new WebMarkupContainer(ID_KIND_CONTAINER);
        kindContainer.setOutputMarkupId(true);
        add(kindContainer);

        DropDownChoicePanel<ShadowKindType> kindSelector = WebComponentUtil.createEnumPanel(ShadowKindType.class, ID_KIND,
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), Model.of(),
                ResourceTypeAssignmentPopupTabPanel.this, true);
        kindSelector.setOutputMarkupId(true);
        kindSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getIntentDropDown());
            }
        });
        kindSelector.getBaseFormComponent().add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0;
            }
        });
        kindSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        kindSelector.setOutputMarkupPlaceholderTag(true);
        kindContainer.add(kindSelector);

        WebMarkupContainer intentContainer = new WebMarkupContainer(ID_INTENT_CONTAINER);
        intentContainer.setOutputMarkupId(true);
        add(intentContainer);

        DropDownChoicePanel<String> intentSelector = new DropDownChoicePanel<String>(ID_INTENT,
                Model.of(), intentValues, true);
        intentSelector.getBaseFormComponent().add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0;
            }
        });
        intentSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        intentSelector.setOutputMarkupId(true);
        intentSelector.setOutputMarkupPlaceholderTag(true);
        intentContainer.add(intentSelector);

    }

    private void initModels(){
        intentValues = new LoadableModel<List<String>>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                List<String> availableIntentValues = new ArrayList<>();
                PopupObjectListPanel resourcesListPanel = getObjectListPanel();
                if (resourcesListPanel != null) {
                    List<ResourceType> selectedResources = resourcesListPanel.getSelectedObjects();
                    if (selectedResources != null && selectedResources.size() > 0) {
                        ResourceType selectedResource = selectedResources.get(0);

                        try {
                            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(selectedResource.asPrismObject());
                            if (refinedSchema != null) {
                                ShadowKindType kind = (ShadowKindType) getKindDropDown().getBaseFormComponent().getModelObject();
                                List<? extends RefinedObjectClassDefinition> definitions = refinedSchema.getRefinedDefinitions(kind);
                                for (RefinedObjectClassDefinition def : definitions) {
                                    availableIntentValues.add(def.getIntent());
                                }
                            }
                        } catch (SchemaException ex) {
                            LOGGER.error("Cannot get refined resource schema for resource {}. {}", selectedResource.getName().getOrig(), ex.getLocalizedMessage());
                        }

                    }
                }
                if (availableIntentValues.size() > 0){
                    intentValue = availableIntentValues.get(0);
                }
                return availableIntentValues;
            }
        };
    }

    @Override
    protected Set<AssignmentType> getSelectedAssignmentsList(){
        Set<AssignmentType> assignmentList = new HashSet<>();

        List<ResourceType> selectedObjects = getSelectedObjectsList();
        ShadowKindType kind = getKindValue();
        String intent = getIntentValue();
        selectedObjects.forEach(selectedObject -> {
            assignmentList.add(ObjectTypeUtil.createAssignmentWithConstruction(selectedObject.asPrismObject(), kind, intent));
        });
        return assignmentList;
    }

    public ShadowKindType getKindValue(){
        DropDownChoicePanel<ShadowKindType> kindDropDown = getKindDropDown();
        return kindDropDown.getModel() != null ? kindDropDown.getModel().getObject() : null;
    }

    public String getIntentValue(){
        DropDownChoicePanel<String> intentDropDown = getIntentDropDown();
        return intentDropDown.getModel() != null ? intentDropDown.getModel().getObject() : null;
    }

    private DropDownChoicePanel<String> getIntentDropDown(){
        return (DropDownChoicePanel<String>)get(ID_INTENT_CONTAINER).get(ID_INTENT);
    }

    private DropDownChoicePanel<ShadowKindType> getKindDropDown(){
        return (DropDownChoicePanel<ShadowKindType>)get(ID_KIND_CONTAINER).get(ID_KIND);
    }

    @Override
    protected void onSelectionPerformed(AjaxRequestTarget target){
        target.add(getObjectListPanel());
    }

    @Override
    protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ResourceType>> rowModel){
        List selectedObjects = getSelectedObjectsList();
        return Model.of(selectedObjects == null || selectedObjects.size() == 0
                || (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isSelected()));
    }
}
