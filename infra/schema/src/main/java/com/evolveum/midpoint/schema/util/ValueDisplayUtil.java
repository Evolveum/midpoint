/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.*;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * TODO unify with PrettyPrinter somehow
 */
public class ValueDisplayUtil {

    public static PolyString toStringValue(PrismPropertyValue propertyValue) {
        if (propertyValue == null || propertyValue.getValue() == null){
            return null;
        }
        return toStringValue(propertyValue.getValue());
    }

    private static PolyString createPolyString(String orig, String key, Object... params) {
        PolyString poly = new PolyString(orig);

        if (key == null) {
            return poly;
        }

        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey(key);
        translation.fallback(orig);
        Arrays.stream(params).forEach(param -> {

            PolyStringTranslationArgumentType arg;
            if (param instanceof PolyStringTranslationArgumentType) {
                arg = (PolyStringTranslationArgumentType) param;
            } else {
                String argKey = Objects.toString(param);
                arg = new PolyStringTranslationArgumentType(argKey);
            }

            translation.getArgument().add(arg);
        });

        poly.setTranslation(translation);

        return poly;
    }

    private static String calendarToStringValue(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }

        return dateToStringValue(calendar.toGregorianCalendar().getTime());
    }

    private static String dateToStringValue(Date date) {
        if (date == null) {
            return null;
        }

        return date.toLocaleString();  // todo fix localization
    }

    private static PolyString toStringValue(Object value) {
        String param = value.getClass().getSimpleName();
        PolyString defaultStr = createPolyString("(a value of type " + param + ")", "ValueDisplayUtil.defaultStr", param);

        if (value instanceof String) {
            return new PolyString((String) value);
        } else if (value instanceof PolyString) {
            return (PolyString) value;
        } else if (value instanceof ProtectedStringType) {
            return createPolyString("(protected string)", "ValueDisplayUtil.protectedString");
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return new PolyString(value.toString());
        } else if (value instanceof XMLGregorianCalendar) {
            String text = calendarToStringValue((XMLGregorianCalendar) value);
            return new PolyString(text);
        } else if (value instanceof Date) {
            return new PolyString(dateToStringValue((Date) value));
        } else if (value instanceof LoginEventType) {
            LoginEventType loginEventType = (LoginEventType) value;
            if (loginEventType.getTimestamp() != null) {
                return new PolyString(calendarToStringValue(loginEventType.getTimestamp()));
            } else {
                return new PolyString("");
            }
        } else if (value instanceof ScheduleType) {
            return new PolyString(SchemaDebugUtil.prettyPrint((ScheduleType) value));
        } else if (value instanceof ApprovalSchemaType) {
            ApprovalSchemaType as = (ApprovalSchemaType) value;
            return new PolyString(as.getName() + (as.getDescription() != null ? (": " + as.getDescription()) : "") + " (...)");
        } else if (value instanceof ConstructionType) {
            ConstructionType ct = (ConstructionType) value;
            String resourceOid = ct.getResourceRef() != null ? ct.getResourceRef().getOid() : null;

            String paramResource = resourceOid != null ? " on " + resourceOid : "";
            String paramDescription = ct.getDescription() != null ? ": " + ct.getDescription() : "";
            String defaultValue = "resource object" + paramResource + paramDescription;

            PolyStringTranslationType resourceArg = new PolyStringTranslationType().key("ValueDisplayUtil.constructionTypeResource");
            resourceArg.getArgument().add(new PolyStringTranslationArgumentType(resourceOid != null ? resourceOid : ""));

            return createPolyString(defaultValue, "ValueDisplayUtil.constructionType", new PolyStringTranslationArgumentType(resourceArg), paramDescription);
        } else if (value instanceof Enum) {
            return createPolyString(value.toString(), value.getClass().getSimpleName() + "." + ((Enum<?>) value).name());
        } else if (value instanceof ResourceAttributeDefinitionType) {
            ResourceAttributeDefinitionType radt = (ResourceAttributeDefinitionType) value;
            ItemPathType ref = radt.getRef();
            String path;
            if (ref != null) {
                path = ref.getItemPath().toString();
            } else {
                path = "(null)";
            }
            StringBuilder sb = new StringBuilder();
            MappingType mappingType = radt.getOutbound();
            if (mappingType != null) {
                if (mappingType.getExpression() == null) {
                    sb.append("Empty mapping for ").append(path);
                } else {
                    sb.append(path).append(" = ");
                    boolean first = true;
                    for (JAXBElement<?> evaluator : mappingType.getExpression().getExpressionEvaluator()) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                        if (QNameUtil.match(SchemaConstants.C_VALUE, evaluator.getName()) && evaluator.getValue() instanceof RawType) {
                            RawType raw = (RawType) evaluator.getValue();
                            try {
                                sb.append(raw.extractString("(a complex value)"));
                            } catch (RuntimeException e) {
                                sb.append("(an invalid value)");
                            }
                        } else {
                            sb.append("(a complex expression)");
                        }
                    }
                }
                if (mappingType.getStrength() != null) {
                    sb.append(" (").append(mappingType.getStrength().value()).append(")");
                }
            } else {
                sb.append("Empty mapping for ").append(path);
            }
            return new PolyString(sb.toString()); // todo fix
        } else if (value instanceof QName) {
            QName qname = (QName) value;
            return new PolyString(qname.getLocalPart());
        } else if (value instanceof Number) {
            return new PolyString(String.valueOf(value));
        } else if (value instanceof byte[]) {
            return createPolyString("(binary data)", "ValueDisplayUtil.binaryData");
        } else if (value instanceof RawType) {
            try {
                Object parsedValue = ((RawType) value).getValue();
                return toStringValue(parsedValue);
            } catch (SchemaException e) {
                return new PolyString(PrettyPrinter.prettyPrint(value));
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
            return new PolyString(sb.toString());
        } else if (value instanceof ExpressionType) {
            ExpressionType expression = (ExpressionType) value;
            StringBuilder expressionString = new StringBuilder();
            if (expression.getExpressionEvaluator() != null) {
                expression.getExpressionEvaluator().forEach(evaluator -> {
                    if (evaluator.getValue() instanceof RawType) {
                        expressionString.append(PrettyPrinter.prettyPrint(evaluator.getValue()));
                        expressionString.append("; ");
                    } else if (evaluator.getValue() instanceof SearchObjectExpressionEvaluatorType) {
                        SearchObjectExpressionEvaluatorType evaluatorValue = (SearchObjectExpressionEvaluatorType) evaluator.getValue();
                        if (evaluatorValue.getFilter() != null) {
                            DebugUtil.debugDumpMapMultiLine(expressionString, evaluatorValue.getFilter().getFilterClauseXNode().toMap(),
                                    0, false, null);

                            //TODO temporary hack: removing namespace part of the QName
                            while (expressionString.indexOf("}") >= 0 && expressionString.indexOf("{") >= 0 &&
                                    expressionString.indexOf("}") - expressionString.indexOf("{") > 0) {
                                expressionString.replace(expressionString.indexOf("{"), expressionString.indexOf("}") + 1, "");
                            }
                        }
                    } else {
                        expressionString.append(defaultStr);
                    }
                });
            }
            return new PolyString(expressionString.toString());
        } else {
            return defaultStr;
        }
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
            return (ref.getTargetType() != null ? ref.getTargetType().getLocalPart()+":" : "")
                    + (ref.getTargetName() != null ? ref.getTargetName() : ref.getOid());
        }
    }
}
