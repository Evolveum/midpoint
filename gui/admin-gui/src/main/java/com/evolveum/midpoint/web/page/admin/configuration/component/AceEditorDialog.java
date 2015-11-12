package com.evolveum.midpoint.web.page.admin.configuration.component;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;

public class AceEditorDialog extends SimplePanel<String> {

	 private static final Trace LOGGER = TraceManager.getTrace(AceEditorDialog.class);

	
	    private static final String ID_RESULT = "resultAceEditor";
	    private static final String ID_BUTTON_CLOSE = "closeButton";
	  

	    private IModel<String> xmlModel = new Model<String>("");
	    
	    public AceEditorDialog(String id){
	    	super(id);
	    }

	   
	  
	    @Override
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
	    	acePanel.setMinSize(500);
	    	return acePanel;
	    }
	    
	    public void updateModel(IModel<String> model){
	    	xmlModel = model;
	    	addOrReplace(createAceEditor());
	    }

	    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
	        return new StringResourceModel(resourceKey, this, new Model<String>(), resourceKey, objects);
	    }
	    
	    public void closePerformed(AjaxRequestTarget target){
	    	
	    }

	  
	  
}
