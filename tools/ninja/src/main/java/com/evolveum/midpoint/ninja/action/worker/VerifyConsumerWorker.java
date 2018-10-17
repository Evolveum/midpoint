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
import com.evolveum.midpoint.ninja.opts.VerifyOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.schema.validator.ObjectValidator;
import com.evolveum.midpoint.schema.validator.ValidationItem;
import com.evolveum.midpoint.schema.validator.ValidationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Radovan Semancik
 */
public class VerifyConsumerWorker extends AbstractWriterConsumerWorker<VerifyOptions> {
	
	private ObjectValidator validator;

    public VerifyConsumerWorker(NinjaContext context, VerifyOptions options, BlockingQueue<PrismObject> queue,
                                OperationStatus operation) {
        super(context, options, queue, operation);
    }

	@Override
	protected void init() {
		validator = new ObjectValidator(context.getPrismContext());
		String warnOption = options.getWarn();
		if (warnOption == null) {
			validator.setAllWarnings();
		} else {
			String[] warnCategories = warnOption.split(",");
			for (String warnCategory: warnCategories) {
				switch (warnCategory) {
					case "deprecated":
						validator.setWarnDeprecated(true);
						break;
					case "plannedRemoval":
						validator.setWarnPlannedRemoval(true);
						break;
					default:
						System.err.println("Unknown warn option '"+warnCategory+"'");
						break;
				}
			}
		}
	}

	@Override
	protected String getProlog() {
		return null;
	}

	@Override
	protected <O extends ObjectType> void write(Writer writer, PrismObject<O> object)
			throws SchemaException, IOException {
		ValidationResult validationResult = validator.validate(object);
		for (ValidationItem validationItem : validationResult.getItems()) {
			writeValidationItem(writer, object, validationItem);
		}
	}

	private <O extends ObjectType> void writeValidationItem(Writer writer, PrismObject<O> object, ValidationItem validationItem) throws IOException {
		if (validationItem.getStatus() != null) {
			writer.append(validationItem.getStatus().toString());
			writer.append(" ");
		} else {
			writer.append("INFO ");
		}
		writer.append(object.toString());
		writer.append(" ");
		if (validationItem.getItemPath() != null) {
			writer.append(validationItem.getItemPath().toString());
			writer.append(" ");
		}
		writeMessage(writer, validationItem.getMessage());
		writer.append("\n");
	}

	private void writeMessage(Writer writer, LocalizableMessage message) throws IOException {
		if (message == null) {
			return;
		}
		// TODO: localization?
		writer.append(message.getFallbackMessage());
	}

	@Override
	protected String getEpilog() {
		return null;
	}
}
