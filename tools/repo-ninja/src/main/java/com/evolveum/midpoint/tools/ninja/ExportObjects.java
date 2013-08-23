package com.evolveum.midpoint.tools.ninja;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.dom.DomSerializer;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

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
    
    public boolean execute() {
        System.out.println("Starting objects export.");

        File file = new File(filePath);
        if (file.exists() || file.canRead()) {
            System.out.println("XML file already exists, export won't be done.");
            return false;
        }

        ClassPathXmlApplicationContext context = null;
        OutputStreamWriter stream = null;
        
        try {
            System.out.println("Loading spring contexts.");
            context = new ClassPathXmlApplicationContext(CONTEXTS);
            
            System.out.println("Set repository.");
            
            RepositoryService repository = context.getBean("repositoryService", RepositoryService.class);
            PrismContext prismContext = context.getBean(PrismContext.class);
            
            OperationResult result = new OperationResult("search set");
            final List<PrismObject<ObjectType>> objects = new ArrayList<PrismObject<ObjectType>>();

            ResultHandler<ObjectType> handler = new ResultHandler<ObjectType>() {
                @Override
                public boolean handle(PrismObject<ObjectType> object, OperationResult parentResult) {
                    objects.add(object);

                    return true;
                }
            };
            
            repository.searchObjectsIterative(ObjectType.class, null, handler, null, result);
                
            System.out.println("Creating xml file " + file.getName());
            stream = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
            
            String stringObject;
            stream.write(createHeaderForXml());
           
            for (PrismObject<ObjectType> object : objects) 
            {
                String displayName = getDisplayName(object);
                System.out.println("Exporting object " + displayName);

                OperationResult resultExport = new OperationResult("Export " + displayName);
                try 
                {
                	stringObject = prismContext.getPrismDomProcessor().serializeObjectToString(object);
                    stream.write("\t" + stringObject + "\n");
                } 
                catch (Exception ex) 
                {
                	System.out.println("Failed to parse objects to string for xml. Reason: " +  ex);
                    result.recordFatalError("Failed to parse objects to string for xml. Reason: ", ex);
                }
            }
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
