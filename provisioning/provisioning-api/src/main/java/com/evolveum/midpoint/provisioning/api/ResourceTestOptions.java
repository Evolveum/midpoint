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

    /** Full or partial? */
    private final TestMode testMode;

    /** Whether to update repo-cached (in-definition) capabilities and schema. */
    private final ResourceCompletionMode resourceCompletionMode;

    /**
     * If `false`, object in repository is not updated (not even by state change messages).
     *
     * The default is `true` for resources with non-null OID. For resources with no OID, this flag is ignored,
     * as they cannot be updated in repository anyway.
     */
    private final Boolean updateInRepository;

    /**
     * If `false`, object in memory is not updated (not even by state change messages).
     *
     * The default is `true` for resources with null OID.
     */
    private final Boolean updateInMemory;

    /**
     * If `true`, the connector is not cached (even if completion is requested). This is to avoid caching resource objects
     * that have been provided by the client, i.e. that are not fetched right from the repository. See MID-8020.
     */
    private final boolean doNotCacheConnector;

    public static final ResourceTestOptions DEFAULT = new ResourceTestOptions();

    public ResourceTestOptions() {
        this(null, null, null, null, false);
    }

    private ResourceTestOptions(
            TestMode testMode,
            ResourceCompletionMode resourceCompletionMode,
            Boolean updateInRepository,
            Boolean updateInMemory,
            boolean doNotCacheConnector) {
        this.testMode = testMode;
        this.resourceCompletionMode = resourceCompletionMode;
        this.updateInRepository = updateInRepository;
        this.updateInMemory = updateInMemory;
        this.doNotCacheConnector = doNotCacheConnector;
    }

    public TestMode getTestMode() {
        return testMode;
    }

    public ResourceTestOptions testMode(TestMode testMode) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, updateInRepository, updateInMemory, doNotCacheConnector);
    }

    public static ResourceTestOptions partial() {
        return new ResourceTestOptions().testMode(TestMode.PARTIAL);
    }

    public boolean isFullMode() {
        return testMode == null || testMode == TestMode.FULL;
    }

    public ResourceCompletionMode getResourceCompletionMode() {
        return resourceCompletionMode;
    }

    public ResourceTestOptions resourceCompletionMode(ResourceCompletionMode resourceCompletionMode) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, updateInRepository, updateInMemory, doNotCacheConnector);
    }

    public Boolean isUpdateInRepository() {
        return updateInRepository;
    }

    public ResourceTestOptions updateInRepository(Boolean updateInRepository) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, updateInRepository, updateInMemory, doNotCacheConnector);
    }

    public Boolean isUpdateInMemory() {
        return updateInMemory;
    }

    public ResourceTestOptions updateInMemory(Boolean updateInMemory) {
        return new ResourceTestOptions(testMode, resourceCompletionMode, updateInRepository, updateInMemory, doNotCacheConnector);
    }

    public boolean isDoNotCacheConnector() {
        return doNotCacheConnector;
    }

    public ResourceTestOptions doNotCacheConnector() {
        return new ResourceTestOptions(testMode, resourceCompletionMode, updateInRepository, updateInMemory, true);
    }

    // Note: the object is immutable, so actually there's no need to use this method.
    // TODO or should it simply return "this"?
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
        appendFlag(sb, "updateInRepository", updateInRepository);
        appendFlag(sb, "updateInMemory", updateInMemory);
        appendFlag(sb, "doNotCacheConnector", doNotCacheConnector);
        removeLastComma(sb);
    }

    /** Checks we leave nothing set to default. */
    public void checkAllValuesSet() {
        stateCheck(testMode != null, "Test mode not set in %s", this);
        stateCheck(resourceCompletionMode != null, "Completion mode not set in %s", this);
        stateCheck(updateInRepository != null, "Repository updates not set in %s", this);
        stateCheck(updateInMemory != null, "In memory updates not set in %s", this);
    }

    public enum TestMode {
        /**
         * Traditional (full) test. This is the default.
         */
        FULL,

        /**
         * Only the partial connectivity on the main connector is tested. Corresponds to "testPartialConfiguration" method.
         * Resource in repository is never updated in this mode. Resource is never completed. (Attempt to set these
         * options to enable this behavior leads to a failure.) In-memory updates are disabled by default, but can be
         * enabled.
         */
        PARTIAL
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
