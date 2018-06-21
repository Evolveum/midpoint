package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.assignment.GdprAssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

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
		GdprAssignmentPanel consentRoles =  new GdprAssignmentPanel(ID_ROLES,
				new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), new ItemPath(FocusType.F_ASSIGNMENT)));
		add(consentRoles);
		consentRoles.setOutputMarkupId(true);

	}
}
