/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * Interface that provide ability to query status of asynchronous operation.
 *
 * @author Radovan Semancik
 */
@FunctionalInterface
public interface AsynchronousOperationQueryable {

    OperationResultStatus queryOperationStatus(String asynchronousOperationReference, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException;

}
