/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.tools.ninja;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class ImportObjects extends BaseNinjaAction {

    private String filePath;

    public ImportObjects(String filePath) {
        this.filePath = filePath;
    }

    public boolean execute() {
        System.out.println("Starting objects import.");

        File objects = new File(filePath);
        if (!objects.exists() || !objects.canRead()) {
            System.out.println("XML file with objects '" + objects.getAbsolutePath() + "' doesn't exist or can't be read.");
            return false;
        }

        ClassPathXmlApplicationContext context = null;
        try {
            System.out.println("Loading spring contexts.");
            context = new ClassPathXmlApplicationContext(CONTEXTS);

            System.out.println("Parsing import file.");
            PrismContext prismContext = context.getBean(PrismContext.class);
            PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
            List<PrismObject<?>> list = domProcessor.parseObjects(objects);
            list = list != null ? list : new ArrayList<PrismObject<?>>();

            System.out.println("Found '" + list.size() + "' objects, starting import.");
            RepositoryService repository = context.getBean("repositoryService", RepositoryService.class);
            for (PrismObject object : list) {
                String displayName = getDisplayName(object);
                System.out.println("Importing object " + displayName);

                OperationResult result = new OperationResult("Import " + displayName);
                try {
                    repository.addObject(object, null, result);
                } catch (Exception ex) {
                    System.out.println("Exception occurred during import, reason: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    result.recomputeStatus();
                }

                if (!result.isSuccess()) {
                    System.out.println("Operation result was not success, dumping result.\n" + result.debugDump(3));
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred during context loading, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
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
