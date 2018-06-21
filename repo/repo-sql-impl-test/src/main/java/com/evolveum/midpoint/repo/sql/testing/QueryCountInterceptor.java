/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.testing;

import org.hibernate.EmptyInterceptor;

/**
 * Created by Viliam Repan (lazyman).
 */
public class QueryCountInterceptor extends EmptyInterceptor {

    private ThreadLocal<Integer> queryCount = new ThreadLocal<>();

    public void startCounter() {
        queryCount.set(0);
    }

    public int getQueryCount() {
        Integer i = queryCount.get();
        return i == null ? 0 : i;
    }

    public void clearCounter() {
        queryCount.remove();
    }

    @Override
    public String onPrepareStatement(String sql) {
        Integer count = queryCount.get();
        if (count != null) {
            queryCount.set(count + 1);
        }

        return super.onPrepareStatement(sql);
    }
}
