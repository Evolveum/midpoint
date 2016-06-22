/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.resources.component;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

/**
 * 
 * @author katkav
 *
 */
public class TestConnectionResultPanel extends BasePanel<List<OpResult>> implements Popupable{
	
	public TestConnectionResultPanel(String id, IModel<List<OpResult>> model, Page parentPage) {
		super(id, model);
		initLayout(parentPage);
	}

	private static final long serialVersionUID = 1L;
	
	private static final String ID_RESULT = "result";
	private static final String ID_OK = "ok";

	

	private void initLayout(Page parentPage) {
		RepeatingView resultView = new RepeatingView(ID_RESULT);

		for (OpResult result : getModel().getObject()) {
			OperationResultPanel resultPanel = new OperationResultPanel(resultView.newChildId(), new Model<>(result), parentPage);
			resultPanel.setOutputMarkupId(true);
			resultView.add(resultPanel);
		}

		resultView.setOutputMarkupId(true);
		add(resultView);
		
		AjaxButton ok = new AjaxButton(ID_OK) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				getPageBase().hideMainPopup(target);
				okPerformed(target);
				
			}
			
		};
		
		add(ok);
	}
	
	protected void okPerformed(AjaxRequestTarget target) {
		
	}

	@Override
	public int getWidth() {
		return 600;
	}

	@Override
	public int getHeight() {
		return 400;
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("TestConnectionResultPanel.testConnection.result");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}
