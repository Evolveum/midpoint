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
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
		GdprAssignmentPanel consentRoles =  new GdprAssignmentPanel(ID_ROLES,
				new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), new ItemPath(FocusType.F_ASSIGNMENT)));
		add(consentRoles);
		consentRoles.setOutputMarkupId(true);

	}
}
