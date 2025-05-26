/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.report.impl;

import java.io.File;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.UserFriendlyPrettyPrinter;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.report.impl.controller.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.delta.ItemTreeDelta;
import com.evolveum.midpoint.schema.delta.ObjectTreeDelta;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.*;

/**
 * Utility methods for report. Mostly pretty print functions. Do not use any
 * "prism" object and anything related to them.
 * Methods have to work with both common schema types and extended schema types (prism).
 *
 * @author Katarina Valalikova
 * @author Martin Lizner
 */
public class ReportUtils {

    private static final String EXPORT_DIR_NAME = "export/";

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

    public static String prettyPrintReferencesForReport(@NotNull List<? extends Referencable> references, boolean showType) {
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
        // TODO: reconsider the time part with -, this is good for file names, but for printing : would be better
        SimpleDateFormat formatDate = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");
        return formatDate.format(createDate);
    }

    public static String getPropertyString(String key) {
        return getPropertyString(key, null);
    }

    public static String getPropertyString(String key, String defaultValue) {
        String val = (defaultValue == null) ? key : defaultValue;
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("localization/schema", new Locale("en", "US"));
        } catch (MissingResourceException e) {
            return (defaultValue != null) ? defaultValue : key; //workaround for reports
        }
        if (bundle != null && bundle.containsKey(key)) {
            val = bundle.getString(key);
        }
        return val;
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

    public static String prettyPrintForReport(Referencable prv) {
        return prettyPrintForReport(prv, true);
    }

    public static String prettyPrintForReport(Referencable prv, boolean showType) {
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

    public static String prettyPrintForReport(ObjectType object, LocalizationService localizationService) {
        if (object == null) {
            return "";
        }
        String name = PolyString.getOrig(object.getName());

        PolyStringType displayName = ObjectTypeUtil.getDisplayName(object);
        if (displayName != null) {
            if (localizationService != null) {
                String translatedValue = localizationService.translate(PolyString.toPolyString(displayName), Locale.getDefault(), true);
                if (StringUtils.isNotEmpty(translatedValue)) {
                    name = translatedValue;
                }
            } else {
                name = PolyString.getOrig(ObjectTypeUtil.getDisplayName(object));
            }
        }
        return StringUtils.isEmpty(name) ? object.getOid() : name;
    }

    public static String prettyPrintUsersForReport(List<? extends Referencable> userRefList) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Referencable userRef : userRefList) {
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
        return "[" + (ba).length + " bytes]";
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

        //special handling for byte[]
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
        return PrettyPrinter.prettyPrint(value);
    }

    private static String printItemDeltaValues(ItemDeltaType itemDelta) {
        List<?> values = itemDelta.getValue();
        StringBuilder sb = new StringBuilder();
        for (Object value : values) {
            String v = printItemDeltaValue(itemDelta.getPath().getItemPath(), value);
            if (StringUtils.isNotBlank(v)) {
                sb.append(v);
                sb.append(", ");
            }
        }
        sb.setLength(Math.max(sb.length() - 2, 0)); // delete last delimiter
        return sb.toString();
    }

