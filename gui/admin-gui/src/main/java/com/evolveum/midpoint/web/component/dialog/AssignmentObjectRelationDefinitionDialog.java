/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.ObjectReferenceChoiceRenderer;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AssignmentObjectRelationDefinitionDialog extends BasePanel<AssignmentObjectRelation> implements Popupable {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(AssignmentObjectRelationDefinitionDialog.class);
    private static final String ID_OBJECT_TYPE = "type";
    private static final String ID_RELATION = "relation";
    private static final String ID_ARCHETYPE = "archetype";
    private static final String ID_BUTTON_OK = "okButton";
    private static final String ID_CANCEL_OK = "cancelButton";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private final Map<String, String> archetypeMap = new HashMap<>();
    private boolean confirmed = false;

    public AssignmentObjectRelationDefinitionDialog(String id) {
        super(id, Model.of(new AssignmentObjectRelation()));
    }
    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        MessagePanel<String> warningMessage = new MessagePanel<>(ID_WARNING_FEEDBACK, MessagePanel.MessagePanelType.WARN,
                createStringResource("AssignmentObjectRelationDefinitionDialog.required.attributes.warning"));
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(this::validationErrorExists));
        add(warningMessage);

        DropDownFormGroup<QName> type = new DropDownFormGroup<>(ID_OBJECT_TYPE, createObjectTypeModel(),
                Model.ofList(getSupportedObjectTypes()),
                new QNameObjectTypeChoiceRenderer(), createStringResource("abstractRoleMemberPanel.type"),
                createStringResource("chooseFocusTypeAndRelationDialogPanel.tooltip.type"),
                null, null, true){
            @Override
            protected String getLabelContainerCssClass() {
                return "col-md-4";
            }

            @Override
            protected String getPropertyContainerCssClass() {
                return "col-md-8";
            }
        };
        type.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        type.getInput().add(new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(AssignmentObjectRelationDefinitionDialog.this);

            }
        });
        type.setOutputMarkupId(true);
        add(type);

        DropDownFormGroup<QName> relation = new DropDownFormGroup<>(ID_RELATION, createRelationModel(),
                Model.ofList(getSupportedRelations()),
                new QNameObjectTypeChoiceRenderer(), createStringResource("relationDropDownChoicePanel.relation"),
                createStringResource("relationDropDownChoicePanel.tooltip.relation"),
                null, null, true){
            @Override
            protected String getLabelContainerCssClass() {
                return "col-md-4";
            }

            @Override
            protected String getPropertyContainerCssClass() {
                return "col-md-8";
            }
        };
        relation.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relation.setOutputMarkupId(true);
        add(relation);

        DropDownFormGroup<ObjectReferenceType> archetype = new DropDownFormGroup<>(ID_ARCHETYPE, createArchetypeModel(),
                getArchetypeListModel(),
                new ObjectReferenceChoiceRenderer(archetypeMap),
                createStringResource("AssignmentObjectRelationDefinitionDialog.archetype"),
                createStringResource("AssignmentObjectRelationDefinitionDialog.archetype.tooltip"),
                null, null, false){
            @Override
            protected String getLabelContainerCssClass() {
                return "col-md-4";
            }

            @Override
            protected String getPropertyContainerCssClass() {
                return "col-md-8";
            }

            @Override
            protected VisibleEnableBehaviour getDropDownVisibleEnableBehavior() {
                return new VisibleEnableBehaviour(() -> true, () -> getSelectedObjectType() != null);
            }
        };
        archetype.getInput().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        archetype.setOutputMarkupId(true);
        add(archetype);

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                confirmed = true;
                if (validationErrorExists()) {
                    target.add(AssignmentObjectRelationDefinitionDialog.this);
                    return;
                }
                AssignmentObjectRelationDefinitionDialog.this.okPerformed(
                        AssignmentObjectRelationDefinitionDialog.this.getModelObject(), target);
                getPageBase().hideMainPopup(target);

            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_OK,
                createStringResource("Button.cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(cancelButton);
    }

    protected abstract void okPerformed(AssignmentObjectRelation assignmentObjectRelation, AjaxRequestTarget target);

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("AssignmentObjectRelationDefinitionDialog.title");
    }

    protected abstract List<QName> getSupportedObjectTypes();
    protected abstract List<QName> getSupportedRelations();

    private IModel<List<ObjectReferenceType>> getArchetypeListModel() {
        return new LoadableModel<List<ObjectReferenceType>>() {
            @Override
            protected List<ObjectReferenceType> load() {
                return getArchetypeList();
            }
        };
    }

    private List<ObjectReferenceType> getArchetypeList() {
        if (getSelectedObjectType() != null) {
            try {
                OperationResult result = new OperationResult("loadArchetypes");
                List<ArchetypeType> archetypes = getPageBase().getModelInteractionService()
                        .getFilteredArchetypesByHolderType(
                                (Class<? extends AssignmentHolderType>) WebComponentUtil.qnameToClass(getSelectedObjectType()),
                                result);
                return WebModelServiceUtils.createObjectReferenceListForObjects(getPageBase(),
                        archetypes.stream().map(ArchetypeType::asPrismObject).collect(Collectors.toList()), archetypeMap);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't load archetypes for object type {}", getSelectedObjectType(), e);
                getSession().error("Couldn't load archetypes for object type " + getSelectedObjectType());
            }
        }
        return new ArrayList<>();
    }

    private IModel<QName> createObjectTypeModel() {
        return new IModel<QName>() {
            private static final long serialVersionUID = 1L;
            @Override
            public QName getObject() {
                return getSelectedObjectType();
            }

            @Override
            public void setObject(QName object) {
                setSelectedObjectType(object);
            }
        };
    }

    private QName getSelectedObjectType() {
        return CollectionUtils.isNotEmpty(getModelObject().getObjectTypes()) ?
                getModelObject().getObjectTypes().get(0) : null;
    }

    private void setSelectedObjectType(QName type) {
        if (getModelObject().getObjectTypes() == null) {
            getModelObject().setObjectTypes(new ArrayList<>());
        }
        getModelObject().getObjectTypes().clear();
        getModelObject().getObjectTypes().add(type);
    }

    private IModel<QName> createRelationModel() {
        return new IModel<QName>() {
            private static final long serialVersionUID = 1L;
            @Override
            public QName getObject() {
                return getSelectedRelation();
            }

            @Override
            public void setObject(QName object) {
                setSelectedRelation(object);
            }
        };
    }

    private QName getSelectedRelation() {
        return CollectionUtils.isNotEmpty(getModelObject().getRelations()) ?
                getModelObject().getRelations().get(0) : null;
    }

    private void setSelectedRelation(QName type) {
        if (getModelObject().getRelations() == null) {
            getModelObject().setRelations(new ArrayList<>());
        }
        getModelObject().getRelations().clear();
        getModelObject().getRelations().add(type);
    }

    private IModel<ObjectReferenceType> createArchetypeModel() {
        return new IModel<ObjectReferenceType>() {
            private static final long serialVersionUID = 1L;
            @Override
            public ObjectReferenceType getObject() {
                return getSelectedArchetype();
            }

            @Override
            public void setObject(ObjectReferenceType object) {
                setSelectedArchetype(object);
            }
        };
    }

    private ObjectReferenceType getSelectedArchetype() {
        return CollectionUtils.isNotEmpty(getModelObject().getArchetypeRefs()) ?
                getModelObject().getArchetypeRefs().get(0) : null;
    }

    private void setSelectedArchetype(ObjectReferenceType type) {
        if (getModelObject().getArchetypeRefs() == null) {
            getModelObject().setArchetypeRefs(new ArrayList<>());
        }
        getModelObject().getArchetypeRefs().clear();
        getModelObject().getArchetypeRefs().add(type);
    }

    private boolean validationErrorExists() {
        return confirmed && (getSelectedObjectType() == null || getSelectedRelation() == null);
    }
}
