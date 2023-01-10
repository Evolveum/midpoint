/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.operationStatus;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.web.component.prism.show.VisualizationPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

public class ModelOperationStatusPanel extends BasePanel<ModelOperationStatusDto> {

    private static final String ID_STATE = "state";
    private static final String ID_FOCUS_TYPE = "focusType";
    private static final String ID_FOCUS_NAME = "focusName";
    private static final String ID_PRIMARY_DELTA = "primaryDelta";

    public ModelOperationStatusPanel(String id, IModel<ModelOperationStatusDto> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

        add(new Label(ID_STATE, new StringResourceModel("ModelOperationStatusPanel.state.${}", new PropertyModel<ModelState>(getModel(), ModelOperationStatusDto.F_STATE))));
//        add(new Label(ID_FOCUS_TYPE, new PropertyModel<String>(getModelService(), ModelOperationStatusDto.F_FOCUS_TYPE)));
//        add(new Label(ID_FOCUS_NAME, new PropertyModel<String>(getModelService(), ModelOperationStatusDto.F_FOCUS_NAME)));


        VisualizationPanel deltaPanel = new VisualizationPanel(ID_PRIMARY_DELTA, new PropertyModel<>(getModel(), ModelOperationStatusDto.F_PRIMARY_DELTA));
        deltaPanel.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModelObject() != null && getModelObject().getPrimaryDelta() != null;
            }
        });
        add(deltaPanel);

    }
}
