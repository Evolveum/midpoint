package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.Component;
import org.apache.wicket.model.StringResourceModel;

public interface Popupable {

	public int getWidth();
	public int getHeight();
	public StringResourceModel getTitle();
	public Component getComponent();


}
