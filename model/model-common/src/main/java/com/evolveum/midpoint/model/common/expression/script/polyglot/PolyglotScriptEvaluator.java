package com.evolveum.midpoint.model.common.expression.script.polyglot;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.schema.constants.MidPointConstants;

/**
 * A script evaluator implementation that compiles and evaluates scripts written in JavaScript using the Polyglot API.
 *
 * This class provides a pool-based evaluation of pre-compiled scripts. For each script that is being compiled with
 * the {@link #compileScript(String, ScriptExpressionEvaluationContext)} the pool containing the compiled script is
 * returned instead of the compiled script directly.
 *
 * == Small introduction to basic concepts of Graal Polyglot API
 *
 * The main class needed for any evaluation/parsing of scripts is the {@link Context} class. Its instance can be used
 * for evaluation of various languages, but we are using it here for evaluation of the JavaScript code.
 *
 * Another important class is the {@link Value} class. When you evaluate (or parse) a script using a context, you
 * will get a non-null {@code Value} instance as a result. This object can represent:
 *
 * * Result of the evaluation (e.g., number, string, etc.).
 * * Evaluated method/function, which you can execute (i.e., if the script contains only function definition without
 *   any other expression).
 * * Parsed script, which you can execute.
 * * Other things, which are not so much relevant now (e.g., host objects).
 *
 * Each value object is bound to a particular context. Most (if not all) methods on the value object can be accessed
 * only if its context is not closed. Context can only be used (directly or via calling methods on a value object)
 * only from one thread at a time. Creation of the context is not cheap, so we cannot afford to create a new context
 * for each script invocation. That brings us to the pooling.
 *
 * == Pooling of parsed scripts
 *
 * Pooling allows us to potentially create more Polyglot {@code Value} for the same script, but each bound to its own
 * Polyglot context if the concurrent evaluation of the same script is required. Mentioned {@code Value} object
 * represents the parsed script, which can be executed, what internally uses the context it is bound to.
 *
 * == Differences from caching
 *
 * The script pool object, which this class creates, may contain more parsed instances of **the same script code**.
 * When more threads want to execute the same script, the pool allows us to enforce that each thread will either:
 *
 * * Get an available parsed script from the pool.
 * * Wait until other threads "return" scripts they were using.
 *
 * Cache would not do that. Cache only stores compiled scripts but does not "safeguard" the concurrent access.
 *
 */
public final class PolyglotScriptEvaluator extends AbstractCachingScriptEvaluator<Void, PolyglotScriptPool> {
    private static final String LANGUAGE_NAME = "GraalJs";
    private static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    public PolyglotScriptEvaluator(PrismContext prismContext, Protector protector,
            LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return LANGUAGE_URL;
    }

    @Override
    protected boolean doesSupportRestrictions() {
        return true;
    }

    /**
     * Parse the code string with the Polyglot Context and return a pool containing a parsed script.
     *
     * This method does not directly return a parsed script, but instead it returns a pool potentially containing more
     * of such scripts. The reason is that parsed scripts are bound to the Polyglot Context, which cannot be used by
     * more than 1 thread at a time. Having a pool of such scripts allows us to execute the same script from more
     * threads concurrently.
     *
     * @param codeString the script to compile
     * @param context the context containing important data for the compilation (e.g., ScriptLanguageExpressionProfile).
     * @return the pool containing a parsed script.
     * @throws ExpressionSyntaxException when the script contains syntax errors.
     */
    @Override
    protected PolyglotScriptPool compileScript(String codeString, ScriptExpressionEvaluationContext context)
            throws ExpressionSyntaxException {
        final Source scriptSource = Source.create("js", codeString);
        try {
            return new ContextPerScriptPool(parseScript(scriptSource), scriptSource);
        } catch (PolyglotException e) {
            if (e.isSyntaxError()) {
                throw new ExpressionSyntaxException("Parsed script contain syntax errors.", e);
            }
            throw e;
        }
    }

    /**
     * Evaluates a script present in the specified script pool and evaluation context.
     *
     * This method attempts to retrieve a script from the provided script pool with a timeout. If a timeout occurs,
     * it attempts to create a new script if the pool has capacity. If no new script can be created, it retrieves a
     * script without a timeout and evaluates it.
     *
     * @param scriptPool the pool of pre-compiled scripts to retrieve the script from
     * @param context the context providing additional information for script evaluation
     * @return the result of evaluating the script
     * @throws Exception if an error occurs during script evaluation or script retrieval
     */
    @Override
    protected Object evaluateScript(PolyglotScriptPool scriptPool, ScriptExpressionEvaluationContext context)
            throws Exception {
        try {
            final PolyglotScript script = scriptPool.pool(500);
            return script.evaluate(Collections.emptyMap());
        } catch (TimeoutException e) {
            final Optional<PolyglotScript> scriptOptional =
                    scriptPool.createIfHasCapacity(PolyglotScriptEvaluator::parseScript);
            return scriptOptional
                    .map(script -> script.evaluate(Collections.emptyMap()))
                    .orElseGet(() -> scriptPool.pool().evaluate(Collections.emptyMap()));
        }
    }

    private static @NotNull PolyglotScript parseScript(Source scriptSource) {
        // TODO: Configure context based on the script profile.
        final Context interpreter = Context.newBuilder("js")
                .allowHostAccess(HostAccess.ALL)
                .build();
        final Value parsedScript = interpreter.parse(scriptSource);
        return new GraalPolyglotScript(parsedScript, interpreter);
    }
}
