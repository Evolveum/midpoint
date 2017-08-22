package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.TypedAssignablePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class GdprAssignmentPanel extends AbstractRoleAssignmentPanel{

	private static final long serialVersionUID = 1L;
	
	public GdprAssignmentPanel(String id, IModel<List<AssignmentDto>> assignmentsModel, PageBase pageBase) {
		super(id, assignmentsModel, pageBase);
	}

	
	@Override
	protected List<IColumn<AssignmentDto, String>> initColumns() {
		List<IColumn<AssignmentDto, String>> columns = new ArrayList<>();
		columns.add(new PropertyColumn<>(createStringResource("AssignmentType.lifecycle"), AssignmentDto.F_VALUE + "." + AssignmentType.F_LIFECYCLE_STATE.getLocalPart()));
		
		columns.add(new CheckBoxColumn<AssignmentDto>(createStringResource("AssignmnetType.accepted")) {
			
			private static final long serialVersionUID = 1L;
			@Override
			protected IModel<Boolean> getEnabled() {
				return Model.of(Boolean.FALSE);
			}
			
			@Override
			protected IModel<Boolean> getCheckBoxValueModel(IModel<AssignmentDto> rowModel) {
				return new AbstractReadOnlyModel<Boolean>() {
					
					private static final long serialVersionUID = 1L;

					@Override
					public Boolean getObject() {
						AssignmentDto assignmentDto = rowModel.getObject();
						AssignmentType assignmentType = assignmentDto.getAssignment();
						if (assignmentType.getLifecycleState() == null) {
							return Boolean.FALSE;
						}
						
						if (assignmentType.getLifecycleState().equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
							return Boolean.TRUE;
						}
						
						return Boolean.FALSE;
					}
				};
			}
			
		
		});
		
		return columns;
	}

	@Override
	protected boolean isRelationVisible() {
		return false;
	}
	
}
