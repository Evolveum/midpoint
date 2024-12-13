/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.RoleAnalysisTabbedPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.loadRoleAnalysisTempTable;

public class RoleAnalysisMultiplePartitionUserPermissionTableTabPopup extends BasePanel<List<RoleAnalysisOutlierPartitionType>> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABS_PANEL = "tabsPanel";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_FORM = "form";

    transient IModel<List<DetectedAnomalyResult>> anomalyModel;
    transient IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisMultiplePartitionUserPermissionTableTabPopup(
            @NotNull String id,
            @NotNull IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel,
            @Nullable IModel<List<DetectedAnomalyResult>> anomalyModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, partitionModel);
        this.anomalyModel = anomalyModel;
        this.outlierModel = outlierModel;
    }

    public List<DetectedAnomalyResult> getAnomalyModelObject() {
        if (anomalyModel == null) {
            return null;
        }
        return anomalyModel.getObject();
    }

    //TODO remove duplicated code
    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);

        addOrReplaceTabPanels(form);

        MessagePanel<?> warningMessage = new MessagePanel<>(
                ID_WARNING_MESSAGE,
                MessagePanel.MessagePanelType.WARN, getWarningMessageModel());
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> getWarningMessageModel() != null));
        add(warningMessage);
    }

    private void addOrReplaceTabPanels(@NotNull Form<?> form) {
        List<ITab> tabs = createPartitionTableResultTabs();

        RoleAnalysisTabbedPanel<ITab> tabPanel = new RoleAnalysisTabbedPanel<>(ID_TABS_PANEL, tabs, null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer newLink(String linkId, final int index) {
                return new AjaxSubmitLink(linkId) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        super.onError(target);
                        target.add(getPageBase().getFeedbackPanel());
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        super.onSubmit(target);

                        setSelectedTab(index);
                        if (target != null) {
                            target.add(findParent(TabbedPanel.class));
                        }
                        assert target != null;
                        target.add(getPageBase().getFeedbackPanel());
                    }

                };
            }
        };
        tabPanel.setOutputMarkupId(true);
        tabPanel.setOutputMarkupPlaceholderTag(true);
        tabPanel.add(AttributeModifier.append("class", "p-0 m-0"));
        form.addOrReplace(tabPanel);
    }

    protected List<ITab> createPartitionTableResultTabs() {
        List<ITab> tabs = prepareCustomTabPanels();
        if (!tabs.isEmpty() && initDefaultTabPanels()) {
            return tabs;
        }

        List<RoleAnalysisOutlierPartitionType> modelObject = getModelObject();
        modelObject.forEach(partition -> {
            ObjectReferenceType sessionRef = partition.getTargetSessionRef();
            String partitionName = "";
            if (sessionRef != null && sessionRef.getTargetName() != null) {
                partitionName += sessionRef.getTargetName().getOrig();
            }

            tabs.add(new PanelTab(
                    getPageBase().createStringResource("RoleAnalysisAnomalyResultTabPopup.tab.title.partition", partitionName),
                    new VisibleEnableBehaviour()) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    RoleAnalysisOutlierType outlier = outlierModel.getObject();
                    DisplayValueOption displayValueOption = new DisplayValueOption();
                    PageBase pageBase = getPageBase();
                    RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                    Task task = pageBase.createSimpleTask("loadDetailsPanel");
                    RoleAnalysisClusterType cluster = roleAnalysisService.prepareTemporaryCluster(
                            outlier, partition, displayValueOption, task);
                    if (cluster == null) {
                        return new WebMarkupContainer(panelId);
                    }

                    RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = loadRoleAnalysisTempTable(
                            panelId, pageBase, getAnomalyModelObject(), partition, outlier, cluster);
                    table.setOutputMarkupId(true);
                    return table;
                }
            });
        });
        return tabs;
    }

    protected @NotNull List<ITab> prepareCustomTabPanels() {
        return new ArrayList<>();
    }

    protected boolean initDefaultTabPanels() {
        return true;
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
        return createStringResource("RoleAnalysisOutlierTable.anomaly.preview");
    }

    public Component getContent() {
        RoleAnalysisMultiplePartitionUserPermissionTableTabPopup components = this;
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
}
