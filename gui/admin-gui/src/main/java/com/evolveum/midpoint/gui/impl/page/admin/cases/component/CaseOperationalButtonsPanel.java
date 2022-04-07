/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

public class CaseOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(CaseOperationalButtonsPanel.class);
    private static final String DOT_CLASS = CaseOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_STOP_CASE_PROCESS = DOT_CLASS + "stopCaseProcess";

    private static final String ID_CASES_BUTTONS = "caseButtons";

    public CaseOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<CaseType>> model) {
        super(id, model);
    }


    @Override
    protected void onInitialize() {
        super.onInitialize();
        initStopProcessButton();
    }

    private void initStopProcessButton() {
        RepeatingView repeatingView = new RepeatingView(ID_CASES_BUTTONS);
        add(repeatingView);

        AjaxIconButton preview = new AjaxIconButton(repeatingView.newChildId(), Model.of(GuiStyleConstants.CLASS_STOP_MENU_ITEM), createStringResource("pageCases.button.stopProcess")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                stopCaseProcessConfirmed(ajaxRequestTarget);
            }
        };
        preview.showTitleAsLabel(true);
        preview.add(AttributeAppender.append("class", "btn btn-default btn-sm"));

        repeatingView.add(preview);
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        OperationResult result = new OperationResult(OPERATION_STOP_CASE_PROCESS);
        Task task = page.createSimpleTask(OPERATION_STOP_CASE_PROCESS);
        try {
            page.getCaseService().cancelCase(getModelObject().getOid(), task, result);
        } catch (Exception ex) {
            LOGGER.error("Couldn't stop case process: {}", ex.getLocalizedMessage());
            result.recordFatalError(createStringResource("PageCases.message.stopCaseProcessConfirmed.fatalError").getString(), ex);
        }
        result.computeStatusComposite();

        page.showResult(result);

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            target.add(page.getFeedbackPanel());
        } else {
            page.redirectBack();
        }
    }

    @Override
    protected boolean isSaveButtonVisible() {
        return false;
    }
}
