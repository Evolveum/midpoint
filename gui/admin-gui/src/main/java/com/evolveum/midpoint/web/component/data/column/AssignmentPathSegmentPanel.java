/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathSegmentMetadataType;

public class AssignmentPathSegmentPanel extends BasePanel<List<AssignmentPathSegmentMetadataType>> {
    private static final String ID_SEGMENTS = "segments";
    private static final String ID_SEGMENT = "segment";

    public AssignmentPathSegmentPanel(String id, IModel<List<AssignmentPathSegmentMetadataType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<AssignmentPathSegmentMetadataType> segments = new ListView<>(ID_SEGMENTS, getModel()) {
            @Override
            protected void populateItem(ListItem<AssignmentPathSegmentMetadataType> listItem) {
                listItem.add(new ObjectReferenceColumnPanel(ID_SEGMENT, () -> listItem.getModelObject().getTargetRef()));
            }
        };
        add(segments);
    }
}
