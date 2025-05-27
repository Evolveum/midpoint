/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.impl.binding.AbstractPlainStructured;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Viliam Repan
 */
public class UserFriendlyPrettyPrinter {

    private static final String KEY_ESTIMATED_OLD = "UserFriendlyPrettyPrinter.estimatedOld";
    private static final String KEY_ADD = "UserFriendlyPrettyPrinter.add";
    private static final String KEY_DELETE = "UserFriendlyPrettyPrinter.delete";
    private static final String KEY_REPLACE = "UserFriendlyPrettyPrinter.replace";
    private static final String KEY_FILTER = "UserFriendlyPrettyPrinter.filter";
    private static final String KEY_UNDEFINED = "UserFriendlyPrettyPrinter.undefined";

    private static final String KEY_ESTIMATED_OLD_DEFAULT = "Estimated old:";
    private static final String KEY_ADD_DEFAULT = "Add:";
    private static final String KEY_DELETE_DEFAULT = "Delete:";
    private static final String KEY_REPLACE_DEFAULT = "Replace:";
    private static final String KEY_FILTER_DEFAULT = "filter";
    private static final String KEY_UNDEFINED_DEFAULT = "undefined";

    private final UserFriendlyPrettyPrinterOptions options;

    private LocalizationService localizationService;

    private Locale locale;

    public UserFriendlyPrettyPrinter() {
        this(new UserFriendlyPrettyPrinterOptions());
    }

    public UserFriendlyPrettyPrinter(UserFriendlyPrettyPrinterOptions options) {
        this.options = options;
    }

    public UserFriendlyPrettyPrinter localizationService(LocalizationService localizationService) {
        this.localizationService = localizationService;
        return this;
    }

    public UserFriendlyPrettyPrinter locale(Locale locale) {
        this.locale = locale;
        return this;
    }

    private String indent(int indent) {
        String indentation = this.options.indentation();

        return StringUtils.isNotEmpty(indentation) ? StringUtils.repeat(this.options.indentation(), indent) : "";
    }

    public String prettyPrintItem(Item<?, ?> item, int indent) {
        if (item instanceof PrismProperty<?> p) {
            return prettyPrintProperty(p, indent);
        } else if (item instanceof PrismContainer<?> c) {
            return prettyPrintContainer(c, indent);
        } else if (item instanceof PrismReference r) {
            return prettyPrintReference(r, indent);
        }

        return PrettyPrinter.prettyPrint(item);
    }

    public String prettyPrintProperty(PrismProperty<?> p, int indent) {
        return prettyPrintItem(p, indent, true);
    }

    public String prettyPrintContainer(PrismContainer<?> c, int indent) {
        return prettyPrintItem(c, indent, false);
    }

    public String prettyPrintReference(PrismReference r, int indent) {
        return prettyPrintItem(r, indent, true);
    }

    private String prettyPrintItem(Item<?, ?> item, int indent, boolean canUseSingleLine) {
        ItemDefinition def = item.getDefinition();

        if (def != null && def.isOperational() && !options.showOperational()) {
            return "";
        }

        if (isItemEmpty(item)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent));
        sb.append(getItemName(item));

        if (item instanceof PrismObject<?> o) {
            String object = Stream.of(o.getOid(), o.getVersion())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));

            if (StringUtils.isNotEmpty(object)) {
                sb.append(" (");
                sb.append(object);
                sb.append(")");
            }
        }

        sb.append(": ");

        if (item.isEmpty()) {
            return sb.toString();
        } else if (canUseSingleLine && item.size() == 1 && isSingleLineType(def)) {
            // indent here is 0 because we are on the same line as the item name, e.g. "givenName: VALUE_WE_ARE_PRETTY_PRINTING"
            sb.append(prettyPrintValue(item.getValues().get(0), 0));

            return sb.toString();
        }

        sb.append("\n");

        if (item instanceof PrismObject<?> o) {
            sb.append(prettyPrintContainerValue(o.getValue(), indent + 1));
            return sb.toString();
        }

        int valueIndent = item instanceof PrismContainer<?> ? indent + 1 : indent + 2;

        String values = item.getValues().stream()
                .map(value -> prettyPrintValue(value, valueIndent))
                .collect(Collectors.joining("\n"));
        sb.append(values);

