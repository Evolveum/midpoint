/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.AnomalyObjectDto;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.createRoleAnalysisTabPanel;

public class RoleAnalysisExplanationTabPanelPopup extends BasePanel<AnomalyObjectDto> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    String selectedRoleOid;

    public RoleAnalysisExplanationTabPanelPopup(
            @NotNull String id,
            @NotNull Model<AnomalyObjectDto> anomalyObjectDtoModel,
            @NotNull SelectableBean<RoleType> selectedRoleObject) {
        super(id, anomalyObjectDtoModel);
        selectedRoleOid = selectedRoleObject.getValue().getOid();
    }

    @Override

    protected void onInitialize() {
        super.onInitialize();

        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        addOrReplaceTabPanels(form);
        initMessagePanel();
    }

    private void initMessagePanel() {
        MessagePanel<?> warningMessage = new MessagePanel<>(
                ID_WARNING_MESSAGE,
                MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);
    }

    private void addOrReplaceTabPanels(@NotNull Form<?> form) {
        List<ITab> tabs = prepareCustomTabPanels();

        RoleAnalysisTabbedPanel<ITab> tabPanel = createRoleAnalysisTabPanel(getPageBase(), ID_TABS_PANEL, tabs);
        tabPanel.setSelectedTab(defaultTab().getIndex());
        tabPanel.add(AttributeModifier.append(CLASS_CSS, "p-0 m-0"));
        form.addOrReplace(tabPanel);
    }

    private @NotNull RoleAnalysisAttributePanel buildAttributePanel(
            String panelId, DetectedAnomalyResultType anomalyResult,
            Set<String> userValueToMark) {

        RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(
                panelId,
                loadAttributeModel(anomalyResult)) {

            @Override
            protected @NotNull String getChartContainerStyle() {
                return "min-height:350px;";
            }

            @Override
            public Set<String> getPathToMark() {
                return userValueToMark;
            }
        };

        roleAnalysisAttributePanel.setOutputMarkupId(true);
        return roleAnalysisAttributePanel;
    }

    private static @NotNull LoadableModel<RoleAnalysisAttributesDto> loadAttributeModel(DetectedAnomalyResultType anomalyResult) {
        return new LoadableModel<>(false) {
            @Override
            protected @NotNull RoleAnalysisAttributesDto load() {
                return RoleAnalysisAttributesDto.fromAnomalyStatistics(
                        "RoleAnalysisAnomalyResultTabPopup.tab.title.attribute",
                        anomalyResult.getStatistics());
            }
        };
    }

    protected @NotNull List<ITab> prepareCustomTabPanels() {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(
                getPageBase().createStringResource(
                        "RoleAnalysisDetectedAnomalyTable.createViewDetailsPeerGroupMenu.title"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                AnomalyObjectDto anomalyObjectDto = getModel().getObject();

                AnomalyObjectDto.AnomalyPartitionMap anomalyPartitionMap = anomalyObjectDto.getAnomalyPartitionMap(selectedRoleOid);
                RoleAnalysisOutlierPartitionType associatedPartition = anomalyPartitionMap.associatedPartition();

                return buildPeerGroupTable(panelId, associatedPartition, anomalyObjectDto);
            }
        });

        tabs.add(new PanelTab(
                getPageBase().createStringResource(
                        "RoleAnalysisDetectedAnomalyTable.createViewDetailsAccessAnalysisMenu.title"),
                new VisibleEnableBehaviour()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                AnomalyObjectDto anomalyObjectDto = getModel().getObject();
                AnomalyObjectDto.AnomalyPartitionMap anomalyPartitionMap = anomalyObjectDto
                        .getAnomalyPartitionMap(selectedRoleOid);
                DetectedAnomalyResultType anomalyResult = anomalyPartitionMap.anomalyResult();

                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                DetectedAnomalyStatisticsType statistics = anomalyResult.getStatistics();
                if (statistics == null || statistics.getAttributeAnalysis() == null) {
                    return new WebMarkupContainer(panelId);
                }

                AttributeAnalysisType attributeAnalysis = statistics.getAttributeAnalysis();
                RoleAnalysisAttributeAnalysisResultType userAttributeAnalysisResult = attributeAnalysis
                        .getUserAttributeAnalysisResult();
                if (userAttributeAnalysisResult == null) {
                    return new WebMarkupContainer(panelId);
                }

                Set<String> userValueToMark = roleAnalysisService.resolveUserValueToMark(userAttributeAnalysisResult);

                RoleAnalysisAttributePanel roleAnalysisAttributePanel = buildAttributePanel(panelId, anomalyResult, userValueToMark);
                roleAnalysisAttributePanel.setOutputMarkupId(true);
                return roleAnalysisAttributePanel;
            }
        });
        return tabs;
    }

    private @NotNull RoleAnalysisPartitionUserPermissionTablePopup buildPeerGroupTable(String panelId, RoleAnalysisOutlierPartitionType associatedPartition, AnomalyObjectDto anomalyObjectDto) {
        RoleAnalysisPartitionUserPermissionTablePopup components = new RoleAnalysisPartitionUserPermissionTablePopup(
                panelId,
                Model.of(associatedPartition),
                Model.ofList(associatedPartition.getDetectedAnomalyResult()),
                Model.of(anomalyObjectDto.getOutlier())) {
            @Override
            public String getUniqueRoleOid() {
                return selectedRoleOid;
            }
        };
        components.setOutputMarkupId(true);
        return components;
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 60;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    public StringResourceModel getTitle() {
        return createStringResource("RoleAnalysisExplanationTabPanelPopup.title", getOutlierName(), getAnomalyName());
    }

    private @Nullable PolyStringType getOutlierName() {
        RoleAnalysisOutlierType outlier = getModelObject().getOutlier();
        if (outlier == null) {
            return null;
        }
        return outlier.getName();
    }

    private @Nullable PolyStringType getAnomalyName() {
        DetectedAnomalyResultType anomalyResult = getModelObject().getAnomalyResult(selectedRoleOid);
        ObjectReferenceType targetObjectRef = anomalyResult.getTargetObjectRef();
        if (targetObjectRef == null) {
            return null;
        }

        return targetObjectRef.getTargetName();
    }

    public Component getContent() {
        RoleAnalysisExplanationTabPanelPopup components = this;
        components.add(AttributeModifier.append("class", "p-0"));
        return components;
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.add(new VisibleBehaviour(() -> false));
        return footer;
    }

    @Override
    public @Nullable Component getTitleComponent() {
        Component titleComponent = Popupable.super.getTitleComponent();
        if (titleComponent != null) {
            titleComponent.add(AttributeModifier.append("class", "m-0"));
        }
        return titleComponent;
    }

    public enum TabType {
        VIEW_DETAILS_PEER_GROUP(0),
        VIEW_DETAILS_ACCESS_ANALYSIS(1);

        private final int index;

        TabType(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }

    public TabType defaultTab() {
        return TabType.VIEW_DETAILS_PEER_GROUP;
    }
}
