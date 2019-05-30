package com.evolveum.midpoint.web.page.admin.archetype;

import javax.xml.namespace.QName;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class ArchetypeSummaryPanel extends FocusSummaryPanel<ArchetypeType>{

	private static final long serialVersionUID = 1L;
	
	public ArchetypeSummaryPanel(String id, IModel<ArchetypeType> model,
			ModelServiceLocator serviceLocator) {
		super(id, ArchetypeType.class, model, serviceLocator);
	}


	@Override
	protected QName getDisplayNamePropertyName() {
		return ArchetypeType.F_DISPLAY_NAME;
	}

	@Override
	protected QName getTitlePropertyName() {
		return ArchetypeType.F_IDENTIFIER;
	}
	
	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return "summary-panel-user";
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return "summary-panel-user";
	}

}
