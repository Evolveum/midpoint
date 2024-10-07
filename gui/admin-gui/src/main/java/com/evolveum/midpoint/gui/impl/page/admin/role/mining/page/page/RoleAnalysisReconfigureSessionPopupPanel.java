/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page;

import java.io.Serial;
import java.util.Collection;

import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemVisibilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectChangesExecutorImpl;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component.ModalFooterPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisReconfigureSessionPopupPanel
        extends BasePanel<AssignmentHolderDetailsModel<RoleAnalysisSessionType>> implements Popupable {

    private static final String ID_ITEMS = "items";

    public static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisReconfigureSessionPopupPanel.class);

    public RoleAnalysisReconfigureSessionPopupPanel(String id, AssignmentHolderDetailsModel<RoleAnalysisSessionType> model) {
        super(id, Model.of(model));
        initLayout();
    }

    public void initLayout() {

        RepeatingView items = new RepeatingView(ID_ITEMS);
        items.setOutputMarkupId(true);
        add(items);

        initFilteringOptionsPanel(items);

        initAnalysisOptionsPanel(items);

        initDetectionOptionsPanel(items);
    }

    private void initDetectionOptionsPanel(@NotNull RepeatingView items) {
        RoleAnalysisContainerPanel containerPanelDetectionOptions = new RoleAnalysisContainerPanel(
                items.newChildId(), getModelObject()) {
            @Contract(" -> new")
            @Override
            public @NotNull IModel<? extends PrismContainerWrapper<RoleAnalysisDetectionOptionType>> getContainerFormModel() {
                PrismContainerWrapperModel<?, RoleAnalysisDetectionOptionType> containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(getModelObject().getObjectWrapperModel(),
                        ItemPath.create(RoleAnalysisSessionType.F_DEFAULT_DETECTION_OPTION));
                containerWrapperModel.getObject().setExpanded(false);
                return containerWrapperModel;
            }

            @SuppressWarnings("rawtypes")
            protected boolean checkMandatory(@NotNull ItemWrapper itemWrapper) {
                ItemName itemName = itemWrapper.getItemName();
                if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                        || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)
                        || itemName.equivalent(RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE)) {
                    return true;
                }
                return itemWrapper.isMandatory();
            }

            @Override
            protected @NotNull ItemVisibilityHandler getVisibilityHandler() {

                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getObjectWrapperModel();
                RoleAnalysisOptionType option = resolveSessionAnalysisOption(objectWrapperModel);
                RoleAnalysisProcedureType procedureType = option.getAnalysisProcedureType();

                return wrapper -> {
                    ItemName itemName = wrapper.getItemName();

                    if (itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_ROLES_OCCUPANCY)
                            || itemName.equivalent(RoleAnalysisDetectionOptionType.F_MIN_USER_OCCUPANCY)) {

                        if (procedureType == RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                            return ItemVisibility.HIDDEN;
                        }
                    }

                    return ItemVisibility.AUTO;
                };
            }

            @Override
            protected IModel<String> getFormTitle() {
                return createStringResource("RoleAnalysisReconfigurePopupPanel.detection.option.title");
            }
        };
        containerPanelDetectionOptions.setOutputMarkupId(true);
        items.add(containerPanelDetectionOptions);
    }

    private void initAnalysisOptionsPanel(@NotNull RepeatingView items) {
        RoleAnalysisContainerPanel containerPanelAnalysisOptions = new RoleAnalysisContainerPanel(
                items.newChildId(), getModelObject()) {
            @Override
            public @NotNull IModel<? extends PrismContainerWrapper<AbstractAnalysisSessionOptionType>> getContainerFormModel() {
                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getObjectWrapperModel();
                RoleAnalysisOptionType processModeObject = resolveSessionAnalysisOption(objectWrapperModel);
                RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();

                PrismContainerWrapperModel<?, AbstractAnalysisSessionOptionType> containerWrapperModel;
                if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                    containerWrapperModel =
                            PrismContainerWrapperModel.fromContainerWrapper(getModelObject().getObjectWrapperModel(),
                                    ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
                } else {
                    containerWrapperModel =
                            PrismContainerWrapperModel.fromContainerWrapper(getModelObject().getObjectWrapperModel(),
                                    ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
                }

                containerWrapperModel.getObject().setExpanded(false);
                return containerWrapperModel;
            }

            @SuppressWarnings("rawtypes")
            protected boolean checkMandatory(@NotNull ItemWrapper itemWrapper) {
                ItemName itemName = itemWrapper.getItemName();
                return itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING);
            }

            @Contract(pure = true)
            @Override
            protected @NotNull ItemVisibilityHandler getVisibilityHandler() {
                return wrapper -> {
                    ItemName itemName = wrapper.getItemName();

                    if (itemName.equals(AbstractAnalysisSessionOptionType.F_QUERY)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE)) {
                        return ItemVisibility.HIDDEN;
                    }

                    if (itemName.equals(AbstractAnalysisSessionOptionType.F_DETAILED_ANALYSIS)) {
                        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getObjectWrapperModel();
                        RoleAnalysisOptionType analysisOption = resolveSessionAnalysisOption(objectWrapperModel);
                        if (analysisOption.getAnalysisProcedureType() != RoleAnalysisProcedureType.OUTLIER_DETECTION) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }

                    return ItemVisibility.AUTO;
                };
            }

            @Override
            protected IModel<String> getFormTitle() {
                return createStringResource("RoleAnalysisReconfigurePopupPanel.analysis.option.title");
            }
        };
        containerPanelAnalysisOptions.setOutputMarkupId(true);
        items.add(containerPanelAnalysisOptions);
    }

    private static RoleAnalysisOptionType resolveSessionAnalysisOption(
            @NotNull LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel) {
        RoleAnalysisSessionType session = objectWrapperModel.getObject().getObject().asObjectable();
        return session.getAnalysisOption();
    }

    private LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> getObjectWrapperModel() {
        return RoleAnalysisReconfigureSessionPopupPanel.this.getModelObject().getObjectWrapperModel();
    }

    private void initFilteringOptionsPanel(@NotNull RepeatingView items) {
        RoleAnalysisContainerPanel containerPanelFilteringOptions = new RoleAnalysisContainerPanel(
                items.newChildId(), getModelObject()) {

            @Override
            public @NotNull IModel<? extends PrismContainerWrapper<?>> getContainerFormModel() {
                LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getObjectWrapperModel();
                RoleAnalysisOptionType processModeObject = resolveSessionAnalysisOption(objectWrapperModel);
                RoleAnalysisProcessModeType processMode = processModeObject.getProcessMode();
                PrismContainerWrapperModel<RoleAnalysisSessionType, Containerable> containerWrapperModel;
                if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                    containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                            ItemPath.create(RoleAnalysisSessionType.F_ROLE_MODE_OPTIONS));
                } else {
                    containerWrapperModel = PrismContainerWrapperModel.fromContainerWrapper(objectWrapperModel,
                            ItemPath.create(RoleAnalysisSessionType.F_USER_MODE_OPTIONS));
                }
                containerWrapperModel.getObject().setExpanded(true);
                return containerWrapperModel;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected boolean checkMandatory(@NotNull ItemWrapper itemWrapper) {
                ItemName itemName = itemWrapper.getItemName();
                return itemName.equivalent(AbstractAnalysisSessionOptionType.F_QUERY)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)
                        || itemName.equivalent(AbstractAnalysisSessionOptionType.F_PROPERTIES_RANGE);
            }

            @Contract(pure = true)
            @Override
            protected @NotNull ItemVisibilityHandler getVisibilityHandler() {
                return wrapper -> {
                    ItemName itemName = wrapper.getItemName();
                    if (itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_PROPERTIES_OVERLAP)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_MIN_MEMBERS_COUNT)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_SIMILARITY_THRESHOLD)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_CLUSTERING_ATTRIBUTE_SETTING)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_USER_ANALYSIS_ATTRIBUTE_SETTING)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_MAX_DISTANCE)
                            || itemName.equals(AbstractAnalysisSessionOptionType.F_DETAILED_ANALYSIS)
                    ) {
                        return ItemVisibility.HIDDEN;
                    }

                    if (itemName.equals(AbstractAnalysisSessionOptionType.F_IS_INDIRECT)) {
                        LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> objectWrapperModel = getObjectWrapperModel();
                        RoleAnalysisOptionType option = resolveSessionAnalysisOption(objectWrapperModel);
                        RoleAnalysisProcessModeType processMode = option.getProcessMode();
                        if (!processMode.equals(RoleAnalysisProcessModeType.USER)) {
                            return ItemVisibility.HIDDEN;
                        } else {
                            return ItemVisibility.AUTO;

                        }
                    }

                    return ItemVisibility.AUTO;
                };
            }

            @Override
            protected IModel<String> getFormTitle() {
                return createStringResource("RoleAnalysisReconfigurePopupPanel.filtering.option.title");
            }
        };
        containerPanelFilteringOptions.setOutputMarkupId(true);
        items.add(containerPanelFilteringOptions);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 30;
    }

    @Override
    public int getHeight() {
        return 20;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("RoleAnalysisReconfigurePopupPanel.title");
    }

    @Override
    public @NotNull Component getTitleComponent() {

        ModalFooterPanel title = new ModalFooterPanel(ID_TITLE, Model.of("footer")) {
            @Override
            protected void addComponentButton(RepeatingView repeatingView) {

                IconWithLabel iconWithLabel = new IconWithLabel(repeatingView.newChildId(),
                        createStringResource("RoleAnalysisReconfigurePopupPanel.title")) {
                    @Override
                    protected String getIconCssClass() {
                        return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
                    }
                };
                iconWithLabel.setOutputMarkupId(true);

                repeatingView.add(iconWithLabel);

                AjaxLink<?> ajaxLinkPanel = new AjaxLink<>(repeatingView.newChildId()) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RoleAnalysisReconfigureSessionPopupPanel.this.getPageBase().hideMainPopup(target);
                    }
                };

                ajaxLinkPanel.setOutputMarkupId(true);
                ajaxLinkPanel.add(AttributeAppender.append("class", "fas fa-minus"));

                repeatingView.add(ajaxLinkPanel);

            }
        };

        title.add(AttributeAppender.replace("class", "col-12"));

        return title;
    }

    @Override
    public @NotNull Component getFooter() {
        ModalFooterPanel footer = new ModalFooterPanel(ID_FOOTER, Model.of("footer")) {
            @Override
            protected void addComponentButton(RepeatingView repeatingView) {
                createCancelButton(repeatingView);
                createSaveButton(repeatingView);
            }
        };
        footer.add(AttributeAppender.replace("class", "card-footer pt-1 pb-1 "));
        return footer;
    }

    private void createSaveButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_ICON_SAVE,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton saveButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("RoleAnalysisReconfigurePopupPanel.save")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                finalSubmitPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        saveButton.titleAsLabel(true);
        saveButton.setOutputMarkupId(true);
        saveButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        repeatingView.add(saveButton);
    }

    private void createCancelButton(@NotNull RepeatingView repeatingView) {
        AjaxLinkPanel cancelButton = new AjaxLinkPanel(repeatingView.newChildId(),
                createStringResource("RoleAnalysisReconfigurePopupPanel.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisReconfigureSessionPopupPanel.this.getPageBase().hideMainPopup(target);
            }
        };
        cancelButton.setOutputMarkupId(true);
        repeatingView.add(cancelButton);
    }

    private void finalSubmitPerform(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("Reconfigure and rebuild role analysis session");
        OperationResult result = task.getResult();

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        Collection<ObjectDelta<? extends ObjectType>> deltas;
        try {
            deltas = getModelObject().collectDeltas(result);

            new ObjectChangesExecutorImpl().executeChanges(deltas, false, task, result, target);

            String sessionOid = getModelObject().getObjectType().getOid();

            PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                    .getSessionTypeObject(sessionOid, task, result);

            if (sessionTypeObject != null) {

                roleAnalysisService.deleteSessionTask(sessionTypeObject.getOid(), task, result);

                ModelInteractionService modelInteractionService = getPageBase().getModelInteractionService();
                roleAnalysisService.executeClusteringTask(
                        modelInteractionService, sessionTypeObject, task, result);
            }
        } catch (Throwable e) {
            LoggingUtils.logException(LOGGER, "Couldn't process clustering", e);
            result.recordFatalError(
                    createStringResource("RoleAnalysisSessionWizardPanel.message.clustering.error").getString()
                    , e);
        }

        setResponsePage(PageRoleAnalysis.class);
        ((PageBase) getPage()).showResult(result);
        target.add(getFeedbackPanel());
    }
}
