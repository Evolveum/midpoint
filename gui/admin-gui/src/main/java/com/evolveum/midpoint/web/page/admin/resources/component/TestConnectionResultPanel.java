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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPanel;
import com.evolveum.midpoint.web.component.AjaxButton;

/**
 * 
 * @author katkav
 *
 */
public class TestConnectionResultPanel extends BasePanel<List<OpResult>> {
	
	public TestConnectionResultPanel(String id, IModel<List<OpResult>> model) {
		super(id, model);
		initLayout();
	}

	private static final long serialVersionUID = 1L;
	
	private static final String ID_RESULT = "result";
	private static final String ID_OK = "ok";

	

	private void initLayout() {
		RepeatingView resultView = new RepeatingView(ID_RESULT);

		for (OpResult result : getModel().getObject()) {
			OperationResultPanel resultPanel = new OperationResultPanel(resultView.newChildId(), new Model<OpResult>(result));
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

}
