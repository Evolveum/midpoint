/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.util.mock;

import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import org.mockito.Mockito;

import static org.mockito.Mockito.withSettings;

/**
 *
 * @author lazyman
 */
public class MockFactory {

	public static ProvisioningService createProvisioningPortType() {
		return Mockito.mock(ProvisioningService.class, withSettings().defaultAnswer(new MidpointDefaultAnswer()));
	}

	public static RepositoryService createRepositoryPortType() {
		return Mockito.mock(RepositoryService.class, withSettings().defaultAnswer(new MidpointDefaultAnswer()));
	}

	public static ChangeNotificationDispatcher createChangeNotificationDispatcher() {
		return Mockito.mock(ChangeNotificationDispatcher.class);
	}

}
