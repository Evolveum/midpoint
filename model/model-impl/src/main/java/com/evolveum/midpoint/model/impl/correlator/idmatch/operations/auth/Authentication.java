/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth;


import org.apache.http.client.CredentialsProvider;

public interface Authentication {

   CredentialsProvider provider();

}
