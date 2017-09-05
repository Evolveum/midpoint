package com.evolveum.midpoint.tools.ninja;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * @author lazyman
 */
public class ExportObjects extends BaseNinjaAction {

    private String filePath;

    public ExportObjects(String filePath) {
        this.filePath = filePath;
    }

    private String createHeaderForXml() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        builder.append("<objects xmlns='").append(SchemaConstantsGenerated.NS_COMMON).append("'\n");
        builder.append("\txmlns:c='").append(SchemaConstantsGenerated.NS_COMMON).append("'\n");
        builder.append("\txmlns:org='").append(SchemaConstants.NS_ORG).append("'>\n");

        return builder.toString();
    }

    public boolean execute() throws UnsupportedEncodingException, FileNotFoundException {
        System.out.println("Starting objects export.");

        File file = new File(filePath);
        if (file.exists() || file.canRead()) {
            System.out.println("XML file already exists, export won't be done.");
            return false;
        }

        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONTEXTS);
        final OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(file), "utf-8");

        try {

            System.out.println("Loading spring contexts.");

            System.out.println("Set repository.");

            RepositoryService repository = context.getBean("repositoryService", RepositoryService.class);

            ResultHandler<ObjectType> handler = new ResultHandler<ObjectType>()
            {

            	PrismContext prismContext = context.getBean(PrismContext.class);

            	@Override
                public boolean handle(PrismObject<ObjectType> object, OperationResult parentResult)
                {
                    String displayName = getDisplayName(object);
                    System.out.println("Exporting object " + displayName);

                    OperationResult resultExport = new OperationResult("Export " + displayName);
                    try
                    {
                    	String stringObject = prismContext.serializeObjectToString(object, PrismContext.LANG_XML);
                        stream.write("\t" + stringObject + "\n");
                    }
                    catch (Exception ex)
                    {
                    	System.out.println("Failed to parse objects to string for xml. Reason: " +  ex);
                        resultExport.recordFatalError("Failed to parse objects to string for xml. Reason: ", ex);
                    }

                    return true;
                }
            };

            stream.write(createHeaderForXml());

            OperationResult result = new OperationResult("search set");

            System.out.println("Creating xml file " + file.getName());

            repository.searchObjectsIterative(ObjectType.class, null, handler, null, false, result);

            stream.write("</objects>");
            System.out.println("Created xml file " + file.getName());

        } catch (Exception ex) {
            System.out.println("Exception occurred during context loading, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            destroyContext(context);
            if (stream != null)
            {
            	IOUtils.closeQuietly(stream);
            }

        }


        System.out.println("Objects export finished.");
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
