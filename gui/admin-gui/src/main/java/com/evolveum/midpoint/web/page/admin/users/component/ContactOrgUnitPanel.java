package com.evolveum.midpoint.web.page.admin.users.component;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.ContactOrgUnitDto;

/**
 * @author lazyman
 */
public class ContactOrgUnitPanel extends BasePanel<ContactOrgUnitDto> {

    private static final String ID_NAME = "name";

    public ContactOrgUnitPanel(String id, IModel<ContactOrgUnitDto> model) {
        super(id, model);
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    protected void initLayout() {
        add(new Label(ID_NAME, new PropertyModel(getModel(), "name")));
    }
}
