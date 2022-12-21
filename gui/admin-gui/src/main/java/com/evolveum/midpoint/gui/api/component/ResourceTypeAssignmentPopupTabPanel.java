/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Created by honchar
 */
public class ResourceTypeAssignmentPopupTabPanel extends AbstractAssignmentPopupTabPanel<ResourceType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_KIND_CONTAINER = "kindContainer";
    private static final String ID_INTENT_CONTAINER = "intentContainer";
    private static final String ID_KIND = "kind";
    private static final String ID_INTENT = "intent";
    private static final String ID_ASSOCIATION_CONTAINER = "associationContainer";
    private static final String ID_ASSOCIATION = "association";

    private LoadableModel<List<String>> intentValues;
    private LoadableModel<List<ResourceAssociationDefinition>> associationValuesModel;

    private static final String DOT_CLASS = ResourceTypeAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(ResourceTypeAssignmentPopupTabPanel.class);

    public ResourceTypeAssignmentPopupTabPanel(String id, AssignmentObjectRelation assignmentObjectRelation) {
        super(id, ObjectTypes.RESOURCE, assignmentObjectRelation);
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel) {
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
        kindSelector.getBaseFormComponent().add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
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
        intentSelector.getBaseFormComponent().add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled() {
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

        DropDownChoicePanel<ResourceAssociationDefinition> associationSelector = new DropDownChoicePanel<>(ID_ASSOCIATION,
                Model.of(), associationValuesModel, new IChoiceRenderer<ResourceAssociationDefinition>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(ResourceAssociationDefinition refinedAssociationDefinition) {
                return WebComponentUtil.getAssociationDisplayName(refinedAssociationDefinition);
            }

            @Override
            public String getIdValue(ResourceAssociationDefinition refinedAssociationDefinition, int index) {
                return Integer.toString(index);
            }

            @Override
            public ResourceAssociationDefinition getObject(String id, IModel<? extends List<? extends ResourceAssociationDefinition>> choices) {
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

    protected void initModels() {
        intentValues = new LoadableModel<List<String>>(true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<String> load() {
                List<String> availableIntentValues = new ArrayList<>();
                List<ResourceType> selectedResources = ResourceTypeAssignmentPopupTabPanel.this.getSelectedObjectsList();
                if (selectedResources != null && selectedResources.size() > 0) {
                    ResourceType selectedResource = selectedResources.get(0);

                    try {
                        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(selectedResource.asPrismObject());
                        if (refinedSchema != null) {
                            ShadowKindType kind = getKindDropDown().getBaseFormComponent().getModelObject();
                            List<? extends ResourceObjectTypeDefinition> definitions = refinedSchema.getObjectTypeDefinitions(kind);
                            for (ResourceObjectTypeDefinition def : definitions) {
                                availableIntentValues.add(def.getIntent());
                            }
                        }
                    } catch (SchemaException | ConfigurationException ex) {
                        LOGGER.error("Cannot get refined resource schema for resource {}. {}", selectedResource.getName().getOrig(), ex.getLocalizedMessage());
                    }

                }
//                if (availableIntentValues.size() > 0){
//                    intentValue = availableIntentValues.get(0);
//                }
                return availableIntentValues;
            }
        };
        associationValuesModel = new LoadableModel<List<ResourceAssociationDefinition>>() {
            @Override
            protected List<ResourceAssociationDefinition> load() {
                ResourceType resource = getSelectedObjectsList() != null && getSelectedObjectsList().size() > 0 ?
                        getSelectedObjectsList().get(0) : null;
                if (resource == null) {
                    return new ArrayList<>();
                }
                return WebComponentUtil.getRefinedAssociationDefinition(resource, getKindValue(), getIntentValue());
            }
        };
    }

    private void kindValueUpdatePerformed(AjaxRequestTarget target) {
        target.add(getIntentDropDown());
        target.add(getAssociationDropDown());
    }

    private void intentValueUpdatePerformed(AjaxRequestTarget target) {
        target.add(getAssociationDropDown());
    }

    @Override
    protected Map<String, AssignmentType> getSelectedAssignmentsMap() {
        Map<String, AssignmentType> assignmentList = new HashMap<>();

        List<ResourceType> selectedObjects = getPreselectedObjects();
        ShadowKindType kind = getKindValue();
        String intent = getIntentValue();
        selectedObjects.forEach(selectedObject -> {
            AssignmentType newConstructionAssignment = ObjectTypeUtil.createAssignmentWithConstruction(
                    selectedObject.asPrismObject(), kind, intent);
            if (isEntitlementAssignment()) {
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

    private ShadowKindType getKindValue() {
        DropDownChoicePanel<ShadowKindType> kindDropDown = getKindDropDown();
        return kindDropDown.getModel() != null ? kindDropDown.getModel().getObject() : null;
    }

    private String getIntentValue() {
        DropDownChoicePanel<String> intentDropDown = getIntentDropDown();
        return intentDropDown.getModel() != null ? intentDropDown.getModel().getObject() : null;
    }

    private ResourceAssociationDefinition getAssociationValue() {
        DropDownChoicePanel<ResourceAssociationDefinition> associationDropDown = getAssociationDropDown();
        return associationDropDown != null ? associationDropDown.getModel().getObject() : null;
    }

    private DropDownChoicePanel<String> getIntentDropDown() {
        return (DropDownChoicePanel<String>) get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_INTENT_CONTAINER, ID_INTENT));
    }

    private DropDownChoicePanel<ShadowKindType> getKindDropDown() {
        return (DropDownChoicePanel<ShadowKindType>) get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_KIND_CONTAINER, ID_KIND));
    }

    private DropDownChoicePanel<ResourceAssociationDefinition> getAssociationDropDown() {
        return (DropDownChoicePanel<ResourceAssociationDefinition>) get(getPageBase().createComponentPath(ID_PARAMETERS_PANEL, ID_ASSOCIATION_CONTAINER, ID_ASSOCIATION));
    }

    @Override
    protected ObjectTypes getObjectType() {
        return ObjectTypes.RESOURCE;
    }

    @Override
    protected void onSelectionPerformed(AjaxRequestTarget target, List<IModel<SelectableBean<ResourceType>>> rowModelList, DataTable dataTable) {
        TableUtil.updateRows(dataTable, target);
        target.add(getKindDropDown());
        target.add(getIntentDropDown());
    }

    @Override
    protected IModel<Boolean> getObjectSelectCheckBoxEnableModel(IModel<SelectableBean<ResourceType>> rowModel) {
        return new ReadOnlyModel<>(() -> CollectionUtils.isEmpty(getSelectedObjectsList()) || (rowModel != null && rowModel.getObject() != null && rowModel.getObject().isSelected()));
    }

    protected boolean isEntitlementAssignment() {
        return false;
    }
}
