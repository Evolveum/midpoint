/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test.smart;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.smart.api.ServiceClient;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

/**
 * Smart integration service client to be used when there is no real service available.
 */
public class MockServiceClientImpl implements ServiceClient {

    private static final Trace LOGGER = TraceManager.getTrace(MockServiceClientImpl.class);

    private Object lastRequest;
    private final Iterator<Object> responses;
    private final List<ResponseGenerator<?, ?>> responseGenerators;

    /**
     * Create mock of the client service.
     *
     * The responses should be configured with the {@link #onRequestOfType(Class)} and
     * {@link MockClientStubWithRequest#respondWith(Function)} chain of methods (or their overrides).
     */
    public MockServiceClientImpl() {
        this.responses = Collections.emptyIterator();
        responseGenerators = new ArrayList<>();
    }

    /**
     * Create mock of the client service, which respond to requests with provided responses.
     *
     * WARNING: In concurrent environment, the order of requests may be arbitrary. That means, you should not rely
     * simply on the order of the responses in a provided list. In such cases rather use the
     * {@link #onRequestOfType(Class)} and {@link MockClientStubWithRequest#respondWith(Function)} chain of methods,
     * which allows you to pair responses with requests.
     *
     * @deprecated Use the {@link MockServiceClientImpl#MockServiceClientImpl()} constructor and configure it via the
     * {@code onRequest...} and {@code respondWith...} chain of methods.
     * @param responses The expected responses.
     */
    @Deprecated
    public MockServiceClientImpl(Object... responses) {
        this.responses = List.of(responses).iterator();
        responseGenerators = new ArrayList<>();
    }

    /**
     * Configure what response should be returned if the request has the specified type.
     *
     * The returned "mock stub" allows to further narrow the matching request criteria using the
     * {@link MockClientStubWithRequest#andWhenRequestMatches(Predicate)} method.
     *
     * NOTE: When all configured responses for the matched request are sent, further requests matched with the same
     * stub will cause an assertion error.
     *
     * NOTE: If the request is not matched by any mock stub, the response for such request will always be `null`
     *
     * @param type The type of the request for which you want to configure response.
     * @return The "mock stub", where you can specify the responses or further narrow the matching request.
     */
    public <T> MockClientStubWithRequest<T> onRequestOfType(Class<T> type) {
        return new MockClientStubWithRequest<>(request -> type.isAssignableFrom(request.getClass()));
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <REQ, RESP> RESP invoke(Method method, REQ request, Class<RESP> responseClass) throws SchemaException {
        LOGGER.debug("Invoking {} with request:\n{}",
                method, PrismContext.get().jsonSerializer().serializeRealValueContent(request));
        lastRequest = request;

        final Object response;
        if (!responseGenerators.isEmpty()) {
            final ResponseGenerator<REQ, RESP> matchingGenerator = responseGenerators.stream()
                    .map(generator -> (ResponseGenerator<REQ, RESP>) generator)
                    .filter(generator -> generator.match(request))
                    .findFirst()
                    .orElse(null);
            if (matchingGenerator == null) {
                return null;
            }
            response = matchingGenerator.generate(request)
                    .orElseThrow(() -> new AssertionError("No more responses available for in the mock service "
                            + "client"));
        } else {
            synchronized (this) {
                if (!responses.hasNext()) {
                    throw new AssertionError("No more responses available in the mock service client");
                }
                response = responses.next();
            }
        }

        if (response instanceof RuntimeException exception) {
            throw exception;
        }
        return (RESP) response;
    }

    @Override
    public <REQ, RESP> CompletableFuture<RESP> invokeAsync(Method method, REQ request, Class<RESP> responseClass) {
        try {
            return CompletableFuture.completedFuture(invoke(method, request, responseClass));
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    public Object getLastRequest() {
        return lastRequest;
    }

    @Override
    public void close() {
    }

    /**
     * Client mock stub, which allows to specify responses for particular request or further narrow the request matching
     * criteria.
     */
    public final class MockClientStubWithRequest<T> {
        private final Predicate<T> requestMatcher;

        private MockClientStubWithRequest(Predicate<T> requestMatcher) {
            this.requestMatcher = requestMatcher;
        }

        /**
         * Narrow the request matching criteria with specified predicate.
         *
         * @param requestMatcher The predicate which will be used to match the request.
         * @return The **new instance** of this mock stub, with all previously configured matching criteria in chain.
         */
        public MockClientStubWithRequest<T> andWhenRequestMatches(Predicate<T> requestMatcher) {
            return new MockClientStubWithRequest<>(this.requestMatcher.and(requestMatcher));
        }

        public <R> MockServiceClientImpl respondWith(R response) {
            MockServiceClientImpl.this.responseGenerators.add(new ResponseGenerator<>(
                    this.requestMatcher,
                    request -> List.of(response).iterator()));
            return MockServiceClientImpl.this;
        }

        /**
         * Configure multiple responses for the request matched by configured request matching criteria.
         *
         * WARNING: If multiple threads issue a request, which is matched by this mock stub, you can not
         * predict, which thread will get which response.
         *
         * @param responses The responses which will be sent back (in the same order).
         * @return The configured mock client (the same instance as was used to create this mock stub).
         */
        public <R> MockServiceClientImpl respondWith(List<R> responses) {
            MockServiceClientImpl.this.responseGenerators.add(new ResponseGenerator<>(
                    this.requestMatcher,
                    request -> responses.iterator()));
            return MockServiceClientImpl.this;
        }

        public <R> MockServiceClientImpl respondWith(Function<T, R> responseGenerator) {
            MockServiceClientImpl.this.responseGenerators.add(new ResponseGenerator<>(
                    this.requestMatcher,
                    request -> List.of(responseGenerator.apply(request)).iterator()));
            return MockServiceClientImpl.this;
        }
    }

    private static final class ResponseGenerator<T, R> {
        private final Predicate<T> requestMatcher;
        private final Function<T, Iterator<R>> responseGenerator;
        private final IteratorReference<R> iteratorReference;

        ResponseGenerator(Predicate<T> requestMatcher, Function<T, Iterator<R>> responseGenerator) {
            this.requestMatcher = requestMatcher;
            this.responseGenerator = responseGenerator;
            this.iteratorReference = new IteratorReference<>();
        }

        boolean match(T request) {
            return this.requestMatcher.test(request);
        }

        synchronized Optional<R> generate(T request) {
            final Iterator<R> responses = this.iteratorReference.getOrSet(
                    () -> this.responseGenerator.apply(request));
            if (responses.hasNext()) {
                return Optional.ofNullable(responses.next());
            }
            return Optional.empty();
        }
    }

    /**
     * Please note, that this class on its own is not thread safe.
     *
     * It is the responsibility of the caller to make sure there will be no concurrent access to methods of this class.
     * If this class will ever be extracted as a standalone class, it should be made thread safe explicitly.
     */
    private static final class IteratorReference<R> {
        private Iterator<R> iterator;

        synchronized Iterator<R> getOrSet(Supplier<Iterator<R>> iteratorSupplier) {
            if (this.iterator != null) {
                return this.iterator;
            }
            this.iterator = iteratorSupplier.get();
            return this.iterator;
        }
    }

}
