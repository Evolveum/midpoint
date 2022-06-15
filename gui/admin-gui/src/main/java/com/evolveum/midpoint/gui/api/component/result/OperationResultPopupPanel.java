/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.ListMultipleChoicePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author honchar
 */
public class OperationResultPopupPanel extends BasePanel<OperationResult> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String ID_OPERATION_RESULTS_PANEL = "operationResultsPanel";
    private static final String ID_BUTTON_OK = "ok";

    public OperationResultPopupPanel(String id, IModel<OperationResult> model){
        super(id, model);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();

        IModel<OpResult> opResultModel = createResultModel();
        OperationResultPanel operationResultPanel = new OperationResultPanel(ID_OPERATION_RESULTS_PANEL,
                opResultModel);
        operationResultPanel.add(new VisibleBehaviour(() -> opResultModel.getObject() != null));
        operationResultPanel.setOutputMarkupId(true);
        add(operationResultPanel);

        AjaxButton okButton = new AjaxButton(ID_BUTTON_OK, createStringResource("Button.ok")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        okButton.setOutputMarkupId(true);
        add(okButton);
    }

    private IModel<OpResult> createResultModel() {
        return new LoadableModel<OpResult>() {
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
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("OperationResultPopupPanel.title");
    }

}
