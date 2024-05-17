/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTablePanel;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumnPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class CertResponseDetailsPanel extends BasePanel<AccessCertificationCaseType> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS_PANEL = "detailsPanel";
    private static final String ID_ACTIONS_PANEL = "actionsPanel";

    int stageNumber;

    public CertResponseDetailsPanel(String id, IModel<AccessCertificationCaseType> model, int stageNumber) {
        super(id, model);
        this.stageNumber = stageNumber;
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
        IModel<List<DetailsTableItem>> detailsModel = createDetailsPanelModel();
        DetailsTablePanel detailsPanel = new DetailsTablePanel(ID_DETAILS_PANEL, detailsPanelDisplayModel, detailsModel);
        detailsPanel.setOutputMarkupId(true);
        add(detailsPanel);
    }

    private IModel<DisplayType> createDetailsPanelDisplayModel() {
        return () -> {
            //todo
            DisplayType display = new DisplayType()
                    .label("John Doe")
                    .help("Reviewer");
            return display;
        };
    }

    private IModel<List<DetailsTableItem>> createDetailsPanelModel() {
        return () -> {
            List<DetailsTableItem> list = new ArrayList<>();
            list.add(new DetailsTableItem(createStringResource("WorkItemsPanel.object")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public Component createValueComponent(String id) {
                    return new ObjectReferenceColumnPanel(id, Model.of(getModelObject().getObjectRef()));
                }
            });
            return list;

        };
    }

    private void addActionsPanel() {
        DisplayType titleDisplay = new DisplayType()
                .label("CertResponseDetailsPanel.activity");

        IModel<List<ChatMessageItem>> actionsModel = createActionsModel();
        ChatPanel actionsPanel = new ChatPanel(ID_ACTIONS_PANEL, Model.of(titleDisplay), actionsModel);
        actionsPanel.setOutputMarkupId(true);
        add(actionsPanel);
    }

    private IModel<List<ChatMessageItem>> createActionsModel() {
        return () -> {
            List<ChatMessageItem> list = new ArrayList<>();

            AccessCertificationCaseType certCase = getModelObject();
            List<AccessCertificationWorkItemType> workItems = certCase.getWorkItem();
            workItems.forEach(workItem -> {
                if (workItem.getStageNumber() != null && workItem.getStageNumber() == stageNumber) {
                    list.add(new ChatMessageItem(
                            createMessageDisplayTypeModel(workItem),
                            createMessageTextModel(workItem)));
                }
            });
            return list;
        };
    }

    private IModel<DisplayType> createMessageDisplayTypeModel(AccessCertificationWorkItemType workItem) {
        return () -> {
            DisplayType display = new DisplayType()
                    .label(WebModelServiceUtils.resolveReferenceName(workItem.getPerformerRef(), getPageBase()))
                    .help(WebComponentUtil.getLocalizedDate(workItem.getOutputChangeTimestamp(),
                            DateLabelComponent.SHORT_SHORT_STYLE));
            return display;
        };
    }

    private IModel<String> createMessageTextModel(AccessCertificationWorkItemType workItem) {
        return workItem.getOutput() != null ? Model.of(workItem.getOutput().getComment()) : Model.of("");
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 600;
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
