/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.model.impl.integrity;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of checking a particular shadow.
 *
 * @author Pavol Mederly
 */
public class ShadowCheckResult {

    static final Trace LOGGER = TraceManager.getTrace(ShadowCheckResult.class);

    private PrismObject<ShadowType> shadow;
    private PrismObject<ResourceType> resource;
    private List<Exception> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> problemCodes = new ArrayList<>();

    private boolean fixByRemovingShadow = false;
    private List<ItemDelta> fixDeltas = new ArrayList<>();
    private List<String> fixForProblems = new ArrayList<>();
    private boolean fixApplied = false;

    public ShadowCheckResult(PrismObject<ShadowType> shadow) {
        this.shadow = shadow;
    }

    public ShadowCheckResult recordError(String problemCode, Exception e) {
        if (problemCode != null) {
            problemCodes.add(problemCode);
        }
        LoggingUtils.logException(LOGGER, "{} - for shadow {} on resource {}",
                e, e.getMessage(), ObjectTypeUtil.toShortString(shadow), ObjectTypeUtil.toShortString(resource));
        errors.add(e);
        return this;
    }

    public ShadowCheckResult recordWarning(String problemCode, String message) {
        if (problemCode != null) {
            problemCodes.add(problemCode);
        }
        LOGGER.warn("{} - for shadow {} on resource {}",
                message, ObjectTypeUtil.toShortString(shadow), ObjectTypeUtil.toShortString(resource));
        warnings.add(message);
        return this;
    }

    public PrismObject<ShadowType> getShadow() {
        return shadow;
    }

    public void setShadow(PrismObject<ShadowType> shadow) {
        this.shadow = shadow;
    }

    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    public void setResource(PrismObject<ResourceType> resource) {
        this.resource = resource;
    }

    public List<Exception> getErrors() {
        return errors;
    }

    public void setErrors(List<Exception> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addFixDelta(PropertyDelta delta, String fixIsForProblem) {
        fixDeltas.add(delta);
        fixForProblems.add(fixIsForProblem);
    }

    public List<ItemDelta> getFixDeltas() {
        return fixDeltas;
    }

    public List<String> getProblemCodes() {
        return problemCodes;
    }

    public boolean isFixByRemovingShadow() {
        return fixByRemovingShadow;
    }

    public void setFixByRemovingShadow(String fixIsForProblem) {
        this.fixByRemovingShadow = true;
        fixForProblems.add(fixIsForProblem);
    }

    public boolean isFixApplied() {
        return fixApplied;
    }

    public void setFixApplied(boolean fixApplied) {
        this.fixApplied = fixApplied;
    }

    public List<String> getFixForProblems() {
        return fixForProblems;
    }
}
