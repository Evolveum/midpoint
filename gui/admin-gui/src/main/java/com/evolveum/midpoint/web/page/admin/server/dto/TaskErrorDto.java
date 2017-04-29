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
package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * Created by honchar.
 */
public class TaskErrorDto implements Serializable{
    public static final String F_OBJECT_REF_NAME = "objectRefName";
    public static final String F_STATUS = "status";
    public static final String F_MESSAGE = "message";

    private String objectRefName;
    private OperationResultStatusType status;
    private String message;
    private String taskOid;

    public TaskErrorDto(){
    }

    public TaskErrorDto(@NotNull ObjectType object, @NotNull String taskOid){
        objectRefName = object.getName().getOrig();

        if (object.getOperationExecution() != null) {
            for (OperationExecutionType execution : object.getOperationExecution()){
                if (execution.getTaskRef() == null){
                    continue;
                }
                if (taskOid.equals(execution.getTaskRef().getOid())){
                    status = execution.getStatus();
                }
            }
        }
    }

}
