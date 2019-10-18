/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

/**
 * @author Pavol Mederly
 */
public enum ProvisioningOperation {

    ICF_GET, ICF_SEARCH,

    ICF_CREATE, ICF_UPDATE, ICF_DELETE,

    ICF_SYNC,

    ICF_SCRIPT,

    ICF_GET_LATEST_SYNC_TOKEN, ICF_GET_SCHEMA
}
