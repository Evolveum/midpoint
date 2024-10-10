/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisInfoPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisSessionTileTable;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/roleAnalysis", matchUrlForSecurity = "/admin/roleAnalysis")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ROLE_ANALYSIS_ALL_URL,
                        label = "PageRoleAnalysis.auth.roleAnalysisAll.label",
                        description = "PageRoleAnalysis.auth.roleAnalysisAll.description")
        })

public class PageRoleAnalysis extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_INFO_FORM = "infoForm";
    private static final String ID_CHART_PANEL = "chartPanel";
    private static final String ID_TABLE = "table";

    private static final String DOT_CLASS = PageRoleAnalysis.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_ANALYSIS_INFO = DOT_CLASS + "loadRoleAnalysisInfo";

    public PageRoleAnalysis(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        Form<?> infoForm = new MidpointForm<>(ID_INFO_FORM);
        add(infoForm);

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        if (checkNative(mainForm, infoForm)) {
            return;
        }

        initInfoPanel(infoForm);

        RoleAnalysisSessionTileTable roleAnalysisSessionTileTable = new RoleAnalysisSessionTileTable(ID_TABLE, (PageBase) getPage());
        roleAnalysisSessionTileTable.setOutputMarkupId(true);
        mainForm.add(roleAnalysisSessionTileTable);

    }

    private void initInfoPanel(@NotNull Form<?> infoForm) {

        AnalysisInfoWidgetDto analysisInfoWidgetDto = modelInitialization();

        RoleAnalysisInfoPanel roleAnalysisInfoPanel = new RoleAnalysisInfoPanel(ID_CHART_PANEL, new LoadableModel<>() {
            @Override
            protected AnalysisInfoWidgetDto load() {
                return analysisInfoWidgetDto;
            }
        });
        roleAnalysisInfoPanel.setOutputMarkupId(true);
        infoForm.add(roleAnalysisInfoPanel);
    }

    protected AnalysisInfoWidgetDto modelInitialization() {
        PageBase pageBase = (PageBase) getPage();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        OperationResult result = new OperationResult(OPERATION_LOAD_ROLE_ANALYSIS_INFO);
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_ROLE_ANALYSIS_INFO);

        AnalysisInfoWidgetDto analysisInfoWidgetDto = new AnalysisInfoWidgetDto();
        analysisInfoWidgetDto.loadOutlierModels(result, roleAnalysisService, pageBase);
        analysisInfoWidgetDto.loadPatternModelsAsync(result, roleAnalysisService, pageBase, task);
        return analysisInfoWidgetDto;
    }

    private boolean checkNative(Form<?> mainForm, Form<?> infoForm) {
        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE, createStringResource("RoleAnalysis.menu.nonNativeRepositoryWarning")));
            infoForm.add(new EmptyPanel(ID_CHART_PANEL));
            return true;
        }
        return false;
    }

}
