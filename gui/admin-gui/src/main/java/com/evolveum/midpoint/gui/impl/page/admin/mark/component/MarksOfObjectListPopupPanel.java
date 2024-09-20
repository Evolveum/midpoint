/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.mark.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

public class MarksOfObjectListPopupPanel<O extends ObjectType> extends SimplePopupable {

    private static final Trace LOGGER = TraceManager.getTrace(MarksOfObjectListPopupPanel.class);

    private static final String DOT_CLASS = MarksOfObjectListPopupPanel.class.getName() + ".";
    private static final String OPERATION_SAVE_MARKS = DOT_CLASS + "saveMarks";

    private static final String ID_PANEL = "panel";
    private static final String ID_BUTTON_SAVE = "saveButton";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footer;
    private final IModel<PrismObjectWrapper<O>> wrapper;

    public MarksOfObjectListPopupPanel(String id, IModel<PrismObjectWrapper<O>> wrapper){
        super(id, 80, 800, () -> LocalizationUtil.translate("MarksOfObjectListPopupPanel.title"));
        this.wrapper = wrapper;
        initLayout();
        initFooter();
    }

    private void initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxSubmitButton save = new AjaxSubmitButton(ID_BUTTON_SAVE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                saveWrapper(target);
                getPageBase().hideMainPopup(target);
            }
        };
        save.setOutputMarkupId(true);
        footer.add(save);

        AjaxButton cancel = new AjaxButton(ID_BUTTON_CANCEL) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        cancel.setOutputMarkupId(true);
        footer.add(cancel);
        this.footer = footer;
    }

    private void saveWrapper(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(OPERATION_SAVE_MARKS);
        OperationResult result = task.getResult();

        try {
            var delta = wrapper.getObject().getObjectDelta();
            getPageBase().getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
        } catch (Exception e) {
            result.recordPartialError(
                    createStringResource(
                            "MarksOfObjectListPopupPanel.message.saveMarks.partialError", wrapper.getObject())
                            .getString(),
                    e);
            LOGGER.error("Could not save marks of object {}", wrapper.getObject(), e);
        }

        result.computeStatusIfUnknown();
        getPageBase().showResult(result);
        target.add(getPageBase().getFeedbackPanel());

        onSave(target);
    }

    protected void onSave(AjaxRequestTarget target) {
    }

    private void initLayout(){
        MarksOfObjectListPanel<O> panel = new MarksOfObjectListPanel<>(ID_PANEL, wrapper);
        panel.setUseCollectionView(false);

        add(panel);
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    protected Component getPanel () {
        return get(ID_PANEL);
    }
}
