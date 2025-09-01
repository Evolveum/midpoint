package com.evolveum.midpoint.model.common.expression.script.polyglot;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.graalvm.polyglot.Source;
import org.testng.annotations.Test;

public class ContextPerScriptPoolTest {

    private static final String SOURCE_CODE = "42";
    private static final FakePolyglotScript SCRIPT = new FakePolyglotScript(SOURCE_CODE);
    private static final Source SCRIPT_SOURCE = Source.create("js", SOURCE_CODE);

    @Test
    void containsAvailableScript_acquireIsCalled_scriptShouldBeReturned() throws InterruptedException,
            TimeoutException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE);
        final PolyglotScript pooledScript = pool.acquire();

        assertEquals(pooledScript, SCRIPT, "Script pool with only one script should return it if available.");
    }

    @Test
    void containsAvailableScript_acquireWithTimeoutIsCalled_scriptShouldBeReturned()
            throws TimeoutException, InterruptedException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE);
        final PolyglotScript pooledScript = pool.acquire(10);

        assertEquals(pooledScript, SCRIPT, "Script pool with only one script should return it if available.");
    }

    @Test(expectedExceptions = TimeoutException.class)
    void doesNotContainAvailableScript_acquireWithTimeoutIsCalled_scriptShouldBeReturned()
            throws TimeoutException, InterruptedException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE);
        pool.acquire(); // Because the pool has only one script, this pool will drain it.
        pool.acquire(10);
    }

    @Test
    void mainThreadDrainThePool_anotherThreadCallAcquireAndMainThreadReturnsScript_scriptShouldBeReturned()
            throws InterruptedException, ExecutionException, TimeoutException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE);
        final PolyglotScript script = pool.acquire();// Because the pool has only one script, this call will drain it.
        final Supplier<PolyglotScript> scriptPooler = () -> {
            try {
                return pool.acquire();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        final CompletableFuture<PolyglotScript> pooler = CompletableFuture.supplyAsync(scriptPooler);
        pool.release(script); // This will return the script to the pool.
        final PolyglotScript scriptFromSecondThread = pooler.get(10, TimeUnit.MILLISECONDS);

        assertEquals(scriptFromSecondThread, script, "Script pool contains only one script, that means script "
                + "retrieved by main thread should be the same as script retrieved by second thread.");
    }

    @Test
    void poolHasOneScript_anotherScriptIsCreatedInPoolAndScriptIsAcquired_bothScriptsShouldBePresent()
            throws InterruptedException, TimeoutException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE);

        // This will create a second script in the pool and immediately returns it as acquired.
        final Optional<PolyglotScript> addedScript =
                pool.createIfHasCapacity(source -> new FakePolyglotScript(source.getCharacters().toString()));
        final PolyglotScript firstScript = pool.acquire();

        assertTrue(addedScript.isPresent());
        assertNotSame(firstScript, addedScript.get());
        assertSame(firstScript, SCRIPT);
    }

    @Test
    void poolHasCapacityOfOneScript_tryToCreateAnotherScript_scriptShouldNotBeCreatedBecauseOfFullCapacity() {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE, 1, 100);

        final Optional<PolyglotScript> addedScript =
                pool.createIfHasCapacity(source -> new FakePolyglotScript(source.getCharacters().toString()));

        assertTrue(addedScript.isEmpty(), "Script pool is full, thus another script should not be created");
    }

    @Test
    void poolHasCapacityOfTwoScripts_tryToCreateAnotherTwoScripts_onlyOneScriptShouldBeCreatedBecauseOfFullCapacity() {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE, 2, 100);

        final Optional<PolyglotScript> firstAddedScript =
                pool.createIfHasCapacity(source -> new FakePolyglotScript(source.getCharacters().toString()));
        final Optional<PolyglotScript> secondAddedScript =
                pool.createIfHasCapacity(source -> new FakePolyglotScript(source.getCharacters().toString()));

        assertTrue(firstAddedScript.isPresent(), "Script should be created because the script pool has free capacity.");
        assertTrue(secondAddedScript.isEmpty(), "Script pool is full, thus another script should not be created");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    void poolHasOneScript_releaseForeignScriptToThePool_illegalArgumentExceptionShouldBeThrown() {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE, 1, 100);

        pool.release(new FakePolyglotScript(""));
    }

    @Test
    void poolCreatesAdditionalScript_releaseCreatedScript_scriptShouldBeReleasedWithoutErrors() {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE, 2, 100);

        pool.createIfHasCapacity(source -> new FakePolyglotScript(""))
                .ifPresent(pool::release);
    }

    @Test
    void poolHasOneScript_acquireScriptAndReleaseItTwoTimes_secondReleaseShouldNotDoAnything()
            throws InterruptedException, TimeoutException {
        final PolyglotScriptPool pool = new ContextPerScriptPool(SCRIPT, SCRIPT_SOURCE, 1, 100);
        final PolyglotScript script = pool.acquire();
        pool.release(script);
        pool.release(script);

        // Test indirectly that the script has not been added to the pool two times.
        assertEquals(pool.acquire(), script);
        assertThrows(TimeoutException.class, () -> pool.acquire(10));
    }
}