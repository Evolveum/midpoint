package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.schema.AbstractOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.ShortDumpable;

import java.io.Serializable;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Options for {@link ProvisioningService#testResource(String, Task, OperationResult)} operation.
 */
public class ResourceTestOptions extends AbstractOptions implements Serializable, Cloneable, ShortDumpable {

    /** Full or basic? */
    private final TestMode testMode;

    /** Whether to update cached (in-definition) capabilities and schema. */
    private final ResourceCompletionMode resourceCompletionMode;

    /**
     * If `true`, object in repository is not updated (not even by state change messages).
     *
     * The default is `false` for resources with non-null OID.
     */
    private final Boolean skipRepositoryUpdates;

    /**
     * If `true`, object in memory is not updated (not even by state change messages).
     *
     * The default is `false` for resources with null OID.
     */
    private final Boolean skipInMemoryUpdates;

    public static final ResourceTestOptions DEFAULT = new ResourceTestOptions();

    public ResourceTestOptions() {
        this(null, null, null, null);
    }

    public ResourceTestOptions(
            TestMode testMode,
            ResourceCompletionMode resourceCompletionMode,
            Boolean skipRepositoryUpdates,
            Boolean skipInMemoryUpdates) {
        this.testMode = testMode;
        this.resourceCompletionMode = resourceCompletionMode;
        this.skipRepositoryUpdates = skipRepositoryUpdates;
        this.skipInMemoryUpdates = skipInMemoryUpdates;
    }

    public TestMode getTestMode() {
        return testMode;
    }

    public ResourceTestOptions testMode(TestMode testMode) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, skipRepositoryUpdates, skipInMemoryUpdates);
    }

    public static ResourceTestOptions basic() {
        return new ResourceTestOptions().testMode(TestMode.BASIC);
    }

    public boolean isFullMode() {
        return testMode == null || testMode == TestMode.FULL;
    }

    public ResourceCompletionMode getResourceCompletionMode() {
        return resourceCompletionMode;
    }

    public ResourceTestOptions resourceCompletionMode(ResourceCompletionMode resourceCompletionMode) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, skipRepositoryUpdates, skipInMemoryUpdates);
    }

    public Boolean isSkipRepositoryUpdates() {
        return skipRepositoryUpdates;
    }

    public ResourceTestOptions skipRepositoryUpdates(Boolean skipRepositoryUpdates) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, skipRepositoryUpdates, skipInMemoryUpdates);
    }

    public Boolean isSkipInMemoryUpdates() {
        return skipInMemoryUpdates;
    }

    public ResourceTestOptions skipInMemoryUpdates(Boolean skipInMemoryUpdates) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, skipRepositoryUpdates, skipInMemoryUpdates);
    }

    @Override
    public ResourceTestOptions clone() {
        try {
            return (ResourceTestOptions) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RepoAddOptions(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendVal(sb, "testMode", testMode);
        appendVal(sb, "resourceCompletionMode", resourceCompletionMode);
        appendFlag(sb, "skipRepositoryUpdates", skipRepositoryUpdates);
        appendFlag(sb, "skipInMemoryUpdates", skipInMemoryUpdates);
        removeLastComma(sb);
    }

    /** Checks we leave nothing set to default. */
    public void checkAllValuesSet() {
        stateCheck(testMode != null, "Test mode not set in %s", this);
        stateCheck(resourceCompletionMode != null, "Completion mode not set in %s", this);
        stateCheck(skipRepositoryUpdates != null, "Repository updates not set in %s", this);
        stateCheck(skipInMemoryUpdates != null, "In memory updates not set in %s", this);
    }

    public enum TestMode {
        /**
         * Traditional (full) test. This is the default.
         */
        FULL,

        /**
         * Only the basic connectivity on the main connector is tested. Corresponds to "testPartialConfiguration" method.
         * Resource in repository is never updated in this mode. Resource is never completed. (Attempt to set these
         * options to enable this behavior leads to a failure.) In-memory updates are disabled by default, but can be
         * enabled.
         */
        BASIC
    }

    /** Whether capabilities and schema should be written back to the resource (into repository or in-memory version). */
    public enum ResourceCompletionMode {

        /** Never do the completion. */
        NEVER,

        /**
         * Complete the resource if it's not complete. This updates both capabilities and schema
         * if either one of them is missing. This is the legacy (pre-4.6) behavior, and is currently
         * the default when repository update is carried out. TODO determine for in-memory updates only.
         */
        IF_NOT_COMPLETE,

        /** Always updates capabilities and schema. */
        ALWAYS
    }
}
