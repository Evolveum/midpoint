/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.DeleteObjectResult;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 */
public class ChangeDescription {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeDescription.class);

    private AddObjectResult<?> addInfo;
    private ModifyObjectResult<?> modifyInfo;
    private PrismObject<?> deletedObject;

    public ChangeDescription(AddObjectResult<?> addInfo, ModifyObjectResult<?> modifyInfo) {
        this.addInfo = addInfo;
        this.modifyInfo = modifyInfo;
    }

    public ChangeDescription(PrismObject<?> deletedObject) {
        this.deletedObject = deletedObject;
    }

    public static <T extends ObjectType> ChangeDescription getFrom(Object additionalInfo, PrismContext prismContext) {
        if (additionalInfo instanceof AddObjectResult<?>) {
            return new ChangeDescription((AddObjectResult<?>) additionalInfo, null);
        } else if (additionalInfo instanceof ModifyObjectResult<?>) {
            return new ChangeDescription(null, ((ModifyObjectResult<?>) additionalInfo));
        } else if (additionalInfo instanceof DeleteObjectResult) {
            String text = ((DeleteObjectResult) additionalInfo).getObjectTextRepresentation();
            String language = ((DeleteObjectResult) additionalInfo).getLanguage();
            if (text != null && language != null) {
                try {
                    return new ChangeDescription(prismContext.parserFor(text).language(language).parse());
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse deleted object text representation for cache "
                            + "invalidation -- continuing as if no additional information was present", e);
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean mayAffect(QueryKey queryKey, MatchingRuleRegistry matchingRuleRegistry) {
        ObjectQuery query = queryKey.getQuery();
        ObjectFilter filter = query != null ? query.getFilter() : null;
        if (filter == null) {
            // We are interested in all objects; so probably in this one as well.
            return true;
        }

        try {
            if (addInfo != null) {
                // We added an object that matches the filter. So the result is no longer valid.
                return filter.match(addInfo.getObject().getValue(), matchingRuleRegistry);
            } else if (modifyInfo != null) {
                // Either the state before matched the query and state after does not, or vice versa.
                // Therefore the result is no longer valid.
                return modifyInfo.getObjectBefore() == null || modifyInfo.getObjectAfter() == null ||
                        filter.match(modifyInfo.getObjectBefore().getValue(), matchingRuleRegistry) !=
                                filter.match(modifyInfo.getObjectAfter().getValue(), matchingRuleRegistry);
            } else if (deletedObject != null) {
                // We deleted an object that matched the filter. So the result is no longer valid.
                return filter.match(deletedObject.getValue(), matchingRuleRegistry);
            } else {
                LOGGER.warn("Invalid change description -- continuing with the assumption that it affects the query: {}", this);
                return true;
            }
        } catch (Throwable t) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't match object being changed to "
                    + "cached query -- continuing as if there might be an overlap: {}", t, this);
            return true;
        }
    }

    @Override
    public String toString() {
        return "[" +
                "addInfo=" + addInfo +
                ", modifyInfo=" + modifyInfo +
                ", deletedObject=" + deletedObject +
                ']';
    }
}
