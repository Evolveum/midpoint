/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * TODO
 */
public interface SynchronizationService extends ResourceObjectChangeListener {

    /**
     * Tries to match specified focus and shadow. Return true if it matches, false otherwise.
     */
    <F extends FocusType> boolean matchUserCorrelationRule(
            PrismObject<ShadowType> shadowedResourceObject,
            PrismObject<F> focus,
            ResourceType resource,
            PrismObject<SystemConfigurationType> configuration,
            Task task,
            OperationResult result) throws
            ConfigurationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException;
}
