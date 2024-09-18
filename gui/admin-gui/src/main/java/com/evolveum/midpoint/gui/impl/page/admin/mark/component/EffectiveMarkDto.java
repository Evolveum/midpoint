/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.mark.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.schema.util.MarkTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyStatementType;

import java.io.Serializable;
import java.util.Optional;

public class EffectiveMarkDto<O extends ObjectType> implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(MarksOfObjectListPanel.class);

    private boolean addedByPolicyStatement = false;
    private boolean addedByMarkingRule = false;
    private boolean addedByPolicyRule = false;
    private boolean transitional = false;
    private PrismReferenceValueWrapperImpl<ObjectReferenceType> effectiveMark;
    private PrismContainerValueWrapper<PolicyStatementType> policyStatement;

    EffectiveMarkDto(PrismObjectWrapper<O> objectWrapper, MarkType markType){
        initEffectiveMark(objectWrapper, markType);
        initPolicyStatement(objectWrapper, markType);
        ObjectReferenceType cleanupEffectivePolicy = getCleanupEffectiveMark();
        if (cleanupEffectivePolicy != null) {
            addedByPolicyStatement = MarkTypeUtil.isAddedByPolicyStatement(cleanupEffectivePolicy);
            transitional = MarkTypeUtil.isTransitional(cleanupEffectivePolicy);
            addedByMarkingRule = MarkTypeUtil.isAddedByMarkingRule(cleanupEffectivePolicy);
            addedByPolicyRule = MarkTypeUtil.isAddedByPolicyRule(cleanupEffectivePolicy);
        }
        if (!addedByPolicyStatement && policyStatement != null) {
            addedByPolicyStatement = true;
        }
    }

    EffectiveMarkDto(PrismContainerValueWrapper<PolicyStatementType> policyStatement){
        addedByPolicyStatement = true;
        this.policyStatement = policyStatement;
    }

    public boolean isAddedByPolicyStatement() {
        return addedByPolicyStatement;
    }

    public boolean isTransitional() {
        return transitional;
    }

    public boolean isAddedByMarkingRule() {
        return addedByMarkingRule;
    }

    public boolean isAddedByPolicyRule() {
        return addedByPolicyRule;
    }

    public PrismReferenceValueWrapperImpl<ObjectReferenceType> getEffectiveMark() {
        return effectiveMark;
    }

    public PrismContainerValueWrapper<PolicyStatementType> getPolicyStatement() {
        return policyStatement;
    }

    private void initEffectiveMark(
            PrismObjectWrapper<O> objectWrapper, MarkType markType) {
        if (markType == null) {
            return;
        }

        String oid = markType.getOid();
        PrismReferenceWrapper<ObjectReferenceType> wrapper = null;
        try {
            wrapper = objectWrapper.findReference(ObjectType.F_EFFECTIVE_MARK_REF);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't find policy statement for " + markType);
            return;
        }

        if (wrapper == null) {
            return;
        }

        Optional<PrismReferenceValueWrapperImpl<ObjectReferenceType>> effectiveMark =
                wrapper.getValues().stream().filter(value -> {
                    if (value == null || value.getRealValue().getOid() == null) {
                        return false;
                    }

                    return value.getRealValue().getOid().equals(oid);
                }).findFirst();

        this.effectiveMark = effectiveMark.orElse(null);
    }

    private ObjectReferenceType getCleanupEffectiveMark() {
        if (effectiveMark == null) {
            return null;
        }

        ObjectReferenceType effectiveMarkBean = effectiveMark.getRealValue().clone();
        WebPrismUtil.cleanupValueMetadata(effectiveMarkBean.asReferenceValue());
        return effectiveMarkBean;
    }

    private void initPolicyStatement(
            PrismObjectWrapper<O> objectWrapper, MarkType markBean) {
        if (markBean == null) {
            return;
        }

            PrismContainerWrapper<PolicyStatementType> container;
            try {
                container = objectWrapper.findContainer(ObjectType.F_POLICY_STATEMENT);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't find effective mark for " + markBean);
                return;
            }

            if (container == null) {
                return;
            }

            Optional<PrismContainerValueWrapper<PolicyStatementType>> policyStatement =
                    container.getValues().stream().filter(value -> {
                        if (value == null || value.getRealValue() == null || value.getRealValue().getMarkRef() == null) {
                            return false;
                        }

                        return markBean.getOid().equals(value.getRealValue().getMarkRef().getOid());
                    }).findFirst();
            this.policyStatement = policyStatement.orElse(null);
    }

}
