/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.model.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Evaluated assignment that contains all constructions and authorizations from the assignment 
 * itself and all the applicable inducements from all the roles referenced from the assignment.
 * 
 * @author Radovan Semancik
 */
public class EvaluatedAssignment<F extends FocusType> implements DebugDumpable {
	
	private static final Trace LOGGER = TraceManager.getTrace(EvaluatedAssignment.class);

	private ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi;
	private DeltaSetTriple<Construction<F>> constructions;
	private Collection<PrismReferenceValue> orgRefVals;
	private Collection<Authorization> authorizations;
	private Collection<Mapping<? extends PrismPropertyValue<?>>> focusMappings;
	private PrismObject<?> target;
	private boolean isValid;
	private boolean forceRecon;

	public EvaluatedAssignment() {
		constructions = new DeltaSetTriple<>();
		orgRefVals = new ArrayList<>();
		authorizations = new ArrayList<>();
		focusMappings = new ArrayList<>();
	}

	public ItemDeltaItem<PrismContainerValue<AssignmentType>> getAssignmentIdi() {
		return assignmentIdi;
	}

	public void setAssignmentIdi(ItemDeltaItem<PrismContainerValue<AssignmentType>> assignmentIdi) {
		this.assignmentIdi = assignmentIdi;
	}
	
	public AssignmentType getAssignmentType() {
		return assignmentIdi.getItemNew().getValue(0).asContainerable();
	}

	public DeltaSetTriple<Construction<F>> getConstructions() {
		return constructions;
	}

	public void addConstructionZero(Construction<F> contruction) {
		constructions.addToZeroSet(contruction);
	}
	
	public void addConstructionPlus(Construction<F> contruction) {
		constructions.addToPlusSet(contruction);
	}
	
	public void addConstructionMinus(Construction<F> contruction) {
		constructions.addToMinusSet(contruction);
	}
	
	public Collection<PrismReferenceValue> getOrgRefVals() {
		return orgRefVals;
	}

	public void addOrgRefVal(PrismReferenceValue org) {
		orgRefVals.add(org);
	}

	public Collection<Authorization> getAuthorizations() {
		return authorizations;
	}
	
	public void addAuthorization(Authorization authorization) {
		authorizations.add(authorization);
	}

	public Collection<Mapping<? extends PrismPropertyValue<?>>> getFocusMappings() {
		return focusMappings;
	}

	public void addFocusMapping(Mapping<? extends PrismPropertyValue<?>> focusMapping) {
		this.focusMappings.add(focusMapping);
	}

	public PrismObject<?> getTarget() {
		return target;
	}

	public void setTarget(PrismObject<?> target) {
		this.target = target;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public boolean isForceRecon() {
		return forceRecon;
	}

	public void setForceRecon(boolean forceRecon) {
		this.forceRecon = forceRecon;
	}

	public Collection<ResourceType> getResources(OperationResult result) throws ObjectNotFoundException, SchemaException {
		Collection<ResourceType> resources = new ArrayList<ResourceType>();
		for (Construction<F> acctConstr: constructions.getAllValues()) {
			resources.add(acctConstr.getResource(result));
		}
		return resources;
	}
	
	public void evaluateConstructions(ObjectDeltaObject<F> focusOdo, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		for (Construction<F> construction :constructions.getAllValues()) {
			construction.setFocusOdo(focusOdo);
			LOGGER.trace("Evaluating construction '{}' in {}", construction, construction.getSource());
			construction.evaluate(task, result);
		}
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpLabel(sb, "EvaluatedAssignment", indent);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "isValid", isValid, indent + 1);
		if (!constructions.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Constructions", constructions, indent+1);
		}
		if (!orgRefVals.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Orgs", indent+1);
			for (PrismReferenceValue org: orgRefVals) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(org.toString());
			}
		}
		if (!authorizations.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Authorizations", indent+1);
			for (Authorization autz: authorizations) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(autz.toString());
			}
		}
		if (!focusMappings.isEmpty()) {
			sb.append("\n");
			DebugUtil.debugDumpLabel(sb, "Focus Mappings", indent+1);
			for (Mapping<? extends PrismPropertyValue<?>> mapping: focusMappings) {
				sb.append("\n");
				DebugUtil.indentDebugDump(sb, indent+2);
				sb.append(mapping.toString());
			}
		}
		if (target != null) {
			sb.append("\n");
			DebugUtil.debugDumpWithLabel(sb, "Target", target.toString(), indent+1);
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "EvaluatedAssignment(acc=" + constructions + "; org="+orgRefVals+"; autz="+authorizations+"; "+focusMappings.size()+" focus mappings)";
	}
	
}