    private static String printItemDeltaValues(ItemDelta<?, ?> itemDelta, Collection<?> values) {
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

    private static String printItemDeltaValue(ItemPath itemPath, Object value) {
        if (value instanceof PrismValue) {
            value = ((PrismValue) value).getRealValue();
        }
        if (value instanceof MetadataType) {
            return "";
        } else if (value instanceof RawType) {
            try {

                if (isMetadata(itemPath)) {
                    return "";
                }

                Object parsedRealValue = ((RawType) value).getParsedRealValue(null, itemPath);
                if (parsedRealValue instanceof Containerable) { // this is for PCV
                    return prettyPrintForReport(((Containerable) parsedRealValue).asPrismContainerValue());
                }
                return prettyPrintForReport(parsedRealValue);
            } catch (Exception e) {
                return "###INTERNAL#ERROR### " + e.getClass().getName() + ": " + e.getMessage() + "; prettyPrintForReport method for value " + value;
            }
        } else {
            return prettyPrintForReport(value);
        }
    }

    private static String printItemDeltaOldValues(ItemPath itemPath, Collection<?> values) {
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
        if (isMetadata(itemDelta.getPath())) {
            return true;
        } else {
            for (Object v : itemDelta.getValue()) {
                if (v instanceof MetadataType) {
                    return true;
                }
            }
            return false;
        }
    }

    private static boolean isMetadata(ItemPathType itemPath) {
        return isMetadata(itemPath.getItemPath());
    }

    private static boolean isMetadata(ItemPath itemPath) {
        boolean retMeta = false;
        for (Object ips : itemPath.getSegments()) {
            if (ItemPath.isName(ips) && ObjectType.F_METADATA.getLocalPart().equals(ItemPath.toName(ips).getLocalPart())) {
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
        itemDelta.getEstimatedOldValue();
        if (!itemDelta.getEstimatedOldValue().isEmpty()) {
            sb.append("Old: ");
            sb.append("{");
            sb.append(printItemDeltaOldValues(itemDelta.getPath().getItemPath(), itemDelta.getEstimatedOldValue()));
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

    public static String prettyPrintForReport(ItemDelta<?, ?> itemDelta) {
        StringBuilder sb = new StringBuilder();
        boolean displayNA = false;

        if (isMetadata(itemDelta.getPath())) {
            return sb.toString();
        }

        sb.append("\t");
        sb.append(itemDelta.getPath());
        sb.append(": ");
        sb.append("{");
        if (itemDelta.getEstimatedOldValues() != null && !itemDelta.getEstimatedOldValues().isEmpty()) {
            sb.append("Old: ");
            sb.append("{");
            sb.append(printItemDeltaOldValues(itemDelta.getPath(), itemDelta.getEstimatedOldValues()));
            sb.append("}");
            sb.append(", ");
            displayNA = true;
        } else if (itemDelta.isReplace() || itemDelta.isAdd()) {
            sb.append("Old: {}, ");
        }

        if (itemDelta.isReplace()) {
            sb.append("Replace: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta, itemDelta.getValuesToReplace()));
            sb.append("}");
            sb.append(", ");
            displayNA = false;
        }

        if (itemDelta.isDelete()) {
            sb.append("Delete: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta, itemDelta.getValuesToDelete()));
            sb.append("}");
            sb.append(", ");
            displayNA = false;
        }

        if (itemDelta.isAdd()) {
            sb.append("Add: ");
            sb.append("{");
            sb.append(printItemDeltaValues(itemDelta, itemDelta.getValuesToAdd()));
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

    public static String getBusinessDisplayName(Referencable ort) {
        return ort.getDescription();
    }

    private static String printChangeType(String objectName, ObjectDeltaType delta, String opName, String resourceName) {
        return printChangeType(objectName, delta.getObjectType().getLocalPart(), delta.getOid(), opName, resourceName);
    }

    private static String printChangeType(String objectName, String objectType, String objectOid, String opName, String resourceName) {
        StringBuilder sb = new StringBuilder();
        sb.append(opName);
        sb.append(" ");
        sb.append(objectType);
        if (StringUtils.isNotBlank(objectName)) {
            sb.append(": ");
            sb.append(objectName);
        } else if (objectOid != null) {
            sb.append(": ");
            sb.append(objectOid);
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
                    for (ItemDeltaType itemDelta : modificationDeltas) {
                        sb.append(prettyPrintForReport(itemDelta));
                    }
                    sb.setLength(Math.max(sb.length() - 1, 0));
                }
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

    /**
     * // todo decide what to do about this method! [viliam]
     * Used in object collections (audit), midPoint initial objects.
     */
    @SuppressWarnings("unused")
    public static <O extends ObjectType> String printDelta(ObjectDeltaOperation<O> deltaOp, ItemPath path) {
        if (path == null || ItemPath.EMPTY_PATH.equals(path)) {
            return printDelta(deltaOp);
        }

        ObjectDelta<O> delta = deltaOp.getObjectDelta();

        ItemDelta<?, ?> itemDelta = findItemDelta(delta, path);
        if (itemDelta == null || itemDelta.isEmpty()) {
            return "";
        }
        return prettyPrintForReport(itemDelta);
    }

    private static ItemDelta<?, ?> findItemDelta(ObjectDelta<? extends ObjectType> delta, ItemPath path) {
        List<ItemDelta<?, ?>> deltas = findItemDelta(delta, path, false);
        if (deltas == null || deltas.isEmpty()) {
            return null;
        }

        return deltas.get(0);
    }

    private static List<ItemDelta<?, ?>> findItemDelta(ObjectDelta<? extends ObjectType> delta, ItemPath path, boolean showPartialDeltas) {
        if (delta == null) {
            return null;
        }

        ObjectTreeDelta<? extends ObjectType> treeDelta = ObjectTreeDelta.fromItemDelta(delta);
        ItemTreeDelta<?, ?, ?, ?> itemTreeDelta = treeDelta.findItemDelta(path);
        if (itemTreeDelta == null) {
            return null;
        }

        if (itemTreeDelta instanceof ObjectTreeDelta<?> && !showPartialDeltas) {
            return null;
        }

        if (itemTreeDelta instanceof ObjectTreeDelta<?>) {
            return null;
        }

        ItemDelta<?,?> mainDelta = itemTreeDelta.toDelta();
        if (!showPartialDeltas) {
            if (mainDelta == null || mainDelta.isEmpty()) {
                return null;
            }

            return List.of(mainDelta);
        }

        List<ItemDelta<?,?>> result = new ArrayList<>();
        if (mainDelta != null && !mainDelta.isEmpty()) {
            result.add(mainDelta);
        }

        result.addAll(itemTreeDelta.toChildDeltas());

        return result;
    }

    @SuppressWarnings("unused")
    public static ItemDelta<?, ?> findItemDelta(ObjectDeltaOperationType deltaOp, ItemPath path) throws SchemaException {
        if (deltaOp == null) {
            return null;
        }

        ObjectDeltaOperation<?> delta = DeltaConvertor.createObjectDeltaOperation(deltaOp, true);
        return findItemDelta(delta.getObjectDelta(), path);
    }

    @SuppressWarnings("unused")
    public static List<String> printDelta(ObjectDeltaOperationType deltaOperation, ItemPath itemPath, DeltaPrinterOptions options)
            throws SchemaException {

        if (deltaOperation == null) {
            return List.of();
        }

        itemPath = itemPath == null ? ItemPath.EMPTY_PATH : itemPath;

        if (options == null) {
            options = new DeltaPrinterOptions();
        }

        ObjectDeltaOperation<?> delta = DeltaConvertor.createObjectDeltaOperation(deltaOperation, true);

        UserFriendlyPrettyPrinter printer = new UserFriendlyPrettyPrinter(options.prettyPrinterOptions());

        boolean useEstimatedOld = options.useEstimatedOldValues();

        if (itemPath.isEmpty()) {
            if (!options.showObjectDelta()) {
                return List.of();
            }

            ObjectDelta objectDelta = delta.getObjectDelta();
            return List.of(printer.prettyPrintObjectDelta(objectDelta, useEstimatedOld, 0));
        }

        List<ItemDelta<?, ?>> deltas = findItemDelta(delta.getObjectDelta(), itemPath, options.showPartialDeltas());
        if (deltas.isEmpty()) {
            return List.of();
        }

        return deltas.stream()
                .map(d -> printer.prettyPrintItemDelta(d, useEstimatedOld, 0))
                .toList();
    }

    public static <O extends ObjectType> String printDelta(ObjectDeltaOperation<O> deltaOp) {
        ObjectDelta<O> delta = deltaOp.getObjectDelta();
        String objectName = deltaOp.getObjectName() == null ? null : deltaOp.getObjectName().toString();
        String resourceName = deltaOp.getResourceName() == null ? null : deltaOp.getResourceName().toString();
        StringBuilder sb = new StringBuilder();

        switch (delta.getChangeType()) {
            case MODIFY:
                Collection<? extends ItemDelta<?, ?>> modificationDeltas = delta.getModifications();
                if (!modificationDeltas.isEmpty()) {
                    sb.append(printChangeType(objectName, delta.getObjectTypeClass().getSimpleName(), delta.getOid(), "Modify", resourceName));
                }
                for (ItemDelta<?, ?> itemDelta : modificationDeltas) {
                    sb.append(prettyPrintForReport(itemDelta));
                }
                sb.setLength(Math.max(sb.length() - 1, 0));
                break;

            case ADD:
                if (delta.getObjectToAdd() != null) {
                    Objectable objectToAdd = delta.getObjectToAdd().asObjectable();
                    sb.append(printChangeType(objectName, delta.getObjectTypeClass().getSimpleName(), delta.getOid(), "Add", resourceName));
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
                sb.append(printChangeType(objectName, delta.getObjectTypeClass().getSimpleName(), delta.getOid(), "Delete", resourceName));
                break;
        }

        return sb.toString();
    }

    public static String join(Collection<String> strings) {
        return StringUtils.join(strings, ", ");
    }

    public static <C extends Containerable> Object getItemRealValue(PrismContainerValue<C> containerValue, String itemName) {
        Item<?, ?> item = containerValue.findItem(new ItemName(itemName));
        if (item == null || item.size() == 0) {
            return null;
        }
        if (item.size() > 1) {
            throw new IllegalStateException("More than one value in item " + item);
        }
        PrismValue value = item.getAnyValue();
        if (value == null) {
            return null;
        }
        if (value instanceof PrismPropertyValue) {
            return ((PrismPropertyValue<?>) value).getValue();
        } else if (value instanceof PrismReferenceValue) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue((PrismReferenceValue) value);
            return ort;
        } else if (value instanceof PrismContainerValue) {
            return ((PrismContainerValue<?>) value).asContainerable();     // questionable
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
        return getPropertyString("AccessCertificationResponseType." + response.name());
    }

    public static String prettyPrintForReport(AccessCertificationResponseType response) {
        if (response == null || response == AccessCertificationResponseType.NO_RESPONSE) {
            return "";
        }
        return getPropertyString("AccessCertificationResponseType." + response.name());
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

    public static String prettyPrintForReport(PrismObjectValue<?> pov) {
        return prettyPrintForReport((PrismContainerValue<?>) pov);
    }

    private static String prettyPrintRuleTriggerForReport(EvaluatedPolicyRuleTriggerType trigger) {
        if (trigger == null) {
            return "";
        }
        return "Rule: " + (trigger.getRuleName() != null ? trigger.getRuleName() : "N/A");
    }

    public static String prettyPrintForReport(Enum<?> e) {
        if (e == null) {
            return "";
        }
        return getPropertyString(e.getClass().getSimpleName() + "." + e.name(), e.name());
    }

    public static String getTypeDisplayName(QName typeName) {
        if (typeName == null) {
            return null;
        }
        return getPropertyString(SchemaConstants.OBJECT_TYPE_KEY_PREFIX + typeName.getLocalPart(), typeName.getLocalPart());
    }

    public static String getEventProperty(List<AuditEventRecordPropertyType> properties, String key) {
        return getEventProperty(properties, key, "empty");
    }

    public static String getEventProperty(List<AuditEventRecordPropertyType> properties, String key, String defaultValue) {
        if (properties != null) {
            return properties.stream()
                    .filter(property -> key.equals(property.getName()))
                    .findFirst()
                    .map(AuditEventRecordPropertyType::getValue)
                    .map(it -> printProperty(it, defaultValue))
                    .orElse(defaultValue);
        } else {
            return defaultValue;
        }
    }

    public static String getEventReferenceOrig(List<AuditEventRecordReferenceType> references, String key) {
        return getEventReferenceOrig(references, key, "empty");
    }

    public static String getEventReferenceOrig(List<AuditEventRecordReferenceType> references, String key, String defaultValue) {
        if (references != null) {
            return references.stream()
                    .filter(ref -> key.equals(ref.getName()))
                    .findFirst()
                    .map(AuditEventRecordReferenceType::getValue)
                    .map(it -> printReference(it, defaultValue))
                    .orElse(defaultValue);
        } else {
            return defaultValue;
        }
    }

    /**
     * Returns delta items modification types.
     */
    public static String getDeltaNature(List<ObjectDeltaOperationType> delta) {
        if (delta == null) {
            return "";
        } else {
            return delta.stream()
                    .map(ObjectDeltaOperationType::getObjectDelta)
                    .filter(Objects::nonNull)
                    .map(ObjectDeltaType::getItemDelta)
                    .flatMap(Collection::stream)
                    .map(ItemDeltaType::getModificationType)
                    .map(ModificationTypeType::name)
                    .map(String::toLowerCase)
                    .map(WordUtils::capitalize)
                    .collect(Collectors.joining("/"));
        }
    }

    public static String getObjectDeltaNature(List<ObjectDeltaOperationType> delta) {
        if (delta == null) {
            return "";
        } else {
            return delta.stream()
                    .map(ObjectDeltaOperationType::getObjectDelta)
                    .filter(Objects::nonNull)
                    .map(ObjectDeltaType::getChangeType)
                    .map(Enum::toString)
                    .collect(Collectors.joining("/"));
        }
    }

    public static String getDeltaForWFReport(List<ObjectDeltaOperationType> delta) {
        if (delta == null) {
            return "";
        } else {
            return delta.stream()
                    .map(ObjectDeltaOperationType::getObjectDelta)
                    .filter(Objects::nonNull)
                    .map(ObjectDeltaType::getItemDelta)
                    .flatMap(Collection::stream)
                    .map(ItemDeltaType::getPath)
                    .map(ItemPathType::toString)
                    .collect(Collectors.joining(", "));
        }
    }

    public static String printReference(List<AuditEventRecordReferenceValueType> reference, String defaultValue) {
        return reference != null ?
                reference.stream()
                        .map(AuditEventRecordReferenceValueType::getTargetName)
                        .map(PolyStringType::getOrig)
                        .collect(Collectors.joining(","))
                : defaultValue;
    }

    public static String printProperty(List<String> property, String defaultValue) {
        return property != null ?
                String.join(",", property)
                : defaultValue;
    }

    public static Map<String, Object> paramsToAuditParams(Map<String, ?> params) {
        Map<String, Object> auditParams = new HashMap<>();
        for (Entry<String, ?> param : params.entrySet()) {
            Object value;
            if (param.getValue() instanceof TypedValue) {
                value = ((TypedValue<?>) param.getValue()).getValue();
            } else {
                value = param.getValue();
            }
            if (value instanceof AuditEventTypeType) {
                auditParams.put(param.getKey(), AuditEventType.fromSchemaValue((AuditEventTypeType) value));
            } else if (value instanceof AuditEventStageType) {
                auditParams.put(param.getKey(), AuditEventStage.fromSchemaValue((AuditEventStageType) value));
            } else {
                auditParams.put(param.getKey(), value);
            }
        }
        return auditParams;
    }

    private static File getExportDir() {
        return new File(System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY), EXPORT_DIR_NAME);
    }

    public static @NotNull DirectionTypeType getDirection(@NotNull ReportType report) {
        return report.getBehavior() != null && report.getBehavior().getDirection() != null ?
                report.getBehavior().getDirection() : DirectionTypeType.EXPORT;
    }

    public static ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> createDataWriter(@NotNull ReportType report,
            @NotNull FileFormatTypeType defaultType, ReportServiceImpl reportService, CompiledObjectCollectionView compiledView) {
        FileFormatTypeType formatType;
        if (report.getFileFormat() != null && report.getFileFormat().getType() != null) {
            formatType = report.getFileFormat().getType();
        } else {
            formatType = defaultType;
        }
        switch (formatType) {
            case HTML:
                return new HtmlReportDataWriter<>(reportService, compiledView, report.getFileFormat());
            case CSV:
                return new CsvReportDataWriter(reportService, report.getFileFormat());
            default:
                throw new AssertionError(formatType);
        }
    }

    public static ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> createDashboardDataWriter(
            @NotNull ReportType report, ReportServiceImpl reportService, Map<String,
                    CompiledObjectCollectionView> mapOfCompiledView) {
        FileFormatTypeType formatType;
        if (report.getFileFormat() != null && report.getFileFormat().getType() != null) {
            formatType = report.getFileFormat().getType();
        } else {
            formatType = FileFormatTypeType.HTML;
        }
        switch (formatType) {
            case HTML:
                return new HtmlDashboardReportDataWriter(reportService, mapOfCompiledView, report.getFileFormat());
            case CSV:
                return new CsvReportDataWriter(reportService, report.getFileFormat());
            default:
                throw new AssertionError(formatType);
        }
    }
}
