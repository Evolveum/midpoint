/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.cli.ninja.action;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.cli.ninja.command.Delete;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaListType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectDeltaOperationListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_3.ModelPortType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public class DeleteAction extends Action<Delete> {

    public DeleteAction(Delete params, JCommander commander) {
        super(params, commander);
    }

    @Override
    protected void executeAction() throws Exception {
        ModelPortType port = createModelPort();

        ModelExecuteOptionsType options = new ModelExecuteOptionsType();
        options.setForce(getParams().isForce());
        options.setRaw(getParams().isRaw());

        QName type = getParams().getType();
        ObjectDeltaType delta = createDeleteDelta(getParams().getOid(), type);
        ObjectDeltaListType deltas = createDeltaList(delta);

        try {
            ObjectDeltaOperationListType result = port.executeChanges(deltas, options);
            List<ObjectDeltaOperationType> operations = result.getDeltaOperation();
            ObjectDeltaOperationType operation = operations.get(0);

            OperationResultType resultType = operation.getExecutionResult();
            STD_OUT.info("Status: {}", resultType.getStatus());
        } catch (FaultMessage ex) {
            handleError("Couldn't delete object '" + type.getLocalPart() + "' with oid '"
                    + getParams().getOid() + "'", ex);
        }
    }
}
