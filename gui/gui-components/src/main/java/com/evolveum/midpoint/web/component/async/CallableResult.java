/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.async;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class CallableResult<T> implements Serializable {

    public static final String F_VALUE = "value";
    public static final String F_RESULT = "result";

    private T value;
    private OperationResult result;

    public CallableResult() {
    }

    public CallableResult(T value, OperationResult result) {
        this.result = result;
        this.value = value;
    }

    public OperationResult getResult() {
        return result;
    }

    public T getValue() {
        return value;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
