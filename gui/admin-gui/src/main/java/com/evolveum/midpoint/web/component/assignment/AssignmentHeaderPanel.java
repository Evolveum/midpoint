/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
