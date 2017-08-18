/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.tools.ninja;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author lazyman
 */
public class ImportObjects extends BaseNinjaAction {

    private String filePath;
	private boolean validateSchema;

    public ImportObjects(String filePath, boolean validateSchema) {
        this.filePath = filePath;
        this.validateSchema = validateSchema;
    }

    public boolean execute() {
        System.out.println("Starting objects import.");

        File objects = new File(filePath);
        if (!objects.exists() || !objects.canRead()) {
            System.out.println("XML file with objects '" + objects.getAbsolutePath() + "' doesn't exist or can't be read.");
            return false;
        }

        InputStream input = null;
        ClassPathXmlApplicationContext context = null;
        try {
            System.out.println("Loading spring contexts.");
            context = new ClassPathXmlApplicationContext(CONTEXTS);

            InputStreamReader reader = new InputStreamReader(new FileInputStream(objects), "utf-8");
            input = new ReaderInputStream(reader, reader.getEncoding());

            final RepositoryService repository = context.getBean("repositoryService", RepositoryService.class);
            PrismContext prismContext = context.getBean(PrismContext.class);

            EventHandler handler = new EventHandler() {

                @Override
                public EventResult preMarshall(Element objectElement, Node postValidationTree, OperationResult objectResult) {
                    return EventResult.cont();
                }

                @Override
                public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement, OperationResult objectResult) {
                    try {
                        String displayName = getDisplayName(object);
                        System.out.println("Importing object " + displayName);

                        repository.addObject((PrismObject<ObjectType>) object, null, objectResult);
                    } catch (Exception ex) {
                        objectResult.recordFatalError("Unexpected problem: " + ex.getMessage(), ex);

                        System.out.println("Exception occurred during import, reason: " + ex.getMessage());
                        ex.printStackTrace();
                    }

                    objectResult.recordSuccessIfUnknown();
                    if (objectResult.isAcceptable()) {
                        // Continue import
                        return EventResult.cont();
                    } else {
                        return EventResult.skipObject(objectResult.getMessage());
                    }
                }

                @Override
                public void handleGlobalError(OperationResult currentResult) {
                }
            };
            Validator validator = new Validator(prismContext, handler);
            validator.setVerbose(true);
            validator.setValidateSchema(validateSchema);

            OperationResult result = new OperationResult("Import objects");
            validator.validate(input, result, OperationConstants.IMPORT_OBJECT);

            result.recomputeStatus();
            if (!result.isSuccess()) {
                System.out.println("Operation result was not success, dumping result.\n" + result.debugDump(3));
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred during import task, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            IOUtils.closeQuietly(input);
            destroyContext(context);
        }

        System.out.println("Objects import finished.");
        return true;
    }

    private String getDisplayName(PrismObject object) {
        StringBuilder builder = new StringBuilder();

        //name
        PolyString name = getName(object);
        if (name != null) {
            builder.append(name.getOrig());
        }

        //oid
        if (builder.length() != 0) {
            builder.append(' ');
        }
        builder.append('\'').append(object.getOid()).append('\'');

        return builder.toString();
    }

    private PolyString getName(PrismObject object) {
        PrismProperty property = object.findProperty(ObjectType.F_NAME);
        if (property == null || property.isEmpty()) {
            return null;
        }

        return (PolyString) property.getRealValue(PolyString.class);
    }
}
