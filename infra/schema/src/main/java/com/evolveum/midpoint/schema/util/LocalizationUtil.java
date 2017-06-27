/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author mederly
 */
public class LocalizationUtil {

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(
			SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH);

	public static String resolve(String key) {
		if (key != null && RESOURCE_BUNDLE.containsKey(key)) {
			return RESOURCE_BUNDLE.getString(key);
		} else {
			return key;
		}
	}

	public static String resolve(String key, Object... params  ) {
		if (key != null && RESOURCE_BUNDLE.containsKey(key)) {
			return MessageFormat.format(RESOURCE_BUNDLE.getString(key), params);
		} else {
			return key;
		}
	}

}
