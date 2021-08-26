/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import com.evolveum.midpoint.web.component.prism.show.ScenePanel;
import com.evolveum.midpoint.web.page.admin.cases.OperationRequestCaseTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
@PanelType(name = "manualCase")
@PanelInstance(identifier = "manualCase",
        display = @PanelDisplay(label = "Manual case"))
public class ManualCasePanel extends AbstractObjectMainPanel<CaseType, AssignmentHolderDetailsModel<CaseType>> {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = OperationRequestCaseTabPanel.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(OperationRequestCaseTabPanel.class);
    private static final String OPERATION_PREPARE_DELTA_VISUALIZATION = DOT_CLASS + "prepareDeltaVisualization";

    private static final String ID_MANUAL_CASE_DETAILS_PANEL = "manualCaseDetailsPanel";
    private IModel<SceneDto> sceneModel;

    public ManualCasePanel(String id, AssignmentHolderDetailsModel<CaseType> objectWrapperModel, ContainerPanelConfigurationType config) {
        super(id, objectWrapperModel, config);
        initModels();
    }

    private void initModels(){
        sceneModel = new LoadableModel<SceneDto>(false) {
            @Override
            protected SceneDto load() {
                PageBase pageBase = ManualCasePanel.this.getPageBase();
                try {
                    return WebComponentUtil.createSceneDtoForManualCase(ManualCasePanel.this.getObjectWrapperModel().getObject().getObject().asObjectable(),
                            pageBase,  OPERATION_PREPARE_DELTA_VISUALIZATION);
                } catch (Exception ex){
                    LOGGER.error("Couldn't prepare delta visualization: {}", ex.getLocalizedMessage());
                }
                return null;
            }
        };
    }

    protected void initLayout() {
        ScenePanel scenePanel = new ScenePanel(ID_MANUAL_CASE_DETAILS_PANEL, sceneModel);
        scenePanel.setOutputMarkupId(true);
        add(scenePanel);
    }

}
