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

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author mederly
 */
public class ScannerHandlerDto extends HandlerDto {

	public static final String F_LAST_SCAN_TIMESTAMP = "lastScanTimestamp";

	public ScannerHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public String getLastScanTimestamp() {
		PrismProperty<XMLGregorianCalendar> lastScanTimestampProperty = taskDto.getExtensionProperty(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
		if (lastScanTimestampProperty != null && lastScanTimestampProperty.getRealValue() != null) {
			return WebComponentUtil.formatDate(lastScanTimestampProperty.getRealValue());		// TODO correct date
		} else {
			return null;
		}
	}

}
