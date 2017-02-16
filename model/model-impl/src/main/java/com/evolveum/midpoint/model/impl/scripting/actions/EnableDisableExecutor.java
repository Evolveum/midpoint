/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class EnableDisableExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(EnableDisableExecutor.class);

    private static final String NAME_ENABLE = "enable";
    private static final String NAME_DISABLE = "disable";

    @Autowired
    private OperationsHelper operationsHelper;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME_ENABLE, this);
        scriptingExpressionEvaluator.registerActionExecutor(NAME_DISABLE, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        boolean isEnable = NAME_ENABLE.equals(expression.getType());
        boolean raw = getParamRaw(expression, input, context, result);
        boolean dryRun = getParamDryRun(expression, input, context, result);

        for (PrismValue value : input.getData()) {
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectType = prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                try {
                    if (objectType instanceof FocusType) {
                        operationsHelper.applyDelta(createEnableDisableDelta((FocusType) objectType, isEnable), operationsHelper.createExecutionOptions(raw), dryRun, context, result);
                        context.println((isEnable ? "Enabled " : "Disabled ") + prismObject.toString() + rawDrySuffix(raw, dryRun));
                    } else if (objectType instanceof ShadowType) {
                        operationsHelper.applyDelta(createEnableDisableDelta((ShadowType) objectType, isEnable), context, result);
                        context.println((isEnable ? "Enabled " : "Disabled ") + prismObject.toString() + rawDrySuffix(raw, dryRun));
                    } else {
                        throw new ScriptExecutionException("Item could not be enabled/disabled, because it is not a FocusType nor ShadowType: " + value.toString());
                    }
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
                    throw ex;
                }
            } else {
                throw new ScriptExecutionException("Item could not be enabled/disabled, because it is not a PrismObject: " + value.toString());
            }
        }
        return Data.createEmpty();
    }

    private ObjectDelta createEnableDisableDelta(FocusType focus, boolean isEnable) {
        return ObjectDelta.createModificationReplaceProperty(focus.getClass(),
                focus.getOid(), new ItemPath(FocusType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), prismContext,
                isEnable ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
    }

    private ObjectDelta createEnableDisableDelta(ShadowType shadow, boolean isEnable) {
        return ObjectDelta.createModificationReplaceProperty(shadow.getClass(),
                shadow.getOid(), new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), prismContext,
                isEnable ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
    }

}
