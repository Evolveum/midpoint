/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web.component.behavior;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Args;

/**
 * Registers a client-side event handler through Wicket's nonce-aware header rendering.
 */
public class JavaScriptEventBehavior extends Behavior {

    @Serial private static final long serialVersionUID = 1L;

    private final String event;
    private final IModel<String> scriptModel;

    public JavaScriptEventBehavior(String event, String script) {
        this(event, Model.of(script));
    }

    public JavaScriptEventBehavior(String event, IModel<String> scriptModel) {
        this.event = Args.notEmpty(event, "event");
        this.scriptModel = Args.notNull(scriptModel, "scriptModel");
    }

    @Override
    public void bind(Component component) {
        super.bind(component);
        component.setOutputMarkupId(true);
    }

    @Override
    public void renderHead(Component component, IHeaderResponse response) {
        super.renderHead(component, response);
        String script = scriptModel.getObject();
        OnEventHeaderItem eventHeaderItem = OnEventHeaderItem.forComponent(component, event, script);
        response.render(OnDomReadyHeaderItem.forScript(eventHeaderItem.getCompleteJavaScript()));
    }

    @Override
    public void detach(Component component) {
        scriptModel.detach();
        super.detach(component);
    }
}
