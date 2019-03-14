/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 *  An instance of this interface is created by calling static method create(AsyncUpdateSourceType configuration)
 *  on the implementing class.
 */
public interface AsyncUpdateSource {

	/**
	 * Starts listening on this async update source.
	 * Returns a ListeningActivity that is to be used to stop the listening.
	 */
	ListeningActivity startListening(AsyncUpdateMessageListener listener) throws SchemaException;

	/**
	 * Tests this async update source.
	 */
	void test(OperationResult parentResult);

	// TODO consider adding lifecycle methods like connect(), disconnect(), dispose() here
	//  However, they are not really needed now, as the existing sources are stateless.

}
