package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

final class GraalPolyglotScript implements PolyglotScript {
    private final Value parsedScript;
    private final Context scriptContext;

    GraalPolyglotScript(Value parsedScript, Context scriptContext) {
        this.parsedScript = parsedScript;
        this.scriptContext = scriptContext;
    }

    @Override
    public Object evaluate(Map<String, Object> variables) {
        // TODO:
        //  * Bind variables to the context
        //  * Execute the script
        return null;
    }
}
