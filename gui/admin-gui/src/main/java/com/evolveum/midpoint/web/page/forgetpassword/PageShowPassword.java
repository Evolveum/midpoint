package com.evolveum.midpoint.web.page.forgetpassword;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.page.PageBase;

@PageDescriptor(url = "/resetpasswordsuccess")
public class PageShowPassword extends PageBase{

	
		
		
	public PageShowPassword() {
		 System.out.println("onload:"+getSession().getAttribute("pwdReset"));
		add(new Label("pass", getSession().getAttribute("pwdReset")));
		getSession().removeAttribute("pwdReset");
	}
		@Override
		protected IModel<String> createPageTitleModel() {
			return new Model<String>("");
		}
		
		


	}

