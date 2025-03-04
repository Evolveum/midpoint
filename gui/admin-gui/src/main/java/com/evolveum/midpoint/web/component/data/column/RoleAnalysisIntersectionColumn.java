/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.refreshCells;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.resolveCellTypeUserTable;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.applySquareTableCell;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.createCompositedObjectIcon;

import java.util.*;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.DetailedPatternSelectionPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.PatternStatistics;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.Nullable;

public abstract class RoleAnalysisIntersectionColumn<B extends MiningBaseTypeChunk, A extends MiningBaseTypeChunk> extends RoleAnalysisMatrixColumn<A> {

    private final B baseMiningChunk;
    private final RoleAnalysisTableTools.StyleResolution styleWidth;
    private final boolean isOutlierMode;

    protected RoleAnalysisIntersectionColumn(
            B baseMiningChunk,
            IModel<RoleAnalysisObjectDto> model,
            PageBase pageBase) {
        super(model, pageBase);
        this.baseMiningChunk = baseMiningChunk;
        this.styleWidth = RoleAnalysisTableTools.StyleResolution.resolveSize(baseMiningChunk.getMembers().size());
        this.isOutlierMode = getModel().getObject().isOutlierDetection();
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

        applySquareTableCell(cellItem, styleWidth, styleHeight);

        RoleAnalysisTableCellFillResolver.Status isInclude = resolveCellTypeUserTable(componentId, cellItem, rowChunk,
                baseMiningChunk, isOutlierMode, getColorPaletteModel());

        if (isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_INCLUDE)) {
            setRelationSelected(true);
        }

        markOutlierChunkIfRequested(cellItem, rowChunk, baseMiningChunk, isInclude);

