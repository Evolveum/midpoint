/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Util {

    static List<String> createIndexList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> String.format("%04d", i))
                .collect(Collectors.toList());
    }
}
