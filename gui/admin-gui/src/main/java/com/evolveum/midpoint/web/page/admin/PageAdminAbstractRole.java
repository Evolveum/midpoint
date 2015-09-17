package com.evolveum.midpoint.web.page.admin;

import java.util.List;

import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.assignment.AssignmentTableDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public abstract class PageAdminAbstractRole <T extends AbstractRoleType> extends PageAdminFocus<T> {
	
	

	private static final String ID_INDUCEMENTS_TABLE = "inducementsPanel";
	
	@Override
	protected void prepareFocusDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
super.prepareFocusDeltaForModify(focusDelta);
		
		// handle inducements
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
		
		PrismContainerDefinition inducementDef = objectDefinition
				.findContainerDefinition(OrgType.F_INDUCEMENT);
		AssignmentTablePanel inducementPanel = (AssignmentTablePanel) get(createComponentPath(ID_MAIN_FORM,
				ID_INDUCEMENTS_TABLE));
		inducementPanel.handleAssignmentDeltas(focusDelta, inducementDef, AbstractRoleType.F_INDUCEMENT);
	}
	
	@Override
	protected void prepareFocusForAdd(PrismObject<T> focus) throws SchemaException {
		super.prepareFocusForAdd(focus);
		// handle inducements
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition orgDef = registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
		PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(AbstractRoleType.F_INDUCEMENT);
		AssignmentTablePanel inducementPanel = (AssignmentTablePanel) get(createComponentPath(ID_MAIN_FORM,
				ID_INDUCEMENTS_TABLE));
		inducementPanel.handleAssignmentsWhenAdd(focus, inducementDef, focus.asObjectable()
				.getInducement());

	}
	
	@Override
	protected void initCustomLayout(Form mainForm) {
		AssignmentTablePanel inducements = initInducements();
		mainForm.add(inducements);
		
	}


	private AssignmentTablePanel initInducements() {
		AssignmentTablePanel inducements = new AssignmentTablePanel(ID_INDUCEMENTS_TABLE,
				new Model<AssignmentTableDto>(), createStringResource("PageOrgUnit.title.inducements")) {

			@Override
			public List<AssignmentType> getAssignmentTypeList() {
				return ((AbstractRoleType) getFocusWrapper().getObject().asObjectable()).getInducement();
			}

			@Override
			public String getExcludeOid() {
				return getFocusWrapper().getObject().asObjectable().getOid();
			}
		};
		return inducements;
	}
}
