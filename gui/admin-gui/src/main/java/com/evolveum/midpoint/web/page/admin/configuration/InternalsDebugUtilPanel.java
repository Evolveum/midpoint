package com.evolveum.midpoint.web.page.admin.configuration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InternalsConfigDto;

public class InternalsDebugUtilPanel extends BasePanel<InternalsConfigDto>{

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(InternalsDebugUtilPanel.class);
	
	private static final String ID_FORM = "form";
	private static final String ID_DETAILED_DEBUG_DUMP = "detailedDebugDump";
    private static final String ID_SAVE_DEBUG_UTIL = "saveDebugUtil";
    private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-8";
	
	public InternalsDebugUtilPanel(String id, IModel<InternalsConfigDto> model) {
		super(id, model);
	}
	
	protected void onInitialize() {
		super.onInitialize();
		
		setOutputMarkupId(true);
		Form form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);
		CheckFormGroup detailed = new CheckFormGroup(ID_DETAILED_DEBUG_DUMP,
                new PropertyModel<Boolean>(getModel(), InternalsConfigDto.F_DETAILED_DEBUG_DUMP),
                createStringResource("PageInternals.detailedDebugDump"), LABEL_SIZE, INPUT_SIZE);
        form.add(detailed);

        
        AjaxSubmitButton update = new AjaxSubmitButton(ID_SAVE_DEBUG_UTIL,
                createStringResource("PageBase.button.update")) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateDebugPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        form.add(update);
	};
	
	 private void updateDebugPerformed(AjaxRequestTarget target){
	        getModelObject().saveDebugUtil();

	        LOGGER.trace("Updated debug util, detailedDebugDump={}", DebugUtil.isDetailedDebugDump());
	        success(getString("PageInternals.message.debugUpdatePerformed", DebugUtil.isDetailedDebugDump()));
	        target.add(getPageBase().getFeedbackPanel(), getDebugUtilForm());
	    }
	 
	 private Form getDebugUtilForm(){
	        return (Form) get(ID_FORM);
	    }
}
