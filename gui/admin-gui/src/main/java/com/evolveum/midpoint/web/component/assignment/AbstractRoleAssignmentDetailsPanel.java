package com.evolveum.midpoint.web.component.assignment;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;

public class AbstractRoleAssignmentDetailsPanel extends AbstractAssignmentDetailsPanel {
	
	private static final long serialVersionUID = 1L;
	
	public AbstractRoleAssignmentDetailsPanel(String id, IModel<AssignmentDto> assignmentModel, PageBase pageBase) {
		super(id, assignmentModel, pageBase);
	}

        
//		protected IModel<String> createHeaderModel(){
//	        return Model.of(AssignmentsUtil.getName(getModelObject().getAssignment(), pageBase));
//	    }
//
//	    protected IModel<String> createImageModel(){
//	        return Model.of(GuiStyleConstants.CLASS_ICON_ASSIGNMENTS);
//	    }
}
