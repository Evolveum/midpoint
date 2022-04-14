package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
    @NotNull Classification classify(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException;

    /**
     * Result of the object classification.
     */
    class Classification {

        @Nullable private final ResourceObjectTypeDefinition definition;

        private Classification(@Nullable ResourceObjectTypeDefinition definition) {
            this.definition = definition;
        }

        public static Classification unknown() {
            return new Classification(null);
        }

        public static Classification of(@Nullable ResourceObjectTypeDefinition definition) {
            return new Classification(definition);
        }

        public @Nullable ResourceObjectTypeDefinition getDefinition() {
            return definition;
        }

        public @NotNull ResourceObjectTypeDefinition getDefinitionRequired() {
            return Objects.requireNonNull(definition, "no definition");
        }

        public @NotNull ShadowKindType getKind() {
            return definition != null ? definition.getKind() : ShadowKindType.UNKNOWN;
        }

        public @NotNull String getIntent() {
            return definition != null ? definition.getIntent() : SchemaConstants.INTENT_UNKNOWN;
        }

        public boolean isKnown() {
            return definition != null;
        }

        @Override
        public String toString() {
            return "Classification{" + definition + '}';
        }
    }
}
