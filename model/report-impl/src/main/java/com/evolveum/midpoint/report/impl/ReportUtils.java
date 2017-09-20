/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;

import java.io.File;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

/**
 * Utility methods for report. Mostly pretty print functions. Do not use any
 * "prism" object and anything related to them. Methods has to work with both,
 * common schema types and extended schema types (prism)
 *
 * @author Katarina Valalikova
 * @author Martin Lizner
 *
 */
public class ReportUtils {

    private static String MIDPOINT_HOME = System.getProperty("midpoint.home");
    private static String EXPORT_DIR = MIDPOINT_HOME + "export/";

    private static final Trace LOGGER = TraceManager
            .getTrace(ReportUtils.class);

    public static Timestamp convertDateTime(XMLGregorianCalendar dateTime) {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try {
            timestamp = new Timestamp(dateTime.toGregorianCalendar().getTimeInMillis());
        } catch (Exception ex) {
            LOGGER.trace("Incorrect date time value {}", dateTime);
        }

        return timestamp;
    }

    public static String prettyPrintPerformerOrAssigneesForReport(PrismContainerValue<AbstractWorkItemType> workItemPcv) {
        if (workItemPcv == null) {      // should not occur
            return "";
        }
        AbstractWorkItemType workItem = workItemPcv.asContainerable();
        if (workItem.getPerformerRef() != null && workItem.getOutput() != null
                && (workItem.getOutput().getOutcome() != null || StringUtils.isNotBlank(workItem.getOutput().getComment()))) {
            // performer is shown only if there's a real outcome (either result or comment)
            return prettyPrintForReport(workItem.getPerformerRef(), false);
        } else {
            return "(" + prettyPrintReferencesForReport(workItem.getAssigneeRef(), false) + ")";
        }
    }

    public static String prettyPrintOutputChangeForReport(PrismContainerValue<AccessCertificationWorkItemType> workItemPcv) {
        if (workItemPcv == null) {      // should not occur
            return "";
        }
        AccessCertificationWorkItemType workItem = workItemPcv.asContainerable();
        if (workItem.getOutputChangeTimestamp() != null && workItem.getOutput() != null
                && (workItem.getOutput().getOutcome() != null || StringUtils.isNotBlank(workItem.getOutput().getComment()))) {
            // output change timestamp is shown only if there's a real outcome (either result or comment)
            return prettyPrintForReport(workItem.getOutputChangeTimestamp());
        } else {
            return "";
        }
    }

    public static String prettyPrintReferencesForReport(@NotNull List<ObjectReferenceType> references, boolean showType) {
        return references.stream()
                .map(ref -> prettyPrintForReport(ref, showType))
                .collect(Collectors.joining(", "));
    }

    public static String prettyPrintCertOutcomeForReport(String uri, boolean noResponseIfEmpty) {
        return prettyPrintForReport(OutcomeUtils.fromUri(uri), noResponseIfEmpty);
    }

    public static String prettyPrintCertOutcomeForReport(String uri) {
        return prettyPrintCertOutcomeForReport(uri, false);
    }

    public static String prettyPrintCertOutcomeForReport(AbstractWorkItemOutputType output) {
        return prettyPrintCertOutcomeForReport(output, false);
    }

    public static String prettyPrintCertOutcomeForReport(AbstractWorkItemOutputType output, boolean noResponseIfEmpty) {
        String outcome = output != null ? output.getOutcome() : null;
        if (noResponseIfEmpty && outcome == null) {
            outcome = SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE;
        }
        return prettyPrintCertOutcomeForReport(outcome, noResponseIfEmpty);
    }

    public static String prettyPrintCertCommentForReport(AbstractWorkItemOutputType output) {
        return output != null ? output.getComment() : null;
    }

    public static String prettyPrintForReport(XMLGregorianCalendar dateTime) {
        if (dateTime == null) {
            return "";
        }
        SimpleDateFormat formatDate = new SimpleDateFormat();
        return formatDate.format(new Date(dateTime.toGregorianCalendar().getTimeInMillis()));
    }

    public static String prettyPrintForReport(Date date) {
        if (date == null) {
            return "";
        }
        SimpleDateFormat formatDate = new SimpleDateFormat();
        return formatDate.format(date);
    }

    public static String getDateTime() {
        Date createDate = new Date(System.currentTimeMillis());
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy hh-mm-ss");
        return formatDate.format(createDate);
    }

