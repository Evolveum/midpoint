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
     * Classifies the resource object. The object may or may not be adopted (i.e. connected to its repository shadow).
     *
     * TODO clarify resourceObject vs. repoShadow -- currently we send there "combined" resource object plus shadow
     *
     * @param resourceObject Resource object that we want to classify
     * @param resource Resource on which the resource object was found
     * @param repoShadow The current repository shadow associated with the resource object.
     * It is needed e.g. to determine the tag value.
     */
    @NotNull Classification classify(@NotNull PrismObject<ShadowType> resourceObject, @NotNull PrismObject<ResourceType> resource,
            @NotNull PrismObject<ShadowType> repoShadow, @NotNull Task task, @NotNull OperationResult result)
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
