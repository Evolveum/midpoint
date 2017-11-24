package com.evolveum.midpoint.ninja.util;

import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BasicLightweightIdentifierGenerator implements LightweightIdentifierGenerator {

    private int sequence;

    @Override
    public LightweightIdentifier generate() {
        return new LightweightIdentifier(System.currentTimeMillis(), 0, ++sequence);
    }
}
