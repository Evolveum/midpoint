/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;


/**
 * @author semancik
 *
 */
public interface ExpressionEvaluatorFactory {

	QName getElementName();

	<V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(
			Collection<JAXBElement<?>> evaluatorElements,
			D outputDefinition,
			ExpressionProfile expressionProfile,
			ExpressionFactory factory,
			String contextDescription, Task task, OperationResult result)
					throws SchemaException, ObjectNotFoundException, SecurityViolationException;

}
