/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;

import java.io.Serializable;

/**
 * A simple data structure describing what has to be approved (itemToApprove) and by which means (approvalSchema).
 * Coupled with general item approval process (com.evolveum.midpoint.wf.processes.general).
 *
 * @param <I>
 */
@Deprecated
public interface ApprovalRequest<I extends Serializable> extends Serializable {

    long serialVersionUID = 5111362449970050179L;

    ApprovalSchemaType getApprovalSchemaType();

    I getItemToApprove();
}
