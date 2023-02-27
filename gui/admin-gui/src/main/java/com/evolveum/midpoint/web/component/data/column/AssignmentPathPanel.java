/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

public class AssignmentPathPanel extends BasePanel<List<AssignmentPathMetadataType>> {
    private static final String ID_PATHS = "paths";
    private static final String ID_PATH = "path";
    private static final String ID_DETAILED_VIEW_BUTTON = "detailedViewButton";
    private static final String ID_DETAILED_VIEW_ICON = "detailedViewIcon";

    public AssignmentPathPanel(String id, IModel<List<AssignmentPathMetadataType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<AssignmentPathMetadataType> assignmentPaths = new ListView<>(ID_PATHS, getModel()) {
            @Override
            protected void populateItem(ListItem<AssignmentPathMetadataType> listItem) {
                IModel<Boolean> showAllSegmentsModel = Model.of(false);

                IModel<List<AssignmentPathSegmentMetadataType>> segmentsModel = loadSegmentsModel(listItem.getModel());
                listItem.add(new AssignmentPathSegmentPanel(ID_PATH, segmentsModel) {

                    @Override
                    protected boolean showAllSegments() {
                        return showAllSegmentsModel.getObject();
                    }
                });
                listItem.setOutputMarkupId(true);

                AjaxButton detailedViewButton = new AjaxButton(ID_DETAILED_VIEW_BUTTON) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        showAllSegmentsModel.setObject(!showAllSegmentsModel.getObject());
                        ajaxRequestTarget.add(listItem);
                    }
                };
                detailedViewButton.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(segmentsModel.getObject())));
                detailedViewButton.setOutputMarkupId(true);

                WebMarkupContainer detailedViewIcon = new WebMarkupContainer(ID_DETAILED_VIEW_ICON);
                detailedViewIcon.add(AttributeAppender.append("class", getIconStyleModel(showAllSegmentsModel)));
                detailedViewButton.add(detailedViewIcon);
                listItem.add(detailedViewButton);
            }
        };
        add(assignmentPaths);
    }

    private IModel<List<AssignmentPathSegmentMetadataType>> loadSegmentsModel(IModel<AssignmentPathMetadataType> assignmentModel) {
        return () -> {
            AssignmentPathMetadataType assignmentPathType = assignmentModel.getObject();
           List<AssignmentPathSegmentMetadataType> segments = assignmentPathType.getSegment();
           if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
               return new ArrayList<>();
           }
           return segments;
        };
    }

    private LoadableModel<String> getIconStyleModel(IModel<Boolean> showAllSegmentsModel) {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                return showAllSegmentsModel.getObject() ? "fa fa-search-minus" : "fa fa-search-plus";
            }
        };
    }
}
