/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

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

        VisibleBehaviour isAdd = new VisibleBehaviour(() -> getModel().getObject().isAdd());
        VisibleEnableBehaviour isNotAdd = new VisibleBehaviour(() -> !getModel().getObject().isAdd());

        Label oidLabel = new Label(ID_OID_LABEL, new ResourceModel("DeltaPanel.label.oid"));
        oidLabel.add(isNotAdd);
        add(oidLabel);
        Label oid = new Label(ID_OID, new PropertyModel<String>(getModel(), DeltaDto.F_OID));
        oid.add(isNotAdd);
        add(oid);

        VisibleBehaviour never = new VisibleBehaviour(() -> false);

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
        modificationsLabel.add(never);
        add(modificationsLabel);

        ModificationsPanel modificationsPanel = new ModificationsPanel(ID_MODIFICATIONS, getModel());
        modificationsPanel.add(isNotAdd);
        add(modificationsPanel);
    }
}
