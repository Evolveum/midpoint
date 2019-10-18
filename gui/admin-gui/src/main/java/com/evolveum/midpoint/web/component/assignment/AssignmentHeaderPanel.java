/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class AssignmentHeaderPanel extends BasePanel<AssignmentItemDto> {
    private static final long serialVersionUID = 1L;

    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_TARGET_DISPLAY_NAME = "targetDisplayName";
    private static final String ID_TARGET_RELATION = "targetRelation";

    public AssignmentHeaderPanel(String id, IModel<AssignmentItemDto> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        add(new Label(ID_TARGET_NAME, new PropertyModel<String>(getModel(), AssignmentItemDto.F_NAME)));
        add(new Label(ID_TARGET_DISPLAY_NAME, new PropertyModel<String>(getModel(), AssignmentItemDto.F_DESCRIPTION)));
        add(new Label(ID_TARGET_RELATION, new PropertyModel<String>(getModel(), AssignmentItemDto.F_RELATION)));
    }
}
