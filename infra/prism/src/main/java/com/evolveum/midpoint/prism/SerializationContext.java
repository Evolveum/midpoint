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

package com.evolveum.midpoint.prism;

/**
 * Everything we want to maintain during the serialization process.
 * (First of all, processing options.)
 *
 * @author Pavol Mederly
 */
public class SerializationContext {

    private SerializationOptions options;

    public SerializationContext(SerializationOptions options) {
        this.options = options;
    }

    public SerializationOptions getOptions() {
        return options;
    }

    public void setOptions(SerializationOptions options) {
        this.options = options;
    }

    public static boolean isSerializeReferenceNames(SerializationContext ctx) {
        return ctx != null && SerializationOptions.isSerializeReferenceNames(ctx.getOptions());
    }
}
