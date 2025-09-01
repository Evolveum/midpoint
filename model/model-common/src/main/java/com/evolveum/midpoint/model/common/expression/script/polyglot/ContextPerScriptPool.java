package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.graalvm.polyglot.Source;

/**
 * Script pool implementation with a timeout on the {@link #acquire()} operation.
 */
final class ContextPerScriptPool implements PolyglotScriptPool {

    private static final int DEFAULT_CAPACITY = 50;
    private static final int DEFAULT_TIMEOUT = 1800_000;

    private final Source scriptSource;
    private final BlockingQueue<PolyglotScript> scriptsQueue;
    private final HashSet<PolyglotScript> knownScripts;
    private final int capacity;
    private final int timeout;

    /**
     * Constructor with a default capacity of 15 and a default timeout of 1800 seconds.
     */
    ContextPerScriptPool(PolyglotScript script, Source scriptSource) {
        this(script, scriptSource, DEFAULT_CAPACITY, DEFAULT_TIMEOUT);
    }

    ContextPerScriptPool(PolyglotScript script, Source scriptSource, int capacity, int timeout) {
        this.scriptSource = scriptSource;
        this.scriptsQueue = new ArrayBlockingQueue<>(capacity, false);
        this.scriptsQueue.add(script);
        this.knownScripts = new HashSet<>(capacity);
        this.knownScripts.add(script);
        this.capacity = capacity;
        this.timeout = timeout;
    }

    @Override
    public PolyglotScript acquire() throws InterruptedException, TimeoutException {
        return acquire(this.timeout);
    }

    @Override
    public PolyglotScript acquire(long timeoutMilliseconds) throws TimeoutException, InterruptedException {
        final PolyglotScript script = this.scriptsQueue.poll(timeoutMilliseconds, TimeUnit.MILLISECONDS);
        if (script == null) {
            throw new TimeoutException("Timeout exceeded while waiting for available script from the scripts pool.");
        }
        return script;
    }

    @Override
    public void release(PolyglotScript script) {
        if (!this.knownScripts.contains(script)) {
            throw new IllegalArgumentException("Attempt to release unknown script. Only scripts created via this pool"
                    + " can be released to it.");
        }
        if (this.scriptsQueue.contains(script)) {
            // It could be implemented in a more efficient way (e.g., by counting), but this is simpler because we
            // don't need to synchronize anything ourselves.
            return;
        }
        if (!this.scriptsQueue.offer(script)) {
            // If this happens, then we most likely have a bug.
            throw new IllegalStateException("Attempt to release a script to a pool at full capacity.");
        }
    }

    @Override
    public synchronized Optional<PolyglotScript> createIfHasCapacity(Function<Source, PolyglotScript> scriptParser) {
        if (this.knownScripts.size() < this.capacity) {
            final PolyglotScript script = scriptParser.apply(this.scriptSource);
            this.knownScripts.add(script);
            return Optional.of(script);
        }
        return Optional.empty();
    }

}
