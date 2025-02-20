/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableCellFillResolver.updateFrequencyBased;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.createCompositedObjectIcon;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class RoleAnalysisObjectColumn<A extends MiningBaseTypeChunk> extends RoleAnalysisMatrixColumn<A> {

    public RoleAnalysisObjectColumn(
            IModel<RoleAnalysisObjectDto> miningOperationChunk,
            PageBase pageBase) {
        super(miningOperationChunk, pageBase);
    }

    @Override
    public void populateItem(@NotNull Item<ICellPopulator<A>> cellItem, String componentId, IModel<A> rowModel) {
        AjaxLinkTruncatePanelAction panel = createColumnDisplayPanel(
                componentId,
                Model.of(loadColumnHeaderModelObject(rowModel)),
                rowModel
        );
        cellItem.add(panel);

    }

    @Override
    public Component getHeader(String componentId) {

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon("fa fa-expand",
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton compressButton = new AjaxCompositedIconSubmitButton(componentId,
                iconBuilder.build(),
                getPageBase().createStringResource("RoleMining.operation.panel.${chunkModeValue}.button.title", getModel())) {
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
                        IconCssStyle.IN_ROW_STYLE).build();
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

                    options.setUserAnalysisUserDef(createNameAnalysisAttribute(UserType.class));
                    options.setRoleAnalysisRoleDef(createNameAnalysisAttribute(RoleType.class));
                }

                resetTable(target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        compressButton.titleAsLabel(true);
        compressButton.setOutputMarkupId(true);
        compressButton.add(AttributeModifier.append("class", "btn btn-default btn-sm"));
        compressButton.add(AttributeModifier.append("style",
                "  writing-mode: vertical-lr;  -webkit-transform: rotate(90deg);"));

        return compressButton;
    }

    private RoleAnalysisAttributeDef createNameAnalysisAttribute(Class<? extends FocusType> type) {
        ItemPath path = ItemPath.create(UserType.F_NAME);
        PrismObjectDefinition<? extends FocusType> objectDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
        return new RoleAnalysisAttributeDef(path, objectDef.findItemDefinition(path), type);
    }

    @Contract("_ -> new")
    private @NotNull AjaxLinkTruncateDto loadColumnHeaderModelObject(IModel<A> rowModel) {
        return new AjaxLinkTruncateDto(
                loadColumnHeaderModelObjectTitle(rowModel), createCompositedObjectIcon(rowModel.getObject(), getModel()),
                getOperationMode(rowModel), computeToolTip(rowModel)) {
            @Override
            public boolean isActionEnabled() {
                RoleAnalysisObjectDto roleAnalysis = getModel().getObject();
                return !roleAnalysis.isOutlierDetection();
            }
        };
    }

    private RoleAnalysisOperationMode getOperationMode(IModel<A> rowModel) {
        if (getSelectedPatterns().size() > 1) {
            return RoleAnalysisOperationMode.DISABLE;
        }

        return rowModel.getObject().getStatus();
    }

    private String loadColumnHeaderModelObjectTitle(IModel<A> rowModel) {
        updateFrequencyBased(rowModel, getMinFrequency(), getMaxFrequency(), isOutlierDetection());
        return rowModel.getObject().getChunkName();
    }

    private @Nullable String computeToolTip(IModel<A> rowModel) {
        String title = loadColumnHeaderModelObjectTitle(rowModel);
        if (isOutlierDetection()) {
            double confidence = rowModel.getObject().getFrequencyItem().getzScore();
            title = title + " (" + confidence + "confidence) ";

            //TODO tmp experiment REMOVE later
//            title = computeOutlierExperimentalInfo(rowModel, title);

            return title;
        }
        return null;
    }

    @Override
    public String getCssClass() {
        return "role-mining-static-row-header";
    }

    private boolean isOutlierDetection() {
        return getModel().getObject().isOutlierDetection();
    }

    protected abstract void resetTable(AjaxRequestTarget target);

    @Override
    protected <T extends MiningBaseTypeChunk> List<String> getElements(@NotNull T miningBaseTypeChunk) {
        return miningBaseTypeChunk.getMembers();
    }
}