    public static String getReportOutputFilePath(ReportType reportType) {
        File exportFolder = new File(EXPORT_DIR);
        if (!exportFolder.exists() || !exportFolder.isDirectory()) {
            exportFolder.mkdir();
        }

        String output = EXPORT_DIR + reportType.getName().getOrig() + " " + getDateTime();

        switch (reportType.getExport()) {
            case PDF:
                output = output + ".pdf";
                break;
            case CSV:
                output = output + ".csv";
                break;
            case XML:
                output = output + ".xml";
                break;
            case XML_EMBED:
                output = output + "_embed.xml";
                break;
            case HTML:
                output = output + ".html";
                break;
            case RTF:
                output = output + ".rtf";
                break;
            case XLS:
                output = output + ".xls";
                break;
            case ODT:
                output = output + ".odt";
                break;
            case ODS:
                output = output + ".ods";
                break;
            case DOCX:
                output = output + ".docx";
                break;
            case XLSX:
                output = output + ".xlsx";
                break;
            case PPTX:
                output = output + ".pptx";
                break;
            case XHTML:
                output = output + ".x.html";
                break;
            case JXL:
                output = output + ".jxl.xls";
                break;
            default:
                break;
        }

        return output;
    }

    public static String getPropertyString(String key) {
        return getPropertyString(key, (String) null);
    }

