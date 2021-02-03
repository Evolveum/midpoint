/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * TODO
 */
public interface SynchronizationService extends ResourceObjectChangeListener {

    <F extends FocusType> SynchronizationContext<F> loadSynchronizationContext(PrismObject<ShadowType> applicableShadow,
            PrismObject<ShadowType> currentShadow, ObjectDelta<ShadowType> objectDelta, PrismObject<ResourceType> resource,
            String sourceChanel, String itemProcessingIdentifier, PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result) throws
            SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException;

    <F extends FocusType> boolean matchUserCorrelationRule(PrismObject<ShadowType> shadow, PrismObject<F> focus,
            ResourceType resourceType, PrismObject<SystemConfigurationType> configuration, Task task, OperationResult result) throws
            ConfigurationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException;
}
