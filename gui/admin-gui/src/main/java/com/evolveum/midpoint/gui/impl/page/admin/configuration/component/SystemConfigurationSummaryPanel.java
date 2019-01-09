package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_4.SystemConfigurationType;

public class SystemConfigurationSummaryPanel extends ObjectSummaryPanel<SystemConfigurationType> {

	private static final long serialVersionUID = 1L;

	public SystemConfigurationSummaryPanel(String id, Class type, IModel<PrismObject<SystemConfigurationType>> model, ModelServiceLocator serviceLocator) {
		super(id, type, model, serviceLocator);
		initLayoutCommon(serviceLocator);
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {
		return null;
	}

	@Override
	protected String getBoxAdditionalCssClass() {
		return null;
	}
	
	@Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
