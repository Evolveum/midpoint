/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;

public class TestParseUserYaml extends TestParseUser {

	@Override
	protected String getSubdirName() {
		return "yaml/ns";
	}

	@Override
	protected String getLanguage() {
		return PrismContext.LANG_YAML;
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}

	@Override
	protected boolean hasNamespaces() {
		return true;
	}
}
