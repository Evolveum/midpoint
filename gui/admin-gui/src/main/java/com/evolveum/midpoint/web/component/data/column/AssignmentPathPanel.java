/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPathSegmentMetadataType;

public class AssignmentPathPanel extends BasePanel<List<AssignmentPathMetadataType>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_PATHS = "paths";
    private static final String ID_FIRST_SEGMENT = "firstSegment";
    private static final String ID_OPEN = "open";
    private static final String ID_BODY = "body";
    private static final String ID_SEGMENTS = "segments";
    private static final String ID_SEGMENT = "segment";

    public AssignmentPathPanel(String id, IModel<List<AssignmentPathMetadataType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "d-flex flex-column"));

        ListView<AssignmentPathMetadataType> paths = new ListView<>(ID_PATHS, getModel()) {

            @Override
            protected void populateItem(ListItem<AssignmentPathMetadataType> item) {
                item.setOutputMarkupId(true);

                ObjectReferenceColumnPanel firstSegment = new ObjectReferenceColumnPanel(ID_FIRST_SEGMENT,
                        () -> item.getModelObject().getSegment().get(0).getTargetRef());
                item.add(firstSegment);

                IModel<Boolean> openModel = Model.of(false);

                AjaxIconButton open = new AjaxIconButton(ID_OPEN,
                        () -> openModel.getObject() ? "fa fa-search-minus" : "fa fa-search-plus",
                        () -> openModel.getObject() ? "AssignmentPathPanel.hideDetails" : "AssignmentPathPanel.showDetails") {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        openModel.setObject(!openModel.getObject());
                        target.add(item);
                    }
                };
                item.add(open);

                WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
                body.add(new VisibleBehaviour(() -> openModel.getObject()));
                item.add(body);

                ListView<AssignmentPathSegmentMetadataType> segments = new ListView<>(ID_SEGMENTS, () -> item.getModelObject().getSegment().subList(1, item.getModelObject().getSegment().size())) {

                    @Override
                    protected void populateItem(ListItem<AssignmentPathSegmentMetadataType> item) {
                        ObjectReferenceColumnPanel segment = new ObjectReferenceColumnPanel(ID_SEGMENT, () -> item.getModelObject().getTargetRef());
                        item.add(segment);
                    }
                };
                body.add(segments);
            }
        };
        add(paths);
    }
}
