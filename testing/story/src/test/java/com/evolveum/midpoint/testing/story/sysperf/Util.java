/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
