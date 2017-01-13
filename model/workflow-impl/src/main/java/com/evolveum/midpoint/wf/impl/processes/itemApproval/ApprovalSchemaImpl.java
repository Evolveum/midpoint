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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author mederly
 */
public class ApprovalSchemaImpl implements ApprovalSchema, Serializable {

    private static final long serialVersionUID = 5995218487603801207L;

    private String name;
    private String description;
    @NotNull private final List<ApprovalLevelImpl> levels = new ArrayList<>();

    private transient PrismContext prismContext;

    ApprovalSchemaImpl(ApprovalSchemaType approvalSchemaType, @NotNull PrismContext prismContext,
            RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        setPrismContext(prismContext);
        initFromApprovalSchemaType(approvalSchemaType, relationResolver, referenceResolver);
    }

    ApprovalSchemaImpl(ApprovalSchemaType approvalSchema, List<ObjectReferenceType> approverRefList,
            List<ExpressionType> approverExpressionList, ExpressionType automaticallyApproved,
            @NotNull PrismContext prismContext, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        setPrismContext(prismContext);
        if (approvalSchema != null) {
            initFromApprovalSchemaType(approvalSchema, relationResolver, referenceResolver);
        } else if ((approverRefList != null && !approverRefList.isEmpty()) || (approverExpressionList != null && !approverExpressionList.isEmpty())) {
            ApprovalLevelImpl level = new ApprovalLevelImpl(approverRefList, approverExpressionList,
                    automaticallyApproved, prismContext, referenceResolver);
            addLevel(level);
        } else {
            throw new IllegalArgumentException("Neither approvalSchema nor approverRef/approverExpression is filled-in");
        }
    }

    private void initFromApprovalSchemaType(ApprovalSchemaType approvalSchemaType,
			RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        this.name = approvalSchemaType.getName();
        this.description = approvalSchemaType.getDescription();
        for (ApprovalLevelType levelType : approvalSchemaType.getLevel()) {
            addLevel(levelType, relationResolver, referenceResolver);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public List<? extends ApprovalLevel> getLevels() {
		List<ApprovalLevelImpl> rv = new ArrayList<>(levels);
		Collections.sort(rv, Comparator.comparingInt(ApprovalLevelImpl::getOrder));
        return Collections.unmodifiableList(rv);
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public void setPrismContext(@NotNull PrismContext prismContext) {
        this.prismContext = prismContext;
        for (ApprovalLevel approvalLevel : levels) {
            approvalLevel.setPrismContext(prismContext);
        }
    }

    @Override
    public void toApprovalSchemaType(ApprovalSchemaType approvalSchemaType) {
        approvalSchemaType.setName(getName());
        approvalSchemaType.setDescription(getDescription());
        for (ApprovalLevel level : getLevels()) {
            approvalSchemaType.getLevel().add(level.toApprovalLevelType(prismContext));
        }
    }

    @Override public ApprovalSchemaType toApprovalSchemaType() {
        ApprovalSchemaType ast = new ApprovalSchemaType();
        toApprovalSchemaType(ast);
        return ast;
    }

    private void addLevel(ApprovalLevelImpl level) {
        levels.add(level);
    }

    private void addLevel(ApprovalLevelType levelType, RelationResolver relationResolver, ReferenceResolver referenceResolver) {
        addLevel(new ApprovalLevelImpl(levelType, prismContext, relationResolver, referenceResolver));
    }

    @Override
    public String toString() {
        return "ApprovalSchemaImpl{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", levels=" + levels +
                '}';
    }

    @Override
    public boolean isEmpty() {
        return levels.stream().allMatch(l -> l.isEmpty());
    }
}
