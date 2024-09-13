/*
 * Copyright (c) 2017-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedAbstractConstruction;
import com.evolveum.midpoint.model.impl.lens.construction.EvaluatedConstructionPack;
import com.evolveum.midpoint.prism.delta.DeltaMapTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;

/**
 * @author Radovan Semancik
 *
 */
public interface ComplexConstructionConsumer<K, EC extends EvaluatedAbstractConstruction<?>> {

    boolean before(K key);

    void onAssigned(K key, String desc, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    void onUnchangedValid(K key, String desc, Task task, OperationResult result)
            throws SchemaException, ConfigurationException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException;

    void onUnchangedInvalid(K key, String desc) throws SchemaException, ConfigurationException;

    void onUnassigned(K key, String desc) throws SchemaException, ConfigurationException;

    void after(K key, String desc, DeltaMapTriple<K, EvaluatedConstructionPack<EC>> constructionMapTriple);
}
