/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

/**
 * This enum defines source from where a change in property value occurred.
 *
 * @author lazyman
 *
 * FIXME: Abstraction leak
 * TODO: Metadata candidate
 */
public enum OriginType {

    SYNC_ACTION, RECONCILIATION, INBOUND, OUTBOUND, ASSIGNMENTS, ACTIVATIONS, CREDENTIALS, USER_ACTION, USER_POLICY;
}