//        addItemSeparatorEnd(sb);

        return sb.toString();
    }

    private boolean isItemEmpty(Item<?, ?> item) {
        if (item == null || item.isEmpty()) {
            return true;
        }

        if (options.showOperational()) {
            return false;
        }

        // check whether there's at least one non-operational item (non-empty)
        if (!(item instanceof PrismContainer<?> pc)) {
            return false;
        }

        for (PrismContainerValue<?> pcv : pc.getValues()) {
            for (Item<?, ?> child : pcv.getItems()) {
                if (!child.getDefinition().isOperational()) {
                    return false;
                }
            }
        }

        return true;
    }

    private void addItemSeparatorStart(StringBuilder sb) {
        if (options.containerSeparatorStart() == null) {
            return;
        }

        sb.append(options.containerSeparatorStart());
    }

    private void addItemSeparatorStart(StringBuilder sb, boolean additionalCondition) {
        if (!additionalCondition) {
            return;
        }

        addItemSeparatorStart(sb);
    }

    private void addItemSeparatorEnd(StringBuilder sb) {
        if (options.containerSeparatorEnd() == null) {
            return;
        }

        sb.append(options.containerSeparatorEnd());
    }

    private void addItemSeparatorEnd(StringBuilder sb, boolean additionalCondition) {
        if (!additionalCondition) {
            return;
        }

        addItemSeparatorEnd(sb);
    }

    private boolean isSingleLineType(ItemDefinition<?> def) {
        if (def == null) {
            return false;
        }

        Class<?> type = def.getTypeClass();
        if (type == null) {
            return false;
        }

        return isJavaSimpleType(type)
                || Enum.class.isAssignableFrom(type)
                || XMLGregorianCalendar.class.isAssignableFrom(type)
                || PolyString.class.isAssignableFrom(type)
                || PolyStringType.class.isAssignableFrom(type)
                || ObjectReferenceType.class.isAssignableFrom(type)
                || com.evolveum.prism.xml.ns._public.types_3.ObjectReferenceType.class.isAssignableFrom(type);
    }

    public static boolean isJavaSimpleType(Class<?> type) {
        return type.isPrimitive() ||
                type.equals(String.class) ||
                type.equals(Boolean.class) ||
                type.equals(Byte.class) ||
                type.equals(Character.class) ||
                type.equals(Short.class) ||
                type.equals(Integer.class) ||
                type.equals(Long.class) ||
                type.equals(Float.class) ||
                type.equals(Double.class) ||
                type.equals(Void.class);
    }

    private <O extends ObjectType> String prettyPrintObjectTypeClass(Class<O> type) {
        if (type == null) {
            return "";
        }

        ObjectTypes t = ObjectTypes.getObjectTypeIfKnown(type);
        if (t == null) {
            return type.getSimpleName();
        }

        return translateObjectType(t.getTypeQName());
    }

    public <O extends ObjectType> String prettyPrintObjectDelta(ObjectDelta<O> delta, int indent) {
        return prettyPrintObjectDelta(delta, false, indent);
    }

    public <O extends ObjectType> String prettyPrintObjectDelta(ObjectDelta<O> delta, boolean useEstimatedOld, int indent) {
        if (delta == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent));
        sb.append(delta.getOid());
        sb.append(", ");
        sb.append(prettyPrintObjectTypeClass(delta.getObjectTypeClass()));
        sb.append(" (");
        sb.append(translateEnum(delta.getChangeType()));   // todo localization?
        sb.append(")");
        sb.append(": ");
        sb.append("\n");

        if (delta.getObjectToAdd() != null) {
            if (options.showFullAddObjectDelta()) {
                String object = prettyPrintItem(delta.getObjectToAdd(), indent + 1);
                sb.append(object);
            } else {
                sb.append(prettyPrintObjectSimple(delta.getObjectToAdd(), indent + 1));
            }
        }

        List<String> itemDeltaStr = delta.getModifications().stream()
                .map(d -> prettyPrintItemDelta(d, useEstimatedOld, indent + 1))
                .filter(StringUtils::isNotEmpty)
                .toList();

        sb.append(StringUtils.join(itemDeltaStr, "\n"));

        return sb.toString();
    }

    private String prettyPrintObjectSimple(PrismObject<?> object, int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent));

        if (object.getName() != null) {
            sb.append(object.getName().getOrig());
        }

        sb.append(" (");

        if (object.getOid() != null) {
            sb.append(object.getOid());
            sb.append(", ");
        }

        // noinspection unchecked
        sb.append(prettyPrintObjectTypeClass((Class<? extends ObjectType>) object.getCompileTimeClass()));

        sb.append(")");

        return sb.toString();
    }

    public String prettyPrintItemDelta(ItemDelta<?, ?> item, int indent) {
        return prettyPrintItemDelta(item, false, indent);
    }

    public String prettyPrintItemDelta(ItemDelta<?, ?> item, boolean useEstimatedOld, int indent) {
        if (item == null) {
            return "";
        }

        if (item.isOperational() && !options.showOperational()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        if (options.showDeltaItemPath()) {
            sb.append(indent(indent));
            sb.append(item.getPath());
            sb.append(": ");
        }

        boolean canUseSingleLine = isSingleLineType(item.getDefinition());

        if (useEstimatedOld) {
            String old = prettyPrintItemModifications(null, item.getEstimatedOldValues(), indent + 1, canUseSingleLine);
            if (StringUtils.isNotEmpty(old)) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(old);
            }

            return sb.toString();
        }
        prettyPrintItemModifications(sb, ModificationType.ADD, item.getValuesToAdd(), indent + 1, canUseSingleLine);
        prettyPrintItemModifications(sb, ModificationType.DELETE, item.getValuesToDelete(), indent + 1, canUseSingleLine);
        prettyPrintItemModifications(sb, ModificationType.REPLACE, item.getValuesToReplace(), indent + 1, canUseSingleLine);

        return sb.toString();
    }

    private void prettyPrintItemModifications(StringBuilder sb, ModificationType modificationType, Collection<?> values, int indent, boolean canUseSingleLine) {
        String itemModifications = prettyPrintItemModifications(modificationType, values, indent, canUseSingleLine);
        if (StringUtils.isNotEmpty(itemModifications)) {
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(itemModifications);
        }
    }

    private String prettyPrintItemModifications(ModificationType modificationType, Collection<?> values, int indent, boolean canUseSingleLine) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        // todo localization
        String operation;
        if (modificationType == null) {
            operation = translate(KEY_ESTIMATED_OLD, KEY_ESTIMATED_OLD_DEFAULT);
        } else {
            operation = switch (modificationType) {
                case ADD -> translate(KEY_ADD, KEY_ADD_DEFAULT);
                case DELETE -> translate(KEY_DELETE, KEY_DELETE_DEFAULT);
                case REPLACE -> translate(KEY_REPLACE, KEY_REPLACE_DEFAULT);
            };
        }

        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent));
        sb.append(operation);
        sb.append(" ");

        int valueIndent = canUseSingleLine ? 0 : indent + 1;
        List<String> valuesStr = values.stream()
                .map(v -> prettyPrintValue((PrismValue) v, valueIndent))
                .toList();

        if (canUseSingleLine) {
            sb.append(valuesStr.stream().collect(Collectors.joining(", ")));
        } else {
            valuesStr.forEach(v -> {
                sb.append("\n");
                sb.append(v);
            });
        }

        return sb.toString();
    }

    private String getItemName(Item<?, ?> item) {
        String defaultValue = getDefaultItemName(item);

        ItemDefinition<?> def = item.getDefinition();
        if (def == null) {
            return defaultValue;
        }

        if (!useLocalization()) {
            return defaultValue;
        }

        String displayName = def.getDisplayName() != null ? def.getDisplayName() : defaultValue;

        return translate(displayName, displayName);
    }

    private String getDefaultItemName(Item<?, ?> item) {
        ItemDefinition<?> def = item.getDefinition();
        if (def == null) {
            return PrettyPrinter.prettyPrint(item.getElementName());
        }

        String namespace = item.getElementName().getNamespaceURI();
        String localPart = item.getElementName().getLocalPart();
        if (SchemaConstantsGenerated.NS_COMMON.equals(namespace)) {
            return localPart;
        }

        SchemaDescription description = PrismContext.get().getSchemaRegistry().getSchemaDescriptions().stream()
                .filter(sd -> Objects.equals(sd.getNamespace(), namespace))
                .findFirst()
                .orElse(null);

        if (description != null && description.getUsualPrefix() != null) {
            return description.getUsualPrefix() + ":" + localPart;
        }

        return PrettyPrinter.prettyPrint(item.getElementName());
    }

    public String prettyPrintValue(PrismValue value, int indent) {
        StringBuilder sb = new StringBuilder();

        if (value instanceof PrismPropertyValue<?> ppv) {
            sb.append(prettyPrintPropertyValue(ppv, indent));
        } else if (value instanceof PrismContainerValue<?> pcv) {
            sb.append(prettyPrintContainerValue(pcv, indent));
        } else if (value instanceof PrismReferenceValue prv) {
            sb.append(prettyPrintReferenceValue(prv));
        } else {
            sb.append(PrettyPrinter.prettyPrint(value));
        }

        return sb.toString();
    }

    private boolean isSingleValueContainer(PrismContainerable<?> container) {
        if (container == null) {
            return false;
        }
        PrismContainerDefinition<?> def = container.getDefinition();
        return def != null && def.isSingleValue();
    }

    public String prettyPrintContainerValue(PrismContainerValue<?> pcv, int indent) {
        StringBuilder sb = new StringBuilder();

        boolean isObjectValue = pcv instanceof PrismObjectValue<?>;
        boolean isSingleValueContainer = isSingleValueContainer(pcv.getParent());
        if (!isObjectValue && !isSingleValueContainer) {
            sb.append(indent(indent));
            sb.append(pcv.getId());
            sb.append(":");

            addItemSeparatorStart(sb);

            sb.append("\n");
        } else {
            addItemSeparatorStart(sb);
        }

        String values = pcv.getItems().stream()
                .map(item -> prettyPrintItem(item, isObjectValue || isSingleValueContainer ? indent : indent + 1))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n"));

        sb.append(values);

        addItemSeparatorEnd(sb);

        return sb.toString();
    }

    public String prettyPrintPropertyValue(PrismPropertyValue<?> ppv, int indent) {
        Object value = ppv.getValue();

        String result;
        if (value instanceof Enum<?> e) {
            result = translateEnum(e);
        } else if (value instanceof XMLGregorianCalendar cal) {
            result = translateXmlGregorianCalendar(cal);
        } else if (value instanceof AbstractPlainStructured) {
            result = ToStringBuilder.reflectionToString(value, options.toStringStyle());
        } else {
            result = PrettyPrinter.prettyPrint(value);
        }

        return indent(indent) + result;
    }

    private String translateXmlGregorianCalendar(XMLGregorianCalendar cal) {
        if (cal == null) {
            return "";
        }

        if (!useLocalization()) {
            return cal.toString();
        }

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, getLocale());
        return dateFormat.format(cal.toGregorianCalendar().getTime());
    }

    public String prettyPrintReferenceValue(PrismReferenceValue value) {
        // todo reference filter

        StringBuilder sb = new StringBuilder();

        if (value.getTargetName() != null) {
            sb.append(value.getTargetName());
        } else if (value.getOid() != null) {
            sb.append(value.getOid());
        } else if (value.getFilter() != null) {
            sb.append(translate(KEY_FILTER, KEY_FILTER_DEFAULT));
        } else {
            sb.append(translate(KEY_UNDEFINED, KEY_UNDEFINED_DEFAULT));
        }

        if (value.getTargetType() != null) {
            sb.append(" (");
            sb.append(translateObjectType(value.getTargetType()));
            sb.append(")");
        }

        return sb.toString();
    }

    private String translateObjectType(QName type) {
        if (type == null) {
            return "";
        }

        if (!useLocalization()) {
            return type.getLocalPart();
        }

        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);
        String key = LocalizationUtil.createKeyForEnum(ot);

        return translate(key, type.getLocalPart());
    }

    private String translateEnum(Enum<?> e) {
        if (e == null) {
            return "";
        }

        if (!useLocalization()) {
            return e.name();
        }

        String key = LocalizationUtil.createKeyForEnum(e);
        return translate(key, e.name());
    }

    private LocalizationService getLocalizationService() {
        return localizationService != null ? localizationService : options.localizationService();
    }

    private Locale getLocale() {
        return locale != null ? locale : options.locale();
    }

    private boolean useLocalization() {
        return getLocalizationService() != null && getLocale() != null;
    }

    private String translate(String key, String defaultValue) {
        LocalizationService localizationService = getLocalizationService();
        Locale locale = getLocale();

        if (localizationService == null || locale == null) {
            return defaultValue;
        }

        return localizationService.translate(key, new Object[0], locale, defaultValue);
    }
}
