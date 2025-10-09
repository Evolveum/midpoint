/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  Determines oldest sync token that was successfully processed; in order to know where to continue when Live Sync starts again.
 *  The idea is that each change (that arrives completely or not) is given a sequential number as a unique identifier.
 *  This identifier is used to find successfully processed changes.
 *
 *  (Note that in the meanwhile, the Change object got its own local sequence number. These numbers are yet to be
 *  reconciled.)
 */
class OldestTokenWatcher {

    private static final Trace LOGGER = TraceManager.getTrace(OldestTokenWatcher.class);

    private final AtomicInteger counter = new AtomicInteger(0);
    private final Map<Integer, TokenInfo> tokenInfoMap = new LinkedHashMap<>();

    synchronized int changeArrived(LiveSyncToken token) {
        int seq = counter.getAndIncrement();
        tokenInfoMap.put(seq, new TokenInfo(token));
        LOGGER.trace("changeArrived: seq={}, token={}", seq, token);
        return seq;
    }

    synchronized void changeProcessed(int sequentialNumber) {
        TokenInfo tokenInfo = tokenInfoMap.get(sequentialNumber);
        LOGGER.trace("changeProcessed: seq={}, tokenInfo={}", sequentialNumber, tokenInfo);
        if (tokenInfo != null) {
            tokenInfo.processed = true;
            stripProcessed();
        } else {
            LOGGER.error("Token info #{} was not found", sequentialNumber);
            dumpTokenInfoMap();
            throw new IllegalStateException("Token info with sequential number " + sequentialNumber + " was not found");
        }
    }

    private void dumpTokenInfoMap() {
        tokenInfoMap.forEach((key, value) -> LOGGER.info(" - #{}: {}", key, value));
    }

    // Not very clean implementation but it should work.
    // We simply strip entries that were processed, but we keep the last one!
    private void stripProcessed() {
        while (stripFirstIfPossible()) {
            // cycle until something is stripped
        }
    }

    private boolean stripFirstIfPossible() {
        Iterator<Map.Entry<Integer, TokenInfo>> iterator = tokenInfoMap.entrySet().iterator();
        Map.Entry<Integer, TokenInfo> first = iterator.hasNext() ? iterator.next() : null;
        Map.Entry<Integer, TokenInfo> second = iterator.hasNext() ? iterator.next() : null;
        if (first != null && first.getValue().processed && second != null && second.getValue().processed) {
            tokenInfoMap.remove(first.getKey());
            LOGGER.trace("Stripped {}, remaining {} items", first, tokenInfoMap.size());
            return true;
        } else {
            return false;
        }
    }

    synchronized LiveSyncToken getOldestTokenProcessed() {
        Iterator<Map.Entry<Integer, TokenInfo>> iterator = tokenInfoMap.entrySet().iterator();
        Map.Entry<Integer, TokenInfo> first = iterator.hasNext() ? iterator.next() : null;
        if (first != null && first.getValue().processed) {
            LiveSyncToken token = first.getValue().token;
            if (token == null) {
                // This is quite unfortunate situation. It should not occur in any reasonable conditions.
                LOGGER.warn("Restart point is a null token!");
                dumpTokenInfoMap();
            }
            LOGGER.trace("Oldest token processed: {}", token);
            return token;
        } else {
            return null;
        }
    }

    synchronized boolean isEverythingProcessed() {
        return tokenInfoMap.values().stream()
                .allMatch(tokenInfo -> tokenInfo.processed);
    }

    private static class TokenInfo {
        private final LiveSyncToken token;
        private boolean processed;

        private TokenInfo(LiveSyncToken token) {
            this.token = token;
        }

        @Override
        public String toString() {
            return "Token=" + token + ", processed=" + processed;
        }
    }
}
