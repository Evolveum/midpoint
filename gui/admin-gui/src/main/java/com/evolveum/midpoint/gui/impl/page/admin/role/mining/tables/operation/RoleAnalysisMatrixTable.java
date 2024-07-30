/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformDefaultPattern;
import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPattern;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getObjectNameDef;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidAssignment;
import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisUtils.getRolesOidInducements;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.object.RoleAnalysisObjectUtils.executeChangesOnCandidateRole;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

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
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.chunk.*;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
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
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisTableOpPanelItemPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver;
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

public class RoleAnalysisMatrixTable<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends BasePanel<PrismObject<RoleAnalysisClusterType>> {

    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisMatrixTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";
    private static final String OP_PROCESS_CANDIDATE_ROLE = DOT_CLASS + "processCandidate";

    public static final String PARAM_CANDIDATE_ROLE_ID = "candidateRoleId";
    public static final String PARAM_DETECTED_PATER_ID = "detectedPatternId";
    public static final String PARAM_TABLE_SETTING = "tableSetting";


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

    private final LoadableModel<MiningOperationChunk> miningOperationChunk;
    private final LoadableDetachableModel<OperationPanelModel> operationPanelModel;

    private final LoadableDetachableModel<DisplayValueOption> displayValueOptionModel;

    private IModel<List<DetectedPattern>> patternsToAnalyzeModel = null;

        //TODO new:
    private final boolean isRoleMode;

    public RoleAnalysisMatrixTable(
            @NotNull String id,
            @NotNull LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
            @NotNull IModel<PrismObject<RoleAnalysisClusterType>> cluster,
            boolean isRoleMode) {
        super(id, cluster);

        this.displayValueOptionModel = displayValueOptionModel;
        this.isRoleMode = isRoleMode;


        patternsToAnalyzeModel = new LoadableModel<>(false) {
            @Override
            protected List<DetectedPattern> load() {
                Task task = getPageBase().createSimpleTask("Prepare mining structure");
                OperationResult result = task.getResult();
                if (getCandidateRoleContainerId() != null) {
                    return analysePattersForCandidateRole(task, result);
                }

                return loadDetectedPattern();
            }
        };


        this.operationPanelModel = new LoadableDetachableModel<>() {
            @Override
            protected OperationPanelModel load() {
                OperationPanelModel model = new OperationPanelModel();
                model.createDetectedPatternModel(getClusterPatterns());
                model.createCandidatesRolesRoleModel(getClusterCandidateRoles());
                model.addSelectedPattern(patternsToAnalyzeModel.getObject());
                return model;
            }
        };

        this.miningOperationChunk = new LoadableModel<>(false) {

            @Override
            protected MiningOperationChunk load() {
                Task task = getPageBase().createSimpleTask("Prepare mining structure");
                OperationResult result = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                //chunk mode
                MiningOperationChunk chunk = roleAnalysisService.prepareMiningStructure(getModelObject().asObjectable(), displayValueOptionModel.getObject(),
                        isRoleMode ? RoleAnalysisProcessModeType.ROLE : RoleAnalysisProcessModeType.USER, result, task);

                //sort mode
                RoleAnalysisSortMode sortMode = displayValueOptionModel.getObject().getSortMode();
                if (sortMode == null) {
                    displayValueOptionModel.getObject().setSortMode(RoleAnalysisSortMode.NONE);
                    sortMode = RoleAnalysisSortMode.NONE;
                }

                List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks(sortMode);
                List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks(sortMode);

                List<DetectedPattern> defaultDisplayedPatterns = patternsToAnalyzeModel.getObject();
                if (defaultDisplayedPatterns != null && !defaultDisplayedPatterns.isEmpty()) {
                    if (isRoleMode) {
                        initRoleBasedDetectionPattern(getPageBase(), users, roles, defaultDisplayedPatterns, minFrequency, maxFrequency, task, result);
                    } else {
                        initUserBasedDetectionPattern(getPageBase(), users, roles, defaultDisplayedPatterns, minFrequency, maxFrequency, task, result);
                    }
                }

                specialColumnCount = users.size();
                toCol = Math.min(toCol, specialColumnCount);


                return chunk;
            }
        };

        RoleAnalysisClusterType clusterObject = getModelObject().asObjectable();
        RoleAnalysisDetectionOptionType detectionOption = clusterObject.getDetectionOption();
        RangeType frequencyRange = detectionOption.getFrequencyRange();

        if (frequencyRange != null) {
            this.minFrequency = frequencyRange.getMin() / 100;
            this.maxFrequency = frequencyRange.getMax() / 100;
        }

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }


