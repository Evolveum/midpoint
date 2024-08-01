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

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.data.RoleAnalysisObjectDto;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningBaseTypeChunk;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisObjectStatus;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.MembersDetailsPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;

public abstract class RoleAnalysisMatrixColumn<A extends MiningBaseTypeChunk> extends AbstractColumn<A, String> {

    private static final String DOT_CLASS = RoleAnalysisMatrixColumn.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

//    private final IModel<OperationPanelModel> opPanelModel;
//    private final IModel<PrismObject<RoleAnalysisClusterType>> clusterModel;
//    private final LoadableDetachableModel<DisplayValueOption> displayValueOptionModel;
//    private final IModel<MiningOperationChunk> miningOperationChunk;
    private final PageBase pageBase;


    private final IModel<RoleAnalysisObjectDto> model;


    public RoleAnalysisMatrixColumn(
            IModel<RoleAnalysisObjectDto> model,

//    IModel<OperationPanelModel> opPanelModel,
//            IModel<PrismObject<RoleAnalysisClusterType>> clusterModel,
//            LoadableDetachableModel<DisplayValueOption> displayValueOptionModel,
//            IModel<MiningOperationChunk> miningOperationChunk,
            PageBase pageBase) {
        super(new StringResourceModel(""));
        this.model = model;
//        this.opPanelModel = opPanelModel;
//        this.clusterModel = clusterModel;
//        this.displayValueOptionModel = displayValueOptionModel;
//        this.miningOperationChunk = miningOperationChunk;
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

//    public List<DetectedPattern> getSelectedPatterns() {
//        return opPanelModel.getObject().getSelectedPatterns();
//    }
//
//    public IModel<OperationPanelModel> getOpPanelModel() {
//        return opPanelModel;
//    }

//    public PrismObject<RoleAnalysisClusterType> getCluster() {
//        return clusterModel.getObject();
//    }

    public RoleAnalysisSortMode getRoleAnalysisSortMode() {
        RoleAnalysisSortMode sortMode = model.getObject().getSortMode();
        if (sortMode != null) {
            return sortMode;
        }

        return RoleAnalysisSortMode.NONE;
    }

    protected String getCompressStatus() {
        return model.getObject().getChunkModeValue(); //displayValueOptionModel.getObject().getChunkMode().getValue();
    }

    protected RoleAnalysisChunkAction getChunkAction() {
        return model.getObject().getChunkAction(); //displayValueOptionModel.getObject().getChunkAction();
    }

    protected PageBase getPageBase() {
        return pageBase;
    }

//    protected DisplayValueOption getDisplayValueOption() {
//        return displayValueOptionModel.getObject();
//    }

    protected final <T extends MiningBaseTypeChunk> AjaxLinkTruncatePanelAction createColumnDisplayPanel(
            String componentId,
            IModel<AjaxLinkTruncateDto> model,
            IModel<T> miningChunkModel,
            String flexDirection,
            String truncateClass) {
        return new AjaxLinkTruncatePanelAction(componentId, model) {

            @Override
            public String getFlexDirection() {
                return flexDirection;
            }

            @Override
            public String getTruncateClass() {
                return truncateClass;
            }

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
                Task task = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS); //TODO task name

                List<String> elements = getElements(miningChunkModel.getObject());
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
                        getPageBase().getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"),
                        objects, RoleAnalysisProcessModeType.USER) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                getPageBase().showMainPopup(detailsPanel, target);
            }
//                        }

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
//    protected abstract void resetCellsAndActionButton(AjaxRequestTarget target);
    protected abstract void setRelationSelected(boolean isRelationSelected);

    protected abstract List<DetectedPattern> getSelectedPatterns();

}
