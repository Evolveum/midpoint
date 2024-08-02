/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.common.mining.utils.RoleAnalysisAttributeDefUtils.getObjectNameDef;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.updateFrequencyBased;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;

import com.google.common.collect.ListMultimap;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectionOption;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.OutlierPatternResolver;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.operation.SimpleHeatPattern;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.StringResourceModel;

public abstract class RoleAnalysisObjectColumn<A extends MiningBaseTypeChunk> extends RoleAnalysisMatrixColumn<A> {

    public RoleAnalysisObjectColumn(
            IModel<RoleAnalysisObjectDto> miningOperationChunk,
            PageBase pageBase) {
        super(miningOperationChunk, pageBase);
    }

    @Override
    public void populateItem(Item<ICellPopulator<A>> cellItem, String componentId, IModel<A> rowModel) {

        AjaxLinkTruncatePanelAction panel = createColumnDisplayPanel(componentId, Model.of(loadColumnHeaderModelObject(rowModel)), rowModel, " flex-row-reverse justify-content-between", null);
        cellItem.add(panel);

    }

    @Override
    public Component getHeader(String componentId) {

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton compressButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                getPageBase().createStringResource("RoleMining.operation.panel.${chunkModeValue}.button.title", getModel())){
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public CompositedIcon getIcon() {

                String icon;
                if (RoleAnalysisChunkMode.COMPRESS == getCompressStatus()) {
                    icon = "fa fa-expand";
                } else {
                    icon = "fa fa-compress";
                }
                return new CompositedIconBuilder().setBasicIcon(icon,
                            LayeredIconCssStyle.IN_ROW_STYLE).build();
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisSortMode roleAnalysisSortMode = getRoleAnalysisSortMode();

                DisplayValueOption options = getModel().getObject().getDisplayValueOption();

                options.setSortMode(roleAnalysisSortMode);

                RoleAnalysisChunkMode chunkMode = options.getChunkMode();
                if (chunkMode.equals(RoleAnalysisChunkMode.COMPRESS)) {
                    options.setChunkMode(RoleAnalysisChunkMode.EXPAND);
                } else {
                    options.setChunkMode(RoleAnalysisChunkMode.COMPRESS);
                    options.setUserAnalysisUserDef(getObjectNameDef());
                    options.setRoleAnalysisRoleDef(getObjectNameDef());
                }

                resetTable(target);
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


    private AjaxLinkTruncateDto loadColumnHeaderModelObject(IModel<A> rowModel) {

        return new AjaxLinkTruncateDto(computeTitle(rowModel), createCompositedIcon(rowModel), getOperationMode(rowModel));
    }

    private RoleAnalysisOperationMode getOperationMode(IModel<A> rowModel) {
        if (getSelectedPatterns().size() > 1) {
            return RoleAnalysisOperationMode.DISABLE;
        }

        return rowModel.getObject().getStatus();
    }

    private String computeTitle(IModel<A> rowModel) {
        updateFrequencyBased(rowModel, getMinFrequency(), getMaxFrequency(), isOutlierDetection());
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
        return title;

    }

    private CompositedIcon createCompositedIcon(IModel<A> rowModel) {
        A object = rowModel.getObject();
        List<String> roles = object.getRoles();

        String defaultBlackIcon = IconAndStylesUtil.createDefaultBlackIcon(RoleType.COMPLEX_TYPE);
        CompositedIconBuilder compositedIconBuilder = new CompositedIconBuilder().setBasicIcon(defaultBlackIcon,
                LayeredIconCssStyle.IN_ROW_STYLE);

        String iconColor = object.getIconColor();
        if (iconColor != null) {
            compositedIconBuilder.appendColorHtmlValue(iconColor);
        }

        List<ObjectReferenceType> resolvedPattern = getModel().getObject().getResolvedPattern();
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
    public String getCssClass() {
        return "role-mining-static-row-header";
    }

    protected abstract boolean isOutlierDetection();
    protected abstract void resetTable(AjaxRequestTarget target);

    @Override
    protected <T extends MiningBaseTypeChunk> List<String> getElements(T miningBaseTypeChunk) {
        return miningBaseTypeChunk.getRoles();
    }
}
