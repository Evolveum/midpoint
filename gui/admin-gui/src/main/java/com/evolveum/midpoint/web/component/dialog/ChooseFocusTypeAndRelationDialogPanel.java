/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.RelationUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class ChooseFocusTypeAndRelationDialogPanel extends BasePanel<String> implements Popupable {

    private static final String ID_OBJECT_TYPE = "type";
    private static final String ID_RELATION = "relation";
    private static final String ID_BUTTON_OK = "ok";
    private static final String ID_CANCEL_OK = "cancel";
    private static final String ID_INFO_MESSAGE = "infoMessage";
    private static final String ID_WARNING_FEEDBACK = "warningFeedback";
    private static final String ID_RELATION_REQUIRED = "relationRequired";
    private static final String ID_CONFIGURE_TASK = "configureTask";

    public ChooseFocusTypeAndRelationDialogPanel(String id) {
        this(id, null);
    }

    public ChooseFocusTypeAndRelationDialogPanel(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        MessagePanel<?> warningMessage = new MessagePanel<>(ID_WARNING_FEEDBACK, MessagePanel.MessagePanelType.WARN, getWarningMessageModel()){};
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);

        DropDownFormGroup<QName> type = new DropDownFormGroup<>(ID_OBJECT_TYPE, Model.of(getDefaultObjectType()), Model.ofList(getSupportedObjectTypes()),
                new QNameObjectTypeChoiceRenderer(), createStringResource("chooseFocusTypeAndRelationDialogPanel.type"),
                createStringResource("chooseFocusTypeAndRelationDialogPanel.tooltip.type"), null, null, true){
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
        type.setOutputMarkupId(true);
        type.add(new VisibleBehaviour(this::isFocusTypeSelectorVisible));
        add(type);

        ListMultipleChoicePanel<QName> relation = new ListMultipleChoicePanel<>(ID_RELATION, Model.ofList(getDefaultRelations()),
                new ListModel<>(getSupportedRelations()), RelationUtil.getRelationChoicesRenderer(), null);
        relation.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        relation.setOutputMarkupId(true);
        add(relation);

        WebMarkupContainer relationRequired = new WebMarkupContainer(ID_RELATION_REQUIRED);
        relationRequired.add(new VisibleBehaviour((this::isRelationRequired)));
        add(relationRequired);

        Label label = new Label(ID_INFO_MESSAGE, getModel());
        label.add(new VisibleBehaviour(() -> getModel() != null && getModelObject() != null));
        add(label);

        AjaxButton confirmButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                DropDownFormGroup<QName> type = getTypePanel(getParent());
                QName typeChosen = type.getModelObject();

                ListMultipleChoicePanel<QName> relation = getRelationPanel(getParent());
                Collection<QName> relationChosen = relation.getModelObject();
                if (relationChosen.contains(PrismConstants.Q_ANY)) {
                    relationChosen = getSupportedRelations();
                }
                ChooseFocusTypeAndRelationDialogPanel.this.okPerformed(typeChosen, relationChosen, target);
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

        AjaxButton configuredButton = new AjaxButton(ID_CONFIGURE_TASK,
                new StringResourceModel("ConfigureTaskConfirmationPanel.configure", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                DropDownFormGroup<QName> type = getTypePanel(getParent());
                QName typeChosen = type.getModelObject();

                ListMultipleChoicePanel<QName> relation = getRelationPanel(getParent());
                Collection<QName> relationChosen = relation.getModelObject();
                PrismObject<TaskType> task = createTask(typeChosen, relationChosen, target);
                if (task == null) {
                    return;
                }
                ((PageBase) getPage()).hideMainPopup(target);
                DetailsPageUtil.dispatchToObjectDetailsPage(
                        task, true, ChooseFocusTypeAndRelationDialogPanel.this);
            }
        };
        configuredButton.add(new VisibleBehaviour(this::isTaskConfigureButtonVisible));
        add(configuredButton);

    }

    private ListMultipleChoicePanel<QName> getRelationPanel(Component parent) {
        return (ListMultipleChoicePanel<QName>) parent.get(ID_RELATION);
    }

    private DropDownFormGroup<QName> getTypePanel(Component parent) {
        return (DropDownFormGroup<QName>) parent.get(ID_OBJECT_TYPE);
    }

    protected boolean isTaskConfigureButtonVisible() {
        return false;
    }

    /**
     * Creates a task that will execute requested bulk operation (like "unassign all role members").
     * The task may be presented to the user for editing before direct execution.
     * Returns `null` if the operation is not supported; or that it was cancelled for some reason.
     */
    protected PrismObject<TaskType> createTask(QName type, Collection<QName> relations, AjaxRequestTarget target) {
        return null;
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    private boolean isRelationRequired() {
        return true;
    }

    protected void okPerformed(QName type, Collection<QName> relation, AjaxRequestTarget target) {
        // TODO Auto-generated method stub

    }

    protected List<QName> getSupportedObjectTypes() {
        return ObjectTypeListUtil.createFocusTypeList(true);
    }

    protected QName getDefaultObjectType() {
        List<QName> supportedObjectTypes = getSupportedObjectTypes();
        if (CollectionUtils.isEmpty(supportedObjectTypes)) {
            return FocusType.COMPLEX_TYPE;
        }
        return supportedObjectTypes.iterator().next();
    }

    protected List<QName> getSupportedRelations() {
        return RelationUtil.getAllRelations(getPageBase());
    }

    protected List<QName> getDefaultRelations() {
        return new ArrayList<>();
    }

    protected boolean isFocusTypeSelectorVisible() {
        return true;
    }

    @Override
    public int getWidth() {
        return 400;
    }

    @Override
    public int getHeight() {
        return 300;
    }

    @Override
    public String getWidthUnit(){
        return "px";
    }

    @Override
    public String getHeightUnit(){
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ChooseFocusTypeDialogPanel.chooseType");
    }

}
