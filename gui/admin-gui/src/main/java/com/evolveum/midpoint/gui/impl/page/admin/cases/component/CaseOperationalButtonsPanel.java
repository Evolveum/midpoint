/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.util.Objects;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.component.AssignmentHolderOperationalButtonsPanel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

public class CaseOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<CaseType> {

    private static final Trace LOGGER = TraceManager.getTrace(CaseOperationalButtonsPanel.class);
    private static final String DOT_CLASS = CaseOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_STOP_CASE_PROCESS = DOT_CLASS + "stopCaseProcess";

    private static final String ID_STOP_PROCESS = "stopProcess";

    public CaseOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<CaseType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initStopProcessButton();
    }

    private void initStopProcessButton() {
        AjaxIconButton stop = new AjaxIconButton(ID_STOP_PROCESS, Model.of(GuiStyleConstants.CLASS_STOP_MENU_ITEM), createStringResource("pageCases.button.stopProcess")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                stopCaseProcessConfirmed(ajaxRequestTarget);
            }
        };
        stop.showTitleAsLabel(true);
        stop.add(new VisibleBehaviour(() -> {
            PrismObjectWrapper<CaseType> wrapper = getModelObject();
            if (wrapper == null) {
                return false;
            }
            CaseType c = wrapper.getObject().asObjectable();
            return !Objects.equals(c.getState(), SchemaConstants.CASE_STATE_CLOSED);
        }));

        add(stop);
    }

    private void stopCaseProcessConfirmed(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        Task task = page.createSimpleTask(OPERATION_STOP_CASE_PROCESS);
        OperationResult result = task.getResult();
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
