/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Preview panel for literal expressions. Extracts and displays
 * all literal values defined in the expression.
 */
public class LiteralExpressionPreviewDetailsPanel extends BasePanel<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(LiteralExpressionPreviewDetailsPanel.class);

    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";

    public LiteralExpressionPreviewDetailsPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(new ListView<>(ID_VALUES, Model.ofList(getLiteralValues())) {
            @Override
            protected void populateItem(ListItem<String> item) {
                item.add(new Label(ID_VALUE, item.getModel()));
            }
        });
    }

    private List<String> getLiteralValues() {
        ExpressionType expression = getModelObject();
        if (expression == null) {
            return List.of();
        }

        try {
            List<String> values = ExpressionUtil.getLiteralExpressionValues(expression);
            return CollectionUtils.isEmpty(values) ? List.of() : values;
        } catch (SchemaException ex) {
            LOGGER.error("Couldn't get literal expression value: {}", ex.getLocalizedMessage(), ex);
            return new ArrayList<>();
        }
    }
}
