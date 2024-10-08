/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * TODO unify with PrettyPrinter somehow
 */
public class ValueDisplayUtil {

    public static LocalizableMessage toStringValue(PrismPropertyValue propertyValue) {
        if (propertyValue == null || propertyValue.getValue() == null) {
            return null;
        }
        return toStringValue(propertyValue.getValue());
    }

    private static LocalizableMessage createMessage(String fallback) {
        return createMessage(fallback, null);
    }

    private static LocalizableMessage createMessage(String fallback, String key, Object... params) {
        return new SingleLocalizableMessage(key, params, fallback);
    }

    private static Date calendarToStringValue(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }

        return calendar.toGregorianCalendar().getTime();
    }

    private static LocalizableMessage toStringValue(Object value) {
        String param = value.getClass().getSimpleName();
        LocalizableMessage msg = createMessage("(a value of type " + param + ")", "ValueDisplayUtil.defaultStr", param);

        if (value instanceof String) {
            return createMessage((String) value);
        } else if (value instanceof PolyString) {
            return createMessage(((PolyString) value).getOrig(), "ValueDisplayUtil.singleValue", value);
        } else if (value instanceof ProtectedStringType) {
            return createMessage("(protected string)", "ValueDisplayUtil.protectedString");
        } else if (value instanceof Boolean) {
            return createMessage(Boolean.toString((boolean) value), "Boolean." + value);
        } else if (value instanceof Integer || value instanceof Long) {
            return createMessage(value.toString());
        } else if (value instanceof XMLGregorianCalendar) {
            Date date = calendarToStringValue((XMLGregorianCalendar) value);
            return createMessage(DateFormat.getDateTimeInstance().format(date), "ValueDisplayUtil.singleValue", date);
        } else if (value instanceof Date) {
            return createMessage(DateFormat.getDateTimeInstance().format((Date) value), "ValueDisplayUtil.singleValue", value);
        } else if (value instanceof LoginEventType) {
            LoginEventType loginEventType = (LoginEventType) value;
            if (loginEventType.getTimestamp() != null) {
                return toStringValue(loginEventType.getTimestamp());
            } else {
                return createMessage("");
            }
        } else if (value instanceof ScheduleType) {
            // todo fix translation
            return createMessage(SchemaDebugUtil.prettyPrint((ScheduleType) value));
        } else if (value instanceof ApprovalSchemaType) {
            ApprovalSchemaType as = (ApprovalSchemaType) value;
            return createMessage(as.getName() + (as.getDescription() != null ? (": " + as.getDescription()) : "") + " (...)");
        } else if (value instanceof ConstructionType) {
            ConstructionType ct = (ConstructionType) value;
            String resourceOid = ct.getResourceRef() != null ? ct.getResourceRef().getOid() : null;

            String paramResource = resourceOid != null ? " on " + resourceOid : "";
            String paramDescription = ct.getDescription() != null ? ": " + ct.getDescription() : "";
            String defaultValue = "resource object" + paramResource + paramDescription;

            return createMessage(defaultValue, "ValueDisplayUtil.constructionType",
                    createMessage(paramResource, "ValueDisplayUtil.constructionTypeResource", resourceOid),
                    paramDescription);
        } else if (value instanceof Enum) {
            return createMessage(value.toString(), value.getClass().getSimpleName() + "." + ((Enum<?>) value).name());
        } else if (value instanceof ResourceAttributeDefinitionType) {
            ResourceAttributeDefinitionType radt = (ResourceAttributeDefinitionType) value;
            return resourceAttributeDefinitionTypeToString(radt);
        } else if (value instanceof QName) {
            QName qname = (QName) value;
            return createMessage(qname.getLocalPart());
        } else if (value instanceof Number) {
            return createMessage(String.valueOf(value));
        } else if (value instanceof byte[]) {
            return createMessage("(binary data)", "ValueDisplayUtil.binaryData");
        } else if (value instanceof RawType) {
            try {
                Object parsedValue = ((RawType) value).getValue();
                return toStringValue(parsedValue);
            } catch (SchemaException e) {
                return createMessage(PrettyPrinter.prettyPrint(value));
            }
        } else if (value instanceof ItemPathType) {
            ItemPath itemPath = ((ItemPathType) value).getItemPath();
            StringBuilder sb = new StringBuilder();
            itemPath.getSegments().forEach(segment -> {
                if (ItemPath.isName(segment)) {
                    sb.append(PrettyPrinter.prettyPrint(ItemPath.toName(segment)));
                } else if (ItemPath.isVariable(segment)) {
                    sb.append(PrettyPrinter.prettyPrint(ItemPath.toVariableName(segment)));
                } else {
                    sb.append(segment.toString());
                }
                sb.append("; ");
            });
            return createMessage(sb.toString());
        } else if (value instanceof ExpressionType) {
            ExpressionType expression = (ExpressionType) value;
            StringBuilder expressionString = new StringBuilder();
            if (expression.getExpressionEvaluator() != null) {
                expression.getExpressionEvaluator().forEach(evaluator -> {
                    if (evaluator.getValue() instanceof RawType) {
                        expressionString.append(PrettyPrinter.prettyPrint(evaluator.getValue()));
                        expressionString.append("; ");
                    } else if (evaluator.getValue() instanceof SearchObjectExpressionEvaluatorType searchEvaluatorValue) {
                        for (var filter : searchEvaluatorValue.getFilter()) {
                            DebugUtil.debugDumpMapMultiLine(expressionString, filter.getFilterClauseXNode().toMap(),
                                    0, false, null);

                            //TODO temporary hack: removing namespace part of the QName
                            while (expressionString.indexOf("}") >= 0 && expressionString.indexOf("{") >= 0 &&
                                    expressionString.indexOf("}") - expressionString.indexOf("{") > 0) {
                                expressionString.replace(expressionString.indexOf("{"), expressionString.indexOf("}") + 1, "");
                            }
                        }
                    }
                });
            }
            if (!expressionString.isEmpty()) {
                return createMessage(expressionString.toString());
            }
        }
        return msg;
    }

    private static LocalizableMessage resourceAttributeDefinitionTypeToString(ResourceAttributeDefinitionType radt) {
        ItemPathType ref = radt.getRef();
        String path;

        LocalizableMessage pathArg;
        if (ref != null) {
            path = ref.getItemPath().toString();
            pathArg = createMessage(path);
        } else {
            path = "(null)";
            pathArg = createMessage(path, "ValueDisplayUtil.nullPath");
        }

        StringBuilder sb = new StringBuilder();
        MappingType mappingType = radt.getOutbound();

        if (mappingType == null || mappingType.getExpression() == null) {
            sb.append("Empty mapping for ").append(path);

            LocalizableMessage strengthArg = createMessage("");
            if (mappingType.getStrength() != null) {
                String strength = " (" + mappingType.getStrength().value() + ")";
                sb.append(strength);
                strengthArg = createMessage(strength, "ValueDisplayUtil.mappingStrength",
                        createMessage(mappingType.getStrength().value(), MappingStrengthType.class.getSimpleName() + "." + mappingType.getStrength().value()));
            }

            return createMessage(sb.toString(), "ValueDisplayUtil.emptyMapping", pathArg, strengthArg);
        }

        List<LocalizableMessage> msgs = new ArrayList<>();

        sb.append(path).append(" = ");
        boolean first = true;
        for (JAXBElement<?> evaluator : mappingType.getExpression().getExpressionEvaluator()) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }

            msgs.add(createExpressionEvaluatorArg(evaluator, sb));
        }

        return new LocalizableMessageList(
                msgs,
                createMessage(", ", "ValueDisplayUtil.resourceAttributeDefinitionSeparator"),
                createMessage(path + " = ", "ValueDisplayUtil.resourceAttributeDefinition", pathArg),
                null);

    }

    private static LocalizableMessage createExpressionEvaluatorArg(JAXBElement<?> evaluator, StringBuilder sb) {
        LocalizableMessage msg;

        if (QNameUtil.match(SchemaConstants.C_VALUE, evaluator.getName()) && evaluator.getValue() instanceof RawType) {
            RawType raw = (RawType) evaluator.getValue();
            try {
                String fallback = raw.extractString("(a complex value)");
                try {
                    msg = toStringValue(raw.getValue());
                } catch (SchemaException ex) {
                    // intentionally ignore
                    msg = createMessage(fallback, fallback);
                }

                String rawString = raw.extractString("(a complex value)");
                sb.append(rawString);
            } catch (RuntimeException e) {
                String invalid = "(an invalid value)";
                sb.append(invalid);
                msg = createMessage(invalid, "ValueDisplayUtil.invalidValue");
            }
        } else {
            String complex = "(a complex expression)";
            sb.append(complex);
            msg = createMessage(complex, "ValueDisplayUtil.complexExpression");
        }

        return msg;
    }

    public static String toStringValue(PrismReferenceValue ref) {
        String rv = getReferredObjectInformation(ref);
        if (ref.getRelation() != null) {
            rv += " [" + ref.getRelation().getLocalPart() + "]";
        }
        return rv;
    }

    private static String getReferredObjectInformation(PrismReferenceValue ref) {
        if (ref.getObject() != null) {
            return ref.getObject().toString();
        } else {
            return (ref.getTargetType() != null ? ref.getTargetType().getLocalPart() + ":" : "")
                    + (ref.getTargetName() != null ? ref.getTargetName() : ref.getOid());
        }
    }
}
