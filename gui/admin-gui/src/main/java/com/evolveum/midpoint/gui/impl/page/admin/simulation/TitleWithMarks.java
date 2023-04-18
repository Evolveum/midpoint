/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.IconComponent;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class TitleWithMarks extends BasePanel<String> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_TITLE = "title";
    private static final String ID_ICON_LINK = "iconLink";
    private static final String ID_ICON = "icon";
    private static final String ID_REAL_MARKS = "realMarks";
    private static final String ID_PROCESSED_MARKS = "processedMarks";

    private final IModel<String> realMarks;

    public TitleWithMarks(String id, IModel<String> title, IModel<String> realMarks) {
        super(id, title);

        this.realMarks = realMarks;

        initLayout();
    }

    private void initLayout() {
        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onTitleClicked(target);
            }
        };
        link.add(new EnableBehaviour(this::isTitleLinkEnabled));
        add(link);

        Label title = new Label(ID_TITLE, getModel());
        link.add(title);

        IModel<String> iconCssModel = createIconCssModel();
        AjaxLink<Void> iconLink = new AjaxLink<>(ID_ICON_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onIconClicked(target);
            }
        };
        iconLink.add(new VisibleBehaviour(() -> iconCssModel.getObject() != null));
        add(iconLink);

        IconComponent icon = new IconComponent(ID_ICON, iconCssModel, createIconTitleModel());
        iconLink.add(icon);

        Label realMarks = new Label(ID_REAL_MARKS, this.realMarks);
        realMarks.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(this.realMarks.getObject())));
        add(realMarks);

        IModel<String> processedMarksModel = createProcessedMarksContainer();

        Label processedMarks = new Label(ID_PROCESSED_MARKS, processedMarksModel);
        processedMarks.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(processedMarksModel.getObject())));
        add(processedMarks);

    }

    protected boolean isTitleLinkEnabled() {
        return true;
    }

    protected void onTitleClicked(AjaxRequestTarget target) {

    }

    protected void onIconClicked(AjaxRequestTarget target) {

    }

    protected IModel<String> createIconCssModel() {
        return () -> null;
    }

    protected IModel<String> createIconTitleModel() {
        return () -> null;
    }

    protected IModel<String> createProcessedMarksContainer() {
        return () -> null;
    }
}
