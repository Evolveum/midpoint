/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

/**
 * Created by honchar
 */
public class ResourceTypeAssignmentPopupTabPanel extends AbstractAssignmentPopupTabPanel<ResourceType>{

    private static final long serialVersionUID = 1L;

    private static final String ID_KIND_CONTAINER = "kindContainer";
    private static final String ID_INTENT_CONTAINER = "intentContainer";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_ASSOCIATION_CONTAINER = "associationContainer";
    private static final String ID_ASSOCIATION = "association";

    private LoadableModel<List<String>> intentValues;
    private LoadableModel<List<RefinedAssociationDefinition>> associationValuesModel;

    private static final String DOT_CLASS = ResourceTypeAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeAssignmentPopupTabPanel.class);

    public ResourceTypeAssignmentPopupTabPanel(String id){
        super(id, ObjectTypes.RESOURCE);
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel){
        initModels();

        WebMarkupContainer kindContainer = new WebMarkupContainer(ID_KIND_CONTAINER);
        kindContainer.setOutputMarkupId(true);
        parametersPanel.add(kindContainer);

        DropDownChoicePanel<ShadowKindType> kindSelector = WebComponentUtil.createEnumPanel(ShadowKindType.class, ID_KIND,
                WebComponentUtil.createReadonlyModelFromEnum(ShadowKindType.class), Model.of(),
                ResourceTypeAssignmentPopupTabPanel.this, true);
        kindSelector.setOutputMarkupId(true);
        kindSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                kindValueUpdatePerformed(target);
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
        parametersPanel.add(intentContainer);

        DropDownChoicePanel<String> intentSelector = new DropDownChoicePanel<String>(ID_INTENT,
                Model.of(), intentValues, true);
        intentSelector.getBaseFormComponent().add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled(){
                return getKindValue() != null && getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0;
            }
        });
        intentSelector.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                intentValueUpdatePerformed(target);
            }
        });
        intentSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        intentSelector.setOutputMarkupId(true);
        intentSelector.setOutputMarkupPlaceholderTag(true);
        intentContainer.add(intentSelector);

        WebMarkupContainer associationContainer = new WebMarkupContainer(ID_ASSOCIATION_CONTAINER);
        associationContainer.setOutputMarkupId(true);
        associationContainer.add(new VisibleBehaviour(() -> isEntitlementAssignment()));
        parametersPanel.add(associationContainer);

        DropDownChoicePanel<RefinedAssociationDefinition> associationSelector = new DropDownChoicePanel<>(ID_ASSOCIATION,
                Model.of(), associationValuesModel, new IChoiceRenderer<RefinedAssociationDefinition>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(RefinedAssociationDefinition refinedAssociationDefinition) {
                return WebComponentUtil.getAssociationDisplayName(refinedAssociationDefinition);
            }

            @Override
            public String getIdValue(RefinedAssociationDefinition refinedAssociationDefinition, int index) {
                return Integer.toString(index);
            }

            @Override
            public RefinedAssociationDefinition getObject(String id, IModel<? extends List<? extends RefinedAssociationDefinition>> choices) {
                return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
            }
        }, true);
        associationSelector.setOutputMarkupId(true);
        associationSelector.getBaseFormComponent().add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
                return getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0 && getKindValue() != null &&
                        StringUtils.isNotEmpty(getIntentValue());
            }
        });
        associationSelector.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        associationSelector.setOutputMarkupPlaceholderTag(true);
        associationContainer.add(associationSelector);

    }

    protected void initModels(){
        intentValues = new LoadableModel<List<String>>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                List<String> availableIntentValues = new ArrayList<>();
                List<ResourceType> selectedResources = ResourceTypeAssignmentPopupTabPanel.this.getSelectedObjectsList();
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
//                if (availableIntentValues.size() > 0){
//                    intentValue = availableIntentValues.get(0);
//                }
                return availableIntentValues;
            }
        };
        associationValuesModel = new LoadableModel<List<RefinedAssociationDefinition>>() {
            @Override
            protected List<RefinedAssociationDefinition> load() {
                ResourceType resource = getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0 ?
                        getSelectedObjectsList().get(0) : null;
                if (resource == null) {
                    return new ArrayList<>();
                }
                return WebComponentUtil.getRefinedAssociationDefinition(resource, getKindValue(), getIntentValue());
            }
        };
    }

    private void kindValueUpdatePerformed(AjaxRequestTarget target){
        target.add(getIntentDropDown());
        target.add(getAssociationDropDown());
    }

    private void intentValueUpdatePerformed(AjaxRequestTarget target){
        target.add(getAssociationDropDown());
    }

    @Override
    protected Map<String, AssignmentType> getSelectedAssignmentsMap(){
        Map<String, AssignmentType> assignmentList = new HashMap<>();

        List<ResourceType> selectedObjects = getSelectedObjectsList();
        ShadowKindType kind = getKindValue();
        String intent = getIntentValue();
        selectedObjects.forEach(selectedObject -> {
            AssignmentType newConstructionAssignment = ObjectTypeUtil.createAssignmentWithConstruction(
                    selectedObject.asPrismObject(), kind, intent, getPageBase().getPrismContext());
            if (isEntitlementAssignment()){
                NameItemPathSegment segment = getAssociationValue() != null ? new NameItemPathSegment(getAssociationValue().getName()) : null;

                if (segment != null) {
                    ResourceObjectAssociationType association = new ResourceObjectAssociationType();
                    association.setRef(new ItemPathType(ItemPath.create(segment)));
                    newConstructionAssignment.getConstruction().getAssociation().add(association);
                }
            }
            assignmentList.put(selectedObject.getOid(), newConstructionAssignment);
        });
        return assignmentList;
    }

    private ShadowKindType getKindValue(){
        DropDownChoicePanel<ShadowKindType> kindDropDown = getKindDropDown();
        return kindDropDown.getModel() != null ? kindDropDown.getModel().getObject() : null;
    }

    private String getIntentValue(){
        DropDownChoicePanel<String> intentDropDown = getIntentDropDown();
        return intentDropDown.getModel() != null ? intentDropDown.getModel().getObject() : null;
    }

    private RefinedAssociationDefinition getAssociationValue(){
        DropDownChoicePanel<RefinedAssociationDefinition> associationDropDown = getAssociationDropDown();
        return associationDropDown != null ? associationDropDown.getModel().getObject() : null;
    }

    private DropDownChoicePanel<String> getIntentDropDown(){
        return (DropDownChoicePanel<String>)get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_INTENT_CONTAINER, ID_INTENT));
    }

    private DropDownChoicePanel<ShadowKindType> getKindDropDown(){
        return (DropDownChoicePanel<ShadowKindType>)get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_KIND_CONTAINER, ID_KIND));
    }

    private DropDownChoicePanel<RefinedAssociationDefinition> getAssociationDropDown(){
        return (DropDownChoicePanel<RefinedAssociationDefinition>) get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_ASSOCIATION_CONTAINER, ID_ASSOCIATION));
    }

    @Override
    protected ObjectTypes getObjectType(){
        return ObjectTypes.RESOURCE;
    }

    @Override
    protected void onSelectionPerformed(AjaxRequestTarget target, IModel<SelectableBean<ResourceType>> rowModel){
        target.add(getObjectListPanel());
        target.add(getKindDropDown());
        target.add(getIntentDropDown());
    }

    @Override
    protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ResourceType>> rowModel){
        List selectedObjects = getSelectedObjectsList();
        return Model.of(selectedObjects == null || selectedObjects.size() == 0
                || (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isSelected()));
    }

    protected boolean isEntitlementAssignment(){
        return false;
    }
}
