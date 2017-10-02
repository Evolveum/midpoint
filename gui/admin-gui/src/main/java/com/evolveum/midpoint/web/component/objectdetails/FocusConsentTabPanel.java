package com.evolveum.midpoint.web.component.objectdetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentDto;
import com.evolveum.midpoint.web.component.assignment.GdprAssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class FocusConsentTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F>{

	private static final long serialVersionUID = 1L;

	private static final String ID_ROLES = "roles";

	private LoadableModel<List<AssignmentType>> consentsModel;

	public FocusConsentTabPanel(String id, Form<ObjectWrapper<F>> mainForm, LoadableModel<ObjectWrapper<F>> objectWrapperModel,
			PageBase pageBase) {
		super(id, mainForm, objectWrapperModel, pageBase);

		initLayout();
	}

	private void initLayout() {
		ContainerWrapper<AssignmentType> assignmentsContainerWrapper = getObjectWrapper().findContainerWrapper(new ItemPath(FocusType.F_ASSIGNMENT));

		GdprAssignmentPanel consentRoles =  new GdprAssignmentPanel(ID_ROLES, getConsentsModel(assignmentsContainerWrapper), assignmentsContainerWrapper);
		add(consentRoles);
		consentRoles.setOutputMarkupId(true);

	}

	private IModel<List<ContainerValueWrapper<AssignmentType>>> getConsentsModel(ContainerWrapper<AssignmentType> assignmentsContainerWrapper){
		List<ContainerValueWrapper<AssignmentType>> consentsList = new ArrayList<>();
		assignmentsContainerWrapper.getValues().forEach(a -> {
			if (isConsentAssignment(a.getContainerValue().getValue())){
				consentsList.add(a);
			}
		});
//		Collections.sort(consentsList);
		return Model.ofList(consentsList);
	}

	private boolean isConsentAssignment(AssignmentType assignment) {
		return assignment.getTargetRef() != null && QNameUtil.match(assignment.getTargetRef().getRelation(), SchemaConstants.ORG_CONSENT);
	}
}
