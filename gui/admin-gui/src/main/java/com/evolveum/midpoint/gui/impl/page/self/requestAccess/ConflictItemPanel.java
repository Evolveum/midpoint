/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItemPanel extends BasePanel<Conflict> {

    private static final long serialVersionUID = 1L;

    public ConflictItemPanel(String id, IModel<Conflict> model) {
        super(id, model);
    }
}
