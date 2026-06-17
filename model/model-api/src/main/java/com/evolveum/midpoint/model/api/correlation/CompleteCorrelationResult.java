/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.correlation;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_CORRELATOR_STATE_PATH;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_OWNER_OPTIONS_PATH;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_RESULTING_OWNER_PATH;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.CORRELATION_SITUATION_PATH;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.correlator.CandidateOwner;
import com.evolveum.midpoint.model.api.correlator.CandidateOwners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Result of a correlation operation.
 *
 */
public class CompleteCorrelationResult extends AbstractCorrelationResult<ObjectType> {

    private final XMLGregorianCalendar correlationStart;
    private final XMLGregorianCalendar correlationEnd;
    /** May be null when the result is fetched from the shadow. */
    @Nullable private final CandidateOwners candidateOwners;

    /**
     * Options for the operator to select from. Derived from {@link #candidateOwners}.
     *
     * Typically present when {@link CorrelationSituationType#UNCERTAIN} but (as an auxiliary information)
     * may be present also for {@link CorrelationSituationType#EXISTING_OWNER}.
     *
     * May be null when the result is fetched from the shadow.
     */
    @Nullable private final ResourceObjectOwnerOptionsType ownerOptions;

    /** If the situation is {@link CorrelationSituationType#ERROR}, here must be the details. Null otherwise. */
    @Nullable private final CorrelationErrorDetails errorDetails;
    @Nullable private final AbstractCorrelatorStateType correlatorState;

    private CompleteCorrelationResult(Builder builder) {
        super(builder.situation, builder.owner);
        this.candidateOwners = builder.candidateOwners;
        this.ownerOptions = builder.ownerOptions;
        this.errorDetails = builder.errorDetails;
        this.correlationStart = builder.correlationStart;
        this.correlationEnd = builder.correlationEnd;
        this.correlatorState = builder.correlatorState;
    }

    public static WithNoOwner builderForNoOwner(XMLGregorianCalendar correlationStart,
            XMLGregorianCalendar correlationEnd) {
        return new Builder(correlationStart, correlationEnd)
                .situation(NO_OWNER)
                .candidateOwners(new CandidateOwners());
    }

    public static WithExistingOwnerBuilder builderForExistingOwner(XMLGregorianCalendar correlationStart,
            XMLGregorianCalendar correlationEnd) {
        return new Builder(correlationStart, correlationEnd)
                .situation(EXISTING_OWNER);
    }

    public static WithUncertainOwnerBuilder builderForUncertainOwner(XMLGregorianCalendar correlationStart,
            XMLGregorianCalendar correlationEnd) {
        return new Builder(correlationStart, correlationEnd)
                .situation(UNCERTAIN);
    }

    public static WithErrorBuilder builderForError(XMLGregorianCalendar correlationStart,
            XMLGregorianCalendar correlationEnd) {
        return new Builder(correlationStart, correlationEnd)
                .situation(ERROR);
    }

    public @Nullable CandidateOwners getCandidateOwnersMap() {
        return candidateOwners;
    }

    public @Nullable ResourceObjectOwnerOptionsType getOwnerOptions() {
        return ownerOptions;
    }

    /**
     * Check if the result is uncertain.
     *
     * Correlation is uncertain, when there are some owner candidates, but confidence is not high enough for any of
     * them.
     *
     * NOTE: If you want to know if the result is *certain*, use the {@link #isCertain()} method, not the negation of
     * this one, because negation of this one would include also the case where correlation ended with error.
     */
    public boolean isUncertain() {
        return situation == UNCERTAIN;
    }

