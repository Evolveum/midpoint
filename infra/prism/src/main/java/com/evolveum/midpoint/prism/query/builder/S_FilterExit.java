/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.prism.query.builder;

import com.evolveum.midpoint.prism.path.ItemPath;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public interface S_FilterExit extends S_QueryExit {

    S_AtomicFilterExit endBlock();
    S_FilterExit asc(QName... names);
    S_FilterExit asc(ItemPath path);
    S_FilterExit desc(QName... names);
    S_FilterExit desc(ItemPath path);
    S_FilterExit group(QName... names);
    S_FilterExit group(ItemPath path);
    S_FilterExit offset(Integer n);
    S_FilterExit maxSize(Integer n);
}
