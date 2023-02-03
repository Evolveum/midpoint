/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathSegmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AssignmentPathSegmentPanel extends BasePanel<List<AssignmentPathSegmentType>> {
    private static final String ID_SEGMENTS = "segments";
    private static final String ID_SEGMENT = "segment";


    public AssignmentPathSegmentPanel(String id, IModel<List<AssignmentPathSegmentType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<AssignmentPathSegmentType> segments = new ListView<>(ID_SEGMENTS, getModel()) {
            @Override
            protected void populateItem(ListItem<AssignmentPathSegmentType> listItem) {
                listItem.add(new ObjectReferenceColumnPanel(ID_SEGMENT, () -> listItem.getModelObject().getTargetRef()));
            }
        };
        add(segments);
    }
}
