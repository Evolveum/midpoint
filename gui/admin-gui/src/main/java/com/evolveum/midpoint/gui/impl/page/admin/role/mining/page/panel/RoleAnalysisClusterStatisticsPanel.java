/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel;

import java.io.Serial;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPopupPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AnalysisClusterStatisticType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

@PanelType(name = "roleAnalysisStatisticsPanel")
@PanelInstance(
        identifier = "clusterStatistic",
        applicableForType = RoleAnalysisClusterType.class,
        display = @PanelDisplay(
                label = "AnalysisClusterStatisticType.clusterStatistic",
                icon = GuiStyleConstants.CLASS_REPORT_ICON,
                order = 40
        ),
        containerPath = "clusterStatistics",
        type = "AnalysisClusterStatisticType",
        expanded = true
)
public class RoleAnalysisClusterStatisticsPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_CHART_PANEL = "chartPanel";
    private static final String ID_CONTAINER_PANEL = "container";

    public RoleAnalysisClusterStatisticsPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        initAttributeStatisticsPanel();
        SingleContainerPanel mainPanel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration()) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                return getBasicTabVisibility(itemWrapper.getPath());
            }

            @Override
            protected ItemEditabilityHandler getEditabilityHandler() {
                return wrapper -> true;
            }

        };
        add(mainPanel);
    }

    private ItemVisibility getBasicTabVisibility(ItemPath path) {

        if (path.equivalent(ItemPath.create(RoleAnalysisClusterType.F_CLUSTER_STATISTICS,
                AnalysisClusterStatisticType.F_ROLE_ATTRIBUTE_ANALYSIS_RESULT))) {
            return ItemVisibility.HIDDEN;
        } else if (path.equivalent(ItemPath.create(RoleAnalysisClusterType.F_CLUSTER_STATISTICS,
                AnalysisClusterStatisticType.F_USER_ATTRIBUTE_ANALYSIS_RESULT))) {
            return ItemVisibility.HIDDEN;
        }
        return ItemVisibility.AUTO;

    }

    public void initAttributeStatisticsPanel() {

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer(ID_CONTAINER_PANEL);
        webMarkupContainer.setOutputMarkupId(true);
        add(webMarkupContainer);


        if (getObjectWrapperModel() != null) {

            RoleAnalysisClusterType cluster = getObjectWrapperModel().getObject().getObject().getRealValue();

            CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                    .setBasicIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, LayeredIconCssStyle.IN_ROW_STYLE);

            AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(ID_CHART_PANEL, iconBuilder.build(),
                    Model.of(createStringResource("RoleMining.button.title.chart").getString())) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {

                    RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                            ((PageBase) getPage()).getMainPopupBodyId(),
                            Model.of("Analyzed members details panel"),
                            cluster) {
                        @Override
                        public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                            super.onClose(ajaxRequestTarget);
                        }
                    };
                    ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                }

            };
            objectButton.titleAsLabel(true);
            objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm col-10"));
            webMarkupContainer.add(objectButton);
        }else {
            WebMarkupContainer empty = new WebMarkupContainer(ID_CHART_PANEL);
            empty.setOutputMarkupId(true);
            webMarkupContainer.add(empty);
        }

    }

}
