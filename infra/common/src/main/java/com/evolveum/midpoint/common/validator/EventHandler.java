/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.validator;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Set of callback methods used to convey information from the validator to the "working" code.
 *
 * It is used e.g. to connect validator to the code that executes import. It makes validator quite a generic tool.
 * E.g. it can be used as the base of the import in the system and the same validator can be used in tests to
 * check the validity of samples.
 *
 * @author Radovan Semancik
 */
public interface EventHandler {

    /**
     * Call-back called after deserializing to DOM and static schema validation but before unmarshal to JAXB.
     * It can be used for extra DOM-based checks or transformations of the object.
     *
     * @param objectElement DOM tree parsed from file
     * @param postValidationTree post-validation DOM tree
     * @param objectResult Operation result for this object
     * @return true if the process should continue, false if it should stop
     */
    EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult);

    /**
     * Call-back called after the object is unmarshalled.
     *
     * The compliance with static schemas should already be checked. This is the "main" call-back as it is expected that
     * this call-back will do the primary part of work such storing the object to repository during import.
     *
     * @param object unmarshalled JAXB object
     * @param objectElement DOM tree parsed from the fil
     * @param objectResult Operation result for this object
     * @return true if the process should continue, false if it should stop
     */
    <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult);

    /**
     * Call-back to handle global errors.
     *
     * This callback will be called with any error that cannot be attributed to any particular object.
     *
     * @param currentResult Operation result pointing to the particular error.
     * @return true if the process should continue, false if it should stop
     */
    void handleGlobalError(OperationResult currentResult);

}
