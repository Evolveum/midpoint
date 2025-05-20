/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.string.Strings;

import com.evolveum.midpoint.common.UserFriendlyPrettyPrinter;
import com.evolveum.midpoint.common.UserFriendlyPrettyPrinterOptions;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class DeltaColumnPanel extends BasePanel<ItemDelta<? extends PrismValue, ?>> {

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("(?m)^(&amp;emsp;)+");

    private static final String ID_OLD_VALUES = "oldValues";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUES = "newValues";
    private static final String ID_MODIFICATION_TYPE = "modificationType";
    private static final String ID_NEW_VALUE = "newValue";
    private static final String ID_ARROW = "arrow";

    private boolean showOldValues = true;

    private boolean showNewValues = true;

    public DeltaColumnPanel(String id, IModel<ItemDelta<?, ?>> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        IModel<List<PrismValue>> oldValuesModel = new LoadableDetachableModel<>() {

            @Override
            protected List<PrismValue> load() {
                Collection values = getModelObject().getEstimatedOldValues();
                return values != null ? new ArrayList<>(values) : List.of();
            }
        };

        ListView<PrismValue> oldValues = new ListView<>(ID_OLD_VALUES, oldValuesModel) {

            @Override
            protected void populateItem(ListItem<PrismValue> item) {
                MultiLineLabel oldValue = new MultiLineLabel(ID_OLD_VALUE, () -> prettyPrint(item.getModelObject()));
                oldValue.setEscapeModelStrings(false);  // value escaped in {@link #prettyPrint(PrismValue)}
                oldValue.setRenderBodyOnly(true);
                item.add(oldValue);
            }
        };
        oldValues.add(new VisibleBehaviour(() -> showOldValues));
        add(oldValues);

        WebMarkupContainer arrow = new WebMarkupContainer(ID_ARROW);
        arrow.add(new VisibleBehaviour(() -> showOldValues && showNewValues && !oldValuesModel.getObject().isEmpty()));
        add(arrow);

        IModel<List<Pair<ModificationType, PrismValue>>> newValuesModel = new LoadableDetachableModel<>() {

            @Override
            protected List<Pair<ModificationType, PrismValue>> load() {
                List<Pair<ModificationType, PrismValue>> result = new ArrayList<>();

                addModifications(ModificationType.ADD, getModelObject().getValuesToAdd(), result);
                addModifications(ModificationType.DELETE, getModelObject().getValuesToDelete(), result);
                addModifications(ModificationType.REPLACE, getModelObject().getValuesToReplace(), result);

                return result;
            }
        };

        ListView<Pair<ModificationType, PrismValue>> newValues = new ListView<>(ID_NEW_VALUES, newValuesModel) {

            @Override
            protected void populateItem(ListItem<Pair<ModificationType, PrismValue>> item) {
                WebMarkupContainer modificationType = new WebMarkupContainer(ID_MODIFICATION_TYPE);
                modificationType.add(AttributeAppender.append("class", () -> {
                    ModificationType modification = item.getModelObject().getLeft();
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

                MultiLineLabel newValue = new MultiLineLabel(ID_NEW_VALUE, () -> prettyPrint(item.getModelObject().getRight()));
                newValue.setEscapeModelStrings(false);    // value escaped in {@link DeltaColumnPanel#prettyPrint(PrismValue)}
                newValue.setRenderBodyOnly(true);
                item.add(newValue);
            }
        };
        newValues.add(new VisibleBehaviour(() -> showNewValues));
        add(newValues);
    }

    private void addModifications(ModificationType type, Collection<? extends PrismValue> values, List<Pair<ModificationType, PrismValue>> result) {
        if (values != null) {
            values.forEach(v -> result.add(Pair.of(type, v)));
        }
    }

    /**
     * This method also has to escape HTML entities in the string so that the multi-line label component can render it as is.
     *
     * Escaping is done via wicket {@link Strings#escapeMarkup(CharSequence)}.
     */
    private String prettyPrint(PrismValue value) {
        if (value == null) {
            return LocalizationUtil.translate("ChangedItemColumn.nullValue");
        }

        StringBuilder sb = new StringBuilder();

        if (value instanceof PrismContainerValue<?> pcv && pcv.getId() != null) {
            sb.append(getModelObject().getPath());
            sb.append("/");
        }

        sb.append(
                new UserFriendlyPrettyPrinter(
                        new UserFriendlyPrettyPrinterOptions()
                                .indentation("&emsp;"))
                        .locale(getLocale())
                        .localizationService(getPageBase().getLocalizationService())
                        .prettyPrintValue(value, 0));

        String escaped = Strings.escapeMarkup(sb.toString()).toString();

        Matcher matcher = ESCAPE_PATTERN.matcher(escaped);
        return matcher.replaceAll(match -> "&emsp;".repeat(match.group(0).length() / "&amp;emsp;".length()));
    }

    public DeltaColumnPanel setShowNewValues(boolean showNewValues) {
        this.showNewValues = showNewValues;
        return this;
    }

    public DeltaColumnPanel setShowOldValues(boolean showOldValues) {
        this.showOldValues = showOldValues;
        return this;
    }
}
