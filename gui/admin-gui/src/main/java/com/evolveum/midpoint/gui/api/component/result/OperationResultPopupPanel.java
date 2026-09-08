/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.result;

import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import java.io.Serial;

/**
 * @author honchar
 */
public class OperationResultPopupPanel extends SimplePopupable<OperationResult> {

    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULTS_PANEL = "operationResultsPanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_REPEATER = "repeater";

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
        RepeatingView repeatingView = new RepeatingView(ID_REPEATER);
        customizeFooterButtons(repeatingView);
        footer.add(repeatingView);
        return footer;
    }

    protected void customizeFooterButtons(RepeatingView repeatingView) {
        AjaxLinkPanel close = new AjaxLinkPanel(repeatingView.newChildId(), createStringResource("Button.close")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCloseClicked(target);
            }
        };
        close.add(AttributeAppender.replace("class", "btn btn-outline-primary"));
        repeatingView.add(close);
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
