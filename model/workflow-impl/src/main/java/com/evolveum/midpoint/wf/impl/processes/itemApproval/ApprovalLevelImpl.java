/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.util.SerializationSafeContainer;
import com.evolveum.midpoint.wf.impl.util.SingleItemSerializationSafeContainerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
public class ApprovalLevelImpl implements ApprovalLevel, Serializable {

    private static final long serialVersionUID = 1989606281279792045L;

	private final int order;
    private String name;
    private String displayName;
    private String description;
    private List<LightweightObjectRefImpl> approverRefs = new ArrayList<>();
    private List<SerializationSafeContainer<ExpressionType>> approverExpressions = new ArrayList<>();
    private LevelEvaluationStrategyType evaluationStrategy;
    private SerializationSafeContainer<ExpressionType> automaticallyApproved;
    private SerializationSafeContainer<ExpressionType> additionalInformation;
	@NotNull private final ApprovalLevelOutcomeType outcomeIfNoApprovers;
	@NotNull private final GroupExpansionType groupExpansion;

    private transient PrismContext prismContext;

    ApprovalLevelImpl(ApprovalLevelType levelType, PrismContext prismContext,
            RelationResolver relationResolver, ReferenceResolver referenceResolver) {

        Validate.notNull(prismContext, "prismContext must not be null");

		this.order = levelType.getOrder() != null ? levelType.getOrder() : 0;
        this.name = levelType.getName();
        this.displayName = levelType.getDisplayName();
        this.description = levelType.getDescription();

        setPrismContext(prismContext);

	    levelType.getApproverRef().forEach(ref -> addApproverRef(referenceResolver, ref));
	    relationResolver.getApprovers(levelType.getApproverRelation()).forEach(this::addResolvedApproverRef);
	    levelType.getApproverExpression().forEach(this::addApproverExpression);
        this.evaluationStrategy = levelType.getEvaluationStrategy();
        setAdditionalInformation(levelType.getAdditionalInformation());
        setAutomaticallyApproved(levelType.getAutomaticallyApproved());
        outcomeIfNoApprovers = resolveDefaultOutcomeIfNoApprovers(levelType.getOutcomeIfNoApprovers());
        groupExpansion = resolveDefaultGroupExpansion(levelType.getGroupExpansion());
    }

    //default: all approvers in one level, evaluation strategy = allMustApprove
    ApprovalLevelImpl(List<ObjectReferenceType> approverRefList, List<ExpressionType> approverExpressionList,
            ExpressionType automaticallyApproved, @NotNull PrismContext prismContext,
			@NotNull ReferenceResolver referenceResolver) {

        Validate.notNull(prismContext, "prismContext must not be null");

        setPrismContext(prismContext);

		order = 0;

        if (approverRefList != null) {
	        approverRefList.forEach(ref -> addApproverRef(referenceResolver, ref));
        }
        if (approverExpressionList != null) {
	        approverExpressionList.forEach(this::addApproverExpression);
        }
        setEvaluationStrategy(LevelEvaluationStrategyType.ALL_MUST_AGREE);
        setAutomaticallyApproved(automaticallyApproved);
		outcomeIfNoApprovers = resolveDefaultOutcomeIfNoApprovers(null);
		groupExpansion = resolveDefaultGroupExpansion(null);
    }

	private ApprovalLevelOutcomeType resolveDefaultOutcomeIfNoApprovers(ApprovalLevelOutcomeType value) {
    	return value != null ? value : ApprovalLevelOutcomeType.REJECT;
	}

	private GroupExpansionType resolveDefaultGroupExpansion(GroupExpansionType value) {
    	return value != null ? value : GroupExpansionType.BY_CLAIMING_WORK_ITEMS;
	}

	@Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	@Override
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<? extends LightweightObjectRef> getApproverRefs() {
        return approverRefs;
    }

    @Override
    public List<ExpressionType> getApproverExpressions() {
        List<ExpressionType> retval = new ArrayList<>();
        for (SerializationSafeContainer<ExpressionType> approverExpression : approverExpressions) {
            if (prismContext != null && approverExpression.getPrismContext() == null) {
                approverExpression.setPrismContext(prismContext);
            }
            retval.add(approverExpression.getValue());
        }
        return Collections.unmodifiableList(retval);
    }

    @Override
    public LevelEvaluationStrategyType getEvaluationStrategy() {
        return evaluationStrategy;
    }

