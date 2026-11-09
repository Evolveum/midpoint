/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.rest.impl;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedDataType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Minimizes and redacts audit records before they are returned through REST.
 *
 * . By default, deltas and payloads are not returned at all. They are returned only if explicitly asked for
 * via {@link Includes}; the execution result of a delta needs to be asked for separately.
 * . When deltas are returned, the content of all protected values (encrypted, hashed, clear or external data
 * of {@link ProtectedDataType}) is always removed.
 * . The redaction is fail-closed: if a delta cannot be redacted safely, it is not returned.
 *
 * Other items (e.g. `message`, `parameter`, `remoteHostAddress`) are returned as stored. This is consistent
 * with what a holder of the `auditRead` authorization sees in the GUI.
 */
public class AuditRecordRedactor {

    private static final Trace LOGGER = TraceManager.getTrace(AuditRecordRedactor.class);

    public static final String INCLUDE_DELTA = "delta";
    public static final String INCLUDE_DELTA_EXECUTION_RESULT = "delta/executionResult";
    public static final String INCLUDE_PAYLOAD = "payload";

    /** Local names of the {@link ProtectedDataType} items that carry the (possibly sensitive) data. */
    private static final Set<String> PROTECTED_DATA_ITEMS = Set.of("encryptedData", "hashedData", "clearValue", "externalData");

    /** Local names of the types holding protected data. */
    private static final Set<String> PROTECTED_DATA_TYPES =
            Set.of("ProtectedDataType", "ProtectedStringType", "ProtectedByteArrayType");

    private static final QName PROBE_ROOT_NAME = new QName(SchemaConstants.NS_C, "value");

    /** What parts of audit records the client asked for. */
    public record Includes(boolean delta, boolean deltaExecutionResult, boolean payload) {

        public static final Includes NONE = new Includes(false, false, false);

        public static @NotNull Includes parse(@Nullable List<String> values) throws AuditSearchGuard.RejectedException {
            boolean delta = false;
            boolean executionResult = false;
            boolean payload = false;
            if (values != null) {
                for (String rawValue : values) {
                    String value = rawValue != null ? rawValue.trim() : "";
                    switch (value) {
                        case INCLUDE_DELTA -> delta = true;
                        case INCLUDE_DELTA_EXECUTION_RESULT -> {
                            delta = true;
                            executionResult = true;
                        }
                        case INCLUDE_PAYLOAD -> payload = true;
                        default -> throw new AuditSearchGuard.RejectedException("Unsupported 'include' value '" + value
                                + "'. Supported values are: " + INCLUDE_DELTA + ", " + INCLUDE_DELTA_EXECUTION_RESULT
                                + ", " + INCLUDE_PAYLOAD + ".");
                    }
                }
            }
            return new Includes(delta, executionResult, payload);
        }

        /**
         * Retrieval options telling the repository not to load what is not needed. The repository may ignore them;
         * the redaction itself does not depend on them.
         */
        public @Nullable Collection<SelectorOptions<GetOperationOptions>> toGetOperationOptions() {
            if (delta && payload) {
                return null;
            }
            var builder = SchemaService.get().getOperationOptionsBuilder();
            if (!delta) {
                builder = builder.item(AuditEventRecordType.F_DELTA).dontRetrieve();
            }
            if (!payload) {
                builder = builder.item(AuditEventRecordType.F_PAYLOAD).dontRetrieve();
            }
            return builder.build();
        }
    }

    @NotNull private final PrismContext prismContext;

