package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Map;

class FakePolyglotScript implements PolyglotScript {

    private final String result;

    FakePolyglotScript(String result) {
        this.result = result;
    }

    @Override
    public Object evaluate(Map<String, Object> variables) {
        return this.result;
    }

}
