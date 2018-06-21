/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

/**
 * @author mederly
 */
public class DeltaPanel extends BasePanel<DeltaDto> {

    private static final Trace LOGGER = TraceManager.getTrace(DeltaPanel.class);

    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_OID = "oid";
    private static final String ID_OID_LABEL = "oidLabel";
    private static final String ID_OBJECT_TO_ADD = "objectToAdd";
    private static final String ID_OBJECT_TO_ADD_LABEL = "objectToAddLabel";
    private static final String ID_MODIFICATIONS = "modifications";
    private static final String ID_MODIFICATIONS_LABEL = "modificationsLabel";

    public DeltaPanel(String id, IModel<DeltaDto> model) {
        super(id, model);
		initLayout();
    }

    protected void initLayout() {

        Label changeType = new Label(ID_CHANGE_TYPE, new PropertyModel<String>(getModel(), DeltaDto.F_CHANGE_TYPE));
        add(changeType);

        VisibleEnableBehaviour isAdd = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return getModel().getObject().isAdd();
            }
        };

        VisibleEnableBehaviour isNotAdd = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return !getModel().getObject().isAdd();
            }
        };

        Label oidLabel = new Label(ID_OID_LABEL, new ResourceModel("DeltaPanel.label.oid"));
        oidLabel.add(isNotAdd);
        add(oidLabel);
        Label oid = new Label(ID_OID, new PropertyModel<String>(getModel(), DeltaDto.F_OID));
        oid.add(isNotAdd);
        add(oid);

        VisibleEnableBehaviour never = new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return false;
            }
        };

        Label objectToAddLabel = new Label(ID_OBJECT_TO_ADD_LABEL, new ResourceModel("DeltaPanel.label.objectToAdd"));
        //objectToAddLabel.add(isAdd);
        objectToAddLabel.add(never);
        add(objectToAddLabel);

        ContainerValuePanel objectToAddPanel =
                new ContainerValuePanel(ID_OBJECT_TO_ADD,
                    new PropertyModel<>(getModel(), DeltaDto.F_OBJECT_TO_ADD));
        objectToAddPanel.add(isAdd);
        add(objectToAddPanel);

        Label modificationsLabel = new Label(ID_MODIFICATIONS_LABEL, new ResourceModel("DeltaPanel.label.modifications"));
        //modificationsLabel.add(isNotAdd);
        modificationsLabel.add(never);
        add(modificationsLabel);

        ModificationsPanel modificationsPanel = new ModificationsPanel(ID_MODIFICATIONS, getModel());
        modificationsPanel.add(isNotAdd);
        add(modificationsPanel);
    }
}
