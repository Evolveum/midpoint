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

package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.CountOptions;
import com.evolveum.midpoint.ninja.util.FileReference;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CountRepositoryAction extends RepositoryAction<CountOptions> {

    private static final String DOT_CLASS = CountRepositoryAction.class.getName() + ".";

    private static final String OPERATION_COUNT = DOT_CLASS + "count";

    @Override
    public void execute() throws Exception {
        RepositoryService repository = context.getRepository();

        FileReference fileReference = options.getFilter();
        if (fileReference != null && options.getFilter() == null) {
            throw new NinjaException("Type must be defined");
        }

        ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context);

        List<ObjectTypes> types = getTypes();

        int total = 0;
        OperationResult result = new OperationResult(OPERATION_COUNT);
        for (ObjectTypes type : types) {
            Class<? extends ObjectType> clazz = type.getClassDefinition();
            if (Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            int count = repository.countObjects(clazz, query, new ArrayList<>(), result);
            if (count == 0 && options.getType() == null) {
                continue;
            }
            log.info("{}:\t{}", type.name(), count);

            total += count;
        }

        log.info("===\nTotal:\t{}", total);
    }

    private List<ObjectTypes> getTypes() {
        List<ObjectTypes> types = new ArrayList<>();

        ObjectTypes type = options.getType();
        if (type != null) {
            types.add(type);
        } else {
            types.addAll(Arrays.asList(ObjectTypes.values()));
        }

        Collections.sort(types);

        return types;
    }
}
