/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.binding.AbstractPlainStructured;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Viliam Repan
 */
public class UserFriendlyPrettyPrinter {

    public static final String DEFAULT_INDENT = "  ";

    private static final ToStringStyle TO_STRING_STYLE = new UserFriendlyToStringStyle();

    private String indent = DEFAULT_INDENT;

    private LocalizationService localizationService;

    private Locale locale = Locale.getDefault();

    public UserFriendlyPrettyPrinter localizationService(LocalizationService localizationService) {
        this.localizationService = localizationService;
        return this;
    }

    public UserFriendlyPrettyPrinter locale(Locale locale) {
        this.locale = locale != null ? locale : Locale.getDefault();
        return this;
    }

    public UserFriendlyPrettyPrinter indent(String indent) {
        this.indent = indent;

        return this;
    }

    private String indent(int indent) {
        return StringUtils.repeat(this.indent, indent);
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
        } else if (canUseSingleLine && item.size() == 1 && isSingleLineType(item)) {
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

        return sb.toString();
    }

    private boolean isSingleLineType(Item<?, ?> item) {
        ItemDefinition<?> def = item.getDefinition();
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

    private String getItemName(Item<?, ?> item) {
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
            sb.append(":\n");
        }

        String values = pcv.getItems().stream()
                .map(item -> prettyPrintItem(item, isObjectValue || isSingleValueContainer ? indent : indent + 1))
                .collect(Collectors.joining("\n"));

        sb.append(values);

        return sb.toString();
    }

    public String prettyPrintPropertyValue(PrismPropertyValue<?> ppv, int indent) {
        Object value = ppv.getValue();

        String result;
        if (value instanceof AbstractPlainStructured) {
            result = ToStringBuilder.reflectionToString(value, TO_STRING_STYLE);
        } else {
            result = PrettyPrinter.prettyPrint(value);
        }

        return indent(indent) + result;
    }

    public String prettyPrintReferenceValue(PrismReferenceValue value) {
        // todo reference filter

        StringBuilder sb = new StringBuilder();

        if (value.getTargetName() != null) {
            sb.append(value.getTargetName());
        } else if (value.getOid() != null) {
            sb.append(value.getOid());
        } else if (value.getFilter() != null) {
            sb.append(translate("UserFriendlyPrettyPrinter.filter", "filter"));
        } else {
            sb.append(translate("UserFriendlyPrettyPrinter.undefined", "undefined"));
        }

        if (value.getTargetType() != null) {
            sb.append(" (");
            sb.append(translateObjectType(value.getTargetType()));
            sb.append(")");
        }

        return sb.toString();
    }

    private String translateObjectType(QName type) {
        ObjectTypes ot = ObjectTypes.getObjectTypeFromTypeQName(type);

        String key = LocalizationUtil.createKeyForEnum(ot);

        return translate(key, type.getLocalPart());
    }

    private String translate(String key, String defaultValue) {
        if (localizationService == null || locale == null) {
            return defaultValue;
        }

        return localizationService.translate(key, new Object[0], locale, defaultValue);
    }

    private static class UserFriendlyToStringStyle extends ToStringStyle {

        public UserFriendlyToStringStyle() {
            setUseIdentityHashCode(false);
            setUseShortClassName(true);
        }

        @Override
        public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
            if (value != null) {
                super.append(buffer, fieldName, value, fullDetail);
            }
        }

        @Override
        public void append(StringBuffer buffer, String fieldName, Object[] array, Boolean fullDetail) {
            if (array != null) {
                super.append(buffer, fieldName, array, fullDetail);
            }
        }
    }
}
