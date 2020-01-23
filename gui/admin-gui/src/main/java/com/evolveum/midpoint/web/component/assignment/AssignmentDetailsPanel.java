/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class AssignmentDetailsPanel extends BasePanel<AssignmentEditorDto> {
    private static final String ID_DETAILS_PANEL = "detailsPanel";

    public AssignmentDetailsPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);
        ShoppingCartEditorPanel assignmentDetailsPanel = new ShoppingCartEditorPanel(ID_DETAILS_PANEL, getModel());
        assignmentDetailsPanel.setOutputMarkupId(true);
        add(assignmentDetailsPanel);

    }
}
