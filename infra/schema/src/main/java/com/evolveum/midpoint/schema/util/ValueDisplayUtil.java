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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.util.Date;

/**
 * TODO unify with PrettyPrinter somehow
 *
 * @author mederly
 */
public class ValueDisplayUtil {
    public static String toStringValue(PrismPropertyValue propertyValue) {
        if (propertyValue == null || propertyValue.getValue() == null){
            return null;
        }
        Object value = propertyValue.getValue();
        String defaultStr = "(a value of type " + value.getClass().getSimpleName() + ")";  // todo i18n
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof PolyString) {
            return ((PolyString) value).getOrig();
        } else if (value instanceof ProtectedStringType) {
            return "(protected string)";        // todo i18n
        } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
            return value.toString();
        } else if (value instanceof XMLGregorianCalendar) {
            return ((XMLGregorianCalendar) value).toGregorianCalendar().getTime().toLocaleString(); // todo fix
        } else if (value instanceof Date) {
            return ((Date) value).toLocaleString(); // todo fix
        } else if (value instanceof LoginEventType) {
            LoginEventType loginEventType = (LoginEventType) value;
            if (loginEventType.getTimestamp() != null) {
                return loginEventType.getTimestamp().toGregorianCalendar().getTime().toLocaleString(); // todo fix
            } else {
                return "";
            }
        } else if (value instanceof ScheduleType) {
            return SchemaDebugUtil.prettyPrint((ScheduleType) value);
        } else if (value instanceof ApprovalSchemaType) {
            ApprovalSchemaType approvalSchemaType = (ApprovalSchemaType) value;
            return approvalSchemaType.getName() + (approvalSchemaType.getDescription() != null ? (": " + approvalSchemaType.getDescription()) : "") + " (...)";
        } else if (value instanceof ConstructionType) {
            ConstructionType ct = (ConstructionType) value;
            Object resource = (ct.getResourceRef() != null ? ct.getResourceRef().getOid() : null);
            return "resource object" + (resource != null ? " on " + resource : "") + (ct.getDescription() != null ? ": " + ct.getDescription() : "");
        } else if (value instanceof Enum) {
            return value.toString();
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
            return sb.toString();
        } else if (value instanceof QName) {
            QName qname = (QName) value;
            return qname.getLocalPart();
//            if (StringUtils.isNotEmpty(qname.getNamespaceURI())) {
//                return qname.getLocalPart() + " (in " + qname.getNamespaceURI() + ")";
//            } else {
//                return qname.getLocalPart();
//            }
        } else if (value instanceof Number) {
            return String.valueOf(value);
        } else if (value instanceof byte[]) {
            return "(binary data)";
        } else if (value instanceof RawType) {
            return PrettyPrinter.prettyPrint(value);
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
            return sb.toString();
        } else if (value instanceof ExpressionType) {
            StringBuilder expressionString = new StringBuilder();
            if (((ExpressionType)value).getExpressionEvaluator() != null && ((ExpressionType) value).getExpressionEvaluator().size() > 0){
                ((ExpressionType) value).getExpressionEvaluator().forEach(evaluator -> {
                    if (evaluator.getValue() instanceof RawType){
                        expressionString.append(PrettyPrinter.prettyPrint(evaluator.getValue()));
                        expressionString.append("; ");
                    } else if (evaluator.getValue() instanceof SearchObjectExpressionEvaluatorType){
                        SearchObjectExpressionEvaluatorType evaluatorValue = (SearchObjectExpressionEvaluatorType)evaluator.getValue();
                        if (evaluatorValue.getFilter() != null) {
                            DebugUtil.debugDumpMapMultiLine(expressionString, evaluatorValue.getFilter().getFilterClauseXNode().toMap(),
                                    0, false, null);

                            //TODO temporary hack: removing namespace part of the QName
                            while (expressionString.indexOf("}") >= 0 && expressionString.indexOf("{") >= 0 &&
                                    expressionString.indexOf("}") - expressionString.indexOf("{") > 0){
                                expressionString.replace(expressionString.indexOf("{"), expressionString.indexOf("}") + 1, "");
                            }
                        }
                    } else {
                        expressionString.append(defaultStr);
                    }
                });
            }
            return expressionString.toString();
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
