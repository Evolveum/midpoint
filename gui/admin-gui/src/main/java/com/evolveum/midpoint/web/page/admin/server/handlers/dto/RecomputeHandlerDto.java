/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;

/**
 * @author mederly
 */
public class RecomputeHandlerDto extends QueryBasedHandlerDto {

    public RecomputeHandlerDto(TaskDto taskDto) {
        super(taskDto);
    }

}
