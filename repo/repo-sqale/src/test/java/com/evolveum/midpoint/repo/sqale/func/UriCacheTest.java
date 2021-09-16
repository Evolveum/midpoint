/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.repo.sqale.UriCache.UNKNOWN_ID;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.UriCache;

/**
 * This tests {@link UriCache} including multi-node simulation using two separate cache instances.
 * It's not strictly true multi-node test but two separate URI caches in a single JVM
 * are just as isolated as two caches in two JVMs.
 */
public class UriCacheTest extends SqaleRepoBaseTest {

    private UriCache uriCache1;
    private UriCache uriCache2;

    @BeforeClass
    public void init() {
        uriCache1 = new UriCache();
        uriCache1.initialize(sqlRepoContext::newJdbcSession);
        uriCache2 = new UriCache();
        uriCache2.initialize(sqlRepoContext::newJdbcSession);
    }

    // basic test for a single instance
    @Test
    public void test100UriCacheOperations() {
        when("URI is stored in the cache");
        String uriValue = "test-uri-" + getTestNameShort();
        Integer uriId = uriCache1.processCacheableUri(uriValue);

        then("cached URI can be obtained by value or ID");
        assertThat(uriCache1.getUri(uriId)).isEqualTo(uriValue);
        assertThat(uriCache1.getId(uriValue)).isEqualTo(uriId);
        assertThat(uriCache1.resolveToUri(uriId)).isEqualTo(uriValue);
        assertThat(uriCache1.resolveUriToId(uriValue)).isEqualTo(uriId);
        assertThat(uriCache1.searchId(uriValue)).isEqualTo(uriId);

        and("nonexistent URI or ID return null/unknown or throw");
        assertThat(uriCache1.getUri(Integer.MIN_VALUE)).isNull(); // nonexistent Id
        assertThatThrownBy(() -> uriCache1.resolveToUri(Integer.MIN_VALUE));
        assertThat(uriCache1.getId(uriValue + "nonexistent")).isNull();
        assertThatThrownBy(() -> uriCache1.resolveUriToId(uriValue + "nonexistent"));
        assertThat(uriCache1.searchId(uriValue + "nonexistent")).isEqualTo(UNKNOWN_ID);
    }

    @Test
    public void test200WriteInOneUriCacheIsVisibleInOtherUriCache() {
        when("URI is stored in cache 1");
        String uriValue = "test-uri-" + getTestNameShort();
        Integer uriId = uriCache1.processCacheableUri(uriValue);

        then("it can be obtained/resolved by cache 2");
        assertThat(uriCache2.getUri(uriId)).isEqualTo(uriValue);
        assertThat(uriCache2.getId(uriValue)).isEqualTo(uriId);
        assertThat(uriCache2.resolveToUri(uriId)).isEqualTo(uriValue);
        assertThat(uriCache2.resolveUriToId(uriValue)).isEqualTo(uriId);
        assertThat(uriCache2.searchId(uriValue)).isEqualTo(uriId);
    }

    @Test
    public void test300ConflictInCache() {
        when("URI is stored in cache 1");
        String uriValue = "test-uri-" + getTestNameShort();
        Integer uriId = uriCache1.processCacheableUri(uriValue);
        then("it when stored to cache, id from cache 1 will be obtained");
        assertThat(uriCache2.processCacheableUri(uriValue)).isEqualTo(uriId);
    }
}
