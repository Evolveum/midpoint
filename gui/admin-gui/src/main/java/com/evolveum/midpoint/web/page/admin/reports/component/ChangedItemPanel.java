/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangedItemPanel extends BasePanel<ChangedItem> {

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("(?m)^(&amp;emsp;)+");

    private static final String ID_OLD_VALUES = "oldValues";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUES = "newValues";
    private static final String ID_MODIFICATION_TYPE = "modificationType";
    private static final String ID_NEW_VALUE = "newValue";

    public ChangedItemPanel(String id, IModel<ChangedItem> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        ListView<PrismValue> oldValues = new ListView<>(ID_OLD_VALUES, () -> getModelObject().oldValues()) {

            @Override
            protected void populateItem(ListItem<PrismValue> item) {
                MultiLineLabel oldValue = new MultiLineLabel(ID_OLD_VALUE, () -> prettyPrint(item.getModelObject()));
                oldValue.setEscapeModelStrings(false);  // value escaped in {@link #prettyPrint(PrismValue)}
                oldValue.setRenderBodyOnly(true);
                item.add(oldValue);
            }
        };
        oldValues.add(new VisibleBehaviour(() -> !getModelObject().oldValues().isEmpty()));
        add(oldValues);

        ListView<ChangedItemValue> newValues = new ListView<>(ID_NEW_VALUES, () -> getModelObject().newValues()) {

            @Override
            protected void populateItem(ListItem<ChangedItemValue> item) {
                WebMarkupContainer modificationType = new WebMarkupContainer(ID_MODIFICATION_TYPE);
                modificationType.add(AttributeAppender.append("class", () -> {
                    ModificationType modification = item.getModelObject().modificationType();
                    if (modification == null) {
                        return null;
                    }

                    return switch (modification) {
                        case ADD -> GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS;
                        case DELETE -> GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER;
                        case REPLACE -> GuiStyleConstants.CLASS_CIRCLE_FULL;
                    };
                }));
                item.add(modificationType);

                MultiLineLabel newValue = new MultiLineLabel(ID_NEW_VALUE, () -> prettyPrint(item.getModelObject().value()));
                newValue.setEscapeModelStrings(false);    // value escaped in {@link #prettyPrint(PrismValue)}
                newValue.setRenderBodyOnly(true);
                item.add(newValue);
            }
        };
        add(newValues);
    }

    private String prettyPrint(PrismValue value) {
        if (value == null) {
            return LocalizationUtil.translate("ChangedItemColumn.nullValue");
        }

        StringBuilder sb = new StringBuilder();

        if (value instanceof PrismContainerValue<?> pcv && pcv.getId() != null) {
            sb.append(getModelObject().path());
            sb.append("/");
        }

        sb.append(new UserFriendlyPrettyPrinter()
                .indent("&emsp;")
                .prettyPrintValue(value, 0));

        String escaped = Strings.escapeMarkup(sb.toString()).toString();

        Matcher matcher = ESCAPE_PATTERN.matcher(escaped);
        return matcher.replaceAll(match -> "&emsp;".repeat(match.group(0).length() / "&amp;emsp;".length()));
    }
}
