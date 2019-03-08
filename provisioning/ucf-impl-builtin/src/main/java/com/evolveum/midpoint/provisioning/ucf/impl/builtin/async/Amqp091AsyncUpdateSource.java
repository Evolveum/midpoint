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

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async;

import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateMessageListener;
import com.evolveum.midpoint.provisioning.ucf.api.AsyncUpdateSource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncUpdateSourceType;

/**
 *
 */
public class Amqp091AsyncUpdateSource implements AsyncUpdateSource {

	public static void create(AsyncUpdateSourceType configuration) {

	}

	@Override
	public void startListening(AsyncUpdateMessageListener listener) {

	}

	@Override
	public void stopListening() {

	}

	@Override
	public void test() {

	}
}
