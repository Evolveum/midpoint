package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.LoginEventType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * @author mederly
 */
public class ValueUtil {
    public static String toStringValue(PrismPropertyValue propertyValue) {
        Object value = propertyValue.getValue();
        if (value == null) {
            return null;
        } else if (value instanceof String) {
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
        } else if (value instanceof ApprovalSchemaType) {
            ApprovalSchemaType approvalSchemaType = (ApprovalSchemaType) value;
            return approvalSchemaType.getName() + (approvalSchemaType.getDescription() != null ? (": " + approvalSchemaType.getDescription()) : "") + " (...)";
        } else if (value instanceof ConstructionType) {
            ConstructionType ct = (ConstructionType) value;
            Object resource = (ct.getResource() != null ? ct.getResource().getName() : (ct.getResourceRef() != null ? ct.getResourceRef().getOid() : null));
            return "resource object" + (resource != null ? " on " + resource : "") + (ct.getDescription() != null ? ": " + ct.getDescription() : "");
        }
        else {
            return "(a value of type " + value.getClass().getName() + ")";  // todo i18n
        }
    }

    public static String toStringValue(PrismReferenceValue ref) {

        if (ref.getObject() != null) {
            return ref.getObject().toString();
        } else {
            return (ref.getTargetType() != null ? (ref.getTargetType().getLocalPart()+":") : "") + ref.getOid();
        }
    }
}
