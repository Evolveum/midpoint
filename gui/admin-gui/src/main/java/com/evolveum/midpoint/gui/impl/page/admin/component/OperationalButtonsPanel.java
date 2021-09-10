/*
 * Copyright (C) 2020-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.Iterator;

public class OperationalButtonsPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OperationalButtonsPanel.class);

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STATE_BUTTONS = "stateButtons";
    private static final String ID_EXECUTE_OPTIONS = "executeOptions";
    private String saveButtonPath = "";

    public OperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        super(id, wrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        RepeatingView repeatingView = new RepeatingView(ID_BUTTONS);
        add(repeatingView);
        createSaveButton(repeatingView);
        createDeleteButton(repeatingView);
        createEditRawButton(repeatingView);

        addButtons(repeatingView);

        createBackButton(repeatingView);

        RepeatingView stateButtonsView = new RepeatingView(ID_STATE_BUTTONS);
        add(stateButtonsView);

        addStateButtons(stateButtonsView);
    }

    private void createEditRawButton(RepeatingView repeatingView) {
        AjaxIconButton edit = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_EDIT_MENU_ITEM),
                getPageBase().createStringResource("AbstractObjectMainPanel.editXmlButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                editRawPerformed(ajaxRequestTarget);
            }
        };
        edit.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(edit);
    }

    private void createBackButton(RepeatingView repeatingView) {
        AjaxIconButton edit = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.ARROW_LEFT),
                getPageBase().createStringResource("pageAdminFocus.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                backPerformed(ajaxRequestTarget);
            }
        };
        edit.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(edit);
    }

    private void createDeleteButton(RepeatingView repeatingView) {
        AjaxIconButton remove = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_ICON_REMOVE),
                getPageBase().createStringResource("OperationalButtonsPanel.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                deletePerformed(ajaxRequestTarget);
            }
        };
        remove.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(remove);
    }

    protected void addButtons(RepeatingView repeatingView) {

    }

    private void createSaveButton(RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SAVE, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton save = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(), iconBuilder.build(),
                getPageBase().createStringResource("PageBase.button.save")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
//        save.add(getVisibilityForSaveButton());
        save.setOutputMarkupId(true);
        save.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(save);

        Form form = save.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(save);
        }

    }

    protected void savePerformed(AjaxRequestTarget target) {

    }

    private void backPerformed(AjaxRequestTarget target) {
        getPageBase().redirectBack();
    }

    private void editRawPerformed(AjaxRequestTarget ajaxRequestTarget) {
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                getPageBase().createStringResource("AbstractObjectMainPanel.confirmEditXmlRedirect")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                PrismObject<O> object = getPrismObject();
                parameters.add(PageDebugView.PARAM_OBJECT_ID, object.getOid());
                parameters.add(PageDebugView.PARAM_OBJECT_TYPE, object.getCompileTimeClass().getSimpleName());
                getPageBase().navigateToNext(PageDebugView.class, parameters);
            }

        };

        getPageBase().showMainPopup(confirmationPanel, ajaxRequestTarget);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(), createStringResource("do you really want to delete " + getObjectType())) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteConfirmPerformed(target);
            }
        };
        getPageBase().showMainPopup(confirmationPanel, target);
    }

    protected void deleteConfirmPerformed(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("Delete object");
        OperationResult result = task.getResult();

        try {
            PrismObject<O> object = getPrismObject();
            ObjectDelta<O> deleteDelta = getPrismContext().deltaFor(object.getCompileTimeClass()).asObjectDelta(object.getOid());
            deleteDelta.setChangeType(ChangeType.DELETE);

            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(deleteDelta), null, task, result);

            result.computeStatusIfUnknown();
            getPageBase().redirectBack();
        } catch (Throwable e) {
            result.recordFatalError("Cannot delete user " + getPrismObject() + ", " + e.getMessage(), e);
            LOGGER.error("Error while deleting user {}, {}", getPrismObject(), e.getMessage(), e);
        }

        getPageBase().showResult(result);

    }

    protected void addStateButtons(RepeatingView stateButtonsView) {

    }

    public boolean buttonsExist(){
        RepeatingView repeatingView = (RepeatingView) get(ID_BUTTONS);
        boolean buttonsExist = repeatingView != null && repeatingView.iterator().hasNext();
        if (buttonsExist) {
            Iterator<Component> buttonsIt = repeatingView.iterator();
            while (buttonsIt.hasNext()) {
                Component comp = buttonsIt.next();
                comp.configure();
                if (comp.isVisible()){
                    return true;
                }
            }
        }
        return false;
    }

    //TODO temporary
    protected boolean getOptionsPanelVisibility() {
        if (getModelObject().isReadOnly()) {
            return false;
        }
        return ItemStatus.NOT_CHANGED != getModelObject().getStatus()
                || getModelObject().canModify();
    }

    public ExecuteChangeOptionsDto getExecuteChangeOptions() {
        ExecuteChangeOptionsPanel optionsPanel = (ExecuteChangeOptionsPanel) get(ID_EXECUTE_OPTIONS);
        return optionsPanel != null ? optionsPanel.getModelObject() : new ExecuteChangeOptionsDto();
    }

    public PrismObject<O> getPrismObject() {
        return getModelObject().getObject();
    }

    public O getObjectType() {
        return getPrismObject().asObjectable();
    }
}
