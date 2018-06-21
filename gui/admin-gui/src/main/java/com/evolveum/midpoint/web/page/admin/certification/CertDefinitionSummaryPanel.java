/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
public class CertDefinitionSummaryPanel extends ObjectSummaryPanel<AccessCertificationDefinitionType> {
	private static final long serialVersionUID = 1L;

	public CertDefinitionSummaryPanel(String id,
			IModel<PrismObject<AccessCertificationDefinitionType>> model, ModelServiceLocator serviceLocator) {
		super(id, AccessCertificationDefinitionType.class, model, serviceLocator);
		initLayoutCommon(serviceLocator);
	}

	@Override
	protected String getIconCssClass() {
		return GuiStyleConstants.CLASS_OBJECT_CERT_DEF_ICON;
	}

	@Override
	protected String getIconBoxAdditionalCssClass() {		// TODO
		return "summary-panel-task"; // TODO
	}

	@Override
	protected String getBoxAdditionalCssClass() {			// TODO
		return "summary-panel-task"; // TODO
	}

	@Override
	protected boolean isIdentifierVisible() {
		return false;
	}
}
