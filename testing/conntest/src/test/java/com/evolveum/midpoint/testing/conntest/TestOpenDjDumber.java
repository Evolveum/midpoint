/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import java.io.File;

import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * OpenDJ, but without permissive modify, shortcut attributes, with manual matching rules, etc.
 *
 * @author semancik
 */
public class TestOpenDjDumber extends TestOpenDj {

	private static final int INITIAL_SYNC_TOKEN = 21;

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "opendj-dumber");
	}

	@Override
	protected boolean hasAssociationShortcut() {
		return false;
	}

	@Override
	protected int getInitialSyncToken() {
		return INITIAL_SYNC_TOKEN;
	}

	@Override
	protected boolean isUsingGroupShortcutAttribute() {
		return false;
	}
}