    public AuditRecordRedactor(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    /**
     * Redacts the records in place.
     *
     * @return the number of deltas whose content had to be removed, because it could not be safely redacted
     */
    public int redact(@NotNull List<AuditEventRecordType> records, @NotNull Includes includes) {
        int removed = 0;
        for (AuditEventRecordType record : records) {
            if (!includes.payload()) {
                record.getPayload().clear();
            }
            if (!includes.delta()) {
                record.getDelta().clear();
                continue;
            }
            for (ObjectDeltaOperationType deltaOperation : record.getDelta()) {
                if (!includes.deltaExecutionResult()) {
                    deltaOperation.setExecutionResult(null);
                }
                if (!removeProtectedData(deltaOperation)) {
                    deltaOperation.setObjectDelta(null);
                    deltaOperation.setExecutionResult(null);
                    removed++;
                }
            }
        }
        return removed;
    }

    /**
     * Removes the content of protected values from the delta operation.
     *
     * @return true if the delta operation is safe to be returned
     */
    public boolean removeProtectedData(@NotNull ObjectDeltaOperationType deltaOperation) {
        try {
            if (!containsProtectedData(deltaOperation)) {
                return true; // the usual case
            }
            ObjectDeltaType deltaBean = deltaOperation.getObjectDelta();
            if (deltaBean != null && containsProtectedData(deltaBean)) {
                eraseProtectedData(deltaBean);
            }
            OperationResultType executionResult = deltaOperation.getExecutionResult();
            if (executionResult != null && containsProtectedData(executionResult)) {
                deltaOperation.setExecutionResult(null);
            }
            // Anything left (e.g. raw values that could not be resolved using the schema) means we cannot return the delta.
            return !containsProtectedData(deltaOperation);
        } catch (Exception | LinkageError e) {
            // Neither the delta nor the exception message (which may contain parts of the delta) is logged on WARN.
            LOGGER.warn("Couldn't redact protected data in an audited delta ({}), its content will not be returned",
                    e.getClass().getSimpleName());
            LOGGER.debug("Redaction failure details", e);
            return false;
        }
    }

    /**
     * Erases protected data in the object to add and in the values of item deltas. The values are interpreted using
     * the schema; values of items without definitions (e.g. attributes of shadows) are left intact, so the final check
     * rejects the delta.
     *
     * We intentionally work with the delta bean instead of converting it to {@link ObjectDelta} and back: such
     * a conversion may refuse (now empty) protected values when consistency checks are enabled.
     */
    private void eraseProtectedData(@NotNull ObjectDeltaType deltaBean) throws SchemaException {
        Objectable objectToAdd = deltaBean.getObjectToAdd();
        if (objectToAdd != null) {
            objectToAdd.asPrismObject().accept(new ProtectedDataEraser());
        }
        QName objectTypeName = deltaBean.getObjectType();
        PrismObjectDefinition<?> objectDefinition = objectTypeName != null
                ? prismContext.getSchemaRegistry().findObjectDefinitionByType(objectTypeName)
                : null;
        if (objectDefinition == null) {
            return;
        }
        for (ItemDeltaType itemDelta : deltaBean.getItemDelta()) {
            ItemPathType pathBean = itemDelta.getPath();
            ItemDefinition<?> itemDefinition = pathBean != null
                    ? objectDefinition.findItemDefinition(pathBean.getItemPath())
                    : null;
            if (itemDefinition != null) {
                eraseProtectedData(itemDelta.getValue(), itemDefinition);
                eraseProtectedData(itemDelta.getEstimatedOldValue(), itemDefinition);
            }
        }
    }

    private void eraseProtectedData(@NotNull List<RawType> values, @NotNull ItemDefinition<?> itemDefinition)
            throws SchemaException {
        for (ListIterator<RawType> iterator = values.listIterator(); iterator.hasNext(); ) {
            RawType value = iterator.next();
            if (value == null) {
                continue;
            }
            PrismValue parsedValue = value.getParsedValue(itemDefinition, itemDefinition.getItemName());
            if (parsedValue != null) {
                parsedValue.accept(new ProtectedDataEraser());
                iterator.set(new RawType(parsedValue, itemDefinition.getTypeName()));
            }
        }
    }

    /**
     * Checks the serialized form of the value. This covers also raw (unparsed) parts of deltas, e.g. values
     * of attributes without definitions.
     */
    private boolean containsProtectedData(@NotNull Object realValue) throws Exception {
        RootXNode root = prismContext.xnodeSerializer().serializeRealValue(realValue, PROBE_ROOT_NAME);
        return containsProtectedData(root);
    }

    private static boolean containsProtectedData(@Nullable XNode node) {
        if (node == null) {
            return false;
        }
        if (node instanceof RootXNode root) {
            return containsProtectedData(root.getSubnode());
        } else if (node instanceof MapXNode map) {
            for (Map.Entry<QName, ? extends XNode> entry : map.toMap().entrySet()) {
                if (PROTECTED_DATA_ITEMS.contains(entry.getKey().getLocalPart())
                        || containsProtectedData(entry.getValue())) {
                    return true;
                }
            }
            return false;
        } else if (node instanceof ListXNode list) {
            for (XNode item : list.asList()) {
                if (containsProtectedData(item)) {
                    return true;
                }
            }
            return false;
        } else {
            // A protected value serialized in a non-structured (e.g. primitive) form cannot be checked for its content.
            // (An already redacted protected value is an empty map, which is fine.)
            QName typeName = node.getTypeQName();
            return typeName != null && PROTECTED_DATA_TYPES.contains(typeName.getLocalPart());
        }
    }

    /** Removes all data from protected values. Modelled after the visitor in `CryptoUtil`. */
    @SuppressWarnings({ "rawtypes" })
    private static class ProtectedDataEraser implements ConfigurableVisitor, JaxbVisitor {

        @Override
        public void visit(JaxbVisitable visitable) {
            if (visitable instanceof ProtectedDataType<?> protectedData) {
                protectedData.setEncryptedData(null);
                protectedData.setHashedData(null);
                protectedData.setClearValue(null);
                protectedData.setExternalData(null);
            } else {
                JaxbVisitable.visitPrismStructure(visitable, this);
            }
        }

        @Override
        public void visit(Visitable visitable) {
            if (visitable instanceof PrismPropertyValue<?> propertyValue
                    && propertyValue.getRealValue() instanceof JaxbVisitable jaxbVisitable) {
                jaxbVisitable.accept(this);
            }
        }

        @Override
        public boolean shouldVisitEmbeddedObjects() {
            return true;
        }
    }
}
