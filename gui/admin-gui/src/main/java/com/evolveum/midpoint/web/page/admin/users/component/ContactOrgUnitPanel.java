package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.ContactOrgUnitDto;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ContactOrgUnitPanel extends SimplePanel {

    private static final String ID_NAME = "name";

    public ContactOrgUnitPanel(String id, IModel<ContactOrgUnitDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        add(new Label(ID_NAME, new PropertyModel(getModel(), "name")));
    }
}
