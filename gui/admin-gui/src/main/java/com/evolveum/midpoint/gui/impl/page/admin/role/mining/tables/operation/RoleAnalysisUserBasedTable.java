/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPatternWithAttributes;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.object.RoleAnalysisObjectUtils.executeChangesOnCandidateRole;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applyTableScaleScript;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.*;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.OutlierAnalyseActionDetailsPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.experimental.RoleAnalysisTableSettingPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItemPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkTruncatePanelAction;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanelStatus;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisUserBasedTable extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisUserBasedTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String OP_PROCESS_CANDIDATE_ROLE = DOT_CLASS + "processCandidate";
    private final OperationResult result = new OperationResult(OP_PREPARE_OBJECTS);

    private String valueTitle = null;
    private int currentPageView = 0;
    private int columnPageCount = 100;
    private int fromCol = 1;
    private int toCol = 100;
    private int specialColumnCount;
    double minFrequency;
    double maxFrequency;
    boolean isRelationSelected = false;
    boolean showAsExpandCard = false;
    private final MiningOperationChunk miningOperationChunk;
    LoadableDetachableModel<OperationPanelModel> operationPanelModel;

    public RoleAnalysisUserBasedTable(
            @NotNull String id,
            @NotNull MiningOperationChunk miningOperationChunk,
            @Nullable List<DetectedPattern> defaultDisplayedPatterns,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        super(id);

        this.operationPanelModel = new LoadableDetachableModel<>() {
            @Override
            protected OperationPanelModel load() {
                OperationPanelModel model = new OperationPanelModel();
                model.createDetectedPatternModel(getClusterPatterns());
                model.createCandidatesRolesRoleModel(getClusterCandidateRoles());
                model.addSelectedPattern(defaultDisplayedPatterns);
                model.setDisplayValueOption(displayValueOptionModel.getObject());
                return model;
            }
        };

        this.miningOperationChunk = miningOperationChunk;

        RoleAnalysisClusterType clusterObject = cluster.asObjectable();
        RoleAnalysisDetectionOptionType detectionOption = clusterObject.getDetectionOption();
        RangeType frequencyRange = detectionOption.getFrequencyRange();

        if (frequencyRange != null) {
            this.minFrequency = frequencyRange.getMin() / 100;
            this.maxFrequency = frequencyRange.getMax() / 100;
        }

        initLayout(cluster, displayValueOptionModel);
        initOperationPanel();
    }

    private void initOperationPanel() {
        RoleAnalysisTableOpPanelItemPanel itemPanel = new RoleAnalysisTableOpPanelItemPanel("panel", operationPanelModel) {

            @Override
            public void onPatternSelectionPerform(@NotNull AjaxRequestTarget ajaxRequestTarget) {
                loadDetectedPattern(ajaxRequestTarget);
            }

            @Override
            protected void initHeaderItem(@NotNull RepeatingView headerItems) {
                RoleAnalysisTableOpPanelItem refreshIcon = new RoleAnalysisTableOpPanelItem(
                        headerItems.newChildId(), getModelObject()) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Contract(pure = true)
                    @Override
                    public @NotNull String appendIconPanelCssClass() {
                        return "bg-white";
                    }

                    @Override
                    protected void performOnClick(AjaxRequestTarget target) {
                        showAsExpandCard = !showAsExpandCard;
                        toggleDetailsNavigationPanelVisibility(target);
                    }

                    @Contract(pure = true)
                    @Override
                    public @NotNull String replaceIconCssClass() {
                        if (showAsExpandCard) {
                            return "fa-2x fa fa-compress text-dark";
                        }
                        return "fa-2x fas fa-expand text-dark";
                    }

                    @Override
                    public @NotNull Component getDescriptionTitleComponent(String id) {
                        Label label = new Label(id, "Table view");
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected void addDescriptionComponents() {
                        appendText("Switch table view", null);
                    }
                };
                refreshIcon.add(AttributeAppender.replace("class", "btn btn-outline-dark border-0 d-flex"
                        + " align-self-stretch mt-1"));
                headerItems.add(refreshIcon);
            }

        };
        itemPanel.setOutputMarkupId(true);
        add(itemPanel);
    }

    private void initLayout(@NotNull PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel) {
        List<ObjectReferenceType> resolvedPattern = cluster.asObjectable().getResolvedPattern();
        DisplayValueOption object = displayValueOptionModel.getObject();
        RoleAnalysisSortMode roleAnalysisSortMode = RoleAnalysisSortMode.NONE;
        if (object != null) {
            roleAnalysisSortMode = object.getSortMode();
        }

        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(roleAnalysisSortMode);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(roleAnalysisSortMode);

        specialColumnCount = users.size();
        toCol = Math.min(toCol, specialColumnCount);

        //TODO this is just tmp test
        ListMultimap<String, SimpleHeatPattern> totalRelationOfPatternsForChunk = null;
        if (isOutlierDetection()) {
            DetectionOption detectionOption = new DetectionOption(
                    10, 100, 2, 2);
            totalRelationOfPatternsForChunk = new OutlierPatternResolver()
                    .performDetection(RoleAnalysisProcessModeType.USER, roles, detectionOption);
        }

        RoleMiningProvider<MiningRoleTypeChunk> provider = createRoleMiningProvider(roles);
        RoleAnalysisTable<MiningRoleTypeChunk> table = generateTable(provider, users, roles, resolvedPattern,
                cluster, displayValueOptionModel, totalRelationOfPatternsForChunk);
        table.add(AttributeAppender.append("class", "border border-light"));
        add(table);
    }

    private @NotNull RoleMiningProvider<MiningRoleTypeChunk> createRoleMiningProvider(List<MiningRoleTypeChunk> roles) {

        ListModel<MiningRoleTypeChunk> model = new ListModel<>(roles) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<MiningRoleTypeChunk> object) {
                super.setObject(object);
            }
        };

        return new RoleMiningProvider<>(this, model, false);
    }

    public RoleAnalysisTable<MiningRoleTypeChunk> generateTable(
            RoleMiningProvider<MiningRoleTypeChunk> provider,
            List<MiningUserTypeChunk> users,
            List<MiningRoleTypeChunk> roles,
            List<ObjectReferenceType> reductionObjects,
            PrismObject<RoleAnalysisClusterType> cluster,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
            ListMultimap<String, SimpleHeatPattern> totalRelationOfPatternsForChunk) {

        RoleAnalysisTable<MiningRoleTypeChunk> table = new RoleAnalysisTable<>(
                ID_DATATABLE, provider, initColumns(users, roles, reductionObjects, displayValueOptionModel,
                totalRelationOfPatternsForChunk, cluster),
                null, true, specialColumnCount, displayValueOptionModel) {

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0";
            }

            @Override
            protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleContainer() {
                return getCandidateRoleToPerform(cluster.asObjectable());
            }

            @Override
            protected boolean getMigrationButtonVisibility() {
                Set<RoleAnalysisCandidateRoleType> candidateRole = getCandidateRole();
                if (candidateRole != null) {
                    if (candidateRole.size() > 1) {
                        return false;
                    }
                }
                if (getSelectedPatterns().size() > 1) {
                    return false;
                }

                return isRelationSelected;
            }

            @Override
            protected void onSubmitEditButton(AjaxRequestTarget target) {
                onSubmitCandidateRolePerform(target, cluster);
            }

            @Override
            protected @NotNull WebMarkupContainer createButtonToolbar(String id) {

                RepeatingView repeatingView = new RepeatingView(id);
                repeatingView.setOutputMarkupId(true);

                CompositedIconBuilder refreshIconBuilder = new CompositedIconBuilder().setBasicIcon(
                        GuiStyleConstants.CLASS_REFRESH, LayeredIconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton refreshIcon = buildRefreshTableButton(repeatingView, refreshIconBuilder,
                        cluster);
                refreshIcon.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                repeatingView.add(refreshIcon);

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                        "fa fa-cog", LayeredIconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton tableSetting = buildTableSettingButton(repeatingView, iconBuilder,
                        displayValueOptionModel);
                tableSetting.add(AttributeAppender.replace("class", "btn btn-default btn-sm"));
                repeatingView.add(tableSetting);

                return repeatingView;
            }

            @Override
            public void onChange(String value, AjaxRequestTarget target, int currentPage) {
                currentPageView = currentPage;
                String[] rangeParts = value.split(" - ");
                valueTitle = value;
                fromCol = Integer.parseInt(rangeParts[0]);
                toCol = Integer.parseInt(rangeParts[1]);
                getTable().replaceWith(generateTable(provider, users, roles, reductionObjects, cluster,
                        displayValueOptionModel, totalRelationOfPatternsForChunk));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            public void onChangeSize(int value, AjaxRequestTarget target) {
                currentPageView = 0;
                columnPageCount = value;
                fromCol = 1;
                toCol = Math.min(value, specialColumnCount);
                valueTitle = "0 - " + toCol;

                getTable().replaceWith(generateTable(provider, users, roles, reductionObjects, cluster,
                        displayValueOptionModel, totalRelationOfPatternsForChunk));
                target.add(getTable().setOutputMarkupId(true));
                target.appendJavaScript(applyTableScaleScript());
            }

            @Override
            protected int getCurrentPage() {
                return currentPageView;
            }

            @Override
            public int getColumnPageCount() {
                return columnPageCount;
            }

            @Override
            public String getColumnPagingTitle() {
                if (valueTitle == null) {
                    return super.getColumnPagingTitle();
                } else {
                    return valueTitle;
                }
            }
        };
        table.setItemsPerPage(50);
        table.setOutputMarkupId(true);

        return table;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildTableSettingButton(@NotNull RepeatingView repeatingView,
            @NotNull CompositedIconBuilder iconBuilder,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel) {
        AjaxCompositedIconSubmitButton tableSetting = new AjaxCompositedIconSubmitButton(
                repeatingView.newChildId(), iconBuilder.build(), createStringResource("Table settings")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisTableSettingPanel selector = new RoleAnalysisTableSettingPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        createStringResource("RoleAnalysisPathTableSelector.title"), displayValueOptionModel) {

                    @Override
                    public void performAfterFinish(AjaxRequestTarget target) {
                        resetTable(target, displayValueOptionModel.getObject());
                    }
                };
                ((PageBase) getPage()).showMainPopup(selector, target);
            }

        };

        tableSetting.titleAsLabel(true);
        return tableSetting;
    }

    @NotNull
    private AjaxCompositedIconSubmitButton buildRefreshTableButton(
            @NotNull RepeatingView repeatingView,
            @NotNull CompositedIconBuilder refreshIconBuilder,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        AjaxCompositedIconSubmitButton refreshIcon = new AjaxCompositedIconSubmitButton(
                repeatingView.newChildId(), refreshIconBuilder.build(), createStringResource("Refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onRefresh(cluster);
            }

        };

        refreshIcon.titleAsLabel(true);
        return refreshIcon;
    }

    public List<IColumn<MiningRoleTypeChunk, String>> initColumns(
            List<MiningUserTypeChunk> users,
            List<MiningRoleTypeChunk> roles,
            List<ObjectReferenceType> reductionObjects,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
            ListMultimap<String, SimpleHeatPattern> totalRelationOfPatternsForChunk,
            PrismObject<RoleAnalysisClusterType> cluster) {

        List<IColumn<MiningRoleTypeChunk, String>> columns = new ArrayList<>();

        columns.add(new CompositedIconColumn<>(null) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " role-mining-static-header ";
            }

            @Override
            protected CompositedIcon getCompositedIcon(IModel<MiningRoleTypeChunk> rowModel) {

                MiningRoleTypeChunk object = rowModel.getObject();
                List<String> roles = object.getRoles();

                String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE);
                CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                        LayeredIconCssStyle.IN_ROW_STYLE);

                String iconColor = object.getIconColor();
                if (iconColor != null) {
                    compositedIconBuilder.appendColorHtmlValue(iconColor);
                }

                for (ObjectReferenceType ref : reductionObjects) {
                    if (roles.contains(ref.getOid())) {
                        compositedIconBuilder.setBasicIcon(defaultBlackIcon + " " + GuiStyleConstants.GREEN_COLOR,
                                LayeredIconCssStyle.IN_ROW_STYLE);
                        IconType icon = new IconType();
                        icon.setCssClass(GuiStyleConstants.CLASS_OP_RESULT_STATUS_ICON_SUCCESS_COLORED
                                + " " + GuiStyleConstants.GREEN_COLOR);
                        compositedIconBuilder.appendLayerIcon(icon, IconCssStyle.BOTTOM_RIGHT_FOR_COLUMN_STYLE);
                        break;
                    }
                }

                return compositedIconBuilder.build();
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem, String componentId, IModel<MiningRoleTypeChunk> rowModel) {
                MiningRoleTypeChunk object = rowModel.getObject();
                int propertiesCount = object.getRoles().size();

                RoleAnalysisTableTools.StyleResolution styleResolution = RoleAnalysisTableTools
                        .StyleResolution
                        .resolveSize(propertiesCount);

                String sizeInPixels = styleResolution.getSizeInPixels();
                cellItem.add(AttributeAppender.append("style", " width:40px; height:" + sizeInPixels + ";"));
                super.populateItem(cellItem, componentId, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> item, String componentId,
                    IModel<MiningRoleTypeChunk> rowModel) {
                item.add(AttributeAppender.replace("class", " "));
                int propertiesCount = rowModel.getObject().getRoles().size();
                RoleAnalysisTableTools.StyleResolution styleResolution = RoleAnalysisTableTools
                        .StyleResolution
                        .resolveSize(propertiesCount);

                String sizeInPixels = styleResolution.getSizeInPixels();
                item.add(AttributeAppender.append("style", " width:150px; height:" + sizeInPixels + ";"));

                List<String> elements = rowModel.getObject().getRoles();

                updateFrequencyBased(rowModel, minFrequency, maxFrequency, isOutlierDetection());

                AjaxLinkPanel analyzedMembersDetailsPanel = buildDetailPanel(componentId, rowModel, elements);
                analyzedMembersDetailsPanel.add(
                        AttributeAppender.replace("class", "d-inline-block text-truncate"));
                analyzedMembersDetailsPanel.add(AttributeAppender.replace("style", "width:145px"));
                analyzedMembersDetailsPanel.setOutputMarkupId(true);
                item.add(analyzedMembersDetailsPanel);
            }

            @NotNull
            private AjaxLinkPanel buildDetailPanel(
                    @NotNull String componentId,
                    @NotNull IModel<MiningRoleTypeChunk> rowModel,
                    List<String> elements) {

                String title = rowModel.getObject().getChunkName();
                if (isOutlierDetection()) {
                    double confidence = rowModel.getObject().getFrequencyItem().getzScore();
                    title = title + " (" + confidence + "confidence) ";
                    List<SimpleHeatPattern> simpleHeatPatterns = totalRelationOfPatternsForChunk.get(rowModel.getObject().getMembers().get(0));
                    if (!simpleHeatPatterns.isEmpty()) {
                        int totalRelations = 0;
                        for (SimpleHeatPattern simpleHeatPattern : simpleHeatPatterns) {
                            totalRelations += simpleHeatPattern.getTotalRelations();
                        }

                        title = title + " (" + totalRelations + "relations) " + " (" + simpleHeatPatterns.size() + " patterns)";
                    }

                }
                AjaxLinkPanel component = buildMemberDetailsLink(componentId, elements, title);
                component.add(new TooltipBehavior());
                component.add(AttributeAppender.append("title", title));
                return component;
            }

            @NotNull
            private AjaxLinkPanel buildMemberDetailsLink(@NotNull String componentId, List<String> elements, String title) {
                AjaxLinkPanel component = new AjaxLinkPanel(componentId,
                        createStringResource(title)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

                        List<PrismObject<FocusType>> objects = new ArrayList<>();
                        for (String objectOid : elements) {
                            objects.add(getPageBase().getRoleAnalysisService()
                                    .getFocusTypeObject(objectOid, task, result));
                        }

                        MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("Analyzed members details panel"), objects, RoleAnalysisProcessModeType.ROLE) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }

                };
                component.setOutputMarkupId(true);
                return component;
            }

            @Override
            public Component getHeader(String componentId) {

                CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                        LayeredIconCssStyle.IN_ROW_STYLE);
                AjaxCompositedIconSubmitButton compressButton = new AjaxCompositedIconSubmitButton(componentId,
                        iconBuilder.build(),
                        new LoadableDetachableModel<>() {
                            @Override
                            protected String load() {
                                if (RoleAnalysisChunkMode.valueOf(getCompressStatus(displayValueOptionModel))
                                        .equals(RoleAnalysisChunkMode.COMPRESS)) {
                                    return getString("RoleMining.operation.panel.expand.button.title");
                                } else {
                                    return getString("RoleMining.operation.panel.compress.button.title");
                                }
                            }
                        }) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public CompositedIcon getIcon() {

                        CompositedIconBuilder iconBuilder;
                        if (RoleAnalysisChunkMode.valueOf(getCompressStatus(displayValueOptionModel)).equals(RoleAnalysisChunkMode.COMPRESS)) {
                            iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                                    LayeredIconCssStyle.IN_ROW_STYLE);
                        } else {
                            iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-compress",
                                    LayeredIconCssStyle.IN_ROW_STYLE);
                        }

                        return iconBuilder.build();
                    }

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        onPerform(target);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target) {
                        target.add(((PageBase) getPage()).getFeedbackPanel());
                    }
                };
                compressButton.titleAsLabel(true);
                compressButton.setOutputMarkupId(true);
                compressButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                compressButton.add(AttributeAppender.append("style",
                        "  writing-mode: vertical-lr;  -webkit-transform: rotate(90deg);"));

                return compressButton;
            }

            @Override
            public String getCssClass() {
                return "overflow-auto role-mining-static-row-header role-mining-static-header-name align-self-center";
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> item, String componentId,
                    IModel<MiningRoleTypeChunk> rowModel) {

                int propertiesCount = rowModel.getObject().getRoles().size();
                RoleAnalysisTableTools.StyleResolution styleResolution = RoleAnalysisTableTools
                        .StyleResolution
                        .resolveSize(propertiesCount);

                String sizeInPixels = styleResolution.getSizeInPixels();
                item.add(AttributeAppender.append("style", " overflow-wrap: break-word !important; "
                        + "word-break: inherit;"
                        + " width:40px; height:" + sizeInPixels + ";"));

                LinkIconPanelStatus linkIconPanel = new LinkIconPanelStatus(componentId, new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisOperationMode load() {
                        if (getSelectedPatterns().size() > 1) {
                            return RoleAnalysisOperationMode.DISABLE;
                        }

                        return rowModel.getObject().getStatus();
                    }
                }) {
                    @Override
                    protected RoleAnalysisOperationMode onClickPerformed(AjaxRequestTarget target,
                            RoleAnalysisOperationMode status) {
                        isRelationSelected = false;
                        MiningRoleTypeChunk object = rowModel.getObject();
                        RoleAnalysisObjectStatus objectStatus = new RoleAnalysisObjectStatus(status.toggleStatus());
                        objectStatus.setContainerId(new HashSet<>(getPatternIdentifiers()));
                        object.setObjectStatus(objectStatus);
                        resetCellsAndActionButton(target);
                        return rowModel.getObject().getStatus();
                    }
                };

                linkIconPanel.setOutputMarkupId(true);
                item.add(linkIconPanel);

            }

            @Override
            public String getCssClass() {
                return " role-mining-static-header";
            }
        });

        IColumn<MiningRoleTypeChunk, String> column;
        for (int i = fromCol - 1; i < toCol; i++) {
            MiningUserTypeChunk colChunk = users.get(i);
            int membersSize = colChunk.getUsers().size();
            RoleAnalysisTableTools.StyleResolution styleWidth = RoleAnalysisTableTools.StyleResolution.resolveSize(membersSize);
            column = new AbstractColumn<>(createStringResource("")) {

                @Override
                public void populateItem(Item<ICellPopulator<MiningRoleTypeChunk>> cellItem,
                        String componentId, IModel<MiningRoleTypeChunk> model) {
                    MiningRoleTypeChunk rowChunk = model.getObject();
                    List<String> cellRoles = rowChunk.getRoles();
                    int propertiesCount = cellRoles.size();
                    RoleAnalysisTableTools.StyleResolution styleHeight = RoleAnalysisTableTools
                            .StyleResolution
                            .resolveSize(propertiesCount);

                    applySquareTableCell(cellItem, styleWidth, styleHeight);

                    Status isInclude = resolveCellTypeUserTable(componentId, cellItem, rowChunk, colChunk,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected Map<String, String> load() {
                                    return getPatternColorPalette();
                                }
                            });

                    if (isInclude.equals(Status.RELATION_INCLUDE)) {
                        isRelationSelected = true;
                    }

                    if (!isInclude.equals(Status.RELATION_NONE)) {
                        Set<String> markMemberObjects = getMarkMemberObjects();
                        Set<String> markPropertyObjects = getMarkPropertyObjects();
                        if (markMemberObjects != null && markMemberObjects.containsAll(colChunk.getMembers())) {
                            cellItem.add(AttributeAppender.append("style", "border: 5px solid #206f9d;"));
                        } else if (markPropertyObjects != null && markPropertyObjects.containsAll(colChunk.getProperties())) {
                            cellItem.add(AttributeAppender.append("style", "border: 5px solid #206f9d;"));
                        }
                    }

                    RoleAnalysisChunkAction chunkAction = displayValueOptionModel.getObject().getChunkAction();
                    if (!chunkAction.equals(RoleAnalysisChunkAction.SELECTION)) {
                        patternCellResolver(cellItem, rowChunk, colChunk, roles, cluster, chunkAction);
                    } else {
                        chunkActionSelectorBehavior(cellItem, rowChunk, colChunk);
                    }

                }

                @Override
                public String getCssClass() {
                    String cssLevel = RoleAnalysisTableTools.StyleResolution.resolveSizeLevel(styleWidth);
                    return cssLevel + " p-2";
                }

                @Override
                public Component getHeader(String componentId) {
                    List<String> elements = colChunk.getUsers();

                    String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE);
                    CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                            LayeredIconCssStyle.IN_ROW_STYLE);

                    String iconColor = colChunk.getIconColor();
                    if (iconColor != null) {
                        compositedIconBuilder.appendColorHtmlValue(iconColor);
                    }

                    CompositedIcon compositedIcon = compositedIconBuilder.build();

                    String title = colChunk.getChunkName();
                    return new AjaxLinkTruncatePanelAction(componentId,
                            createStringResource(title), createStringResource(title), compositedIcon,
                            new LoadableDetachableModel<>() {
                                @Override
                                protected RoleAnalysisOperationMode load() {
                                    if (getSelectedPatterns().size() > 1) {
                                        return RoleAnalysisOperationMode.DISABLE;
                                    }
                                    return colChunk.getStatus();
                                }
                            }) {

                        @Override
                        protected RoleAnalysisOperationMode onClickPerformedAction(AjaxRequestTarget target,
                                RoleAnalysisOperationMode status) {
                            isRelationSelected = false;
                            RoleAnalysisObjectStatus objectStatus = new RoleAnalysisObjectStatus(status.toggleStatus());
                            objectStatus.setContainerId(new HashSet<>(getPatternIdentifiers()));
                            colChunk.setObjectStatus(objectStatus);

                            resetCellsAndActionButton(target);
                            return colChunk.getStatus();
                        }

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);

                            List<PrismObject<FocusType>> objects = new ArrayList<>();
                            for (String objectOid : elements) {
                                objects.add(getPageBase().getRoleAnalysisService()
                                        .getFocusTypeObject(objectOid, task, result));
                            }
                            if (isOutlierDetection() && cluster.getOid() != null && !elements.isEmpty()) {

                                //TODO session option min members

                                OutlierAnalyseActionDetailsPopupPanel detailsPanel = new OutlierAnalyseActionDetailsPopupPanel(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of("Analyzed members details panel"), elements.get(0), cluster.getOid(), 10) {
                                    @Override
                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                        super.onClose(ajaxRequestTarget);
                                    }
                                };
                                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                            } else {
                                MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of("Analyzed members details panel"),
                                        objects, RoleAnalysisProcessModeType.USER) {
                                    @Override
                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                        super.onClose(ajaxRequestTarget);
                                    }
                                };
                                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                            }
                        }

                    };
                }

            };
            columns.add(column);
        }

        return columns;
    }

    private void patternCellResolver(@NotNull Item<ICellPopulator<MiningRoleTypeChunk>> cellItem,
            MiningRoleTypeChunk roleChunk,
            MiningUserTypeChunk userChunk,
            List<MiningRoleTypeChunk> roles, PrismObject<RoleAnalysisClusterType> cluster, RoleAnalysisChunkAction chunkAction) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                if (userChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE
                        && roleChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE) {
                    getSelectedPatterns().clear();
                    isRelationSelected = false;
                    refreshTableCells(ajaxRequestTarget);
                    return;
                }

                List<String> members = userChunk.getMembers();
                List<SimpleHeatPattern> totalRelationOfPatternsForCell;
                List<String> mustMeet = roleChunk.getProperties();
                List<String> topPattern = new ArrayList<>();

                int patternCount = 0;
                int totalRelations = 0;
                int topPatternRelation = 0;
                double topPatternCoverage = 0;

                if (new HashSet<>(mustMeet).containsAll(members)) {
                    DetectionOption detectionOption = new DetectionOption(
                            10, 100, 2, 2);
                    totalRelationOfPatternsForCell = new OutlierPatternResolver()
                            .performSingleCellDetection(RoleAnalysisProcessModeType.USER, roles, detectionOption, members, mustMeet);

                    patternCount = totalRelationOfPatternsForCell.size();
                    for (SimpleHeatPattern simpleHeatPattern : totalRelationOfPatternsForCell) {
                        int relations = simpleHeatPattern.getTotalRelations();
                        totalRelations += relations;
                        if (relations > topPatternRelation) {
                            topPatternRelation = relations;
                            topPattern = simpleHeatPattern.getPropertiesOids();
                        }
                    }

                    int clusterRelations = 0;
                    for (MiningRoleTypeChunk roleTypeChunk : roles) {
                        int propertiesCount = roleTypeChunk.getProperties().size();
                        int membersCount = roleTypeChunk.getMembers().size();
                        clusterRelations += (propertiesCount * membersCount);
                    }
                    topPatternCoverage = ((double) topPatternRelation / clusterRelations) * 100;

                }

                List<String> finalTopPattern = topPattern;
                double finalTopPatternCoverage = topPatternCoverage;
                int finalTotalRelations = totalRelations;
                int finalTopPatternRelation = topPatternRelation;
                int finalPatternCount = patternCount;

                List<String> roleProperty = new ArrayList<>();
                for (MiningRoleTypeChunk role : roles) {
                    List<String> userProperty = role.getProperties();
                    if (new HashSet<>(userProperty).containsAll(finalTopPattern)) {
                        roleProperty.addAll(role.getMembers());
                    }
                }

                double metric = (finalTopPattern.size() * roleProperty.size()) - finalTopPattern.size();

                RoleAnalysisDetectionPatternType pattern = new RoleAnalysisDetectionPatternType();

                Set<String> users = new HashSet<>(finalTopPattern);
                Set<String> roles = new HashSet<>(roleProperty);

                for (String usersRef : users) {
                    pattern.getUserOccupancy().add(
                            new ObjectReferenceType().oid(usersRef).type(UserType.COMPLEX_TYPE));

                }

                for (String rolesRef : roles) {
                    pattern.getRolesOccupancy().add(
                            new ObjectReferenceType().oid(rolesRef).type(RoleType.COMPLEX_TYPE)
                    );
                }

                pattern.setClusterMetric(metric);

                Map<String, PrismObject<UserType>> userExistCache = new HashMap<>();
                Map<String, PrismObject<RoleType>> roleExistCache = new HashMap<>();
                PageBase pageBase = RoleAnalysisUserBasedTable.this.getPageBase();
                Task task = pageBase.createSimpleTask("InitPattern");
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                ObjectReferenceType roleAnalysisSessionRef = cluster.asObjectable().getRoleAnalysisSessionRef();
                if (roleAnalysisSessionRef != null) {
                    PrismObject<RoleAnalysisSessionType> session = roleAnalysisService
                            .getObject(RoleAnalysisSessionType.class, roleAnalysisSessionRef.getOid(), task, task.getResult());
                    if (session == null) {
                        return;
                    }
                    List<RoleAnalysisAttributeDef> userAnalysisAttributeDef = roleAnalysisService
                            .resolveAnalysisAttributes(session.asObjectable(), UserType.COMPLEX_TYPE);
                    List<RoleAnalysisAttributeDef> roleAnalysisAttributeDef = roleAnalysisService
                            .resolveAnalysisAttributes(session.asObjectable(), RoleType.COMPLEX_TYPE);

                    roleAnalysisService.resolveDetectedPatternsAttributes(Collections.singletonList(pattern), userExistCache,
                            roleExistCache, task, result, roleAnalysisAttributeDef, userAnalysisAttributeDef);

                    double totalDensity = 0.0;
                    int totalCount = 0;
                    RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
                    RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();

                    if (roleAttributeAnalysisResult != null) {
                        totalDensity += calculateDensity(roleAttributeAnalysisResult.getAttributeAnalysis());
                        totalCount += roleAttributeAnalysisResult.getAttributeAnalysis().size();
                    }
                    if (userAttributeAnalysisResult != null) {
                        totalDensity += calculateDensity(userAttributeAnalysisResult.getAttributeAnalysis());
                        totalCount += userAttributeAnalysisResult.getAttributeAnalysis().size();
                    }

                    int itemCount = (roleAttributeAnalysisResult != null
                            ? roleAttributeAnalysisResult.getAttributeAnalysis().size() : 0)
                            + (userAttributeAnalysisResult != null ? userAttributeAnalysisResult.getAttributeAnalysis().size() : 0);

                    double itemsConfidence = (totalCount > 0 && totalDensity > 0.0 && itemCount > 0) ? totalDensity / itemCount : 0.0;
                    pattern.setItemConfidence(itemsConfidence);
                }

                DetectedPattern detectedPattern = transformPatternWithAttributes(pattern);

                if (chunkAction.equals(RoleAnalysisChunkAction.DETAILS_DETECTION)) {
                    DebugLabel debugLabel = new DebugLabel(((PageBase) getPage()).getMainPopupBodyId()) {
                        @Override
                        public DetectedPattern getPattern() {
                            return detectedPattern;
                        }

                        @Override
                        protected void explorePatternPerform(@NotNull DetectedPattern pattern, AjaxRequestTarget target) {
                            getSelectedPatterns().clear();
                            if (pattern.getRoles() != null && !pattern.getRoles().isEmpty()
                                    && pattern.getUsers() != null && !pattern.getUsers().isEmpty()) {
                                getSelectedPatterns().add(pattern);
                                loadDetectedPattern(target);
                            } else {
                                refreshTableCells(target);
                            }

                            getPageBase().hideMainPopup(target);
                        }

                        @Override
                        protected int getDetectedPatternCount() {
                            return finalPatternCount;
                        }

                        @Override
                        protected int getTopPatternRelations() {
                            return finalTopPatternRelation;
                        }

                        @Override
                        protected int getTotalRelations() {
                            return finalTotalRelations;
                        }

                        @Override
                        protected double getMaxCoverage() {
                            return finalTopPatternCoverage;
                        }
                    };
                    debugLabel.setOutputMarkupId(true);
                    ((PageBase) getPage()).showMainPopup(debugLabel, ajaxRequestTarget);
                } else {
                    getSelectedPatterns().clear();

                    if (detectedPattern.getRoles() != null && !detectedPattern.getRoles().isEmpty()
                            && detectedPattern.getUsers() != null && !detectedPattern.getUsers().isEmpty()) {
                        getSelectedPatterns().add(detectedPattern);
                        loadDetectedPattern(ajaxRequestTarget);
                    } else {
                        refreshTableCells(ajaxRequestTarget);
                    }

                }

            }
        });
    }

    private double calculateDensity(@NotNull List<RoleAnalysisAttributeAnalysis> attributeAnalysisList) {
        double totalDensity = 0.0;
        for (RoleAnalysisAttributeAnalysis attributeAnalysis : attributeAnalysisList) {
            Double density = attributeAnalysis.getDensity();
            if (density != null) {
                totalDensity += density;
            }
        }
        return totalDensity;
    }

    private void chunkActionSelectorBehavior(
            @NotNull Item<ICellPopulator<MiningRoleTypeChunk>> cellItem,
            MiningRoleTypeChunk rowChunk,
            MiningUserTypeChunk colChunk) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                isRelationSelected = false;
                RoleAnalysisOperationMode chunkStatus;
                RoleAnalysisOperationMode rowStatus = rowChunk.getStatus();
                RoleAnalysisOperationMode colStatus = colChunk.getStatus();

                if (rowStatus.isDisable() || colStatus.isDisable()) {
                    return;
                }

                if (getSelectedPatterns().size() > 1) {
                    return;
                }

                if (rowStatus.isInclude() && colStatus.isInclude()) {
                    chunkStatus = RoleAnalysisOperationMode.EXCLUDE;
                    rowChunk.setStatus(chunkStatus);
                    colChunk.setStatus(chunkStatus);
                } else if ((rowStatus.isExclude() && colStatus.isInclude())
                        || (rowStatus.isInclude() && colStatus.isExclude())
                        || (rowStatus.isExclude() && colStatus.isExclude())) {
                    chunkStatus = RoleAnalysisOperationMode.INCLUDE;
                    isRelationSelected = true;
                    rowChunk.setStatus(chunkStatus);
                    colChunk.setStatus(chunkStatus);
                }

                resetCellsAndActionButton(ajaxRequestTarget);
            }
        });
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public DataTable<?, ?> getDataTable() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getDataTable();
    }

    public Component getHeaderFooter() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getHeaderFooter();
    }

    public void resetCellsAndActionButton(@NotNull AjaxRequestTarget target) {
        target.add(getDataTable());
        target.add(getHeaderFooter());
    }

    public Component getHeader() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE))).getHeader();
    }

    protected RoleAnalysisTable<?> getTable() {
        return ((RoleAnalysisTable<?>) get(((PageBase) getPage()).createComponentPath(ID_DATATABLE)));
    }

    protected void resetTable(AjaxRequestTarget target, @Nullable DisplayValueOption displayValueOption) {

    }

    protected String getCompressStatus(@NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel) {
        return displayValueOptionModel.getObject().getChunkMode().getValue();
    }

    protected void onPerform(AjaxRequestTarget ajaxRequestTarget) {
    }

    public void refreshTableCells(AjaxRequestTarget ajaxRequestTarget) {
        isRelationSelected = false;
        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        refreshCells(RoleAnalysisProcessModeType.USER, users, roles, minFrequency, maxFrequency);
        resetCellsAndActionButton(ajaxRequestTarget);
    }

    public void loadDetectedPattern(AjaxRequestTarget target) {
        isRelationSelected = false;
        List<MiningUserTypeChunk> users = miningOperationChunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningRoleTypeChunk> roles = miningOperationChunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        refreshCells(RoleAnalysisProcessModeType.USER, users, roles, minFrequency, maxFrequency);

        if (isPatternDetected()) {
            Task task = getPageBase().createSimpleTask("InitPattern");
            OperationResult result = task.getResult();
            initUserBasedDetectionPattern(getPageBase(), users,
                    roles,
                    getSelectedPatterns(),
                    minFrequency,
                    maxFrequency,
                    task,
                    result);
        }

        resetCellsAndActionButton(target);
    }

    private void onRefresh(@NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, cluster.getOid());
        parameters.add("panelId", "clusterDetails");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private void navigateToClusterCandidateRolePanel(@NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        PageParameters parameters = new PageParameters();
        String clusterOid = cluster.getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "candidateRoles");
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    private boolean isPatternDetected() {
        return !getSelectedPatterns().isEmpty();
    }

    public RoleAnalysisSortMode getRoleAnalysisSortMode(
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel) {
        if (displayValueOptionModel.getObject() != null) {
            return displayValueOptionModel.getObject().getSortMode();
        }

        return RoleAnalysisSortMode.NONE;
    }

    protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
        return null;
    }

    private void onSubmitCandidateRolePerform(@NotNull AjaxRequestTarget target,
            @NotNull PrismObject<RoleAnalysisClusterType> cluster) {
        if (miningOperationChunk == null) {
            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        Task task = getPageBase().createSimpleTask(OP_PROCESS_CANDIDATE_ROLE);
        OperationResult result = task.getResult();

        Set<RoleType> candidateInducements = new HashSet<>();

        List<MiningRoleTypeChunk> simpleMiningRoleTypeChunks = miningOperationChunk.getSimpleMiningRoleTypeChunks();
        for (MiningRoleTypeChunk roleChunk : simpleMiningRoleTypeChunks) {
            if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                for (String roleOid : roleChunk.getRoles()) {
                    PrismObject<RoleType> roleObject = getPageBase().getRoleAnalysisService()
                            .getRoleTypeObject(roleOid, task, result);
                    if (roleObject != null) {
                        candidateInducements.add(roleObject.asObjectable());
                    }
                }
            }
        }
        List<MiningUserTypeChunk> simpleMiningUserTypeChunks = miningOperationChunk.getSimpleMiningUserTypeChunks();
        Set<PrismObject<UserType>> candidateMembers = new HashSet<>();
        for (MiningUserTypeChunk userChunk : simpleMiningUserTypeChunks) {
            if (userChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                for (String userOid : userChunk.getUsers()) {
                    PrismObject<UserType> userObject = getPageBase().getRoleAnalysisService()
                            .getUserTypeObject(userOid, task, result);
                    if (userObject != null) {
                        candidateMembers.add(userObject);
                    }
                }
            }
        }

        Set<RoleAnalysisCandidateRoleType> candidateRoleToPerform = getCandidateRoleToPerform(cluster.asObjectable());
        if (candidateRoleToPerform != null) {
            @Nullable List<RoleAnalysisCandidateRoleType> candidateRole = new ArrayList<>(candidateRoleToPerform);
            if (candidateRole.size() == 1) {
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                Set<AssignmentType> assignmentTypeSet = new HashSet<>();
                for (RoleType candidateInducement : candidateInducements) {
                    assignmentTypeSet.add(ObjectTypeUtil.createAssignmentTo(candidateInducement.getOid(), ObjectTypes.ROLE));
                }

                executeChangesOnCandidateRole(roleAnalysisService, pageBase, target,
                        cluster,
                        candidateRole,
                        candidateMembers,
                        assignmentTypeSet,
                        task,
                        result
                );

                result.computeStatus();
                getPageBase().showResult(result);
                navigateToClusterCandidateRolePanel(cluster);
                return;
            }
        }

        PrismObject<RoleType> businessRole = getPageBase().getRoleAnalysisService()
                .generateBusinessRole(new HashSet<>(), PolyStringType.fromOrig(""));

        List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

        for (PrismObject<UserType> member : candidateMembers) {
            BusinessRoleDto businessRoleDto = new BusinessRoleDto(member,
                    businessRole, candidateInducements, getPageBase());
            roleApplicationDtos.add(businessRoleDto);
        }

        BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                cluster, businessRole, roleApplicationDtos, candidateInducements);

        if (!getSelectedPatterns().isEmpty() && getSelectedPatterns().get(0).getId() != null) {
            operationData.setPatternId(getSelectedPatterns().get(0).getId());
        }

        List<BusinessRoleDto> businessRoleDtos = operationData.getBusinessRoleDtos();
        Set<RoleType> inducement = operationData.getCandidateRoles();
        if (!inducement.isEmpty() && !businessRoleDtos.isEmpty()) {
            PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
            setResponsePage(pageRole);
        } else {
            warn(createStringResource("RoleAnalysis.candidate.not.selected").getString());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    public List<String> getPatternIdentifiers() {
        List<String> patternIds = new ArrayList<>();
        if (!getSelectedPatterns().isEmpty()) {
            for (DetectedPattern pattern : getSelectedPatterns()) {
                String identifier = pattern.getIdentifier();
                patternIds.add(identifier);
            }
        }
        return patternIds;
    }

    public boolean isOutlierDetection() {
        return false;
    }

    public List<DetectedPattern> getClusterPatterns() {
        return new ArrayList<>();
    }

    public List<DetectedPattern> getClusterCandidateRoles() {
        return new ArrayList<>();
    }

    @SuppressWarnings("rawtypes")
    protected void toggleDetailsNavigationPanelVisibility(AjaxRequestTarget target) {
        Page page = getPage();
        if (page instanceof AbstractPageObjectDetails) {
            AbstractPageObjectDetails<?, ?> pageObjectDetails = ((AbstractPageObjectDetails) page);
            pageObjectDetails.toggleDetailsNavigationPanelVisibility(target);
        }
    }

    public List<DetectedPattern> getSelectedPatterns() {
        return operationPanelModel.getObject().getSelectedPatterns();
    }

    public Map<String, String> getPatternColorPalette() {
        return operationPanelModel.getObject().getPalletColors();
    }

    private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleToPerform(RoleAnalysisClusterType cluster) {
        if (getSelectedPatterns().size() > 1) {
            return null;
        } else if (getSelectedPatterns().size() == 1) {
            DetectedPattern detectedPattern = getSelectedPatterns().get(0);
            Long id = detectedPattern.getId();
            List<RoleAnalysisCandidateRoleType> candidateRoles = cluster.getCandidateRoles();
            for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
                if (candidateRole.getId().equals(id)) {
                    return Collections.singleton(candidateRole);
                }
            }
        }

        return getCandidateRole();
    }

    protected Set<String> getMarkMemberObjects() {
        return null;
    }

    protected Set<String> getMarkPropertyObjects() {
        return null;
    }

}
