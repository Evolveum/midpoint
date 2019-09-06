/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.repo.sql.SqlAuditServiceFactory;

/**
 * This class is used for test purposes only.
 *
 * @author lazyman
 */
public class TestSqlAuditServiceFactory extends SqlAuditServiceFactory {

	// fake method. just for the dependency analyze plugin to properly detect dependency
	private void fake() {
		Class foo = AuditService.class;
	}
}
