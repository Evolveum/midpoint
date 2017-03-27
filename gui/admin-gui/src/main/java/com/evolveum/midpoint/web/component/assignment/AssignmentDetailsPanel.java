package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar
 */
public class AssignmentDetailsPanel extends BasePanel<AssignmentEditorDto> {
    private static final String ID_DETAILS_PANEL = "detailsPanel";

    public AssignmentDetailsPanel(String id) {
        super(id);
    }

    public AssignmentDetailsPanel(String id, IModel<AssignmentEditorDto> model, PageBase pageBase) {
        super(id, model);
        initLayout(pageBase);
    }

    private void initLayout(PageBase pageBase) {
        setOutputMarkupId(true);
        ShoppingCartEditorPanel assignmentDetailsPanel = new ShoppingCartEditorPanel(ID_DETAILS_PANEL, getModel(), pageBase);
        assignmentDetailsPanel.setOutputMarkupId(true);
        add(assignmentDetailsPanel);

    }
}
