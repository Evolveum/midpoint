/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
