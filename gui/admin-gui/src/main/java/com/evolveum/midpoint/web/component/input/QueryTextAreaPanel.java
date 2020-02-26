/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class QueryTextAreaPanel extends InputPanel {

    private static final Trace LOGGER = TraceManager.getTrace(QueryTextAreaPanel.class);

    private static final String ID_INPUT = "input";

    private IModel<QueryType> model;
    private Integer rowsOverride;

    public QueryTextAreaPanel(String id, IModel<QueryType> model, Integer rowsOverride) {
        super(id);

        this.model = model;
        this.rowsOverride = rowsOverride;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        IModel<String> convertedModel = new IModel<String>() {
            @Override
            public String getObject() {
                if (model.getObject() == null) {
                    return null;
                }
                try {
                    return ((PageBase)getPage()).getPrismContext().xmlSerializer()
                            .serializeRealValue(model.getObject(), QueryType.COMPLEX_TYPE);
                } catch (SchemaException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            public void setObject(String object) {
                try {
                    model.setObject(((PageBase)getPage()).getPrismContext()
                            .parserFor(object).xml().parseRealValue(QueryType.class));
                } catch (SchemaException | IllegalStateException e) {
                    LOGGER.error("Couldn't parse object query from input", e);
                    getPage().error(((PageBase)getPage()).createStringResource("QueryTextAreaPanel.message.error.couldntParse").getString()
                    + ": " + e.getMessage());
                }
            }
        };

        final TextArea<String> text = new TextArea<String>(ID_INPUT, convertedModel) {

            @Override
            protected boolean shouldTrimInput() {
                return false;
            }
        };
        text.add(AttributeModifier.append("style", "max-width: 100%"));

        if (rowsOverride != null) {
            text.add(new AttributeModifier("rows", rowsOverride));
        }
        setOutputMarkupId(true);
        text.setOutputMarkupId(true);
        add(text);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }
}
