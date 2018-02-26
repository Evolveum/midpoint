/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author lazyman
 */
public class PrismIdentifierGenerator<O extends ObjectType> implements DebugDumpable {
	
	private static final Trace LOGGER = TraceManager.getTrace(PrismIdentifierGenerator.class);

    public enum Operation {ADD, ADD_WITH_OVERWRITE, MODIFY}
    
    private final Operation operation;
    private Long lastId = null;
    private Set<Long> usedIds = new HashSet<>();
    
    public PrismIdentifierGenerator(@NotNull Operation operation) {
		super();
		this.operation = operation;
	}

	/**
     * Method inserts id for prism container values, which didn't have ids,
     * also returns all container values which has generated id
     */
    public IdGeneratorResult generate(@NotNull PrismObject<O> object) {
        IdGeneratorResult result = new IdGeneratorResult();
        boolean adding = Operation.ADD.equals(operation);
        result.setGeneratedOid(adding);

        if (StringUtils.isEmpty(object.getOid())) {
            String oid = UUID.randomUUID().toString();
            object.setOid(oid);
            result.setGeneratedOid(true);
        }

        List<PrismContainer<?>> values = listAllPrismContainers(object);
        generateContainerIds(values, result);

        return result;
    }
    
    public IdGeneratorResult generate(Containerable containerable) {
        IdGeneratorResult result = new IdGeneratorResult();
        if (!(containerable instanceof AccessCertificationCaseType)) {
            return result;
        }
        AccessCertificationCaseType aCase = (AccessCertificationCaseType) containerable;

        List<PrismContainer<?>> values = listAllPrismContainers(aCase.asPrismContainerValue());
        generateContainerIds(values, result);

        return result;
    }

    private List<PrismContainer<?>> listAllPrismContainers(Visitable object) {
        List<PrismContainer<?>> values = new ArrayList<>();

        object.accept(visitable -> {
            if (!(visitable instanceof PrismContainer)) {
                return;
            }

            if (visitable instanceof PrismObject) {
                return;
            }

            PrismContainer container = (PrismContainer) visitable;
            PrismContainerDefinition def = container.getDefinition();
            if (def.isSingleValue()) {
                return;
            }

            values.add((PrismContainer) visitable);
        });

        return values;
    }
    
    public void collectUsedIds(@NotNull PrismObject<O> object) {
    	collectUsedIds(listAllPrismContainers(object));
    }
    
    private void collectUsedIds(List<PrismContainer<?>> containers) {
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() != null) {
                    usedIds.add(val.getId());
                }
            }
        }
    }

    private void generateContainerIds(List<PrismContainer<?>> containers, IdGeneratorResult result) {
    	collectUsedIds(containers);
        Long nextId = null;
        for (PrismContainer<?> c : containers) {
            for (PrismContainerValue<?> val : c.getValues()) {
                if (val.getId() != null) {
                    if (operation == Operation.ADD) {
                        result.getValues().add(val);
                    }
                } else {
                    val.setId(nextId());
                    if (operation == Operation.ADD) {
                        result.getValues().add(val);
                    }
                }
            }
        }
    }
    
    public long nextId() {
    	if (lastId == null) {
    		lastId = getStartId();
    	}
    	lastId++;
    	return lastId;
    }
    
    private long getStartId() {
    	if (usedIds.isEmpty()) {
    		return 0L;
    	}
    	return Collections.max(usedIds);
    }

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = DebugUtil.createTitleStringBuilder(PrismIdentifierGenerator.class, indent);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "operation", operation, indent + 1);
		DebugUtil.debugDumpWithLabelToStringLn(sb, "lastId", lastId, indent + 1);
		DebugUtil.debugDumpWithLabel(sb, "usedIds", usedIds, indent + 1);
		return sb.toString();
	}

}
