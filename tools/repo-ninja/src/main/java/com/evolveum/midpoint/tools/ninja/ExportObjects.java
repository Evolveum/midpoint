package com.evolveum.midpoint.tools.ninja;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
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

    public boolean execute() {
        System.out.println("Starting objects export.");

        File objects = new File(filePath);
        if (!objects.exists() || !objects.canRead()) {
            System.out.println("XML file already exists, export won't be done.");
            return false;
        }

        ClassPathXmlApplicationContext context = null;
        try {
            System.out.println("Loading spring contexts.");
            context = new ClassPathXmlApplicationContext(CONTEXTS);


            //todo export objects
        } catch (Exception ex) {
            System.out.println("Exception occurred during context loading, reason: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            destroyContext(context);
        }


        System.out.println("Objects export finished.");
        return true;
    }
}
