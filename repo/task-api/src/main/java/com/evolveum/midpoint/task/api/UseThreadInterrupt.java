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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.task.api;

/**
 * @author Pavol Mederly
 */
public enum UseThreadInterrupt {

    NEVER("never"), WHEN_NECESSARY("whenNecessary"), ALWAYS("always");

    private final String value;

    UseThreadInterrupt(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static UseThreadInterrupt fromValue(String v) {
        for (UseThreadInterrupt c: UseThreadInterrupt.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
