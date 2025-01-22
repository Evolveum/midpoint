/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Referencable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author katka
 */
public class PrismContainerWrapperColumnPanel<C extends Containerable> extends AbstractItemWrapperColumnPanel<PrismContainerWrapper<C>, PrismContainerValueWrapper<C>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismContainerWrapperColumn.class);

    public PrismContainerWrapperColumnPanel(String id, IModel<PrismContainerWrapper<C>> model, ColumnType columnType) {
        super(id, model, columnType);
    }

    @Override
    protected String createLabel(PrismContainerValueWrapper<C> object) {
        C realValue = object.getRealValue();
        if (realValue == null) {
            return "";
        }

        if (PolicyConstraintsType.class.isAssignableFrom(realValue.getClass())) {
            return PolicyRuleTypeUtil.toShortString((PolicyConstraintsType) realValue);
        }

        if (PolicyActionsType.class.isAssignableFrom(realValue.getClass())) {
            return PolicyRuleTypeUtil.toShortString((PolicyActionsType) realValue);
        }

        if (ActivationType.class.isAssignableFrom(realValue.getClass())) {
            return getActivationLabelLabel((ActivationType) realValue);
        }

        //TODO what to show?
        if (LifecycleStateModelType.class.isAssignableFrom(realValue.getClass())) {
            return realValue.toString();
        }

        if (LifecycleStateType.class.isAssignableFrom(realValue.getClass())) {
            LifecycleStateType state = (LifecycleStateType) realValue;
            if (StringUtils.isBlank(state.getDisplayName())) {
                return state.getName();
            }
            return state.getDisplayName();
        }

        if (ResourceObjectAssociationType.class.isAssignableFrom(realValue.getClass())) {
            return getAssociationLabel((ResourceObjectAssociationType) realValue);
        }

        if (PendingOperationType.class.isAssignableFrom(realValue.getClass())) {
            return ProvisioningObjectsUtil.getPendingOperationLabel((PendingOperationType) realValue, this);
        }

        return realValue.toString();

    }

    private String getActivationLabelLabel(ActivationType activation) {
        if (activation.getAdministrativeStatus() != null) {
            return activation.getAdministrativeStatus().value();
        }

        return AssignmentsUtil.createActivationTitleModel(activation, getPageBase()).getObject();
    }

    private String getAssociationLabel(ResourceObjectAssociationType association) {
        if (association == null) {
            return "";
        }
        return association != null ?
                (StringUtils.isNotEmpty(association.getDisplayName()) ? association.getDisplayName() : association.getRef().toString())
                : null;

    }

    private String getLifecycleState(PrismPropertyWrapper<String> lifecycle) {
        if (lifecycle == null) {
            return null;
        }

        List<PrismPropertyValueWrapper<String>> values = lifecycle.getValues();
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        return values.iterator().next().getRealValue();
    }

    @Override
    protected Panel createValuePanel(String id, IModel<PrismContainerWrapper<C>> model) {
        throw new UnsupportedOperationException("Panels not supported for container values.");
    }

    @Override
    protected Panel createLink(String id, IModel<PrismContainerValueWrapper<C>> object) {
        throw new UnsupportedOperationException("Links not supported for container values.");
    }
}
