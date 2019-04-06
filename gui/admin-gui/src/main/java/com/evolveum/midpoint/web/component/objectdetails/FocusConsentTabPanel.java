package com.evolveum.midpoint.web.component.objectdetails;

import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.ObjectWrapperOld;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.assignment.GdprAssignmentPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

public class FocusConsentTabPanel<F extends FocusType> extends AbstractObjectTabPanel<F>{

	private static final long serialVersionUID = 1L;

	private static final String ID_ROLES = "roles";

//	private LoadableModel<List<AssignmentType>> consentsModel;

	public FocusConsentTabPanel(String id, Form<PrismObjectWrapper<F>> mainForm, LoadableModel<PrismObjectWrapper<F>> objectWrapperModel) {
		super(id, mainForm, objectWrapperModel);

		initLayout();
	}

	private void initLayout() {
		GdprAssignmentPanel consentRoles =  new GdprAssignmentPanel(ID_ROLES,
				new PrismContainerWrapperModel<F, AssignmentType>(getObjectWrapperModel(), ItemPath.create(FocusType.F_ASSIGNMENT)));
		add(consentRoles);
		consentRoles.setOutputMarkupId(true);

	}
}
