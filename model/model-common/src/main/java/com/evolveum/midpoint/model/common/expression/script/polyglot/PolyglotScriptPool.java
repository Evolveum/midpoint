package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.graalvm.polyglot.Source;

/**
 * Provides a pool of reusable {@link PolyglotScript} instances that can be used for script execution.
 *
 * Implementations may have limited capacity.
 */
public interface PolyglotScriptPool {
    /**
     * Retrieves a script from the pool, if available.
     *
     * If no script is currently available, it blocks until some gets available again.
     * Implementations may implement their own internal timeout to prevent indefinite waiting.
     *
     * @return a reusable {@link PolyglotScript} instance from the pool
     */
    PolyglotScript pool();

    /**
     * Retrieves a script from the pool, if available.
     *
     * If no script is currently available, it blocks until some gets available again or until the timeout is reached.
     *
     * @return a reusable {@link PolyglotScript} instance from the pool
     * @throws TimeoutException if the specified timeout is reached.
     */
    PolyglotScript pool(long timeoutMilliseconds) throws TimeoutException;

    /**
     * Returns a {@link PolyglotScript} instance to the pool, making it available for reuse.
     *
     * This method allows recycling of previously used scripts, reducing the overhead of creating new instances.
     *
     * @param script the {@link PolyglotScript} instance to be returned to the pool
     */
    void offer(PolyglotScript script);

    /**
     * Attempts to create a new {@link PolyglotScript} instance using the provided script parser.
     *
     * If there is no additional capacity available in the pool for additional scripts, an empty optional is returned.
     *
     * @param scriptParser a function that converts a {@link Source} to a {@link PolyglotScript} instance
     * @return an {@link Optional} containing a newly created {@link PolyglotScript} instance if capacity is available,
     * or an empty {@link Optional} if the capacity limit has been reached
     */
    Optional<PolyglotScript> createIfHasCapacity(Function<Source, PolyglotScript> scriptParser);

}
