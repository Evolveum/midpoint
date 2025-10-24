/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column.icon;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

import static com.evolveum.midpoint.web.component.data.TableHeadersToolbar.HIDDEN_HEADER_ID;

/**
 * @author skublik
 */
public abstract class AbstractIconColumn<T, S> extends AbstractColumn<T, S> {
    @Serial private static final long serialVersionUID = 1L;

    public AbstractIconColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public AbstractIconColumn(IModel<String> displayModel, S sortProperty) {
        super(displayModel, sortProperty);
    }

    @Override
    public Component getHeader(String componentId) {
        if (getDisplayModel() == null || StringUtils.isBlank(getDisplayModel().getObject())) {
            Label label = new Label(componentId, () -> LocalizationUtil.translate("AbstractIconColumn.header"));
            label.add(AttributeAppender.append("class", "sr-only"));
            return label;
        }
        return super.getHeader(componentId);
    }

    @Override
    public String getCssClass() {
        return "align-middle";
    }
}
