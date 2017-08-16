package com.evolveum.midpoint.web.component.assignment;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

public class GdprAssignmentPanel extends AssignmentPanel{

	private static final long serialVersionUID = 1L;
	
	public GdprAssignmentPanel(String id, IModel<List<AssignmentDto>> assignmentsModel, PageBase pageBase) {
		super(id, assignmentsModel, pageBase);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initPaging() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected AbstractAssignmentDetailsPanel createDetailsPanel(String idAssignmentDetails, IModel<AssignmentDto> model,
			PageBase parentPage) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected TableId getTableId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getItemsPerPage() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected List<AssignmentDto> getModelList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected List<IColumn<AssignmentDto, String>> initColumns() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void newAssignmentClickPerformed(AjaxRequestTarget target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected ObjectQuery createObjectQuery() {
		// TODO Auto-generated method stub
		return null;
	}
 
	
}
