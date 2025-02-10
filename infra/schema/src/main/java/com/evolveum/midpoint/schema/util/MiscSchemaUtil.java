/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.Nullable;

/**
 * @author Radovan Semancik
 */
public class MiscSchemaUtil {

    private static final Random RND = new Random();

    public static ObjectListType toObjectListType(List<PrismObject<? extends ObjectType>> list) {
        ObjectListType listType = new ObjectListType();
        for (PrismObject<? extends ObjectType> o : list) {
            listType.getObject().add(o.asObjectable());
        }
        return listType;
    }

    public static <T extends ObjectType> List<PrismObject<T>> toList(
            Class<T> type, ObjectListType listType) {
        List<PrismObject<T>> list = new ArrayList<>();
        for (ObjectType o : listType.getObject()) {
            list.add((PrismObject<T>) o.asPrismObject());
        }
        return list;
    }

    public static <T extends ObjectType> List<T> toObjectableList(List<PrismObject<T>> objectList) {
        if (objectList == null) {
            return null;
        }
        List<T> objectableList = new ArrayList<>(objectList.size());
        for (PrismObject<T> object : objectList) {
            objectableList.add(object.asObjectable());
        }
        return objectableList;
    }

    public static ImportOptionsType getDefaultImportOptions() {
        ImportOptionsType options = new ImportOptionsType();
        options.setOverwrite(false);
        options.setValidateStaticSchema(false);
        options.setValidateDynamicSchema(false);
        options.setEncryptProtectedValues(true);
        options.setFetchResourceSchema(false);
        options.setSummarizeErrors(true);
        options.setSummarizeSucceses(true);
        return options;
    }

