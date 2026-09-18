/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Represents line-numbered code block with a "copy to clipboard" action.
 */
public class CodeBlockPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String ID_LABEL = "label";
    private static final String ID_COPY = "copy";
    private static final String ID_LINE = "line";
    private static final String ID_LINE_NUMBER = "lineNumber";
    private static final String ID_LINE_CONTENT = "lineContent";

    private final IModel<String> labelModel;

    public CodeBlockPanel(String id, IModel<String> labelModel, IModel<String> codeModel) {
        super(id, codeModel);
        this.labelModel = labelModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);

        add(new Label(ID_LABEL, labelModel));
        add(createCopyLink());
        add(createLines());
    }

    private @NotNull ListView<String> createLines() {
        return new ListView<>(ID_LINE, this::getLines) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_LINE_NUMBER, Model.of(item.getIndex() + 1)));
                item.add(new Label(ID_LINE_CONTENT, item.getModelObject()));
            }
        };
    }

    private @NotNull List<String> getLines() {
        String text = getModelObject();
        return text != null ? List.of(text.split("\n", -1)) : List.of();
    }

    private @NotNull AjaxLink<Void> createCopyLink() {
        return new AjaxLink<>(ID_COPY) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                target.appendJavaScript(buildCopyScript());
            }
        };
    }


    private @NotNull String buildCopyScript() {
        return "if (navigator.clipboard) { navigator.clipboard.writeText(%s); }".formatted(toJsStringLiteral(getModelObject()));
    }

    private static String toJsStringLiteral(String text) {
        try {
            return JSON_MAPPER.writeValueAsString(StringUtils.defaultString(text));
        } catch (JsonProcessingException e) {
            return "\"\"";
        }
    }
}
