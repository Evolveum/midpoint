/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.model.delta;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

public class DeltaPanel extends BasePanel<DeltaDto> {

    private static final long serialVersionUID = 1L;

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
        Label changeType = new Label(ID_CHANGE_TYPE, () -> getModelObject().getChangeType());
        add(changeType);

        VisibleBehaviour isAdd = new VisibleBehaviour(() -> getModelObject().isAdd());
        VisibleEnableBehaviour isNotAdd = new VisibleBehaviour(() -> !getModelObject().isAdd());

        Label oidLabel = new Label(ID_OID_LABEL, createStringResource("DeltaPanel.label.oid"));
        oidLabel.add(isNotAdd);
        add(oidLabel);

        Label oid = new Label(ID_OID, () -> getModelObject().getOid());
        oid.add(isNotAdd);
        add(oid);

        Label objectToAddLabel = new Label(ID_OBJECT_TO_ADD_LABEL, createStringResource("DeltaPanel.label.objectToAdd"));
        objectToAddLabel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(objectToAddLabel);

        ContainerValuePanel objectToAddPanel = new ContainerValuePanel(ID_OBJECT_TO_ADD, () -> getModelObject().getObjectToAdd());
        objectToAddPanel.add(isAdd);
        add(objectToAddPanel);

        Label modificationsLabel = new Label(ID_MODIFICATIONS_LABEL, createStringResource("DeltaPanel.label.modifications"));
        modificationsLabel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        add(modificationsLabel);

        ModificationsPanel modificationsPanel = new ModificationsPanel(ID_MODIFICATIONS, getModel());
        modificationsPanel.add(isNotAdd);
        add(modificationsPanel);
    }
}
