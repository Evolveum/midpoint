/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class ChangedItemPanel extends BasePanel<ChangedItem> {

    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUE = "newValue";

    public ChangedItemPanel(String id, IModel<ChangedItem> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        MultiLineLabel oldValue = new MultiLineLabel(ID_OLD_VALUE, () -> getModelObject().oldValue());
        oldValue.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().oldValue())));
        add(oldValue);

        MultiLineLabel newValue = new MultiLineLabel(ID_NEW_VALUE, () -> getModelObject().newValue());
        add(newValue);
    }
}
