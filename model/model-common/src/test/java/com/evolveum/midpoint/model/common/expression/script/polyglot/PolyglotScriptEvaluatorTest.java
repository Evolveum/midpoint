package com.evolveum.midpoint.model.common.expression.script.polyglot;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.graalvm.polyglot.Source;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;

public class PolyglotScriptEvaluatorTest {

    private final static String SCRIPT = "param1 + param2";
    private static final ScriptExpressionEvaluationContext CONTEXT = new ScriptExpressionEvaluationContext();

    @Test
    void scriptContainsSimpleConcatenation_compileScriptCalled_compiledScriptPoolShouldBeReturned()
            throws ExpressionSyntaxException {
        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        final PolyglotScriptPool scriptPool = evaluator.compileScript(SCRIPT, null);

        assertNotNull(scriptPool);
    }

    @Test(expectedExceptions = ExpressionSyntaxException.class)
    void scriptContainsSyntaxError_compileScriptCalled_throwsExpressionSyntaxException()
            throws ExpressionSyntaxException {

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        evaluator.compileScript("(!", null);
    }

    @Test
    void scriptPoolContainsAvailableScript_evaluateScriptIsCalled_scriptShouldBeEvaluated()
            throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final PolyglotScriptPool scriptPool = new FakeScriptPool(scriptSource, false);

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        final Object result = evaluator.evaluateScript(scriptPool, CONTEXT);

        assertTrue(result instanceof String);
        assertEquals(result, "42");
    }

    @Test
    void scriptPoolIsDrained_evaluateScriptIsCalled_afterTimeoutNewScriptIsAddedToPool() throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final FakeScriptPool scriptPool = new FakeScriptPool(scriptSource, false);
        // When a pool with timeout is called, it will throw a timeout exception.
        scriptPool.simulateTimeout();

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        final Object result = evaluator.evaluateScript(scriptPool, CONTEXT);

        assertEquals(result, "42");
    }

    @Test
    void scriptPoolIsDrainedAndPoolIsAtFullCapacity_evaluateScriptIsCalled_evaluatorShouldWaitIndefinitely()
            throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final FakeScriptPool scriptPool = new FakeScriptPool(scriptSource, true);
        // When a pool with timeout is called, it will throw a timeout exception.
        scriptPool.simulateTimeout();

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        final Object result = evaluator.evaluateScript(scriptPool, CONTEXT);

        assertEquals(result, "42");
    }

    @Test
    void scriptPoolContainsAvailableScript_evaluateScript_scriptShouldBeReleasedAfterEvaluation() throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final FakeScriptPool scriptPool = new FakeScriptPool(scriptSource, false);

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        evaluator.evaluateScript(scriptPool, CONTEXT);

        assertEquals(scriptPool.numberOfReleaseCalls(), 1);
    }

    @Test
    void scriptPoolIsDrained_evaluateScriptIsCalled_newScriptShouldBeCreatedThenAcquiredAndReleasedAfterEvaluation()
            throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final FakeScriptPool scriptPool = new FakeScriptPool(scriptSource, false);
        scriptPool.simulateTimeout(); // Simulate that the pool is drained.

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        evaluator.evaluateScript(scriptPool, CONTEXT);

        assertEquals(scriptPool.numberOfReleaseCalls(), 1);
    }

    @Test
    void scriptPoolIsDrainedAndAtFullCapacity_evaluateScriptIsCalled_scriptShouldBeReleasedAfterEvaluation()
            throws Exception {
        final Source scriptSource = Source.create("js", "42");
        final FakeScriptPool scriptPool = new FakeScriptPool(scriptSource, true);
        scriptPool.simulateTimeout(); // Simulate that the pool is drained.
        // Because the pool is at full capacity, a new script will not be created. Instead, the acquire method should be
        // called without a timeout. Our fake pool implementation will return immediately with a new script. We can
        // pretend that it means some script was released to the pool before.

        final PolyglotScriptEvaluator evaluator = new PolyglotScriptEvaluator(null, null, null);
        evaluator.evaluateScript(scriptPool, CONTEXT);

        assertEquals(scriptPool.numberOfReleaseCalls(), 1);
    }

    private static class FakeScriptPool implements PolyglotScriptPool {

        private final Source source;
        private final boolean isFull;
        private boolean simulateTimeout;
        private int releaseCounter;

        private FakeScriptPool(Source source, boolean isFull) {
            this.source = source;
            this.isFull = isFull;
            this.releaseCounter = 0;
        }

        private void simulateTimeout() {
            this.simulateTimeout = true;
        }

        private int numberOfReleaseCalls() {
            return this.releaseCounter;
        }

        @Override
        public PolyglotScript acquire() {
            // Normally, if the pool is drained, it would wait until some script will be available again. But in this
            // fake implementation, return the script immediately regardless of the pool state.
            return new FakePolyglotScript(this.source.getCharacters().toString());
        }

        @Override
        public PolyglotScript acquire(long timeoutMilliseconds) throws TimeoutException {
            if (this.simulateTimeout) {
                // Simulate Interrupt caused by long waiting for an available script.
                throw new TimeoutException("Timeout");
            }
            // Normally, if the pool is drained, it would wait until some script will be available again, or until
            // the timeout will be reached. But in this fake implementation, return the script immediately regardless of
            // the pool state.
            return new FakePolyglotScript(this.source.getCharacters().toString());
        }

        @Override
        public void release(PolyglotScript script) {
            this.releaseCounter++;
        }

        @Override
        public Optional<PolyglotScript> createIfHasCapacity(Function<Source, PolyglotScript> scriptParser) {
            if (this.isFull) {
                return Optional.empty();
            }
            return Optional.of(new FakePolyglotScript(this.source.getCharacters().toString()));
        }

    }

}