        applyMarkAction(cellItem, rowChunk);

    }

    private void applyMarkAction(Item<ICellPopulator<A>> cellItem, A rowChunk) {
        RoleAnalysisChunkAction chunkAction = getChunkAction();
        if (!chunkAction.equals(RoleAnalysisChunkAction.SELECTION)) {
            patternCellResolver(cellItem, rowChunk, baseMiningChunk);
        } else {
            chunkActionSelectorBehavior(cellItem, rowChunk, baseMiningChunk);
        }
    }

    public boolean markOutlierRelations(
            boolean isMarked,
            @NotNull Item<ICellPopulator<A>> cellItem,
            @NotNull A rowChunk,
            @NotNull B colChunk,
            @NotNull RoleAnalysisObjectDto roleAnalysis,
            @Nullable Set<RoleAnalysisObjectDto.MarkedRelation> markedRelations,
            @NotNull RoleAnalysisTableCellFillResolver.@NotNull Status isInclude) {

        if (isMarked) {
            return true;
        }

        if (isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_NONE) ||
                !roleAnalysis.isOutlierDetection() ||
                markedRelations == null ||
                markedRelations.isEmpty()) {
            return false;
        }

        Set<String> members = new HashSet<>(colChunk.getUsers());
        Set<String> roles = new HashSet<>(rowChunk.getRoles());

        for (RoleAnalysisObjectDto.MarkedRelation markedRelation : markedRelations) {
            if (members.stream().anyMatch(markedRelation.userOid()::contains)) {
                cellItem.add(AttributeModifier.append("style", markedRelation.cssStyle()));

                if (roles.stream().anyMatch(markedRelation.roleOid()::contains)) {
                    cellItem.add(AttributeModifier.replace("class", markedRelation.cssClass()));
                    return true;
                }
            }
        }

        return false;
    }

    private boolean markOutlierPatterns(
            boolean isMarked,
            @NotNull Item<ICellPopulator<A>> cellItem,
            @NotNull A rowChunk,
            @NotNull RoleAnalysisObjectDto roleAnalysis,
            @Nullable List<DetectedPattern> selectedPatterns,
            @NotNull RoleAnalysisTableCellFillResolver.@NotNull Status isInclude) {

        if (isMarked) {
            return true;
        }

        if (!roleAnalysis.isOutlierDetection()
                || selectedPatterns == null || selectedPatterns.isEmpty()
                || !(isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_INCLUDE) ||
                isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_DISABLE))) {
            return false;
        }

        Set<String> members = new HashSet<>(this.baseMiningChunk.getMembers());

        for (DetectedPattern pattern : selectedPatterns) {
            String associatedColor = Optional.ofNullable(pattern.getAssociatedColor()).filter(c -> !c.isEmpty()).orElse("#28a745");

            if (pattern.getUsers().stream().anyMatch(members::contains)) {
                isMarked = true;
                cellItem.add(AttributeModifier.append("style", "border: 5px solid " + associatedColor + ";"));

                if (rowChunk.getRoles().stream().anyMatch(pattern.getRoles()::contains)) {
                    cellItem.add(AttributeModifier.replace("class",
                            "p-2 d-flex align-items-center justify-content-center bg-danger"));
                }
                break;
            }
        }

        return isMarked;
    }

    private boolean markOutlierChunkIfRequested(Item<ICellPopulator<A>> cellItem,
            A rowChunk,
            B colChunk,
            RoleAnalysisTableCellFillResolver.@NotNull Status isInclude) {
        RoleAnalysisObjectDto roleAnalysis = getModel().getObject();

        if(!roleAnalysis.isOutlierDetection()){
            return false;
        }

        Set<RoleAnalysisObjectDto.MarkedRelation> markedRelations = roleAnalysis.getMarkedRelations();
        boolean isMarked = false;

        isMarked = markOutlierRelations(isMarked, cellItem, rowChunk, colChunk, roleAnalysis, markedRelations, isInclude);

        isMarked = markOutlierPatterns(isMarked, cellItem, rowChunk, roleAnalysis, getSelectedPatterns(), isInclude);

        isMarked = markMarkedRequestedObjects(isMarked, cellItem, rowChunk, roleAnalysis, isInclude);

        return isMarked;
    }

    private boolean markMarkedRequestedObjects(
            boolean isMarked,
            @NotNull Item<ICellPopulator<A>> cellItem,
            @NotNull A rowChunk,
            @NotNull RoleAnalysisObjectDto roleAnalysis,
            @NotNull RoleAnalysisTableCellFillResolver.@NotNull Status isInclude) {
        Set<String> markedUsers = roleAnalysis.getMarkedUsers();
        Set<String> markedRoles = roleAnalysis.getMarkedRoles();

        if (isMarked) {
            return true;
        }

        if (!roleAnalysis.isOutlierDetection()
                || !(isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_INCLUDE) ||
                isInclude.equals(RoleAnalysisTableCellFillResolver.Status.RELATION_DISABLE))) {
            return false;
        }

        if (markedUsers == null
                || markedUsers.isEmpty()
                || this.baseMiningChunk.getMembers().stream().noneMatch(markedUsers::contains)) {
            return false;
        }

        if (markedRoles != null && !markedRoles.isEmpty() && rowChunk.getRoles().stream().anyMatch(markedRoles::contains)) {
            cellItem.add(AttributeModifier.replace("class",
                    "p-2 d-flex align-items-center justify-content-center bg-danger"));
        }

        cellItem.add(AttributeModifier.append("style", " border: 5px solid #206f9d;"));

        return true;
    }

    @Override
    public String getCssClass() {
        String cssLevel = RoleAnalysisTableTools.StyleResolution.resolveSizeLevel(styleWidth);
        return cssLevel + " p-0";
    }

    private @NotNull AjaxLinkTruncateDto loadColumnHeaderModelObject() {
        String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE);
        CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                IconCssStyle.IN_ROW_STYLE);

        String iconColor = baseMiningChunk.getIconColor();
        if (iconColor != null) {
            compositedIconBuilder.appendColorHtmlValue(iconColor);
        }

        CompositedIcon compositedIcon = createCompositedObjectIcon(baseMiningChunk, getModel());

        return new AjaxLinkTruncateDto(baseMiningChunk.getChunkName(), compositedIcon, baseMiningChunk.getStatus(),
                AjaxLinkTruncatePanelAction.PanelMode.ROTATED) {
            @Override
            public boolean isActionEnabled() {
                RoleAnalysisObjectDto roleAnalysis = getModel().getObject();
                return !roleAnalysis.isOutlierDetection();
            }
        };
    }

    @Override
    public Component getHeader(String componentId) {
        return createColumnDisplayPanel(
                componentId,
                Model.of(loadColumnHeaderModelObject()),
                Model.of(baseMiningChunk));
    }

    private void patternCellResolver(@NotNull Item<ICellPopulator<A>> cellItem,
            A roleChunk,
            B userChunk) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                if (userChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE
                        && roleChunk.getStatus() == RoleAnalysisOperationMode.INCLUDE) {
                    setRelationSelected(false);
                    refreshTableCells(ajaxRequestTarget);
                    return;
                }

                RoleAnalysisObjectDto roleAnalysis = getModel().getObject();
                RoleAnalysisChunkAction chunkAction = roleAnalysis.getChunkAction();
                if (chunkAction.equals(RoleAnalysisChunkAction.DETAILS_DETECTION)) {
                    DetailedPatternSelectionPanel detailedPatternSelectionPanel = createDetailedPatternSelectionPanel(
                            userChunk.getMembers(), roleChunk.getProperties(), roleAnalysis);
                    getPageBase().showMainPopup(detailedPatternSelectionPanel, ajaxRequestTarget);
                } else {
                    //todo this is wrong, tbd whats with temporary detected patterns?
                    PatternStatistics<A> statistics = new PatternStatistics<>(roleAnalysis,
                            userChunk.getMembers(), roleChunk.getProperties(), getPageBase());
                    DetectedPattern detectedPattern = statistics.getDetectedPattern();
                    if (detectedPattern.getRoles() != null && !detectedPattern.getRoles().isEmpty()
                            && detectedPattern.getUsers() != null && !detectedPattern.getUsers().isEmpty()) {
                        loadTemporaryPattern(detectedPattern, getPageBase(), ajaxRequestTarget);
                    } else {
                        refreshTableCells(ajaxRequestTarget);
                    }

                }

            }
        });
    }

    @NotNull
    private DetailedPatternSelectionPanel createDetailedPatternSelectionPanel(List<String> members,
            List<String> mustMeet,
            RoleAnalysisObjectDto roleAnalysisObjectDto) {

        LoadableModel<PatternStatistics<?>> model = new LoadableModel<>() {
            @Override
            protected @NotNull PatternStatistics<?> load() {
                return new PatternStatistics<>(roleAnalysisObjectDto, members, mustMeet, getPageBase());
            }
        };

        DetailedPatternSelectionPanel detailedPatternSelectionPanel = new DetailedPatternSelectionPanel(getPageBase().getMainPopupBodyId(), model) {

            @Override
            protected void explorePatternPerform(@NotNull DetectedPattern pattern, AjaxRequestTarget target) {
                //TODO this is wrong, tbd whats with temporary detected patterns?
                if (pattern.getRoles() != null && !pattern.getRoles().isEmpty()
                        && pattern.getUsers() != null && !pattern.getUsers().isEmpty()) {
                    pattern.setPatternSelected(true);
                    loadTemporaryPattern(pattern, getPageBase(), target);
                } else {
                    refreshTableCells(target);
                }

                getPageBase().hideMainPopup(target);
            }

        };
        detailedPatternSelectionPanel.setOutputMarkupId(true);
        return detailedPatternSelectionPanel;
    }

    private void chunkActionSelectorBehavior(
            @NotNull Item<ICellPopulator<A>> cellItem,
            A rowChunk,
            B colChunk) {
        cellItem.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                onChunkSelectionPerform(ajaxRequestTarget);

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
                refreshTableRows(ajaxRequestTarget);
            }
        });
    }

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
    protected <T extends MiningBaseTypeChunk> List<String> getElements(@NotNull T miningBaseTypeChunk) {
        return miningBaseTypeChunk.getMembers();
    }

    private MiningOperationChunk getMiningChunk() {
        return getModel().getObject().getMininingOperationChunk();
    }

    protected abstract IModel<Map<String, String>> getColorPaletteModel(); //new PropertyModel<>(getOpPanelModel(), OperationPanelModel.F_PALLET_COLORS)

    private void loadTemporaryPattern(DetectedPattern detectedPattern, PageBase pageBase, AjaxRequestTarget ajaxRequestTarget) {
        updateWithPatterns(Collections.singletonList(detectedPattern), pageBase);
        onUniquePatternDetectionPerform(ajaxRequestTarget);
        refreshTable(ajaxRequestTarget);
    }

    private void onChunkSelectionPerform(AjaxRequestTarget target) {
        if (getSelectedPatterns() != null && !getSelectedPatterns().isEmpty()) {
            MiningOperationChunk chunk = getMiningChunk();
            List<MiningRoleTypeChunk> roles = chunk.getMiningRoleTypeChunks();
            List<MiningUserTypeChunk> users = chunk.getMiningUserTypeChunks();
            refreshCells(chunk.getProcessMode(), users, roles, getMinFrequency(), getMaxFrequency());
        }
        onUniquePatternDetectionPerform(target);
    }

    protected void onUniquePatternDetectionPerform(AjaxRequestTarget target) {
    }

}