    public void setEvaluationStrategy(LevelEvaluationStrategyType evaluationStrategy) {
        this.evaluationStrategy = evaluationStrategy;
    }

    @Override
    public ExpressionType getAutomaticallyApproved() {
		return unwrap(automaticallyApproved);
    }

	public void setAutomaticallyApproved(ExpressionType automaticallyApproved) {
		this.automaticallyApproved = wrap(automaticallyApproved);
	}

    @Override
    public ExpressionType getAdditionalInformation() {
		return unwrap(additionalInformation);
    }

	public void setAdditionalInformation(ExpressionType additionalInformation) {
		this.additionalInformation = wrap(additionalInformation);
	}

	@NotNull
	private SingleItemSerializationSafeContainerImpl<ExpressionType> wrap(ExpressionType expression) {
		return new SingleItemSerializationSafeContainerImpl<>(expression, prismContext);
	}

	@Nullable
	private ExpressionType unwrap(SerializationSafeContainer<ExpressionType> wrappedExpression) {
		if (wrappedExpression == null) {
			return null;
		}
		if (prismContext != null && wrappedExpression.getPrismContext() == null) {
			wrappedExpression.setPrismContext(prismContext);
		}
		return wrappedExpression.getValue();
	}

	@NotNull
	@Override
	public ApprovalLevelOutcomeType getOutcomeIfNoApprovers() {
		return outcomeIfNoApprovers;
	}

	@Override
	@NotNull
	public GroupExpansionType getGroupExpansion() {
		return groupExpansion;
	}

	public int getOrder() {
		return order;
	}

	@Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public void setPrismContext(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public ApprovalLevelType toApprovalLevelType(PrismContext prismContext) {
        ApprovalLevelType levelType = new ApprovalLevelType();          // is this ok?
		levelType.setOrder(getOrder());
        levelType.setName(getName());
        levelType.setDisplayName(getDisplayName());
        levelType.setDescription(getDescription());
        levelType.setAutomaticallyApproved(getAutomaticallyApproved());
        levelType.setAdditionalInformation(getAdditionalInformation());
        levelType.setEvaluationStrategy(getEvaluationStrategy());
        for (LightweightObjectRef approverRef : approverRefs) {
            levelType.getApproverRef().add(approverRef.toObjectReferenceType());
        }
        for (SerializationSafeContainer<ExpressionType> approverExpression : approverExpressions) {
            approverExpression.setPrismContext(prismContext);
            levelType.getApproverExpression().add(approverExpression.getValue());
        }
        return levelType;
    }

    private void addApproverRef(ReferenceResolver referenceResolver, ObjectReferenceType approverRef) {
		try {
			referenceResolver.resolveReference(approverRef, toString()).forEach(this::addResolvedApproverRef);
		} catch (SchemaException|ObjectNotFoundException|ExpressionEvaluationException e) {
			throw new SystemException("Couldn't resolve approverRef in " + this, e);
		}
	}

    private void addResolvedApproverRef(ObjectReferenceType resolvedApproverRef) {
        approverRefs.add(new LightweightObjectRefImpl(resolvedApproverRef));
    }

    private void addApproverExpression(ExpressionType expressionType) {
        approverExpressions.add(wrap(expressionType));
    }

    @Override
    public String toString() {
        return "ApprovalLevelImpl{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", evaluationStrategy=" + evaluationStrategy +
                ", automaticallyApproved=" + automaticallyApproved +
                ", approverRefs=" + approverRefs +
                ", approverExpressions=" + approverExpressions +
                '}';
    }

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpWithLabelLn(sb, "Order", order, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Name", name, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Description", description, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Evaluation strategy", String.valueOf(evaluationStrategy), indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Outcome if no approvers", String.valueOf(outcomeIfNoApprovers), indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Group expansion", String.valueOf(groupExpansion), indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Approver refs", approverRefs, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Approver expressions", approverExpressions, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Additional information", additionalInformation, indent);
		DebugUtil.debugDumpWithLabelLn(sb, "Automatically approved", automaticallyApproved, indent);
		return sb.toString();
	}

	@Override
	public String getDebugName() {
		return order + "/" + name;
	}

	public boolean isEmpty() {
        return approverRefs.isEmpty() && approverExpressions.isEmpty();
	}
}
