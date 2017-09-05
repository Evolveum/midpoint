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
package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

public class AceEditorDialog extends BasePanel<String> {

	 private static final Trace LOGGER = TraceManager.getTrace(AceEditorDialog.class);


	    private static final String ID_RESULT = "resultAceEditor";
	    private static final String ID_BUTTON_CLOSE = "closeButton";


	    private IModel<String> xmlModel = new Model<String>("");

	    public AceEditorDialog(String id){
	    	super(id);
			initLayout();
	    }

	    protected void initLayout(){
	
	    	add(createAceEditor());
	
//	    	AjaxButton close = new AjaxButton(ID_BUTTON_CLOSE) {
//
//				@Override
//				public void onClick(AjaxRequestTarget target) {
//					closePerformed(target);
//				}
//
//			};
//			add(close);

	    }


	    private AceEditor createAceEditor(){
	    	AceEditor acePanel = new AceEditor(ID_RESULT, xmlModel);
	    	acePanel.setReadonly(true);
	    	acePanel.setMinHeight(500);
	    	return acePanel;
	    }

	    public void updateModel(IModel<String> model){
	    	xmlModel = model;
	    	addOrReplace(createAceEditor());
	    }

	    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
	    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//	        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
	    }

	    public void closePerformed(AjaxRequestTarget target){
	
	    }



}