        private void initLayout() {
            RoleAnalysisTable<A> table = generateTable();
            add(table);

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
                        headerItems.newChildId(), getModel()) {

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
                        Label label = new Label(id, "Table view"); //TODO string resource model
                        label.setOutputMarkupId(true);
                        return label;
                    }

                    @Override
                    protected void addDescriptionComponents() {
                        appendText("Switch table view", null); //TODO string resource model
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

        private List<B> getMainMiningChunk() {
            RoleAnalysisSortMode roleAnalysisSortMode = displayValueOptionModel.getObject().getSortMode();
        MiningOperationChunk chunk = miningOperationChunk.getObject();
            if (isRoleMode) {
                return (List<B>) chunk.getMiningRoleTypeChunks(roleAnalysisSortMode);
            }
            return (List<B>) chunk.getMiningUserTypeChunks(roleAnalysisSortMode);
        }

    private List<A> getAdditionalMiningChunk() {
        MiningOperationChunk chunk = miningOperationChunk.getObject();
        RoleAnalysisSortMode roleAnalysisSortMode = displayValueOptionModel.getObject().getSortMode();
        if (isRoleMode) {
            return (List<A>) chunk.getMiningUserTypeChunks(roleAnalysisSortMode);
        }
        return (List<A>) chunk.getMiningRoleTypeChunks(roleAnalysisSortMode);
    }

        private @NotNull RoleMiningProvider<A> createRoleMiningProvider() {

            ListModel<A> model = new ListModel<>() {

                @Override
                public List<A> getObject() {
                    return getAdditionalMiningChunk();
                }
            };

            return new RoleMiningProvider<>(this, model, false);
        }

        public  RoleAnalysisTable<A> generateTable() {

            RoleMiningProvider<A> provider = createRoleMiningProvider();


            RoleAnalysisTable<A> table = new RoleAnalysisTable<>(
                    ID_DATATABLE, provider,
                    initColumns(),
                    null, true, specialColumnCount, displayValueOptionModel) {

                @Override
                public String getAdditionalBoxCssClasses() {
                    return " m-0";
                }

                @Override
                protected int getColumnCount() {
                    return specialColumnCount;
                }

                @Override
                protected void resetTable(AjaxRequestTarget target) {
                    miningOperationChunk.reset();
                    refreshTable(target);
                }

                @Override
                protected @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRoleContainer() {
                    return getCandidateRoleToPerform(RoleAnalysisMatrixTable.this.getModelObject().asObjectable());
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
                    onSubmitCandidateRolePerform(target, RoleAnalysisMatrixTable.this.getModelObject());
                }


                @Override
                public void onChange(String value, AjaxRequestTarget target, int currentPage) {
                    currentPageView = currentPage;
                    String[] rangeParts = value.split(" - ");
                    valueTitle = value;
                    fromCol = Integer.parseInt(rangeParts[0]);
                    toCol = Integer.parseInt(rangeParts[1]);

                    refreshTable(target);
                }

                @Override
                public void onChangeSize(int value, AjaxRequestTarget target) {
                    currentPageView = 0;
                    columnPageCount = value;
                    fromCol = 1;
                    toCol = Math.min(value, specialColumnCount);
                    valueTitle = "0 - " + toCol;

                    refreshTable(target);
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

        //TODO B as base chung, A as additional chunk
        public List<IColumn<A, String>> initColumns() {

            List<IColumn<A, String>> columns = new ArrayList<>();

            columns.add(new CompositedIconColumn<>(null) {

                @Serial private static final long serialVersionUID = 1L;

                @Override
                public String getCssClass() {
                    return " role-mining-static-header ";
                }

                @Override
                protected CompositedIcon getCompositedIcon(IModel<A> rowModel) {

                    A object = rowModel.getObject();
                    List<String> roles = object.getRoles();

                    String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE);
                    CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                            LayeredIconCssStyle.IN_ROW_STYLE);

                    String iconColor = object.getIconColor();
                    if (iconColor != null) {
                        compositedIconBuilder.appendColorHtmlValue(iconColor);
                    }

                    List<ObjectReferenceType> resolvedPattern = getModelObject().asObjectable().getResolvedPattern();
                    for (ObjectReferenceType ref : resolvedPattern) {
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
                public void populateItem(Item<ICellPopulator<A>> cellItem, String componentId, IModel<A> rowModel) {
                    A object = rowModel.getObject();
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
                public void populateItem(Item<ICellPopulator<A>> item, String componentId,
                        IModel<A> rowModel) {
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
                        @NotNull IModel<A> rowModel,
                        List<String> elements) {

                    String title = rowModel.getObject().getChunkName();
                    if (isOutlierDetection()) {
                        double confidence = rowModel.getObject().getFrequencyItem().getzScore();
                        title = title + " (" + confidence + "confidence) ";

                        //TODO this is just tmp test
                        DetectionOption detectionOption = new DetectionOption(
                                    10, 100, 2, 2);
                        ListMultimap<String, SimpleHeatPattern> totalRelationOfPatternsForChunk = new OutlierPatternResolver()
                                    .performDetection(RoleAnalysisProcessModeType.USER, getAdditionalMiningChunk(), detectionOption);


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
                                        .getFocusTypeObject(objectOid, task, task.getResult())); //TODO result
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
                                    if (RoleAnalysisChunkMode.valueOf(getCompressStatus())
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
                            if (RoleAnalysisChunkMode.valueOf(getCompressStatus()).equals(RoleAnalysisChunkMode.COMPRESS)) {
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
                            RoleAnalysisSortMode roleAnalysisSortMode = getRoleAnalysisSortMode(displayValueOptionModel);

                            DisplayValueOption options = displayValueOptionModel.getObject();

                            options.setSortMode(roleAnalysisSortMode);

                            RoleAnalysisChunkMode chunkMode = displayValueOptionModel.getObject().getChunkMode();
                            if (chunkMode.equals(RoleAnalysisChunkMode.COMPRESS)) {
                                options.setChunkMode(RoleAnalysisChunkMode.EXPAND);
                            } else {
                                options.setChunkMode(RoleAnalysisChunkMode.COMPRESS);
                                options.setUserAnalysisUserDef(getObjectNameDef());
                                options.setRoleAnalysisRoleDef(getObjectNameDef());
                            }

                            miningOperationChunk.reset();
                            RoleAnalysisMatrixTable.this.refreshTable(target);
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
                public void populateItem(Item<ICellPopulator<A>> item, String componentId,
                        IModel<A> rowModel) {

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
                            A object = rowModel.getObject();
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

            IColumn<A, String> column;
            List<B> mainChunk = getMainMiningChunk();
            for (int i = fromCol - 1; i < toCol; i++) {
                B colChunk = mainChunk.get(i);
                int membersSize = colChunk.getUsers().size();
                RoleAnalysisTableTools.StyleResolution styleWidth = RoleAnalysisTableTools.StyleResolution.resolveSize(membersSize);

                boolean mark = false;
                Set<String> markMemberObjects = getMarkMemberObjects();
                Set<String> markPropertyObjects = getMarkPropertyObjects();
                if (markMemberObjects != null && markMemberObjects.containsAll(colChunk.getMembers())) {
                    mark = true;
                } else if (markPropertyObjects != null && markPropertyObjects.containsAll(colChunk.getProperties())) {
                    mark = true;
                }
                boolean finalMark = mark;
                column = new AbstractColumn<>(createStringResource("")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<A>> cellItem,
                            String componentId, IModel<A> model) {
                        A rowChunk = model.getObject();
                        List<String> cellRoles = rowChunk.getRoles();
                        int propertiesCount = cellRoles.size();
                        RoleAnalysisTableTools.StyleResolution styleHeight = RoleAnalysisTableTools
                                .StyleResolution
                                .resolveSize(propertiesCount);

                        applySquareTableCell(cellItem, styleWidth, styleHeight);

                        RoleAnalysisTableCellFillResolver.Status isInclude = resolveCellTypeUserTable(componentId, cellItem, rowChunk, colChunk,
                                new LoadableDetachableModel<>() {
                                    @Override
                                    protected Map<String, String> load() {
                                        return getPatternColorPalette();
                                    }
                                });

                        if (isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_INCLUDE)) {
                            isRelationSelected = true;
                        }

                        if (!isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_NONE) && finalMark) {
                            cellItem.add(AttributeAppender.append("style", "border: 5px solid #206f9d;"));
                        }

                        RoleAnalysisChunkAction chunkAction = displayValueOptionModel.getObject().getChunkAction();
                        if (!chunkAction.equals(RoleAnalysisChunkAction.SELECTION)) {
                            patternCellResolver(cellItem, rowChunk, colChunk, getAdditionalMiningChunk(), getModelObject(), chunkAction);
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
                                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name

                                List<PrismObject<FocusType>> objects = new ArrayList<>();
                                for (String objectOid : elements) {
                                    objects.add(getPageBase().getRoleAnalysisService()
                                            .getFocusTypeObject(objectOid, task, task.getResult()));
                                }
//                            if (isOutlierDetection() && cluster.getOid() != null && !elements.isEmpty()) {
//
//                                //TODO session option min members
//
//                                OutlierAnalyseActionDetailsPopupPanel detailsPanel = new OutlierAnalyseActionDetailsPopupPanel(
//                                        ((PageBase) getPage()).getMainPopupBodyId(),
//                                        Model.of("Analyzed members details panel"), elements.get(0), cluster.getOid(), 10) {
//                                    @Override
//                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
//                                        super.onClose(ajaxRequestTarget);
//                                    }
//                                };
//                                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
//                            } else {
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
//                        }

                        };
                    }

                };
                columns.add(column);
            }

            return columns;
        }

        private void patternCellResolver(@NotNull Item<ICellPopulator<A>> cellItem,
                A roleChunk,
                B userChunk,
                List<A> roles, PrismObject<RoleAnalysisClusterType> cluster, RoleAnalysisChunkAction chunkAction) {
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

                    if (chunkAction.equals(RoleAnalysisChunkAction.DETAILS_DETECTION)) {
                        DebugLabel debugLabel = new DebugLabel(((PageBase) getPage()).getMainPopupBodyId(), () -> new PatternStatistics<>(roles, userChunk.getMembers(), roleChunk.getProperties(), cluster, getPageBase())) {

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

                        };
                        debugLabel.setOutputMarkupId(true);
                        ((PageBase) getPage()).showMainPopup(debugLabel, ajaxRequestTarget);
                    } else {
                        getSelectedPatterns().clear();

                        PatternStatistics<A> statistics = new PatternStatistics<>(roles, userChunk.getMembers(), roleChunk.getProperties(), cluster, getPageBase());
                        DetectedPattern detectedPattern = statistics.getDetectedPattern();
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


        private void chunkActionSelectorBehavior(
                @NotNull Item<ICellPopulator<A>> cellItem,
                A rowChunk,
                B colChunk) {
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


        protected String getCompressStatus() {
            return displayValueOptionModel.getObject().getChunkMode().getValue();
        }

        public void refreshTableCells(AjaxRequestTarget ajaxRequestTarget) {
            isRelationSelected = false;
            MiningOperationChunk chunk = miningOperationChunk.getObject();
            List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
            List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

            refreshCells(RoleAnalysisProcessModeType.USER, users, roles, minFrequency, maxFrequency);
            resetCellsAndActionButton(ajaxRequestTarget);
        }

        public void loadDetectedPattern(AjaxRequestTarget target) {
            isRelationSelected = false;
            MiningOperationChunk chunk = miningOperationChunk.getObject();
            List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
            List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

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

        private @Nullable Set<RoleAnalysisCandidateRoleType> getCandidateRole() {
            List<String> candidateRoleContainerId = getCandidateRoleContainerId();

            Set<RoleAnalysisCandidateRoleType> candidateRoleTypes = new HashSet<>();
            if (candidateRoleContainerId != null && !candidateRoleContainerId.isEmpty()) {
                RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
                List<RoleAnalysisCandidateRoleType> candidateRoles = clusterType.getCandidateRoles();

                for (RoleAnalysisCandidateRoleType candidateRole : candidateRoles) {
                    if (candidateRoleContainerId.contains(candidateRole.getId().toString())) {
                        candidateRoleTypes.add(candidateRole);
                    }
                }
                if (!candidateRoleTypes.isEmpty()) {
                    return candidateRoleTypes;
                }
                return null;
            }
            return null;

        }


        private <F extends FocusType, CH extends MiningBaseTypeChunk> void fillCandidateList(Class<F> type,
                Set<PrismObject<F>> candidateList,
                List<CH> miningSimpleChunk,
                Task task,
                OperationResult result) {
            for (CH roleChunk : miningSimpleChunk) {
                if (roleChunk.getStatus().equals(RoleAnalysisOperationMode.INCLUDE)) {
                    for (String roleOid : roleChunk.getRoles()) {
                        PrismObject<F> roleObject = WebModelServiceUtils.loadObject(type, roleOid, getPageBase(), task, result);
                        if (roleObject != null) {
                            candidateList.add(roleObject);
                        }
                    }
                }
            }
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

        MiningOperationChunk chunk = miningOperationChunk.getObject();

        Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();
        fillCandidateList(RoleType.class, candidateInducements, chunk.getSimpleMiningRoleTypeChunks(), task, result);

        Set<PrismObject<UserType>> candidateMembers = new HashSet<>();
        fillCandidateList(UserType.class, candidateMembers, chunk.getSimpleMiningUserTypeChunks(), task, result);

        Set<RoleAnalysisCandidateRoleType> candidateRoleToPerform = getCandidateRoleToPerform(cluster.asObjectable());
        if (candidateRoleToPerform != null) {
            @Nullable List<RoleAnalysisCandidateRoleType> candidateRole = new ArrayList<>(candidateRoleToPerform);
            if (candidateRole.size() == 1) {
                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

                Set<AssignmentType> assignmentTypeSet = candidateInducements.stream()
                        .map(candidateInducement -> ObjectTypeUtil.createAssignmentTo(candidateInducement.getOid(), ObjectTypes.ROLE))
                        .collect(Collectors.toSet());

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

        PrismObject<RoleType> businessRole = new RoleType().asPrismObject();

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
        Set<PrismObject<RoleType>> inducement = operationData.getCandidateRoles();
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

        private List<DetectedPattern> getClusterPatterns() {
            RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
            return transformDefaultPattern(clusterType);
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

        protected void refreshTable(AjaxRequestTarget target) {
            var columns = RoleAnalysisMatrixTable.this.initColumns();
            RoleAnalysisTable<?> table = RoleAnalysisMatrixTable.this.getTable();
            table.getDataTable().getColumns().clear();
            table.getDataTable().getColumns().addAll((List) columns);
            target.add(RoleAnalysisMatrixTable.this);
        }


    private List<DetectedPattern> analysePattersForCandidateRole(Task task, OperationResult result) {
        RoleAnalysisClusterType cluster = getModelObject().asObjectable();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
        return roleAnalysisService.findDetectedPatterns(cluster, getCandidateRoleContainerId(), task, result);
    }

        private List<DetectedPattern> loadDetectedPattern() {
            RoleAnalysisClusterType cluster = getModelObject().asObjectable();
            List<RoleAnalysisDetectionPatternType> detectedPattern = cluster.getDetectedPattern();

            for (RoleAnalysisDetectionPatternType pattern : detectedPattern) {
                Long id = pattern.getId();
                if (id.equals(getDetectedPatternContainerId())) {
                    return Collections.singletonList(transformPattern(pattern));
                }
            }
            return new ArrayList<>();
        }


    public Long getDetectedPatternContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_DETECTED_PATER_ID);
        if (!stringValue.isNull()) {
            return Long.valueOf(stringValue.toString());
        }
        return null;
    }

    public List<String> getCandidateRoleContainerId() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_CANDIDATE_ROLE_ID);
        if (!stringValue.isNull()) {
            String[] split = stringValue.toString().split(",");
            return Arrays.asList(split);
        }
        return null;
    }


    private List<DetectedPattern> getClusterCandidateRoles() {
        RoleAnalysisClusterType clusterType = getModelObject().asObjectable();
        return loadAllCandidateRoles(clusterType);
    }

    private @NotNull List<DetectedPattern> loadAllCandidateRoles(@NotNull RoleAnalysisClusterType cluster) {
        List<RoleAnalysisCandidateRoleType> clusterCandidateRoles = cluster.getCandidateRoles();
        List<DetectedPattern> candidateRoles = new ArrayList<>();
        Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name?
        OperationResult result = task.getResult();
        for (RoleAnalysisCandidateRoleType candidateRole : clusterCandidateRoles) {

            RoleAnalysisOperationStatus operationStatus = candidateRole.getOperationStatus();
            boolean isMigrated = operationStatus != null
                    && operationStatus.getOperationChannel() != null
                    && operationStatus.getOperationChannel().equals(RoleAnalysisOperation.MIGRATION);

            if (isMigrated) {
                continue;
            }


            String roleOid = candidateRole.getCandidateRoleRef().getOid();
            //TODO does it make sense to create subresult for each iteration?
            PrismObject<RoleType> rolePrismObject = getPageBase().getRoleAnalysisService().getRoleTypeObject(
                    roleOid, task, result);
            List<String> rolesOidInducements;
            if (rolePrismObject == null) {
                return new ArrayList<>();
            }

            //TODO what is this?
            rolesOidInducements = getRolesOidInducements(rolePrismObject);
            List<String> rolesOidAssignment = getRolesOidAssignment(rolePrismObject.asObjectable());

            Set<String> accessOidSet = new HashSet<>(rolesOidInducements);
            accessOidSet.addAll(rolesOidAssignment);

            ListMultimap<String, String> mappedMembers = getPageBase().getRoleAnalysisService().extractUserTypeMembers(new HashMap<>(),
                    null,
                    Collections.singleton(roleOid),
                    getPageBase().createSimpleTask(OP_PREPARE_OBJECTS),
                    result);

            List<ObjectReferenceType> candidateMembers = candidateRole.getCandidateMembers();
            Set<String> membersOidSet = new HashSet<>();
            for (ObjectReferenceType candidateMember : candidateMembers) {
                String oid = candidateMember.getOid();
                if (oid != null) {
                    membersOidSet.add(oid);
                }
            }

            membersOidSet.addAll(mappedMembers.get(roleOid));
            double clusterMetric = (accessOidSet.size() * membersOidSet.size()) - membersOidSet.size();

            DetectedPattern pattern = new DetectedPattern(
                    accessOidSet,
                    membersOidSet,
                    clusterMetric,
                    null,
                    roleOid);
            pattern.setIdentifier(rolePrismObject.getName().getOrig());
            pattern.setId(candidateRole.getId());
            pattern.setClusterRef(new ObjectReferenceType().oid(cluster.getOid()).type(RoleAnalysisClusterType.COMPLEX_TYPE));

            candidateRoles.add(pattern);
        }
        return candidateRoles;
    }


}

