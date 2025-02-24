/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.pattern;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.component.RoleAnalysisDetectedPatternTileTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.model.RoleAnalysisDetectedPatternsDto;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

@PanelType(name = "sessionRoleSuggestions")
@PanelInstance(
        identifier = "sessionRoleSuggestions",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisDetectionPatternType.action.suggestion",
                icon = GuiStyleConstants.CLASS_ICON_SEARCH,
                order = 30
        )
)
public class SessionRoleSuggestionsPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String DOT_CLASS = SessionRoleSuggestionsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECTS = DOT_CLASS + "loadObjects";

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public SessionRoleSuggestionsPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        @NotNull RoleAnalysisDetectedPatternTileTable components = loadTable();
        container.add(components);
    }

    private @NotNull RoleAnalysisDetectedPatternTileTable loadTable() {
        RoleAnalysisDetectedPatternTileTable components = new RoleAnalysisDetectedPatternTileTable(ID_PANEL, getPageBase(),
                buildModel()) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    @Contract(value = " -> new", pure = true)
    private @NotNull LoadableModel<RoleAnalysisDetectedPatternsDto> buildModel(){
        return new LoadableModel<>() {
            @Override
            protected @NotNull RoleAnalysisDetectedPatternsDto load() {
                RoleAnalysisSessionType session = getObjectDetailsModels().getObjectType();
                RoleAnalysisService roleAnalysisService = ((PageBase) getPage()).getRoleAnalysisService();
                OperationResult result = new OperationResult(OPERATION_LOAD_OBJECTS);
                return new RoleAnalysisDetectedPatternsDto(roleAnalysisService, session, result);
            }
        };
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    private void performOnRefresh() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisSessionType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

}