    public static String getPropertyString(String key, String defaultValue) {
        String val = (defaultValue == null) ? key : defaultValue;
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("localization/schema", new Locale("en", "US"));
        } catch (MissingResourceException e) {
            return (defaultValue != null) ? defaultValue : key; //workaround for Jasper Studio
        }
        if (bundle != null && bundle.containsKey(key)) {
            val = bundle.getString(key);
        }
        return val;
    }

	public static String getPropertyString(String key, Object values, String defaultValue) {
		if (key == null || values == null) {
			return defaultValue;
		}

		if (!List.class.isAssignableFrom(values.getClass())) {
			return getPropertyString((key.endsWith(".") ? key + values.toString() : key + "." + values.toString()), defaultValue);
		}
		List listValues=  (List) values;
		StringBuilder builder = new StringBuilder();
		Iterator<Object> objects = listValues.iterator();
		ResourceBundle bundle;
		try {
			bundle = ResourceBundle.getBundle("localization/schema", new Locale("en", "US"));
		} catch (MissingResourceException e) {
			return defaultValue.toString() != null ? defaultValue.toString() : key; // workaround for Jasper Studio
		}

		while (objects.hasNext()) {
			Object o = objects.next();
			if (o.getClass().isEnum()) {
				String constructedKey = (key.endsWith(".")) ? key + ((Enum) o).name(): key +"." + ((Enum) o).name();
				if (bundle != null && bundle.containsKey(constructedKey)) {
					builder.append(bundle.getString(constructedKey));
				} else {
					builder.append(prettyPrintForReport(o));
				}
			} else {
				String constructedKey = (key.endsWith(".")) ? key + o.toString() : key + "." + o.toString();
				if (bundle != null && bundle.containsKey(key)) {
					builder.append(bundle.getString(constructedKey));
				} else {
					builder.append(prettyPrintForReport(o));
				}
			}
			if (objects.hasNext()) {
				builder.append(", ");
			}
		}

		return builder.toString();

	}

    public static String prettyPrintForReport(QName qname) {
        String ret = "";
        if (qname.getLocalPart() != null) {
            ret = qname.getLocalPart();
        }
        return ret;
    }

    public static String prettyPrintForReport(ProtectedStringType pst) {
        return "*****";
    }

    public static String prettyPrintForReport(PrismPropertyValue<?> ppv) {
        String retPPV;
        try {
            retPPV = prettyPrintForReport(ppv.getValue());
        } catch (Throwable t) {
            return "N/A"; // rare case e.g. for password-type in resource
        }
        return retPPV;
    }

    public static String prettyPrintForReport(PrismContainerValue<?> pcv) {
        StringBuilder sb = new StringBuilder();
        for (Item<?, ?> item : pcv.getItems()) {
            if ("metadata".equals(item.getElementName().getLocalPart())) {
                continue;
            }
            sb.append(prettyPrintForReport(item.getElementName()));
            sb.append("=");
            sb.append("{");
            for (PrismValue item2 : item.getValues()) {
                sb.append(prettyPrintForReport(item2));
                sb.append(", ");
            }
            sb.setLength(Math.max(sb.length() - 2, 0)); // delete last delimiter
            sb.append("}");
            sb.append(", ");
        }
        sb.setLength(Math.max(sb.length() - 2, 0)); // delete last delimiter
        return sb.toString();
    }

    public static String prettyPrintForReport(PrismReferenceValue prv) {
        return prettyPrintForReport(prv, true);
    }

    public static String prettyPrintForReport(PrismReferenceValue prv, boolean showType) {
        StringBuilder sb = new StringBuilder();
        if (showType) {
            sb.append(getTypeDisplayName(prv.getTargetType()));
            sb.append(": ");
        }
        if (prv.getTargetName() != null) {
            sb.append(prv.getTargetName());
        } else {
            sb.append(prv.getOid());
        }
        return sb.toString();
    }

    public static String prettyPrintForReport(ObjectReferenceType prv) {
        return prettyPrintForReport(prv, true);
    }

    public static String prettyPrintForReport(ObjectReferenceType prv, boolean showType) {
        if (prv == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (showType || prv.getTargetName() == null) {
            sb.append(getTypeDisplayName(prv.getType()));
            sb.append(": ");
        }
        if (prv.getTargetName() != null) {
            sb.append(prv.getTargetName());
        } else {
            sb.append(prv.getOid());
        }
        return sb.toString();
    }

    public static String prettyPrintUsersForReport(List<ObjectReferenceType> userRefList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ObjectReferenceType userRef : userRefList) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(prettyPrintForReport(userRef, false));
        }
        return sb.toString();
    }

    public static String prettyPrintForReport(OperationResultType ort) {
        StringBuilder sb = new StringBuilder();
        if (ort.getOperation() != null) {
            sb.append(ort.getOperation());
            sb.append(" ");
        }
        //sb.append(ort.getParams()); //IMPROVE_ME: implement prettyPrint for List<EntryType>
        //sb.append(" ");
        if (ort.getMessage() != null) {
            sb.append(ort.getMessage());
            sb.append(" ");
        }
        sb.append(ort.getStatus());
        return sb.toString();
    }

    public static String prettyPrintForReport(byte[] ba) {
        if (ba == null) {
            return "null";
        }
        return "[" + ((byte[]) ba).length + " bytes]"; //Jasper doesnt like byte[]
    }

    public static String prettyPrintForReport(Collection prismValueList) {
        StringBuilder sb = new StringBuilder();
        for (Object pv : prismValueList) {
            String ps = prettyPrintForReport(pv);
            if (!ps.isEmpty()) {
                sb.append(ps);
                sb.append("#");
            }
        }
        sb.setLength(Math.max(sb.length() - 1, 0)); // delete last # delimiter
        return sb.toString();
    }

    /*
     Multiplexer method for various input classes, using Reflection
     - Mostly Copied from com.evolveum.midpoint.util.PrettyPrinter
     - Credit goes to Evolveum
     */
    public static String prettyPrintForReport(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof MetadataType) {
            return "";
        }

        //special handling for byte[], some problems with jasper when printing
        if (byte[].class.equals(value.getClass())) {
            return prettyPrintForReport((byte[]) value);
        }

        // 1. Try to find prettyPrintForReport in this class first
        if (value instanceof Containerable) { //e.g. RoleType needs to be converted to PCV in order to format properly
            value = (((Containerable) value).asPrismContainerValue());
        }

        for (Method method : ReportUtils.class.getMethods()) {
            if (method.getName().equals("prettyPrintForReport")) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].equals(value.getClass())) {
                    try {
                        return (String) method.invoke(null, value);
                    } catch (Throwable e) {
                        return "###INTERNAL#ERROR### " + e.getClass().getName() + ": " + e.getMessage() + "; prettyPrintForReport method for value " + value;
                    }
                }
            }
        }

        // 2. Default to PrettyPrinter.prettyPrint
        String str = PrettyPrinter.prettyPrint(value);
        if (str.length() > 1000) {
            return str.substring(0, 1000);
        }
        return str;

    }

    private static String printItemDeltaValues(ItemDeltaType itemDelta) {
        List values = itemDelta.getValue();
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            String v = printItemDeltaValue(itemDelta.getPath(), value);
            if (StringUtils.isNotBlank(v)) {
                sb.append(v);
                sb.append(", ");
            }
        }
        sb.setLength(Math.max(sb.length() - 2, 0)); // delete last delimiter
        return sb.toString();
    }

    private static String printItemDeltaValue(ItemPathType itemPath, Object value) {
        if (value instanceof MetadataType) {
            return "";
        } else if (value instanceof RawType) {
            try {

                if (isMetadata(itemPath)) {
                    return "";
                }

                Object parsedRealValue = ((RawType) value).getParsedRealValue(null, itemPath.getItemPath());
                if (parsedRealValue instanceof Containerable) { // this is for PCV
                    return prettyPrintForReport(((Containerable) parsedRealValue).asPrismContainerValue());
                }
                return prettyPrintForReport(parsedRealValue);
            } catch (SchemaException e) {
                return "###INTERNAL#ERROR### " + e.getClass().getName() + ": " + e.getMessage() + "; prettyPrintForReport method for value " + value;
            } catch (RuntimeException e) {
                return "###INTERNAL#ERROR### " + e.getClass().getName() + ": " + e.getMessage() + "; prettyPrintForReport method for value " + value;
            } catch (Exception e) {
                return "###INTERNAL#ERROR### " + e.getClass().getName() + ": " + e.getMessage() + "; prettyPrintForReport method for value " + value;
            }
        } else {
            return prettyPrintForReport(value);
        }
    }

    private static String printItemDeltaOldValues(ItemPathType itemPath, List values) {
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            String v = printItemDeltaValue(itemPath, value);
            if (StringUtils.isNotBlank(v)) {
                sb.append(v);
                sb.append(", ");
            }
        }
        sb.setLength(Math.max(sb.length() - 2, 0)); // delete last delimiter
        return sb.toString();

    }

    private static boolean isMetadata(ItemDeltaType itemDelta) {
        List values = itemDelta.getValue();
        for (Object v : values) {
            if (v instanceof MetadataType) {
                return true;
            } else if (v instanceof RawType) {
                return isMetadata(itemDelta.getPath());
            }
        }

        return false;
    }

    private static boolean isMetadata(ItemPathType itemPath) {
        boolean retMeta = false;
        for (ItemPathSegment ips : itemPath.getItemPath().getSegments()) {
            if (ips instanceof NameItemPathSegment && "metadata".equals(((NameItemPathSegment) ips).getName().getLocalPart())) {
                return true;
            }
        }
        return retMeta;
    }

    public static String prettyPrintForReport(ItemDeltaType itemDelta) {
        StringBuilder sb = new StringBuilder();
        boolean displayNA = false;

        if (isMetadata(itemDelta)) {
            return sb.toString();
        }

        sb.append(">>> ");
        sb.append(itemDelta.getPath());
        sb.append("=");
        sb.append("{");
        if (itemDelta.getEstimatedOldValue() != null && !itemDelta.getEstimatedOldValue().isEmpty()) {
            sb.append("Old: ");
            sb.append("{");
            sb.append(printItemDeltaOldValues(itemDelta.getPath(), itemDelta.getEstimatedOldValue()));
            sb.append("}");
            sb.append(", ");
            displayNA = true;
        }

        if (itemDelta.getModificationType() == ModificationTypeType.REPLACE) {
            sb.append("Replace: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta));
            sb.append("}");
            sb.append(", ");
            displayNA = false;
        }

        if (itemDelta.getModificationType() == ModificationTypeType.DELETE) {
            sb.append("Delete: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta));
            sb.append("}");
            sb.append(", ");
            displayNA = false;
        }

        if (itemDelta.getModificationType() == ModificationTypeType.ADD) {
            sb.append("Add: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta));
            sb.append("}");
            sb.append(", ");
            displayNA = false;
        }

        if (displayNA) {
            sb.append("N/A"); // this is rare case when oldValue is present but replace, delete and add lists are all null
        } else {
            sb.setLength(Math.max(sb.length() - 2, 0));
        }

        sb.append("}");
        sb.append("\n");
        return sb.toString();
    }

    public static String getBusinessDisplayName(ObjectReferenceType ort) {
        return ort.getDescription();
    }

    private static String printChangeType(String objectName, ObjectDeltaType delta, String opName, String resourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append(opName);
        sb.append(" ");
        sb.append(delta.getObjectType().getLocalPart());
        if (StringUtils.isNotBlank(objectName)) {
            sb.append(": ");
            sb.append(objectName);
        } else if (delta.getOid() != null) {
            sb.append(": ");
            sb.append(delta.getOid());
        }
        if (StringUtils.isNotBlank(resourceName)) {
            sb.append(" - ");
            sb.append("Resource: ");
            sb.append(resourceName);
        }
        sb.append("\n");
        return sb.toString();
    }

    public static String printDelta(List<ObjectDeltaType> delta) {
        StringBuilder sb = new StringBuilder();
        for (ObjectDeltaType d : delta) {
            sb.append(printDelta(d, null, null));
            sb.append("\n");
        }
        return sb.toString();
    }

    public static String printDelta(ObjectDeltaType delta, String objectName, String resourceName) {
        StringBuilder sb = new StringBuilder();

        switch (delta.getChangeType()) {
            case MODIFY:
                Collection<ItemDeltaType> modificationDeltas = delta.getItemDelta();
                if (modificationDeltas != null && !modificationDeltas.isEmpty()) {
                    sb.append(printChangeType(objectName, delta, "Modify", resourceName));
                }
                for (ItemDeltaType itemDelta : modificationDeltas) {
                    sb.append(prettyPrintForReport(itemDelta));
                }
                sb.setLength(Math.max(sb.length() - 1, 0));
                break;

            case ADD:
                ObjectType objectToAdd = (ObjectType) delta.getObjectToAdd();
                if (objectToAdd != null) {
                    sb.append(printChangeType(objectName, delta, "Add", resourceName));
                    if (objectToAdd.getName() != null) {
                        sb.append(prettyPrintForReport(objectToAdd.getClass().getSimpleName()));
                        sb.append("=");
                        sb.append(objectToAdd.getName().toString());
                    }
                    sb.append(" {");
                    sb.append(prettyPrintForReport(objectToAdd));
                    sb.append("}");
                }
                break;

            case DELETE:
                sb.append(printChangeType(objectName, delta, "Delete", resourceName));
                break;
        }

        return sb.toString();
    }

    public static String join(Collection<String> strings) {
        return StringUtils.join(strings, ", ");
    }

    public static Object getItemRealValue(PrismContainerValue containerValue, String itemName) {
        Item item = containerValue.findItem(new QName(itemName));
        if (item == null || item.size() == 0) {
            return null;
        }
        if (item.size() > 1) {
            throw new IllegalStateException("More than one value in item " + item);
        }
        PrismValue value = item.getValue(0);
        if (value == null) {
            return null;
        }
        if (value instanceof PrismPropertyValue) {
            return ((PrismPropertyValue) value).getValue();
        } else if (value instanceof PrismReferenceValue) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue((PrismReferenceValue) value);
            return ort;
        } else if (value instanceof PrismContainerValue) {
            return ((PrismContainerValue) value).asContainerable();     // questionable
        } else {
            throw new IllegalStateException("Unknown PrismValue: " + value);
        }
    }

    public static String prettyPrintForReport(AccessCertificationResponseType response, boolean noResponseIfEmpty) {
        if (noResponseIfEmpty) {
            if (response == null) {
                response = AccessCertificationResponseType.NO_RESPONSE;
            }
        } else {
            if (response == null || response == AccessCertificationResponseType.NO_RESPONSE) {
                return "";
            }
        }
        return getPropertyString("AccessCertificationResponseType."+response.name());
    }

    public static String prettyPrintForReport(AccessCertificationResponseType response) {
        if (response == null || response == AccessCertificationResponseType.NO_RESPONSE) {
            return "";
        }
        return getPropertyString("AccessCertificationResponseType."+response.name());
    }

    public static String prettyPrintForReport(EvaluatedPolicyRuleTriggerType trigger) {
        return prettyPrintRuleTriggerForReport(trigger);
    }

    public static String prettyPrintForReport(EvaluatedSituationTriggerType trigger) {
        return prettyPrintRuleTriggerForReport(trigger);
    }

    public static String prettyPrintForReport(EvaluatedExclusionTriggerType trigger) {
        return prettyPrintRuleTriggerForReport(trigger);
    }

    public static String prettyPrintForReport(PrismObjectValue pov) {
        return prettyPrintForReport((PrismContainerValue) pov);
    }

    private static String prettyPrintRuleTriggerForReport(EvaluatedPolicyRuleTriggerType trigger) {
        if (trigger == null) {
            return "";
        }
        return "Rule: " + (trigger.getRuleName()!=null?trigger.getRuleName():"N/A");
    }

    public static String prettyPrintForReport(Enum e) {
        if (e == null) {
            return "";
        }
        return getPropertyString(e.getClass().getSimpleName()+"."+e.name(), e.name());
    }

    public static String getTypeDisplayName(QName typeName) {
        if (typeName == null) {
            return null;
        }
        return getPropertyString(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + typeName.getLocalPart(), typeName.getLocalPart());
    }
}
