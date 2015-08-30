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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Summary report from shadow checking task run.
 *
 * @author Pavol Mederly
 */
public class Statistics {

    public static final String NON_NORMALIZED_IDENTIFIER_VALUE = "Non-normalized identifier value";
    public static final String DUPLICATE_SHADOWS = "Duplicate shadows";
    public static final String NO_RESOURCE_OID = "No resource ref or OID";
    public static final String CANNOT_GET_RESOURCE = "Cannot get resource object";
    public static final String NO_KIND_SPECIFIED = "No kind specified";
    public static final String NO_INTENT_SPECIFIED = "No intent specified";
    public static final String NO_RESOURCE_REFINED_SCHEMA = "No resource refined schema";
    public static final String CANNOT_GET_REFINED_SCHEMA = "Cannot get resource refined schema";
    public static final String NO_OBJECT_CLASS_REFINED_SCHEMA = "No object class refined schema";
    public static final String OTHER_FAILURE = "Other failure";
    public static final String CANNOT_APPLY_FIX = "Cannot apply fix";

    private int resources;
    private int shadows;
    private int shadowsWithErrors;
    private int shadowsWithWarnings;
    private int unfinishedShadows;

    private String[] codeList = {
            NON_NORMALIZED_IDENTIFIER_VALUE,
            DUPLICATE_SHADOWS,
            NO_RESOURCE_OID,
            CANNOT_GET_RESOURCE,
            NO_KIND_SPECIFIED,
            NO_INTENT_SPECIFIED,
            NO_RESOURCE_REFINED_SCHEMA,
            CANNOT_GET_REFINED_SCHEMA,
            NO_OBJECT_CLASS_REFINED_SCHEMA,
            OTHER_FAILURE,
            CANNOT_APPLY_FIX
    };

    private List<String> fixable = Arrays.asList(NON_NORMALIZED_IDENTIFIER_VALUE);

    // problem code -> number of occurrences [0] and number of shadows [1]
    Map<String,Counts> problemCount = new HashMap<>();


    public void incrementResources() {
        resources++;
    }

    public void incrementShadows() {
        shadows++;
    }

    public void incrementShadowsWithErrors() {
        shadowsWithErrors++;
    }

    public void incrementShadowsWithWarnings() {
        shadowsWithWarnings++;
    }

    public void incrementUnfinishedShadows() {
        unfinishedShadows++;
    }

    public int getResources() {
        return resources;
    }

    public void setResources(int resources) {
        this.resources = resources;
    }

    public int getShadows() {
        return shadows;
    }

    public void setShadows(int shadows) {
        this.shadows = shadows;
    }

    public int getShadowsWithErrors() {
        return shadowsWithErrors;
    }

    public void setShadowsWithErrors(int shadowsWithErrors) {
        this.shadowsWithErrors = shadowsWithErrors;
    }

    public int getShadowsWithWarnings() {
        return shadowsWithWarnings;
    }

    public void setShadowsWithWarnings(int shadowsWithWarnings) {
        this.shadowsWithWarnings = shadowsWithWarnings;
    }

    public int getUnfinishedShadows() {
        return unfinishedShadows;
    }

    public void setUnfinishedShadows(int unfinishedShadows) {
        this.unfinishedShadows = unfinishedShadows;
    }

    public void registerProblemCodeOccurrences(List<String> problemCodes) {
        Set<String> alreadySeen = new HashSet<>();
        for (String code : problemCodes) {
            Counts value = problemCount.get(code);
            if (value == null) {
                value = new Counts();
                problemCount.put(code, value);
            }
            value.cases++;
            if (alreadySeen.add(code)) {
                value.shadows++;
            }
        }
    }

    public void registerProblemsFixes(List<String> problemCodesFixed) {
        Set<String> alreadySeen = new HashSet<>();
        for (String code : problemCodesFixed) {
            Counts value = problemCount.get(code);
            if (value == null) {    // shouldn't occur
                value = new Counts();
                problemCount.put(code, value);
            }
            value.casesFixed++;
            if (alreadySeen.add(code)) {
                value.shadowsFixed++;
            }
        }
    }

    public String getDetailsFormatted() {
        StringBuilder sb = new StringBuilder();
        for (String code : codeList) {
            Counts counts = problemCount.get(code);
            if (counts == null) {
                counts = new Counts();
            }
            sb.append("     - ").append(code).append(": ");
            sb.append(counts.cases).append(" cases");
            if (counts.cases > 0) {
                sb.append(" (").append(counts.shadows).append(" shadows)");
            }
            if (fixable.contains(code)) {
                sb.append("; fixed ");
                sb.append(counts.casesFixed).append(" cases");
                if (counts.casesFixed > 0) {
                    sb.append(" (").append(counts.shadowsFixed).append(" shadows)");
                }
            }
            sb.append(".\n");
        }
        return sb.toString();
    }

    public static class Counts {
        int cases = 0;
        int shadows = 0;
        int casesFixed = 0;
        int shadowsFixed = 0;
    }
}
