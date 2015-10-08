package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageDialog;

/**
 * A page hosting the object selection panel. Although these are a bit coupled, we decided
 * to work with them separately to make panel reusable.
 */
public class ObjectSelectionPage extends PageDialog {

    public static final String ID_OBJECT_SELECTION_PANEL = "objectSelectionPanel";

    public ObjectSelectionPage(ObjectSelectionPanel innerPanel, PageBase callingPage) {
        super(callingPage);
        innerPanel.setOutputMarkupId(true);
        add(innerPanel);
    }

}
