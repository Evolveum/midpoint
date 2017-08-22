package com.evolveum.midpoint.gui.api.component;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DisplayNamePanel<C extends Containerable> extends BasePanel<C>{
	
	private static final long serialVersionUID = 1L;
	
	private final static String ID_DESCRIPTION = "description";
    private final static String ID_TYPE_IMAGE = "typeImage";
    private final static String ID_ASSIGNMENT_NAME = "assignmentName";

	public DisplayNamePanel(String id, IModel<C> model) {
		super(id, model);
		
		initLayout();
	}
	
	private void initLayout() {
		WebMarkupContainer typeImage = new WebMarkupContainer(ID_TYPE_IMAGE);
        typeImage.setOutputMarkupId(true);
        typeImage.add(AttributeModifier.append("class", createImageModel()));
        typeImage.add(AttributeModifier.append("class", getAdditionalNameLabelStyleClass()));
        add(typeImage);

        Label name = new Label(ID_ASSIGNMENT_NAME, createHeaderModel());
        name.add(AttributeModifier.append("class", getAdditionalNameLabelStyleClass()));
        name.setOutputMarkupId(true);
        add(name);

        add(new Label(ID_DESCRIPTION, new PropertyModel<String>(getModel(), ObjectType.F_DESCRIPTION.getLocalPart())));
        

	}
	
	private String createImageModel() {
		if (ObjectType.class.isAssignableFrom(getModelObject().getClass())) {
			return WebComponentUtil.createDefaultIcon((ObjectType) getModelObject());
		} 
		
		return WebComponentUtil.createDefaultColoredIcon(getModelObject().asPrismContainerValue().getComplexTypeDefinition().getTypeName());
		
	}
	
	private IModel<String> getAdditionalNameLabelStyleClass() {
        return Model.of("text-bold");
    }
	
	private IModel<String> createHeaderModel() {
		if (ObjectType.class.isAssignableFrom(getModelObject().getClass())) {
			return Model.of(WebComponentUtil.getEffectiveName((ObjectType) getModelObject(), AbstractRoleType.F_DISPLAY_NAME));
		} 
		PrismProperty<String> name = getModelObject().asPrismContainerValue().findProperty(ObjectType.F_NAME);
		if (name == null || name.isEmpty()) {
			return Model.of("");
		}
		return Model.of(name.getRealValue());
	}
	
	
	
	
}
