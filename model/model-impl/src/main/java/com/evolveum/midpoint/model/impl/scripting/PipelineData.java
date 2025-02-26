/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.helpers.ScriptingDataUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineDataType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineItemType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionEvaluationOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

/**
 * Data that are passed between individual scripting actions.
 * <p/>
 * The content passed between actions (expressions) is a list of prism values
 * (object, container, reference, property) enriched with some additional information,
 * see {@link PipelineItem}.
 */
public class PipelineData implements DebugDumpable {

    private static final String ITEM_OPERATION_NAME = BulkActionsExecutor.class.getName() + ".process";

    /**
     * The data in the pipeline. All items are not null.
     */
    @NotNull private final List<PipelineItem> data = new ArrayList<>();

    // we want clients to use explicit constructors
    private PipelineData() {
    }

    public @NotNull List<PipelineItem> getData() {
        return data;
    }


    public @NotNull static PipelineData create(@NotNull PrismValue value, @NotNull VariablesMap variables) {
        PipelineData d = createEmpty();
        d.add(new PipelineItem(value, newOperationResult(), variables));
        return d;
    }

    public @NotNull static PipelineData createEmpty() {
        return new PipelineData();
    }

    public @NotNull static OperationResult newOperationResult() {
        return new OperationResult(ITEM_OPERATION_NAME);
    }

    public void add(@NotNull PipelineItem pipelineItem) {
        data.add(pipelineItem);
    }

    public void addAllFrom(PipelineData otherData) {
        if (otherData != null) {
            data.addAll(otherData.getData());
        }
    }

    public void addValue(PrismValue value, VariablesMap variables) {
        addValue(value, null, variables);
    }

    public void addValue(PrismValue value, OperationResult result, VariablesMap variables) {
        data.add(new PipelineItem(value,
                result != null ? result : newOperationResult(),
                variables != null ? variables : VariablesMap.emptyMap()));
    }

    public <T> T getSingleValue(Class<T> clazz) throws SchemaException {
        if (data.isEmpty()) {
            return null;
        } else if (data.size() == 1) {
            return ScriptingDataUtil.getRealValue(data.get(0).getValue(), clazz);
        } else {
            throw new SchemaException("Multiple values where just one is expected");
        }
    }

    /**
     * Returns the pipeline content as a list of references. Objects, PRVs, OIDs are converted directly
     * to references. Search filters and queries are evaluated first.
     *
     * This is a legacy method and its use should be avoided.
     */
    @NotNull
    public List<ObjectReferenceType> getDataAsReferences(QName defaultTargetType, Class<? extends ObjectType> typeForQuery,
            ExecutionContext context, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException,
            SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        List<ObjectReferenceType> retval = new ArrayList<>(data.size());
        for (PipelineItem item : data) {
            PrismValue value = item.getValue();
            if (value instanceof PrismObjectValue<?> objectValue) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setType(objectValue.asPrismObject().getDefinition().getTypeName());
                ref.setOid(objectValue.getOid());
                retval.add(ref);
            } else if (value instanceof PrismPropertyValue) {
                Object realValue = value.getRealValue();
                if (realValue instanceof SearchFilterType) {
                    retval.addAll(
                            resolveQuery(
                                    typeForQuery, new QueryType().filter((SearchFilterType) realValue), context, result));
                } else if (realValue instanceof QueryType) {
                    retval.addAll(
                            resolveQuery(typeForQuery, (QueryType) realValue, context, result));
                } else if (realValue instanceof String) {
                    ObjectReferenceType ref = new ObjectReferenceType();
                    ref.setType(defaultTargetType);
                    ref.setOid((String) realValue);
                    retval.add(ref);
                } else if (realValue instanceof ObjectReferenceType) {
                    retval.add((ObjectReferenceType) realValue);
                } else {
                    throw new UnsupportedOperationException("Unsupported reference type: " + value.getClass());
                }
            } else if (value instanceof PrismReferenceValue referenceValue) {
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setupReferenceValue(referenceValue);
                retval.add(ref);
            }
        }
        return retval;
    }

    private Collection<ObjectReferenceType> resolveQuery(Class<? extends ObjectType> type, QueryType queryBean,
            ExecutionContext context, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectQuery query = context.getQueryConverter().createObjectQuery(type, queryBean);
        SearchResultList<? extends PrismObject<? extends ObjectType>> objects = context.getModelService()
                .searchObjects(type, query, readOnly(), context.getTask(), result);
        return objects.stream().map(o -> ObjectTypeUtil.createObjectRef(o)).collect(Collectors.toList());
    }

    static @NotNull PipelineData parseFrom(ValueListType input, VariablesMap frozenInitialVariables) {
        PipelineData rv = new PipelineData();
        if (input != null) {
            for (Object object : input.getValue()) {
                if (object instanceof PrismValue) {
                    rv.addValue((PrismValue) object, frozenInitialVariables);
                } else if (object instanceof RawType raw) {
                    // a bit of hack: this should have been solved by the parser (we'll fix this later)
                    PrismValue prismValue = raw.getAlreadyParsedValue();
                    if (prismValue != null) {
                        rv.addValue(prismValue, frozenInitialVariables);
                    } else {
                        throw new IllegalArgumentException("Raw value in the input data: " + DebugUtil.debugDump(raw.getXnode()));
                        // TODO attempt to parse it somehow (e.g. by passing to the pipeline and then parsing based on expected type)
                    }
                } else {
                    if (object instanceof Containerable) {
                        rv.addValue(((Containerable) object).asPrismContainerValue(), frozenInitialVariables);
                    } else if (object instanceof Referencable) {
                        rv.addValue(((Referencable) object).asReferenceValue(), frozenInitialVariables);
                    } else {
                        rv.addValue(PrismContext.get().itemFactory().createPropertyValue(object), frozenInitialVariables);
                    }
                }
            }
        }
        return rv;
    }

    PipelineData cloneMutableState() {
        PipelineData rv = new PipelineData();
        data.forEach(d -> rv.add(d.cloneMutableState()));
        return rv;
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(data, indent);
    }

    public static PipelineDataType prepareXmlData(
            List<PipelineItem> output, ScriptingExpressionEvaluationOptionsType options) {
        boolean hideResults = options != null && Boolean.TRUE.equals(options.isHideOperationResults());
        PipelineDataType rv = new PipelineDataType();
        if (output != null) {
            for (PipelineItem item : output) {
                PipelineItemType itemType = new PipelineItemType();
                PrismValue value = item.getValue();
                if (value instanceof PrismReferenceValue) {
                    // This is a bit of hack: value.getRealValue() would return unserializable object (PRV$1 - does not have type QName)
                    ObjectReferenceType ort = new ObjectReferenceType();
                    ort.setupReferenceValue((PrismReferenceValue) value);
                    itemType.setValue(ort);
                } else {
                    itemType.setValue(value.getRealValue());
                }
                if (!hideResults) {
                    itemType.setResult(item.getResult().createBeanReduced());
                }
                rv.getItem().add(itemType);
            }
        }
        return rv;
    }

    @Override
    public String toString() {
        return "PipelineData{" +
                "data=" + data +
                '}';
    }
}
