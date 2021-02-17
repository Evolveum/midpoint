package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

/**
 * Classifies resource objects, i.e. determines their kind and intent.
 *
 * This is a step towards putting this code right into provisioning module:
 * Now we decouple it from the change notification mechanism. The next step will
 * be removal of this interface and move the code right into provisioning module.
 */
@Experimental
public interface ResourceObjectClassifier {

    /**
     * Classifies the shadowed resource object.
     *
     * @param combinedObject Resource object that we want to classify. It should be connected to the shadow,
     * however, exact "shadowization" is not required. Currently it should contain all the information from the shadow,
     * plus all the attributes from resource object. If needed, more elaborate processing (up to full shadowization)
     * can be added later.
     *
     * @param resource Resource on which the resource object was found
     */
    @NotNull Classification classify(@NotNull PrismObject<ShadowType> combinedObject,
            @NotNull PrismObject<ResourceType> resource,
            @NotNull Task task, @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    /**
     * Result of the object classification.
     */
    class Classification {

        @NotNull private final ShadowKindType kind;
        private final String intent;
        private final String tag;

        public Classification(@NotNull ShadowKindType kind, String intent, String tag) {
            this.kind = kind;
            this.intent = intent;
            this.tag = tag;
        }

        public @NotNull ShadowKindType getKind() {
            return kind;
        }

        public String getIntent() {
            return intent;
        }

        public String getTag() {
            return tag;
        }

        @Override
        public String toString() {
            return "Classification{" +
                    "kind=" + kind +
                    ", intent='" + intent + '\'' +
                    ", tag='" + tag + '\'' +
                    '}';
        }
    }
}
