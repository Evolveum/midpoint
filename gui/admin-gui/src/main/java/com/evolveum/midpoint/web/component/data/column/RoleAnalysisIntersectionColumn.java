/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.common.mining.objects.chunk.*;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.DebugLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.PatternStatistics;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class RoleAnalysisIntersectionColumn<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends RoleAnalysisMatrixColumn<A> {

    private final B baseMiningChunk;


    private final RoleAnalysisTableTools.StyleResolution styleWidth;


    public RoleAnalysisIntersectionColumn(
            B baseMiningChunk,
            IModel<RoleAnalysisObjectDto> model,
//            IModel<OperationPanelModel> opPanelModel,
//            IModel<PrismObject<RoleAnalysisClusterType>> cluster,
//            LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
//            LoadableModel<MiningOperationChunk> miningOperationChunk,
            PageBase pageBase) {
        super(model, pageBase);
//
        this.baseMiningChunk = baseMiningChunk;
//
//        this.miningOperationChunk = miningOperationChunk;
        this.styleWidth = RoleAnalysisTableTools.StyleResolution.resolveSize(baseMiningChunk.getUsers().size());
    }

    @Override
        public void populateItem(Item<ICellPopulator<A>> cellItem,
                String componentId, IModel<A> model) {
            A rowChunk = model.getObject();
            List<String> cellRoles = rowChunk.getRoles();
            int propertiesCount = cellRoles.size();
            RoleAnalysisTableTools.StyleResolution styleHeight = RoleAnalysisTableTools
                    .StyleResolution
                    .resolveSize(propertiesCount);

            RoleAnalysisTableTools.StyleResolution styleWidth = RoleAnalysisTableTools.StyleResolution.resolveSize(baseMiningChunk.getUsers().size());

            applySquareTableCell(cellItem, styleWidth, styleHeight);

            RoleAnalysisTableCellFillResolver.Status isInclude = resolveCellTypeUserTable(componentId, cellItem, rowChunk, baseMiningChunk,
                   getColorPaletteModel());

            if (isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_INCLUDE)) {
                setRelationSelected(true);
            }

            boolean mark = false;
            Set<String> markMemberObjects = getMarkMemberObjects();
            Set<String> markPropertyObjects = getMarkPropertyObjects();
            if (markMemberObjects != null && markMemberObjects.containsAll(baseMiningChunk.getMembers())) {
                mark = true;
            } else if (markPropertyObjects != null && markPropertyObjects.containsAll(baseMiningChunk.getProperties())) {
                mark = true;
            }

            if (!isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_NONE) && mark) {
                cellItem.add(AttributeAppender.append("style", "border: 5px solid #206f9d;"));
            }

            RoleAnalysisChunkAction chunkAction = getChunkAction();
            if (!chunkAction.equals(RoleAnalysisChunkAction.SELECTION)) {
                patternCellResolver(cellItem, rowChunk, baseMiningChunk);
            } else {
                chunkActionSelectorBehavior(cellItem, rowChunk, baseMiningChunk);
            }

        }

        @Override
        public String getCssClass() {
            String cssLevel = RoleAnalysisTableTools.StyleResolution.resolveSizeLevel(styleWidth);
            return cssLevel + " p-2";
        }

        private AjaxLinkTruncateDto loadColumnHeaderModelObject() {
            String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE);
            CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                    LayeredIconCssStyle.IN_ROW_STYLE);

            String iconColor = baseMiningChunk.getIconColor();
            if (iconColor != null) {
                compositedIconBuilder.appendColorHtmlValue(iconColor);
            }

            CompositedIcon compositedIcon = compositedIconBuilder.build();

            return new AjaxLinkTruncateDto(baseMiningChunk.getChunkName(), compositedIcon, baseMiningChunk.getStatus());
        }

        @Override
        public Component getHeader(String componentId) {
            return createColumnDisplayPanel(componentId, Model.of(loadColumnHeaderModelObject()), Model.of(baseMiningChunk), " flex-row", " text-truncate");
        }

    private void patternCellResolver(@NotNull Item<ICellPopulator<A>> cellItem,
            A roleChunk,
            B userChunk) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                if (userChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE
                        && roleChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE) {
                    getSelectedPatterns().clear();
                    setRelationSelected(false);
                    refreshTableCells(ajaxRequestTarget);
                    return;
                }

                RoleAnalysisObjectDto roleAnalysis = getModel().getObject();
                RoleAnalysisChunkAction chunkAction = roleAnalysis.getChunkAction();
                if (chunkAction.equals(RoleAnalysisChunkAction.DETAILS_DETECTION)) {
                    DebugLabel debugLabel = createDebugLabelPanel(userChunk.getMembers(), roleChunk.getProperties(), roleAnalysis);
                    getPageBase().showMainPopup(debugLabel, ajaxRequestTarget);
                } else {
                    getSelectedPatterns().clear();

                    PatternStatistics<A> statistics = new PatternStatistics<>(roleAnalysis, userChunk.getMembers(), roleChunk.getProperties(), getPageBase());
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

    @NotNull
    private DebugLabel createDebugLabelPanel(List<String> members, List<String> mustMeet, RoleAnalysisObjectDto roleAnalysisObjectDto) {
        DebugLabel debugLabel = new DebugLabel(getPageBase().getMainPopupBodyId(), () -> new PatternStatistics<>(roleAnalysisObjectDto, members, mustMeet, getPageBase())) {

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
        return debugLabel;
    }

    private void chunkActionSelectorBehavior(
            @NotNull Item<ICellPopulator<A>> cellItem,
            A rowChunk,
            B colChunk) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                setRelationSelected(false);
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
                    setRelationSelected(true);
                    rowChunk.setStatus(chunkStatus);
                    colChunk.setStatus(chunkStatus);
                }

                refreshTable(ajaxRequestTarget);
            }
        });
    }

    protected abstract Set<String> getMarkMemberObjects();

    protected abstract Set<String> getMarkPropertyObjects();

    protected abstract void loadDetectedPattern(AjaxRequestTarget target);


    protected void refreshTableCells(AjaxRequestTarget target) {
        setRelationSelected(false);
        MiningOperationChunk chunk = getMiningChunk();
        List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks(RoleAnalysisSortMode.NONE);
        List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks(RoleAnalysisSortMode.NONE);

        refreshCells(chunk.getProcessMode(), users, roles, getMinFrequency(), getMaxFrequency());

        refreshTable(target);
    }

    @Override
    protected <T extends MiningBaseTypeChunk> List<String> getElements(T miningBaseTypeChunk) {
        return miningBaseTypeChunk.getUsers();
    }

    private MiningOperationChunk getMiningChunk() {
        return getModel().getObject().getMininingOperationChunk();
    }

    protected abstract IModel<Map<String, String>> getColorPaletteModel(); //new PropertyModel<>(getOpPanelModel(), OperationPanelModel.F_PALLET_COLORS)

}