    public static CachingMetadataType generateCachingMetadata() {
        CachingMetadataType cmd = new CachingMetadataType();
        XMLGregorianCalendar xmlGregorianCalendarNow =
                XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis());
        cmd.setRetrievalTimestamp(xmlGregorianCalendarNow);
        cmd.setSerialNumber(generateSerialNumber());
        return cmd;
    }

    private static String generateSerialNumber() {
        return Long.toHexString(RND.nextLong()) + "-" + Long.toHexString(RND.nextLong());
    }

    public static boolean isNullOrEmpty(ProtectedStringType ps) {
        return (ps == null || ps.isEmpty());
    }

    public static void setPassword(CredentialsType credentials, ProtectedStringType password) {
        PasswordType credPass = credentials.getPassword();
        if (credPass == null) {
            credPass = new PasswordType();
            credentials.setPassword(credPass);
        }
        credPass.setValue(password);
    }

    public static Collection<String> toCollection(String entry) {
        List<String> list = new ArrayList<>(1);
        list.add(entry);
        return list;
    }

    public static Collection<ItemPath> itemReferenceListTypeToItemPathList(
            PropertyReferenceListType resolve) {
        Collection<ItemPath> itemPathList = new ArrayList<>(resolve.getProperty().size());
        for (ItemPathType itemXPathElement : resolve.getProperty()) {
            itemPathList.add(PrismContext.get().toPath(itemXPathElement));
        }
        return itemPathList;
    }

    @Deprecated // Moved to GetOperationOptionsUtil
    public static @NotNull SelectorQualifiedGetOptionsType optionsToOptionsType(
            @NotNull Collection<SelectorOptions<GetOperationOptions>> options) {
        return GetOperationOptionsUtil.optionsToOptionsBean(options);
    }

    @Deprecated // Moved to GetOperationOptionsUtil
    public static List<SelectorOptions<GetOperationOptions>> optionsTypeToOptions(
            SelectorQualifiedGetOptionsType objectOptionsType, @SuppressWarnings("unused") PrismContext unused) {
        return GetOperationOptionsUtil.optionsBeanToOptions(objectOptionsType);
    }

    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Collection<ObjectDelta<? extends ObjectType>> createCollection(ObjectDelta<?>... deltas) {
        return (Collection) MiscUtil.createCollection(deltas);
    }

    /**
     * Convenience method that helps avoid some compiler warnings.
     */
    public static Collection<? extends ItemDelta<?, ?>> createCollection(ItemDelta<?, ?>... deltas) {
        return MiscUtil.createCollection(deltas);
    }

    public static Collection<ObjectDelta<? extends ObjectType>> cloneObjectDeltaCollection(
            Collection<ObjectDelta<? extends ObjectType>> origCollection) {
        if (origCollection == null) {
            return null;
        }
        Collection<ObjectDelta<? extends ObjectType>> clonedCollection = new ArrayList<>(origCollection.size());
        for (ObjectDelta<? extends ObjectType> origDelta : origCollection) {
            clonedCollection.add(origDelta.clone());
        }
        return clonedCollection;
    }

    public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneObjectDeltaOperationCollection(
            Collection<ObjectDeltaOperation<? extends ObjectType>> origCollection) {
        if (origCollection == null) {
            return null;
        }
        Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<>(origCollection.size());
        for (ObjectDeltaOperation<? extends ObjectType> origDelta : origCollection) {
            clonedCollection.add(origDelta.clone());
        }
        return clonedCollection;
    }

    public static ObjectReferenceType createObjectReference(String oid, QName type) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        ref.setType(type);
        return ref;
    }

    public static <O extends ObjectType> ObjectReferenceType createObjectReference(
            PrismObject<O> object, Class<? extends ObjectType> implicitReferenceTargetType) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(object.getOid());
        if (implicitReferenceTargetType == null
                || !implicitReferenceTargetType.equals(object.getCompileTimeClass())) {
            ref.setType(ObjectTypes.getObjectType(object.getCompileTimeClass()).getTypeQName());
        }
        ref.setTargetName(PolyString.toPolyStringType(object.getName()));
        return ref;
    }

    public static boolean equalsIntent(String intent1, String intent2) {
        if (intent1 == null) {
            intent1 = SchemaConstants.INTENT_DEFAULT;
        }
        if (intent2 == null) {
            intent2 = SchemaConstants.INTENT_DEFAULT;
        }
        return intent1.equals(intent2);
    }

    public static boolean matchesKind(ShadowKindType expectedKind, ShadowKindType actualKind) {
        if (expectedKind == null) {
            return true;
        }
        return expectedKind.equals(actualKind);
    }

    public static @NotNull AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementMode(
            ProjectionPolicyType accountSynchronizationSettings) {
        if (accountSynchronizationSettings == null) {
            return AssignmentPolicyEnforcementType.RELATIVE; // default
        }
        AssignmentPolicyEnforcementType assignmentPolicyEnforcement =
                accountSynchronizationSettings.getAssignmentPolicyEnforcement();
        return Objects.requireNonNullElse(assignmentPolicyEnforcement, AssignmentPolicyEnforcementType.RELATIVE);
    }

    public static PrismReferenceValue objectReferenceTypeToReferenceValue(ObjectReferenceType refType,
            PrismContext prismContext) {
        if (refType == null) {
            return null;
        }
        PrismReferenceValue rval = prismContext.itemFactory().createReferenceValue();
        rval.setOid(refType.getOid());
        rval.setDescription(refType.getDescription());
        rval.setFilter(refType.getFilter());
        rval.setRelation(refType.getRelation());
        rval.setTargetType(refType.getType());
        return rval;
    }

    /**
     * Given a list of limitation definitions, select the one with appropriate "layer label".
     *
     * * Layer label of `null` means we want to obtain the record that has (multivalued) "layer" property empty.
     * * Non-null label means we want to obtain the record that has that specific value among the "layer" values.
     *
     * In both cases, there can be at most one matching record.
     *
     * NOTE: This method has been renamed from the original `getLimitationsForLayer` because the original name
     * was a bit misleading. The effective limitations for a given layer may be different from the ones returned
     * by this method. See also MID-7929.
     */
    public static @Nullable PropertyLimitationsType getLimitationsLabeled(
            @Nullable Collection<PropertyLimitationsType> definitions,
            @Nullable LayerType layerLabel) throws SchemaException {
        if (definitions == null) {
            return null;
        }
        PropertyLimitationsType found = null;
        for (PropertyLimitationsType limitType : definitions) {
            if (hasLayerLabel(limitType.getLayer(), layerLabel)) {
                if (found == null) {
                    found = limitType;
                } else {
                    throw new SchemaException("Duplicate definition of limitations for layer '" + layerLabel + "'");
                }
            }
        }
        return found;
    }

    private static boolean hasLayerLabel(@NotNull List<LayerType> layers, @Nullable LayerType layer) {
        if (layer == null) {
            return layers.isEmpty();
        } else {
            return layers.contains(layer);
        }
    }

    public static boolean contains(Collection<ObjectReferenceType> collection, ObjectReferenceType item) {
        for (ObjectReferenceType collectionItem : collection) {
            if (matches(collectionItem, item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ObjectReferenceType a, ObjectReferenceType b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return MiscUtil.equals(a.getOid(), b.getOid());
    }

    /**
     * Returns modification time or creation time (if there was no mo
     */
    public static XMLGregorianCalendar getChangeTimestamp(MetadataType metadata) {
        if (metadata == null) {
            return null;
        }
        XMLGregorianCalendar modifyTimestamp = metadata.getModifyTimestamp();
        if (modifyTimestamp != null) {
            return modifyTimestamp;
        } else {
            return metadata.getCreateTimestamp();
        }
    }

    public static boolean referenceMatches(ObjectReferenceType refPattern, ObjectReferenceType ref,
            PrismContext prismContext) {
        if (refPattern.getOid() != null && !refPattern.getOid().equals(ref.getOid())) {
            return false;
        }
        if (refPattern.getType() != null && !QNameUtil.match(refPattern.getType(), ref.getType())) {
            return false;
        }
        if (!prismContext.relationMatches(refPattern.getRelation(), ref.getRelation())) {
            return false;
        }
        return true;
    }

    /**
     * Make quick and reasonably reliable comparison. E.g. compare prism objects only by
     * comparing OIDs. This is ideal for cases when the compare is called often and the
     * objects are unlikely to change (e.g. user interface selectable beans).
     */
    @SuppressWarnings("rawtypes")
    public static boolean quickEquals(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a instanceof PrismObject) {
            if (b instanceof PrismObject) {
                // In case both values are objects then compare only OIDs.
                // that should be enough. Comparing complete objects may be slow
                // (e.g. if the objects have many assignments)
                String aOid = ((PrismObject) a).getOid();
                String bOid = ((PrismObject) b).getOid();
                if (aOid != null && bOid != null) {
                    return aOid.equals(bOid);
                }
            } else {
                return false;
            }
        }
        if (a instanceof ObjectType) {
            if (b instanceof ObjectType) {
                // In case both values are objects then compare only OIDs.
                // that should be enough. Comparing complete objects may be slow
                // (e.g. if the objects have many assignments)
                String aOid = ((ObjectType) a).getOid();
                String bOid = ((ObjectType) b).getOid();
                if (aOid != null && bOid != null) {
                    return aOid.equals(bOid);
                }
            } else {
                return false;
            }
        }
        return a.equals(b);
    }

    @NotNull
    public static InformationType createInformationType(List<LocalizableMessageType> messages) {
        InformationType rv = new InformationType();
        messages.forEach(s -> rv.getPart().add(new InformationPartType().localizableText(s)));
        return rv;
    }

    public static DisplayHint toDisplayHint(DisplayHintType hint) {
        if (hint == null) {
            return null;
        }

        return switch (hint) {
            case HIDDEN -> DisplayHint.HIDDEN;
            case COLLAPSED -> DisplayHint.COLLAPSED;
            case EXPANDED -> DisplayHint.EXPANDED;
            case REGULAR -> DisplayHint.REGULAR;
            case EMPHASIZED -> DisplayHint.EMPHASIZED;
        };
    }

    public static ItemProcessing toItemProcessing(ItemProcessingType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case IGNORE:
                return ItemProcessing.IGNORE;
            case MINIMAL:
                return ItemProcessing.MINIMAL;
            case AUTO:
                return ItemProcessing.AUTO;
            case FULL:
                return ItemProcessing.FULL;
            default:
                throw new IllegalArgumentException("Unknown processing " + type);
        }
    }

    public static <O extends ObjectType> boolean canBeAssignedFrom(
            QName requiredTypeQName, Class<O> providedTypeClass) {
        Class<? extends ObjectType> requiredTypeClass =
                ObjectTypes.getObjectTypeFromTypeQName(requiredTypeQName).getClassDefinition();
        return requiredTypeClass.isAssignableFrom(providedTypeClass);
    }

    /**
     * This is NOT A REAL METHOD. It just returns null. It is here to mark all the places
     * where proper handling of expression profiles should be later added.
     */
    public static ExpressionProfile getExpressionProfile() {
        return null;
    }

    public static void mergeDisplay(DisplayType viewDisplay, DisplayType archetypeDisplay) {
        if (viewDisplay.getLabel() == null) {
            viewDisplay.setLabel(archetypeDisplay.getLabel());
        }
        if (viewDisplay.getSingularLabel() == null) {
            viewDisplay.setSingularLabel(archetypeDisplay.getSingularLabel());
        }
        if (viewDisplay.getPluralLabel() == null) {
            viewDisplay.setPluralLabel(archetypeDisplay.getPluralLabel());
        }
        if (viewDisplay.getHelp() == null) {
            viewDisplay.setHelp(archetypeDisplay.getHelp());
        }

        IconType archetypeIcon = archetypeDisplay.getIcon();
        if (archetypeIcon != null) {
            IconType viewIcon = viewDisplay.getIcon();
            if (viewIcon == null) {
                viewIcon = new IconType();
                viewDisplay.setIcon(viewIcon);
            }
            if (viewIcon.getCssClass() == null) {
                viewIcon.setCssClass(archetypeIcon.getCssClass());
            }
            if (viewIcon.getColor() == null) {
                viewIcon.setColor(archetypeIcon.getColor());
            }
        }
    }

    public static void mergePaging(PagingType existPaging, PagingType newPaging) {
        if (existPaging.getMaxSize() == null) {
            existPaging.setMaxSize(newPaging.getMaxSize());
        }
        if (existPaging.getOffset() == null) {
            existPaging.setOffset(newPaging.getOffset());
        }
        if (existPaging.getOrderBy() == null) {
            existPaging.setOrderBy(newPaging.getOrderBy());
        }
        if (existPaging.getOrderDirection() == null) {
            existPaging.setOrderDirection(newPaging.getOrderDirection());
        }
    }

    public static void mergePagingOptions(PagingOptionsType existPagingOptions, PagingOptionsType newPagingOptions) {
        if (existPagingOptions.getDefaultPageSize() == null) {
            existPagingOptions.setDefaultPageSize(newPagingOptions.getDefaultPageSize());
        }
        if (existPagingOptions.getAvailablePageSize().isEmpty()) {
            existPagingOptions.getAvailablePageSize().addAll(newPagingOptions.getAvailablePageSize());
        }
    }

    public static void mergeColumns(List<GuiObjectColumnType> existingColumns, List<GuiObjectColumnType> newColumns) {
        newColumns.forEach(newColumn -> {
            Optional<GuiObjectColumnType> matchesColumn = existingColumns.stream().filter(
                    existingColumn -> existingColumn.getName().equals(newColumn.getName())).findFirst();
            if (matchesColumn.isPresent()){
                existingColumns.remove(matchesColumn.get());
                existingColumns.add(newColumn);
            } else {
                existingColumns.add(newColumn);
            }
        });
    }

    /*
   the ordering algorithm is: the first level is occupied by
   the column which previousColumn == null || "" || notExistingColumnNameValue.
   Each next level contains columns which
   previousColumn == columnNameFromPreviousLevel
    */
    public static List<GuiObjectColumnType> orderCustomColumns(List<GuiObjectColumnType> customColumns) {
        if (customColumns == null || customColumns.size() == 0) {
            return new ArrayList<>();
        }
        List<GuiObjectColumnType> customColumnsList = new ArrayList<>(customColumns);
        List<String> previousColumnValues = new ArrayList<>();
        previousColumnValues.add(null);
        previousColumnValues.add("");

        Map<String, String> columnRefsMap = new HashMap<>();
        for (GuiObjectColumnType column : customColumns) {
            columnRefsMap.put(column.getName(),
                    column.getPreviousColumn() == null ? "" : column.getPreviousColumn());
        }

        List<String> temp = new ArrayList<>();
        int index = 0;
        while (index < customColumns.size()) {
            int sortFrom = index;
            for (int i = index; i < customColumnsList.size(); i++) {
                GuiObjectColumnType column = customColumnsList.get(i);
                if (previousColumnValues.contains(column.getPreviousColumn()) ||
                        !columnRefsMap.containsKey(column.getPreviousColumn())) {
                    Collections.swap(customColumnsList, index, i);
                    index++;
                    temp.add(column.getName());
                }
            }
            if (temp.size() == 0) {
                temp.add(customColumnsList.get(index).getName());
                index++;
            }
            if (index - sortFrom > 1) {
                customColumnsList.subList(sortFrom, index - 1)
                        .sort((o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName()));
            }
            previousColumnValues.clear();
            previousColumnValues.addAll(temp);
            temp.clear();
        }
        return customColumnsList;
    }

    public static <F extends UserInterfaceFeatureType> void sortFeaturesPanels(List<F> panels) {
        panels.sort((p1, p2) -> {
            int displayOrder1 = (p1 == null || p1.getDisplayOrder() == null) ? Integer.MAX_VALUE : p1.getDisplayOrder();
            int displayOrder2 = (p2 == null || p2.getDisplayOrder() == null) ? Integer.MAX_VALUE : p2.getDisplayOrder();

            return Integer.compare(displayOrder1, displayOrder2);
        });
    }

    public static boolean isObjectType(Class<?> type) {
        return ObjectType.class.isAssignableFrom(type);
    }

    public static boolean isAuditType(Class<?> type) {
        return AuditEventRecordType.class.equals(type);
    }

}
