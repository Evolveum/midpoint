package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;

public class AbstractRoleAssignmentDetailsPanel<F extends FocusType> extends AbstractAssignmentDetailsPanel<F> {

	private static final long serialVersionUID = 1L;

	private static List hiddenItems = new ArrayList<>();

	static  {
			hiddenItems.add(AssignmentType.F_POLICY_RULE);
	};

	public AbstractRoleAssignmentDetailsPanel(String id, Form<?> form, IModel<ContainerValueWrapper<AssignmentType>> assignmentModel) {
		super(id, form, assignmentModel);
	}

	
	@Override
	protected List<ItemPath> collectContainersToShow() {
		List<ItemPath> pathsToShow = new ArrayList<>();
		if (ConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_CONSTRUCTION));
		}
		
		if (PersonaConstructionType.COMPLEX_TYPE.equals(AssignmentsUtil.getTargetType(getModelObject().getContainerValue().getValue()))) {
			pathsToShow.add(getAssignmentPath().append(AssignmentType.F_PERSONA_CONSTRUCTION));
		}
		return pathsToShow;
	}

}
