/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class AssignmentHeaderPanel extends SimplePanel<AssignmentItemDto> {

    private static final String ID_TARGET_NAME = "targetName";
    private static final String ID_TARGET_DISPLAY_NAME = "targetDisplayName";
    private static final String ID_TARGET_RELATION = "targetRelation";

    public AssignmentHeaderPanel(String id, IModel<AssignmentItemDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        add(new Label(ID_TARGET_NAME, new PropertyModel<String>(getModel(), AssignmentItemDto.F_NAME)));
        add(new Label(ID_TARGET_DISPLAY_NAME, new PropertyModel<String>(getModel(), AssignmentItemDto.F_DESCRIPTION)));
        add(new Label(ID_TARGET_RELATION, new PropertyModel<String>(getModel(), AssignmentItemDto.F_RELATION)));
    }
}
