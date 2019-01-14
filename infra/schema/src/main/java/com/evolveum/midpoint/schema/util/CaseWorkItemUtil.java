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

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.NotNull;

/**
 * @author bpowers
 */
public class CaseWorkItemUtil {

    @NotNull
    public static CaseType getCaseRequired(CaseWorkItemType workItem) {
        CaseType rv = getCase(workItem);
        if (rv != null) {
            return rv;
        } else {
            throw new IllegalStateException("No parent case for " + workItem);
        }
    }

    public static CaseType getCase(CaseWorkItemType workItem) {
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerable<CaseWorkItemType> parent = workItem.asPrismContainerValue().getParent();
        if (!(parent instanceof PrismContainer)) {
            return null;
        }
        PrismValue parentParent = ((PrismContainer<CaseWorkItemType>) parent).getParent();
        if (!(parentParent instanceof PrismContainerValue)) {
            return null;
        }
        @SuppressWarnings({"unchecked", "raw"})
        PrismContainerValue<CaseType> parentParentPcv = (PrismContainerValue<CaseType>) parentParent;
        return parentParentPcv.asContainerable();
    }
}
