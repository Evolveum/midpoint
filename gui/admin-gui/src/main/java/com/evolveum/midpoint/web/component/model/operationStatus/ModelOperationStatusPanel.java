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

package com.evolveum.midpoint.web.component.model.operationStatus;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.web.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.model.delta.DeltaPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * @author mederly
 */
public class ModelOperationStatusPanel extends SimplePanel<ModelOperationStatusDto> {

    private static final String ID_STATE = "state";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_FOCUS_NAME = "focusName";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public ModelOperationStatusPanel(String id, IModel<ModelOperationStatusDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        add(new Label(ID_STATE, new StringResourceModel("ModelOperationStatusPanel.state.${}", new PropertyModel<ModelState>(getModel(), ModelOperationStatusDto.F_STATE))));
        add(new Label(ID_FOCUS_TYPE, new PropertyModel<String>(getModel(), ModelOperationStatusDto.F_FOCUS_TYPE)));
        add(new Label(ID_FOCUS_NAME, new PropertyModel<String>(getModel(), ModelOperationStatusDto.F_FOCUS_NAME)));


        DeltaPanel deltaPanel = new DeltaPanel(ID_PRIMARY_DELTA, new PropertyModel<DeltaDto>(getModel(), ModelOperationStatusDto.F_PRIMARY_DELTA));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModel().getObject().getPrimaryDelta() != null;
            }
        });
        add(deltaPanel);

    }
}
