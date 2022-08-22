/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.model.PropertyModel;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperColumnPanel<T> extends AbstractItemWrapperColumnPanel<PrismPropertyWrapper<T>, PrismPropertyValueWrapper<T>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyWrapperColumnPanel.class);

    PrismPropertyWrapperColumnPanel(String id, IModel<PrismPropertyWrapper<T>> model, ColumnType columnType) {
        super(id, model, columnType);
    }

    @Override
    protected String createLabel(PrismPropertyValueWrapper<T> object) {
        return object.toShortString();

    }

    @Override
    protected Panel createValuePanel(String id, IModel<PrismPropertyWrapper<T>> model, PrismPropertyValueWrapper<T> object) {

        Panel panel;
        try {
            panel = getPageBase().initItemPanel(id, model.getObject().getTypeName(), model, createPanelSettings());
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}", model.getObject());
            getSession().error("Cannot create panel for: " + model.getObject());
            return new ErrorPanel(id, createStringResource("PropertyPropertyWrapperColumnPanel.cannot.create.panel"));
        }

        return panel;
    }

    @Override
    protected Panel createLink(String id, IModel<PrismPropertyValueWrapper<T>> object) {
        String humanReadableLinkName = getHumanReadableLinkName(object);
        IModel labelModel;
        if (StringUtils.isEmpty(humanReadableLinkName)){
            labelModel = getPageBase().createStringResource("feedbackMessagePanel.message.undefined");
        } else {
            labelModel = new ReadOnlyModel(() -> humanReadableLinkName);
        }
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(id, labelModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismPropertyWrapperColumnPanel.this.onClick(target, getModelObject().getParent());
            }

        };
        return ajaxLinkPanel;
    }

    protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
    }

    private String getHumanReadableLinkName(IModel<PrismPropertyValueWrapper<T>> object) {
        if (object == null) {
            return null;
        }

        PrismPropertyValueWrapper<T> valueWrapper = object.getObject();
        if (valueWrapper == null) {
            return null;
        }

        return valueWrapper.toShortString();
    }
}
