/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.repo.api.SystemConfigurationChangeDispatcher;
import com.evolveum.midpoint.repo.api.SystemConfigurationChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * @author mederly
 */
public class DummySystemConfigurationChangeDispatcherImpl implements SystemConfigurationChangeDispatcher {

	@Override
	public void dispatch(boolean ignoreVersion, boolean allowNotFound, OperationResult result) {
	}

	@Override
	public void registerListener(SystemConfigurationChangeListener listener) {
	}

	@Override
	public void unregisterListener(SystemConfigurationChangeListener listener) {
	}
}
