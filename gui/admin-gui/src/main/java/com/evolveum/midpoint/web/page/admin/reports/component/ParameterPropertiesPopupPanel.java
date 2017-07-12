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
package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.CheckBoxPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.dto.JasperReportParameterPropertiesDto;

public class ParameterPropertiesPopupPanel extends BasePanel<JasperReportParameterPropertiesDto> implements Popupable {

  private static final long serialVersionUID = 1L;
  
  private static final String ID_KEY = "propertyKey";
  private static final String ID_LABEL = "propertyLabel";
  private static final String ID_TARGET_TYPE = "propertyTargetType";
//  private static final String ID_MULTIVALUE = "propertyMultivalue";
  
  private static final String ID_BUTTON_UPDATE = "update";

//  private static final Trace LOGGER = TraceManager.getTrace(ParameterPropertiesPopupPanel.class);

  public ParameterPropertiesPopupPanel(String id, IModel<JasperReportParameterPropertiesDto> model) {
		super(id, model);
		initLayout();
	}
  
  private void initLayout() {
	  
	  addTextPanel(ID_KEY, "key");
	  addTextPanel(ID_LABEL, "label");
	  addTextPanel(ID_TARGET_TYPE, "targetType");
//	  CheckBoxPanel multivalue = new CheckBoxPanel(ID_MULTIVALUE, new PropertyModel<>(getModel(), "multivalue"), Model.of(Boolean.TRUE));
//	  add(multivalue);
	  
	  AjaxButton update = new AjaxButton(ID_BUTTON_UPDATE) {
		
		private static final long serialVersionUID = 1L;

		@Override
		public void onClick(AjaxRequestTarget target) {
			getPageBase().hideMainPopup(target);
			IModel<JasperReportParameterPropertiesDto> model = ParameterPropertiesPopupPanel.this.getModel();
			updateProperties(model.getObject(), target);
		}
	};
	
	add(update);
	  
  }
  
  private void addTextPanel(String id, String expression){
	  TextPanel<String> keyPanel = new TextPanel<>(id, new PropertyModel<>(getModel(), expression));
	  keyPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
	  add(keyPanel);
  }
  
  protected void updateProperties(JasperReportParameterPropertiesDto properties, AjaxRequestTarget target) {
	  
  }
  
  @Override
	public int getWidth() {
		return 800;
	}

	@Override
	public int getHeight() {
		return 450;
	}

	@Override
	public StringResourceModel getTitle() {
		return createStringResource("JasperReportParameterProperties.title");
	}

	@Override
	public Component getComponent() {
		return this;
	}
    
}
