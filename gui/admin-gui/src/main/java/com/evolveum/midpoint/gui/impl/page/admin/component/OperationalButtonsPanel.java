/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.DeleteConfirmationPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugView;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DetailsPageSaveMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectDetailsPageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Use new {@link OperationsPanel} and {@link OperationPanelPart} to create proper HTML for this panel ("card" with fieldsets that are responsive)
 *
 * @param <O>
 */
public class OperationalButtonsPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(OperationalButtonsPanel.class);

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_STATE_BUTTONS = "stateButtons";

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
        createBackButton(repeatingView);
        createSaveButton(repeatingView);

        addButtons(repeatingView);

        createDeleteButton(repeatingView);
        createEditRawButton(repeatingView);

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
        edit.add(new VisibleBehaviour(this::isEditRawButtonVisible));
        edit.showTitleAsLabel(true);
        edit.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(edit);
    }

    protected boolean isEditRawButtonVisible() {
        return isEditingObject() && !isReadonly() && isDebugPageAuthorized();
    }

    private boolean isDebugPageAuthorized() {
        return WebComponentUtil.isAuthorized(AuthorizationConstants.AUTZ_UI_CONFIGURATION_URL,
                AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUGS_URL, AuthorizationConstants.AUTZ_UI_CONFIGURATION_DEBUG_URL);
    }

    private void createBackButton(RepeatingView repeatingView) {
        AjaxIconButton back = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.ARROW_LEFT),
                getPageBase().createStringResource("pageAdminFocus.button.back")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                backPerformed(ajaxRequestTarget);
            }
        };

        back.showTitleAsLabel(true);
        back.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        repeatingView.add(back);
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
        remove.add(new VisibleBehaviour(this::isDeleteButtonVisible));
        remove.showTitleAsLabel(true);
        remove.add(AttributeAppender.append("class", "btn btn-danger btn-sm"));
        repeatingView.add(remove);
    }

    protected boolean isDeleteButtonVisible() {
        return isEditingObject() && !isReadonly() && isAuthorizedToDelete();
    }

    private boolean isAuthorizedToDelete() {
        return getPageBase().isAuthorized(ModelAuthorizationAction.DELETE, getModelObject().getObject());
    }

    protected boolean isReadonly() {
        return getModelObject().isReadOnly();
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

        save.add(new VisibleBehaviour(this::isSaveButtonVisible));
        save.titleAsLabel(true);
        save.setOutputMarkupId(true);
        save.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
        repeatingView.add(save);

        Form<?> form = save.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(save);
        }
    }

    protected void savePerformed(AjaxRequestTarget target) {

    }

    protected boolean isSaveButtonVisible() {
        return !getModelObject().isReadOnly() && !isForcedPreview();
    }

    protected boolean isForcedPreview() {
        GuiObjectDetailsPageType objectDetails = getPageBase().getCompiledGuiProfile()
                .findObjectDetailsConfiguration(getModelObject().getCompileTimeClass());
        return objectDetails != null && DetailsPageSaveMethodType.FORCED_PREVIEW.equals(objectDetails.getSaveMethod());
    }

    protected boolean hasUnsavedChanges(AjaxRequestTarget target) {
        return false;
    }

    protected void backPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        if (!hasUnsavedChanges(target)) {
            backPerformedConfirmed();
            return;
        }

        ConfirmationPanel confirmationPanel = new ConfirmationPanel(page.getMainPopupBodyId(),
                page.createStringResource("OperationalButtonsPanel.confirmBack")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                backPerformedConfirmed();
            }

        };

        page.showMainPopup(confirmationPanel, target);
    }

    protected void backPerformedConfirmed() {
        getPageBase().redirectBack();
    }

    private void editRawPerformed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        if (!hasUnsavedChanges(target)) {
            editRawPerformedConfirmed();
            return;
        }

        ConfirmationPanel confirmationPanel = new ConfirmationPanel(page.getMainPopupBodyId(),
                getPageBase().createStringResource("AbstractObjectMainPanel.confirmEditXmlRedirect")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                editRawPerformedConfirmed();
            }

        };

        page.showMainPopup(confirmationPanel, target);
    }

    protected void editRawPerformedConfirmed() {
        PageParameters parameters = new PageParameters();
        PrismObject<O> object = getPrismObject();
        parameters.add(PageDebugView.PARAM_OBJECT_ID, object.getOid());
        parameters.add(PageDebugView.PARAM_OBJECT_TYPE, object.getCompileTimeClass().getSimpleName());

        getPageBase().navigateToNext(PageDebugView.class, parameters);
    }

    private void deletePerformed(AjaxRequestTarget target) {
        ConfirmationPanel confirmationPanel = new DeleteConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.deletePerformed", WebComponentUtil.getDisplayNameOrName(getPrismObject()))) {
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
            target.add(getPageBase().getFeedbackPanel());
        }

        getPageBase().showResult(result);

    }

    protected void addStateButtons(RepeatingView stateButtonsView) {

    }

    public boolean buttonsExist() {
        RepeatingView repeatingView = (RepeatingView) get(ID_BUTTONS);
        boolean buttonsExist = repeatingView != null && repeatingView.iterator().hasNext();
        if (buttonsExist) {
            for (Component comp : repeatingView) {
                comp.configure();
                if (comp.isVisible()) {
                    return true;
                }
            }
        }
        return false;
    }

    public PrismObject<O> getPrismObject() {
        return getModelObject().getObject();
    }

    public O getObjectType() {
        return getPrismObject().asObjectable();
    }

    protected boolean isEditingObject() {
        return StringUtils.isNoneEmpty(getModelObject().getOid());
    }
}
