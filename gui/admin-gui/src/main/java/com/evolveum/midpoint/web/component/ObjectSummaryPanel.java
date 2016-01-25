package com.evolveum.midpoint.web.component;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.util.SummaryTag;
import com.evolveum.midpoint.web.model.PrismPropertyRealValueFromPrismObjectModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class ObjectSummaryPanel <O extends ObjectType> extends Panel {	
	private static final long serialVersionUID = -3755521482914447912L;
	
	private static final String ID_BOX = "summaryBox";
	private static final String ID_ICON_BOX = "summaryIconBox";
	private static final String ID_PHOTO = "summaryPhoto";
	private static final String ID_ICON = "summaryIcon";
	private static final String ID_DISPLAY_NAME = "summaryDisplayName";
	private static final String ID_IDENTIFIER = "summaryIdentifier";
	private static final String ID_TITLE = "summaryTitle";
	private static final String ID_ORGANIZATION = "summaryOrganization";
	private static final String ID_TAG_ACTIVATION = "summaryTagActivation";
	
	private static final String BOX_CSS_CLASS = "info-box";
	private static final String ICON_BOX_CSS_CLASS = "info-box-icon";

	protected static final String ICON_CLASS_ACTIVATION_ACTIVE = "fa fa-check";
	protected static final String ICON_CLASS_ACTIVATION_INACTIVE = "fa fa-times";
	
	private WebMarkupContainer box;

	public ObjectSummaryPanel(String id, final IModel<PrismObject<O>> model) {
		super(id, model);
		
		box = new WebMarkupContainer(ID_BOX);
		add(box);
		
		box.add(new AttributeModifier("class", BOX_CSS_CLASS + " " + getBoxAdditionalCssClass()));
		
		box.add(new Label(ID_DISPLAY_NAME, new PrismPropertyRealValueFromPrismObjectModel(model, getDisplayNamePropertyName())));
		box.add(new Label(ID_IDENTIFIER, new PrismPropertyRealValueFromPrismObjectModel(model, getIdentifierPropertyName())));
		if (getTitlePropertyName() == null) {
			box.add(new Label(ID_TITLE, " "));
		} else {
			box.add(new Label(ID_TITLE, new PrismPropertyRealValueFromPrismObjectModel(model, getTitlePropertyName())));
		}
		
		
		
		
		WebMarkupContainer iconBox = new WebMarkupContainer(ID_ICON_BOX);
		box.add(iconBox);
		
		if (getIconBoxAdditionalCssClass() != null) {
			iconBox.add(new AttributeModifier("class", ICON_BOX_CSS_CLASS + " " + getIconBoxAdditionalCssClass()));
		}
		
        Label icon = new Label(ID_ICON,"");
        icon.add(new AttributeModifier("class", getIconCssClass()));
        
        iconBox.add(icon);
	}
	
	public void addTag(SummaryTag<O> tag) {
		box.add(tag);
	}
	
	protected abstract String getIconCssClass();
	
	protected abstract String getIconBoxAdditionalCssClass();
	
	protected abstract String getBoxAdditionalCssClass();

	protected QName getIdentifierPropertyName() {
		return FocusType.F_NAME;
	}

	protected abstract QName getDisplayNamePropertyName();
	
	protected QName getTitlePropertyName() {
		return null;
	}

}
