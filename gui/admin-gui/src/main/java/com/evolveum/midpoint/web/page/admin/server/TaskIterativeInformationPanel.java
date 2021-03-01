/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.web.component.box.BasicInfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskIterativeProgressType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskPartItemsProcessingInformationType;

/**
 * TODO MID-6850 (whole class)
 */
public class TaskIterativeInformationPanel extends BasePanel<IterativeTaskInformationType> {

    private static final String ID_PROGRESS = "progress";

    private static final String ID_PARTS = "parts";
    private static final String ID_SUCESS_ITEM = "successItem";
    private static final String ID_FAILED_ITEM = "failedItem";
    private static final String ID_SKIPPED_ITEM = "skippedItem";

    private static final String ID_CURRENT_ITEMS = "currentItems";
    private static final String ID_CURRENT_ITEM = "currentItem";

    public TaskIterativeInformationPanel(String id, IModel<IterativeTaskInformationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayoutNew();
    }

    private BasicInfoBoxPanel createInfoBoxPanel(IModel<InfoBoxType> boxModel, String boxId) {

        BasicInfoBoxPanel infoBoxPanel = new BasicInfoBoxPanel(boxId, boxModel);
        infoBoxPanel.setOutputMarkupId(true);
        infoBoxPanel.add(new VisibleBehaviour(() -> boxModel.getObject() != null));
        return infoBoxPanel;
    }

    private void initLayoutNew() {

        ListView<IterativeTaskPartItemsProcessingInformationType> partsView = new ListView<>(ID_PARTS, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_PART.getLocalPart())) {

            @Override
            protected void populateItem(ListItem<IterativeTaskPartItemsProcessingInformationType> item) {
                IModel<TaskIterativeProgressType> progressModel = createProgressModel(item);
                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_PROGRESS), ID_PROGRESS));

                ListView<InfoBoxType> currentItems = new ListView<>(ID_CURRENT_ITEMS, new PropertyModel<>(progressModel, TaskIterativeProgressType.F_CURRENT_ITEMS)) {

                    @Override
                    protected void populateItem(ListItem<InfoBoxType> item) {
                        item.add(createInfoBoxPanel(item.getModel(), ID_CURRENT_ITEM));
                    }
                };
                currentItems.setOutputMarkupId(true);
                item.add(currentItems);

                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_SUCCESS_BOX), ID_SUCESS_ITEM));
                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_FAILED_BOX), ID_FAILED_ITEM));
                item.add(createInfoBoxPanel(new PropertyModel<>(progressModel, TaskIterativeProgressType.F_SKIP_BOX), ID_SKIPPED_ITEM));

            }
        };
        partsView.setOutputMarkupId(true);
        add(partsView);
    }

    protected IModel<TaskIterativeProgressType> createProgressModel(ListItem<IterativeTaskPartItemsProcessingInformationType> item) {
        throw new UnsupportedOperationException("Not supported. Should be impelemnted in panel, which uses it");
    }

}
