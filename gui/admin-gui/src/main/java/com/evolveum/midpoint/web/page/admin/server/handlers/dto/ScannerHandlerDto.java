/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
            return WebComponentUtil.formatDate(lastScanTimestampProperty.getRealValue());        // TODO correct date
        } else {
            return null;
        }
    }

}
