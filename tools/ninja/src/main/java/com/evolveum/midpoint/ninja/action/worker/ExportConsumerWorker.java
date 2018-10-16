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

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportConsumerWorker extends AbstractWriterConsumerWorker<ExportOptions> {
	
	private PrismSerializer<String> serializer;

    public ExportConsumerWorker(NinjaContext context, ExportOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue, operation);
    }
    
    @Override
	protected void init() {
    	serializer = context.getPrismContext()
            .xmlSerializer()
            .options(SerializationOptions.createSerializeForExport());
	}

	@Override
	protected String getProlog() {
		return NinjaUtils.XML_OBJECTS_PREFIX;
	}

    @Override
	protected <O extends ObjectType> void write(Writer writer, PrismObject<O> object) throws SchemaException, IOException {
    	String xml = serializer.serialize(object);
        writer.write(xml);
	}
	
    @Override
	protected String getEpilog() {
		return NinjaUtils.XML_OBJECTS_SUFFIX;
	}

}
