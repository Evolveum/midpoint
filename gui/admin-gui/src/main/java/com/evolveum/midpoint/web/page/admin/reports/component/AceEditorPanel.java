/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

/**
 * @author shood
 */
public class AceEditorPanel extends BasePanel<String> {

    private static final String ID_TITLE = "title";
    private static final String ID_TITLE_CONTAINER = "titleContainer";
    private static final String ID_EDITOR = "editor";

    private IModel<String> title;

    public AceEditorPanel(String id, IModel<String> title, IModel<String> data) {
        super(id, data);

        this.title = title;
        initLayout(0);
    }

    public AceEditorPanel(String id, IModel<String> title, IModel<String> data, int minSize) {
        super(id, data);

        this.title = title;
        initLayout(minSize);
    }

    private void initLayout(int minSize) {
        WebMarkupContainer titleContainer = new WebMarkupContainer(ID_TITLE_CONTAINER);
        titleContainer.setOutputMarkupId(true);
        titleContainer.add(new VisibleBehaviour(() -> title != null));
        add(titleContainer);

        Label title = new Label(ID_TITLE, this.title);
        title.setOutputMarkupId(true);
        titleContainer.add(title);

        SimpleAceEditorPanel editor = new SimpleAceEditorPanel(ID_EDITOR, getModel(), minSize);
        add(editor);
    }

    public AceEditor getEditor() {
        SimpleAceEditorPanel panel = (SimpleAceEditorPanel) get(ID_EDITOR);
        return panel != null ? panel.getEditor() : null;
    }
}
