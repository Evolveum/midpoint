/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author honchar
 */
public class OperationResultPopupPanel extends BasePanel<OperationResult> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULTS_PANEL = "operationResultsPanel";

    public OperationResultPopupPanel(String id, IModel<OperationResult> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        OperationResultPanel operationResultPanel = new OperationResultPanel(ID_OPERATION_RESULTS_PANEL,
                Model.of(OpResult.getOpResult(getPageBase(), getModelObject())));
        operationResultPanel.setOutputMarkupId(true);
        add(operationResultPanel);
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 600;
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
    public StringResourceModel getTitle() {
        return new StringResourceModel("OperationResultPopupPanel.title");
    }

    @Override
    public Component getComponent() {
        return this;
    }


}
