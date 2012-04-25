package com.evolveum.midpoint.web.page.admin.roles;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.button.AjaxLinkButton;

public class PopupWindow extends Panel {

	public PopupWindow(String id, final ModalWindow window) {
		super(id);
		add(new AjaxLinkButton("aaa", new Model<String>("Close")) {
			
			@Override
			public void onClick(AjaxRequestTarget target) {
				window.close(target);
			}
		});
		
		
	}
}
