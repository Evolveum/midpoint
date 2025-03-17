/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.common.mining.utils.values.*;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

import org.jetbrains.annotations.NotNull;

public abstract class RoleAnalysisMatrixColumn<A extends MiningBaseTypeChunk> extends AbstractColumn<A, String> {

    private final PageBase pageBase;

    private final IModel<RoleAnalysisObjectDto> model;

    public RoleAnalysisMatrixColumn(
            IModel<RoleAnalysisObjectDto> model,
            PageBase pageBase) {
        super(new StringResourceModel(""));
        this.model = model;
        this.pageBase = pageBase;
    }

    protected List<String> getPatternIdentifiers() {
        List<String> patternIds = new ArrayList<>();
        if (!getSelectedPatterns().isEmpty()) {
            for (DetectedPattern pattern : getSelectedPatterns()) {
                String identifier = pattern.getIdentifier();
                patternIds.add(identifier);
            }
        }
        return patternIds;
    }

    public RoleAnalysisSortMode getRoleAnalysisSortMode() {
        RoleAnalysisSortMode sortMode = model.getObject().getSortMode();
        if (sortMode != null) {
            return sortMode;
        }

        return RoleAnalysisSortMode.NONE;
    }

    public <T extends MiningBaseTypeChunk> RoleAnalysisProcessModeType getRoleAnalysisProcessMode(
            @NotNull IModel<T> miningChunkModel) {
        return miningChunkModel.getObject() instanceof MiningUserTypeChunk
                ? RoleAnalysisProcessModeType.USER
                : RoleAnalysisProcessModeType.ROLE;
    }

    protected RoleAnalysisChunkMode getCompressStatus() {
        return model.getObject().getChunkModeValue();
    }

    protected RoleAnalysisChunkAction getChunkAction() {
        return model.getObject().getChunkAction();
    }

    protected PageBase getPageBase() {
        return pageBase;
    }

    protected final <T extends MiningBaseTypeChunk> AjaxLinkTruncatePanelAction createColumnDisplayPanel(
            String componentId,
            IModel<AjaxLinkTruncateDto> model,
            IModel<T> miningChunkModel) {
        return new AjaxLinkTruncatePanelAction(componentId, model) {

            @Override
            protected RoleAnalysisOperationMode onStatusClick(AjaxRequestTarget target,
                    RoleAnalysisOperationMode status) {

                MiningBaseTypeChunk object = miningChunkModel.getObject();
                setRelationSelected(false);
                RoleAnalysisObjectStatus objectStatus = new RoleAnalysisObjectStatus(status.toggleStatus());
                objectStatus.setContainerId(new HashSet<>(getPatternIdentifiers()));
                object.setObjectStatus(objectStatus);

                refreshTable(target);
                return status;
            }

            @Override
            public void onDisplayNameClick(AjaxRequestTarget target) {
                MembersDetailsPopupPanel detailsPanel = new MembersDetailsPopupPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("RoleAnalysis.analyzed.members.details.panel"),
                        getElements(miningChunkModel.getObject()),
                        getRoleAnalysisProcessMode(miningChunkModel));
                getPageBase().showMainPopup(detailsPanel, target);
            }
        };
    }

    protected double getMinFrequency() {
        return model.getObject().getMinFrequency();
    }

    protected double getMaxFrequency() {
        return model.getObject().getMaxFrequency();
    }

    protected List<A> getAdditionalMiningChunk() {
        return model.getObject().getAdditionalMiningChunk();
    }

    protected <T extends MiningBaseTypeChunk> List<String> getElements(T miningChunk) {
        throw new UnsupportedOperationException("Not supported in parent, implement in children");
    }

    IModel<RoleAnalysisObjectDto> getModel() {
        return model;
    }

    protected abstract void refreshTable(AjaxRequestTarget target);

    protected abstract void refreshTableRows(AjaxRequestTarget target);

    protected abstract void setRelationSelected(boolean isRelationSelected);

    protected abstract List<DetectedPattern> getSelectedPatterns();

    //TODO check it
    protected void updateWithPatterns(List<DetectedPattern> selectedPatterns, PageBase pageBase) {
        for (DetectedPattern selectedPattern : getSelectedPatterns()) {
            selectedPattern.setPatternSelected(false);
        }
        model.getObject().updateWithPatterns(selectedPatterns, pageBase);
    }

}
