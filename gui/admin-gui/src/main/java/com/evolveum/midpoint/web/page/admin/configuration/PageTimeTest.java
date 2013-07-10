package com.evolveum.midpoint.web.page.admin.configuration;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;

public class PageTimeTest extends PageAdminConfiguration{

	@SpringBean(name="clock")
	private Clock clock;
	
	IModel<XMLGregorianCalendar> model = null; //new PropertyModel<XMLGregorianCalendar>(null, "offset.value");
	
	public PageTimeTest(){
		initLayout();
	}
	
	private void initLayout(){
		
		model = new LoadableModel<XMLGregorianCalendar>() {
			
			@Override
			protected XMLGregorianCalendar load() {
				return clock.currentTimeXMLGregorianCalendar();
			}
		};
		
		Form mainForm = new Form("mainForm");
		add(mainForm);
		
		
		DatePanel offset = new DatePanel("offset", model);
		mainForm.add(offset);
		
		AjaxSubmitLinkButton saveButton = new AjaxSubmitLinkButton("save", ButtonType.SIMPLE,
				createStringResource("pageTimeTest.button.save")){

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		mainForm.add(saveButton);
	}
	
	
	private void savePerformed(AjaxRequestTarget target){
		OperationResult result = new OperationResult(PageTimeTest.class.getName()+".changeTime");
		XMLGregorianCalendar offset = model.getObject();
		if (offset != null){
			clock.override(offset);
		}
		
		result.recordSuccess();
		showResult(result);
		target.add(getFeedbackPanel());
		
	}
	 
	
}
