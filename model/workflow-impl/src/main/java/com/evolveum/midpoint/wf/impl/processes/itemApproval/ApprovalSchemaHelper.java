/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * @author mederly
 */
@Component
public class ApprovalSchemaHelper {

	@Autowired
	private PrismContext prismContext;

	@NotNull
	public ApprovalSchemaType mergeIntoSchema(ApprovalSchemaType existing, @NotNull ApprovalSchemaType newSchema) {
        if (existing == null) {
            ApprovalSchemaType cloned = newSchema.clone();
            fixOrders(cloned);
            return cloned;
        }

        int maxOrderExisting = getMaxOrder(existing).orElse(0);
        for (ApprovalLevelType level : newSchema.getLevel()) {
            ApprovalLevelType levelClone = level.clone();
            if (levelClone.getOrder() != null) {
                levelClone.setOrder(maxOrderExisting + levelClone.getOrder() + 1);
            }
            existing.getLevel().add(levelClone);
        }
        fixOrders(existing);
        return existing;
    }

	private void fixOrders(ApprovalSchemaType cloned) {
        int maxOrder = getMaxOrder(cloned).orElse(0);
        for (ApprovalLevelType level : cloned.getLevel()) {
            if (level.getOrder() == null) {
                level.setOrder(++maxOrder);
            }
        }
    }

	public Optional<Integer> getMaxOrder(ApprovalSchemaType schema) {
        if (schema == null) {
            return Optional.empty();
        } else {
            return schema.getLevel().stream()
                    .map(ApprovalLevelType::getOrder)
                    .filter(o -> o != null)
                    .max(Integer::compare);
        }
    }

	public ApprovalSchemaType mergeIntoSchema(ApprovalSchemaType existing,
            @NotNull List<ObjectReferenceType> approverRefList,
            @NotNull List<ExpressionType> approverExpressionList,
			ExpressionType automaticallyApproved,
            @Nullable List<ObjectReferenceType> additionalApprovers) {
        if (existing == null) {
            existing = new ApprovalSchemaType(prismContext);
        }
        int maxOrderExisting = getMaxOrder(existing).orElse(0);

        ApprovalLevelType level = new ApprovalLevelType(prismContext);
        level.setOrder(maxOrderExisting + 1);
        level.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approverRefList));
        if (additionalApprovers != null) {
            level.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(additionalApprovers));
        }
        level.getApproverExpression().addAll(approverExpressionList);
        level.setAutomaticallyApproved(automaticallyApproved);
        existing.getLevel().add(level);
        return existing;
    }
}
