/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author honchar
 */
public class OperationResultPopupPanel extends SimplePopupable<OperationResult> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULTS_PANEL = "operationResultsPanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    public OperationResultPopupPanel(String id, IModel<OperationResult> model) {
        super(id, model, 800, 600, new StringResourceModel("OperationResultPopupPanel.title"));
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        IModel<OpResult> opResultModel = createResultModel();
        OperationResultPanel operationResultPanel = new OperationResultPanel(ID_OPERATION_RESULTS_PANEL, opResultModel);
        operationResultPanel.add(new VisibleBehaviour(() -> opResultModel.getObject() != null));
        operationResultPanel.setOutputMarkupId(true);
        add(operationResultPanel);

        footer = initFooter();

    }

    private Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxLink close = new AjaxLink<Void>(ID_CLOSE) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCloseClicked(target);
            }
        };
        footer.add(close);

        return footer;
    }

    protected void onCloseClicked(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    private IModel<OpResult> createResultModel() {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected OpResult load() {
                if (getModelObject() == null) {
                    return null;
                }
                return OpResult.getOpResult(getPageBase(), getModelObject());
            }
        };
    }
}
