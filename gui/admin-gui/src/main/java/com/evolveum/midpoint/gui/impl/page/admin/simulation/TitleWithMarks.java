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
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
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
    private static final String ID_PRIMARY_MARKS = "primaryMarks";
    private static final String ID_SECONDARY_MARKS = "secondaryMarks";

    private final IModel<String> primaryMarks;

    public TitleWithMarks(String id, IModel<String> title, IModel<String> primaryMarks) {
        super(id, title);

        this.primaryMarks = primaryMarks;

        initLayout();
    }

    protected AbstractLink createTitleLinkComponent(String id) {
        return new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onTitleClicked(target);
            }
        };
    }

    protected void customiseTitleLink(AbstractLink link) {
        link.add(new EnableBehaviour(this::isTitleLinkEnabled));
    }

    private void initLayout() {
        AbstractLink link = createTitleLinkComponent(ID_LINK);
        customiseTitleLink(link);
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

        Label primaryMarks = new Label(ID_PRIMARY_MARKS, this.primaryMarks);
        primaryMarks.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(this.primaryMarks.getObject())));
        primaryMarks.add(AttributeModifier.replace("title", createPrimaryMarksTitle()));
        add(primaryMarks);

        IModel<String> secondaryMarksModel = createSecondaryMarksList();

        Label secondaryMarks = new Label(ID_SECONDARY_MARKS, secondaryMarksModel);
        secondaryMarks.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(secondaryMarksModel.getObject())));
        secondaryMarks.add(AttributeModifier.replace("title", createSecondaryMarksTitle()));
        add(secondaryMarks);
    }

    protected IModel<String> createPrimaryMarksTitle() {
        return createStringResource("TitleWithMarks.realMarks");
    }

    protected IModel<String> createSecondaryMarksTitle() {
        return createStringResource("TitleWithMarks.processedMarks");
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

    protected IModel<String> createSecondaryMarksList() {
        return () -> null;
    }
}
