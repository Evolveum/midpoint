package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Optional;
import java.util.function.Function;

import org.graalvm.polyglot.Source;

final class ContextPerScriptPool implements PolyglotScriptPool {

    private final PolyglotScript script;
    private final Source scriptSource;

    ContextPerScriptPool(PolyglotScript script, Source scriptSource) {
        this.script = script;
        this.scriptSource = scriptSource;
    }

    @Override
    public PolyglotScript pool() {
        return script;
    }

    @Override
    public PolyglotScript pool(long timeoutMilliseconds) {
        return script;
    }

    @Override
    public void offer(PolyglotScript script) {
        // single-script pool, ignore offers in dummy implementation
    }

    @Override
    public Optional<PolyglotScript> createIfHasCapacity(Function<Source, PolyglotScript> scriptParser) {
        return Optional.empty();
    }

}
