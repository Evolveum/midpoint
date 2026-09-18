/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Content of the "Protocol" tab for a log entry.
 */
public class SqlProtocolPanel extends BasePanel<SqlProtocol> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_QUERY = "query";

    public SqlProtocolPanel(String id, IModel<SqlProtocol> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(new CodeBlockPanel(ID_QUERY,
                createStringResource("OperationLogPanel.protocol.sqlQuery"),
                () -> getModelObject().query()));
    }
}