    public boolean isError() {
        return situation == ERROR;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isExistingOwner() {
        return situation == EXISTING_OWNER;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isNoOwner() {
        return situation == NO_OWNER;
    }

    /**
     * Check if the result is certain.
     *
     * Correlation is certain, when there either isn't any owner candidate, or there is one such candidate, for which
     * the confidence is high enough.
     *
     * NOTE: If you want to know if the result is *uncertain*, use the {@link #isUncertain()} method, not the
     * negation of this one, because negation of this one would include also the case where correlation ended with
     * error.
     */
    public boolean isCertain() {
        return isExistingOwner() || isNoOwner();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "status", situation, indent + 1);
        if (owner != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "owner", String.valueOf(owner), indent + 1);
        }
        // TODO candidate owners map
        if (ownerOptions != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "ownerOptions", ownerOptions, indent + 1);
        }
        if (errorDetails != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "errorDetails", errorDetails, indent + 1);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "situation=" + situation +
                ", owner=" + owner +
                ", ownerOptions=" + ownerOptions +
                ", errorDetails=" + errorDetails +
                '}';
    }

    /**
     * Throws a {@link CommonException} or a {@link RuntimeException}, if the state is "error".
     * Normally returns otherwise.
     */
    public void throwCommonOrRuntimeExceptionIfPresent() throws CommonException {
        if (errorDetails != null) {
            errorDetails.throwCommonOrRuntimeExceptionIfPresent();
        }
    }

    public @Nullable String getErrorMessage() {
        return errorDetails != null ? errorDetails.getMessage() : null;
    }

    /**
     * Returns all candidates of given type.
     *
     * Not supported if the result is taken from the shadow.
     */
    @Experimental
    public <F extends ObjectType> @NotNull List<F> getAllCandidates(@NotNull Class<F> focusType) {
        if (candidateOwners == null) {
            throw new UnsupportedOperationException(
                    "Cannot get all candidates from incomplete correlation result (e.g., retrieved from the shadow)");
        }
        //noinspection unchecked
        return candidateOwners.values().stream()
                .map(CandidateOwner::getValue)
                .filter(candidate -> focusType.isAssignableFrom(candidate.getClass()))
                .map(candidate -> (F) candidate)
                .collect(Collectors.toList());
    }

    /**
     * Creates a collection of item deltas from this correlation result for a given shadow.
     *
     * This method creates item deltas for a given shadow, which can be used to store current correlation results.
     * However, it does not simply add all data from this result to deltas. It handles the data in this result with
     * respect to the potentially existing correlation result already present in the shadow. Especially with respect
     * to already existing start/end timestamps, presence of which can have special meaning as described below:
     *
     * |===
     * | start | end | meaning
     *
     * | missing (null)
     * | missing (null)
     * | No correlation has been started for this shadow yet.
     *
     * | present
     * | present
     * | Some correlation process had been started, and it has come to *certain* result (e.g. existing owner or no owner)
     *
     * | present
     * | missing
     * | Some correlation process has started, but it has not yet come to *certain* conclusion, it may e.g. have
     * opened correlation case waiting to resolve manually (or by new automated correlation process e.g. by sync task).
     *
     * | missing
     * | present
     * | This should be invalid combination and should never happen.
     * |===
     *
     * This method will for example never fill in the correlation end timestamp, if the correlation is not *certain*
     * about its result (e.g. existing owner or no owner).
     *
     * In case when this result contains a correlation error, it is added to the delta *only* if the shadow does not
     * already contain the correlation result (state), or its {@link ShadowCorrelationStateType#getSituation()}
     * returns null (situation does not exist yet).
     *
     * @param prismContext The prism context used for creating deltas
     * @param shadow The shadow for which the deltas are being created
     * @return A collection of item deltas representing the changes to the shadow's correlation state
     * @throws SchemaException If there is a schema-related error during delta creation
     */
    public Collection<ItemDelta<?, ?>> toDeltaItems(PrismContext prismContext, ShadowType shadow)
            throws SchemaException {
        S_ItemEntry builder = prismContext.deltaFor(ShadowType.class)
                .oldObject(shadow)
                .optimizing();
        final ShadowCorrelationStateType previousCorrelationState = shadow.getCorrelation();
        builder = addTimestamps(previousCorrelationState, builder);

        if (isError()) {
            if (previousCorrelationState == null || previousCorrelationState.getSituation() == null) {
                builder = builder.item(CORRELATION_SITUATION_PATH)
                        .replace(ERROR);
            }
        } else {
            if (ownerOptionsChanged(shadow)) {
                builder = builder.item(CORRELATION_OWNER_OPTIONS_PATH)
                        .replace(this.ownerOptions);
            }
            builder = builder
                    .item(CORRELATION_SITUATION_PATH)
                        .replace(this.situation)
                    .item(CORRELATION_RESULTING_OWNER_PATH)
                        .replace(ObjectTypeUtil.createObjectRef(this.owner))
                    .item(CORRELATION_CORRELATOR_STATE_PATH)
                        .replace(this.correlatorState);
        }
        return builder.asItemDeltas();
    }

    /**
     * Handles timestamp updates for the correlation state based on the previous correlation status
     * and the certainty of the current result.
     *
     * This method handles four scenarios:
     *
     * * Correlation in progress + not certain: No timestamp changes.
     * * Correlation in progress + certain: Set end timestamp only (finish the correlation).
     * * No correlation in progress + certain: Set both start and end timestamps.
     * * No correlation in progress + not certain: Set start timestamp, clear end timestamp.
     *
     * @param previousCorrelationState the previous correlation state from the shadow, may be null
     * @param deltaBuilder the delta builder to add timestamp changes to
     * @return the updated delta builder
     */
    private S_ItemEntry addTimestamps(@Nullable ShadowCorrelationStateType previousCorrelationState,
            S_ItemEntry deltaBuilder) {
        if (isCorrelationInProgress(previousCorrelationState)) {
            // We don't want to update the "start" timestamp, because the previous correlation is still "not finished".
            if (!isCertain()) {
                // We don't want to set the "end" timestamp, because the correlation result is not yet certain.
                return deltaBuilder;
            }
            // Because the previous correlation is still "not finished", but this correlation is certain about the
            // result, we can "finish" the previous correlation by current end time.
            return addCorrelationEnd(deltaBuilder, this.correlationEnd);
        } else {
            // Previous correlation is either finished (has certain result) or was never started.
            // Either way, we start a new correlation by setting the start timestamp.
            deltaBuilder = deltaBuilder
                    .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_START_TIMESTAMP)
                    .replace(this.correlationStart);
            if (isCertain()) {
                // If current correlation is certain, we want to mark it as "finished" by setting the end timestamp.
                return addCorrelationEnd(deltaBuilder, this.correlationEnd);
            } else {
                // The current correlation result is not certain, so we leave the correlation "open" by clearing
                // any previous end timestamp.
                return addCorrelationEnd(deltaBuilder, null);
            }
        }

    }

    /**
     * Checks if the correlation is still in progress.
     *
     * In this context, "the correlation" does not mean only the "runtime process" of the automated searching of an
     * owner, but it can also involve manual steps (correlation case), when the result of the automatic "runtime"
     * correlation was uncertain. Thus, in this context "the correlation" may span several automated "runtime"
     * correlations plus resolving of the correlation manually with the correlation case.
     *
     * All that is internally covered simply by two items - correlation start/end timestamps. If the end timestamp is
     * empty, while the start timestamp is filled in, it means that the correlation is not "finished" yet and there
     * still is uncertainty in which owner candidate is the true owner (if any at all).
     * @param correlationState The state of the correlation which you want to check.
     * @return True if the checked correlation is still in progress (as defined above).
     */
    private boolean isCorrelationInProgress(@Nullable ShadowCorrelationStateType correlationState) {
        if (correlationState == null) {
            return false;
        }

        return correlationState.getCorrelationStartTimestamp() != null
                && correlationState.getCorrelationEndTimestamp() == null;
    }

    /**
     * Check if the owner options (basically owner candidates) in this result are different from the options already
     * present in the shadow.
     *
     * This method is a helper, because we can not rely on prism/repository in this case to figure it out.
     * The reason is the container IDs, which may be different even though the content is the same.
     *
     * @param shadow The shadow which may already contain some owner options.
     * @return True, if the owner options (candidates) are different.
     */
    private boolean ownerOptionsChanged(ShadowType shadow) {
        final ShadowCorrelationStateType oldCorrelation = shadow.getCorrelation();
        final ResourceObjectOwnerOptionsType oldOptions = oldCorrelation != null
                ? oldCorrelation.getOwnerOptions()
                : null;

        if (oldOptions == null) {
            return this.ownerOptions != null;
        } else {
            if (this.ownerOptions == null) {
                return true;
            } else {
                // We have to ignore auto-generated PCV IDs
                return !oldOptions.asPrismContainerValue().equals(
                        this.ownerOptions.asPrismContainerValue(), EquivalenceStrategy.REAL_VALUE);
            }
        }
    }

    private static S_ItemEntry addCorrelationEnd(S_ItemEntry deltaBuilder, @Nullable XMLGregorianCalendar  end) {
        return deltaBuilder
                .item(ShadowType.F_CORRELATION, ShadowCorrelationStateType.F_CORRELATION_END_TIMESTAMP)
                .replace(end);
    }

    private static final class Builder implements Buildable, WithNoOwner, WithExistingOwnerBuilder,
            WithUncertainOwnerBuilder, WithErrorBuilder {

        private final XMLGregorianCalendar correlationStart;
        private final XMLGregorianCalendar correlationEnd;
        private CorrelationSituationType situation;
        private @Nullable ObjectType owner;
        private @Nullable CandidateOwners candidateOwners;
        private @Nullable ResourceObjectOwnerOptionsType ownerOptions;
        private @Nullable CorrelationErrorDetails errorDetails;
        private @Nullable AbstractCorrelatorStateType correlatorState;

        private Builder(XMLGregorianCalendar correlationStart, XMLGregorianCalendar correlationEnd) {
            this.correlationStart = correlationStart;
            this.correlationEnd = correlationEnd;
        }

        public Builder owner(@Nullable ObjectType owner) {
            this.owner = owner;
            return this;
        }

        public Builder situation(CorrelationSituationType situation) {
            this.situation = situation;
            return this;
        }

        public Builder candidateOwners(@Nullable CandidateOwners candidateOwners) {
            this.candidateOwners = candidateOwners;
            return this;
        }

        public Builder ownerOptions(@Nullable ResourceObjectOwnerOptionsType ownerOptions) {
            this.ownerOptions = ownerOptions;
            return this;
        }

        public Builder error(Exception e) {
            this.errorDetails = CorrelationErrorDetails.forThrowable(e);
            return this;
        }

        public Builder correlatorState(@Nullable AbstractCorrelatorStateType correlatorState) {
            this.correlatorState = correlatorState;
            return this;
        }

        public CompleteCorrelationResult build() {
            return new CompleteCorrelationResult(this);
        }

    }

    public interface Buildable {
        CompleteCorrelationResult build();
    }

    public interface WithNoOwner extends Buildable {
        WithExistingOwnerBuilder correlatorState(@Nullable AbstractCorrelatorStateType correlatorState);
    }

    public interface WithExistingOwnerBuilder extends Buildable {
        WithExistingOwnerBuilder candidateOwners(@Nullable CandidateOwners candidateOwners);
        WithExistingOwnerBuilder ownerOptions(@Nullable ResourceObjectOwnerOptionsType ownerOptions);
        WithExistingOwnerBuilder correlatorState(@Nullable AbstractCorrelatorStateType correlatorState);
        WithExistingOwnerBuilder owner(@Nullable ObjectType owner);
    }

    public interface WithUncertainOwnerBuilder extends Buildable {
        WithUncertainOwnerBuilder correlatorState(@Nullable AbstractCorrelatorStateType correlatorState);
        WithUncertainOwnerBuilder candidateOwners(@Nullable CandidateOwners candidateOwners);
        WithUncertainOwnerBuilder ownerOptions(@Nullable ResourceObjectOwnerOptionsType ownerOptions);
    }

    public interface WithErrorBuilder extends Buildable {
        WithErrorBuilder error(Exception e);
    }
}
