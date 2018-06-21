/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ValueListType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

/**
 * Data that are passed between individual scripting actions.
 *
 * The content passed between actions (expressions) is a list of prism values
 * (object, container, reference, property).
 *
 * @author mederly
 */
public class PipelineData implements DebugDumpable {

    private static final String ITEM_OPERATION_NAME = ScriptingExpressionEvaluator.class.getName() + ".process";

    private final List<PipelineItem> data = new ArrayList<>();			// all items are not null

    // we want clients to use explicit constructors
    private PipelineData() {
    }

    public List<PipelineItem> getData() {
        return data;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return DebugUtil.debugDump(data, indent);
    }

    public static PipelineData create(PrismValue value) {
        return create(value, emptyMap());
    }

    public static PipelineData create(PrismValue value, Map<String, Object> variables) {
        PipelineData d = createEmpty();
        d.add(new PipelineItem(value, newOperationResult(), variables));
        return d;
    }

    public static OperationResult newOperationResult() {
        return new OperationResult(ITEM_OPERATION_NAME);
    }

    public void add(@NotNull PipelineItem pipelineItem) {
        data.add(pipelineItem);
    }

    public static PipelineData createEmpty() {
        return new PipelineData();
    }

    public void addAllFrom(PipelineData otherData) {
        if (otherData != null) {
            data.addAll(otherData.getData());
        }
    }

    public void addValue(PrismValue value, Map<String, Object> variables) {
		addValue(value, null, variables);
    }

    public void addValue(PrismValue value, OperationResult result, Map<String, Object> variables) {
		data.add(new PipelineItem(value,
				result != null ? result : newOperationResult(),
				variables != null ? variables : emptyMap()));
    }

    public String getDataAsSingleString() throws ScriptExecutionException {
        if (!data.isEmpty()) {
            if (data.size() == 1) {
                return (String) ((PrismPropertyValue) data.get(0).getValue()).getRealValue();       // todo implement some diagnostics when this would fail
            } else {
                throw new ScriptExecutionException("Multiple values where just one is expected");
            }
        } else {
            return null;
        }
    }

    static PipelineData createItem(@NotNull PrismValue value, Map<String, Object> variables) throws SchemaException {
        PipelineData data = createEmpty();
	    data.addValue(value, variables);
	    return data;
    }

    public Collection<ObjectReferenceType> getDataAsReferences(QName defaultTargetType, Class<? extends ObjectType> typeForQuery,
		    ExecutionContext context, OperationResult result)
		    throws ScriptExecutionException, CommunicationException, ObjectNotFoundException, SchemaException,
		    SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        Collection<ObjectReferenceType> retval = new ArrayList<>(data.size());
        for (PipelineItem item : data) {
        	PrismValue value = item.getValue();
            if (value instanceof PrismObjectValue) {
                PrismObjectValue objectValue = (PrismObjectValue) value;
                ObjectReferenceType ref = new ObjectReferenceType();
                ref.setType(objectValue.asPrismObject().getDefinition().getTypeName()); // todo check the definition is present
                ref.setOid(objectValue.getOid());                  // todo check if oid is present
                retval.add(ref);
            } else if (value instanceof PrismPropertyValue) {
                Object realValue = ((PrismPropertyValue) value).getRealValue();
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
                    ref.setOid((String) realValue);                         // todo implement search by name
                    retval.add(ref);
                } else if (realValue instanceof ObjectReferenceType) {
                    retval.add((ObjectReferenceType) realValue);
                } else {
                    throw new ScriptExecutionException("Unsupported reference type: " + value.getClass());
                }
            } else if (value instanceof PrismReferenceValue) {
                PrismReferenceValue referenceValue = (PrismReferenceValue) value;
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
		ObjectQuery query = QueryJaxbConvertor.createObjectQuery(type, queryBean, context.getPrismContext());
		SearchResultList<? extends PrismObject<? extends ObjectType>> objects = context.getModelService()
				.searchObjects(type, query, null, context.getTask(), result);
		return objects.stream().map(o -> ObjectTypeUtil.createObjectRef(o)).collect(Collectors.toList());
	}

	static PipelineData parseFrom(ValueListType input, Map<String, Object> frozenInitialVariables, PrismContext prismContext) {
		PipelineData rv = new PipelineData();
		if (input != null) {
			for (Object o : input.getValue()) {
				if (o instanceof RawType) {
					// a bit of hack: this should have been solved by the parser (we'll fix this later)
					RawType raw = (RawType) o;
					PrismValue prismValue = raw.getAlreadyParsedValue();
					if (prismValue != null) {
						rv.addValue(prismValue, frozenInitialVariables);
					} else {
						throw new IllegalArgumentException("Raw value in the input data: " + DebugUtil.debugDump(raw.getXnode()));
						// TODO attempt to parse it somehow (e.g. by passing to the pipeline and then parsing based on expected type)
					}
				} else {
					if (o instanceof Containerable) {
						rv.addValue(((Containerable) o).asPrismContainerValue(), frozenInitialVariables);
					} else if (o instanceof Referencable) {
						rv.addValue(((Referencable) o).asReferenceValue(), frozenInitialVariables);
					} else {
						rv.addValue(new PrismPropertyValue<>(o, prismContext), frozenInitialVariables);
					}
				}
			}
		}
		return rv;
	}

	public PipelineData cloneMutableState() {
		PipelineData rv = new PipelineData();
		data.forEach(d -> rv.add(d.cloneMutableState()));
		return rv;
	}
}
