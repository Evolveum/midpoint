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
