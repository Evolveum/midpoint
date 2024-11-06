/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconWithLabelPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTablePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.util.ObjectReferenceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumnPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Popup panel that displays cert. case activities and responses
 */
public class CertResponseDetailsPanel extends BasePanel<PrismContainerValueWrapper<AccessCertificationCaseType>> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = CertResponseDetailsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REVIEWER_OBJECT = DOT_CLASS + "loadReviewerObject";

    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_ACTIONS_PANEL = "actionsPanel";
    private static final String ID_NO_ACTIONS_LABEL = "noActionsLabel";

    int stageNumber;
    int iteration;

    public CertResponseDetailsPanel(String id, IModel<PrismContainerValueWrapper<AccessCertificationCaseType>> model, int stageNumber) {
        super(id, model);
        this.stageNumber = stageNumber;
        this.iteration = getModelObject().getRealValue().getIteration();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Label label = new Label(ID_TITLE, createStringResource("ResponseViewPopup.title"));
        label.setOutputMarkupId(true);
        add(label);

        addDetailsPanel();

        addActionsPanel();
    }

    private void addDetailsPanel() {
        IModel<DisplayType> detailsPanelDisplayModel = createDetailsPanelDisplayModel();
        IModel<List<DetailsTableItem>> detailsModel = createResponseDetailsPanelModel();
        DetailsTablePanel detailsPanel = new DetailsTablePanel(ID_DETAILS_PANEL, detailsPanelDisplayModel, detailsModel);
        detailsPanel.setOutputMarkupId(true);
        add(detailsPanel);
    }

    private IModel<DisplayType> createDetailsPanelDisplayModel() {
        return DisplayType::new;
    }

    private IModel<List<DetailsTableItem>> createResponseDetailsPanelModel() {
        return () -> {
            List<DetailsTableItem> list = new ArrayList<>();
            list.add(new DetailsTableItem(createStringResource("WorkItemsPanel.object")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    return new ObjectReferenceColumnPanel(id, Model.of(getModelObject().getRealValue().getObjectRef()));
                }
            });
            list.add(new DetailsTableItem(createStringResource("ObjectType.description")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    PrismObject<? extends ObjectType> caseObject = WebModelServiceUtils.loadObject(getModelObject().getRealValue().getObjectRef(),
                            getPageBase());
                    String description = caseObject != null ? caseObject.asObjectable().getDescription() : "";
                    return new Label(id, description);
                }
            });
            list.add(new DetailsTableItem(createStringResource("WorkItemsPanel.target")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    return new ObjectReferenceColumnPanel(id, Model.of(getModelObject().getRealValue().getTargetRef()));
                }
            });
            list.add(new DetailsTableItem(createStringResource("ObjectType.description")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    PrismObject<? extends ObjectType> caseObject = WebModelServiceUtils.loadObject(getModelObject().getRealValue().getTargetRef(),
                            getPageBase());
                    String description = caseObject != null ? caseObject.asObjectable().getDescription() : "";
                    return new Label(id, description);
                }
            });
            list.add(new DetailsTableItem(createStringResource("PageCertCampaign.statistics.response")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    AccessCertificationResponseType response = OutcomeUtils.fromUri(getModelObject().getRealValue().getCurrentStageOutcome());
                    DisplayType responseDisplayType = new CertificationItemResponseHelper(response).getResponseDisplayType();

                    CompositedIcon icon = new CompositedIconBuilder()
                            .setBasicIcon(responseDisplayType.getIcon(), IconCssStyle.IN_ROW_STYLE)
                            .build();

                    CompositedIconWithLabelPanel iconPanel =
                            new CompositedIconWithLabelPanel(id, Model.of(icon), Model.of(responseDisplayType));
                    iconPanel.add(AttributeModifier.replace("class", "d-flex flex-wrap gap-2"));
                    return iconPanel;
                }
            });
            return list;

        };
    }

    private void addActionsPanel() {
        DisplayType titleDisplay = new DisplayType().label("CertResponseDetailsPanel.activity");

        IModel<List<ChatMessageItem>> actionsModel = createActionsModel();
        ChatPanel actionsPanel = new ChatPanel(ID_ACTIONS_PANEL, Model.of(titleDisplay), actionsModel);
        actionsPanel.setOutputMarkupId(true);
        add(actionsPanel);
    }

    private IModel<List<ChatMessageItem>> createActionsModel() {
        return () -> {
            List<ChatMessageItem> list = new ArrayList<>();

            AccessCertificationCaseType certCase = getModelObject().getRealValue();
            List<AccessCertificationWorkItemType> workItems = certCase.getWorkItem();
            workItems
                    .stream()
                    .filter(workItem -> workItem.getStageNumber() == stageNumber && workItem.getIteration() == iteration)
                    .forEach(workItem -> {
                        if (workItem.getStageNumber() != null && workItem.getStageNumber() == stageNumber) {
                            list.add(createChatMessageItem(workItem));
                        }
                    });
            if (list.isEmpty()) {
                list.add(new ChatMessageItem(Model.of(new DisplayType().label("CertResponseDetailsPanel.noActionsLabel")),
                        Model.of("")));
            }
            return list;
        };
    }

    private ChatMessageItem createChatMessageItem(AccessCertificationWorkItemType workItem) {
        ObjectReferenceType userRef = getPerformerOrAssigneeRef(workItem);
        if (userRef == null || StringUtils.isEmpty(userRef.getOid())) {
            return new ChatMessageItem(Model.of(new DisplayType().label("CertResponseDetailsPanel.unavailablePerformer")),
                    Model.of());
        }

        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_REVIEWER_OBJECT);

        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();

        PrismObject<UserType> performer = WebModelServiceUtils.loadObject(UserType.class, userRef.getOid(), options,
                getParentPage(), task, task.getResult());
        return new ChatMessageItem(createMessageDisplayTypeModel(performer, workItem), createMessageTextModel(workItem),
                createImageResourceModel(performer));
    }

    private ObjectReferenceType getPerformerOrAssigneeRef(AccessCertificationWorkItemType workItem) {
        ObjectReferenceType performerRef = workItem.getPerformerRef();
        if (performerRef != null && StringUtils.isNotEmpty(performerRef.getOid())) {
            return performerRef;
        }
        List<ObjectReferenceType> assignees = workItem.getAssigneeRef();
        if (assignees != null && !assignees.isEmpty()) {
            return assignees.get(0);    //todo can be many assignees?
        }
        return null;
    }

    private IModel<DisplayType> createMessageDisplayTypeModel(PrismObject<UserType> performer,
            AccessCertificationWorkItemType workItem) {
        if (performer == null) {
            return Model.of(new DisplayType()
                    .label("CertResponseDetailsPanel.unavailablePerformer")
                    .icon(new IconType()
                            .cssClass("fa fa-user-circle")));
        }

        String label = generateMessageTitle(performer, workItem);
        String description = getMessageDescription(workItem);
        return () -> new DisplayType()
                .label(label)
                .help(description)
                .icon(new IconType()
                        .cssClass("fa fa-user-circle"));
    }

    private String generateMessageTitle(PrismObject<UserType> performer, AccessCertificationWorkItemType workItem) {
        if (hasNoResponse(workItem)) {
            String assigneeName = WebComponentUtil.getDisplayNameOrName(performer);
            return createStringResource("CertResponseDetailsPanel.waitingForResponse", assigneeName).getString();
        }
        String resolverName = WebComponentUtil.getDisplayNameOrName(performer);
        AccessCertificationResponseType response = OutcomeUtils.fromUri(workItem.getOutput().getOutcome());
        String responseName = LocalizationUtil.translateEnum(response);
        return createStringResource("CertResponseDetailsPanel.messageTitle", resolverName, responseName).getString();
    }

    private boolean hasNoResponse(AccessCertificationWorkItemType workItem) {
        return workItem.getOutput() == null || workItem.getOutput().getOutcome() == null;
    }

    private String getMessageDescription(AccessCertificationWorkItemType workItem) {
        return WebComponentUtil.getLocalizedDate(workItem.getOutputChangeTimestamp(), DateLabelComponent.SHORT_SHORT_STYLE);

    }

    private IModel<String> createMessageTextModel(AccessCertificationWorkItemType workItem) {
        return workItem.getOutput() != null ? Model.of(workItem.getOutput().getComment()) : Model.of("");
    }

    private IModel<IResource> createImageResourceModel(PrismObject<UserType> performer) {
        return () -> {
            if (performer == null) {
                return null;
            }
            return WebComponentUtil.createJpegPhotoResource(performer);
        };
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CertResponseDetailsPanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
