package com.evolveum.midpoint.prism.maven;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.DiagramElementFormType;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;

/*
 Converts schema to javascript graph.
 Input is prismSchema
 Output is JavaScript file
 */

public class TestRelDiagram {

    public static final String BASE_URL = "https://www.evolveum.com/downloads/midpoint/4.3/midpoint-4.3-schemadoc/http---midpoint-evolveum-com-xml-ns-public-common-common-3/type/";
    public static final QName OBJECT_TYPE_QNAME = new QName("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "ObjectType");

    File myObj;
    File testFile;
    FileWriter testFileWriter;
    FileWriter myWriter;
    String diagramName;
    SchemaRegistry schemaRegistry;
    ArrayList<Definition> path = new ArrayList<>(); // List of previous schemas
    ArrayList<Integer> idPath = new ArrayList<>();
    HashMap<Definition, Integer> mapOfRefDefLevels = new HashMap<>();
    HashMap<Definition, ArrayList<String>> mapOfSubtypes = new HashMap<>();
    HashMap<Definition, HashMap<String, Integer>> mapOfLevels = new HashMap<>();
    HashMap<String, Definition> stringToDef = new HashMap<>();
    HashMap<Definition, String> mapOfOriginalDefs = new HashMap<>();
    HashMap<String, Definition> mapOfParents = new HashMap<>();
    HashMap<Definition, Definition> mapOfSelectedDefinitionsParents = new HashMap<>();
    HashMap<String, String> selectedDefToRefDef = new HashMap<>(); // todo maybe array when there will be more references
    HashMap<Definition, HashMap<Definition, ArrayList<String>>> mapOfRefs = new HashMap<>(); // todo map on selectedDef but store also original def
    ArrayList<Definition> selectedDefinitions = new ArrayList<>(); //maybe with id so there will be more options where there are multiple types in path.
    int index = 0;

    public TestRelDiagram(String filePath, String diagramName, SchemaRegistry registry) throws IOException {
        this.schemaRegistry = registry;
        this.diagramName = diagramName;
        this.myObj = new File(filePath);
        this.myWriter = new FileWriter(myObj);
        this.testFile = new File("/home/jan/examplePath.js");
        this.testFileWriter = new FileWriter(testFile);
        myWriter.write("var config = {\n"
                + "\tcontainer: \"#hierarchy\",\n"
                + "\t\tlevelSeparation: 45,\n"
                + "\n"
                + "\t\trootOrientation: \"WEST\",\n"
                + "\n"
                + "\t\tnodeAlign: \"BOTTOM\",\n"
                + "\n"
                + "\t\tconnectors: {\n"
                + "\t\t\ttype: \"step\",\n"
                + "\t\t\tstyle: {\n"
                + "\t\t\t\t\"stroke-width\": 2,\n"
                + "\t\t\t\t'stroke': 'black',\n"
                + "\t\t\t\t'arrow-end': 'block-wide-long'\n"
                + "\t\t\t}\n"
                + "\t\t},\n"
                + "\t\tnode: {\n"
                + "\t\t\tHTMLclass: \"big-company\"\n"
                + "\t\t}\n"
                + "}");

    }


    HashMap<String, HashMap<Definition, Integer>> written = new HashMap<>();
    HashMap<String, ArrayList<Definition>> hasBeenNull = new HashMap<>();
    ArrayList<Integer> idList = new ArrayList<>();
    PrismContainerDefinition<?> user;
    Integer id = 0; // Better solution needed
    HashMap<String, String> hashMap = new HashMap<>();
    Integer numberOfChars = 0;

    String charConfig = "chart_config = [config, ";

    public void cycleDefinition(PrismContainerDefinition complexDefinition, PrismSchema schema, Integer parentId) throws IOException, SchemaException {

        List<? extends Definition> definitions = complexDefinition.getDefinitions();

        String stringPath = "";

        boolean added = false;

        if (!path.contains(complexDefinition)) {
            path.add(complexDefinition);
            added = true;
        }

        if (path.size() < 1) {
            stringPath = "schema";
        } else {
            stringPath = String.valueOf(path);
        }

        boolean hasConDefBeen = true;

        ArrayList<Definition> localHasBeenNull = hasBeenNull.get(stringPath);

        HashMap<Definition, Integer> defsAndIds = written.get(stringPath);

        PrismContainerDefinition nextComplexDefinition = null;

        Integer nextId = null;

        ArrayList<String> hasBeen = new ArrayList<>();

        for (Definition definition : definitions) {

            if (definition instanceof PrismContainerDefinition) {
                PrismContainerDefinition definition2 = (PrismContainerDefinition) definition;
                String name = definition2.getItemName().getLocalPart();
                if (!path.contains(definition) && (localHasBeenNull == null || !localHasBeenNull.contains(definition))) {
                    hasBeen.add(name);


                    if (defsAndIds == null || !defsAndIds.containsKey(definition)) {
                        nextId = id;
                        hasConDefBeen = false;

                    } else {
                        hasConDefBeen = true;
                        nextId = defsAndIds.get(definition);
                    }

                    nextComplexDefinition = (PrismContainerDefinition<?>) definition;
                }
            }
        }

        if (!hasConDefBeen) {
            //callWriteDefinition((PrismContainerDefinition<?>) complexDefinition);
        }

        if (added) {
            path.remove(complexDefinition);
        }

        if (nextComplexDefinition == null && (path.size() < 1 || (path.size() == 1 && path.contains(complexDefinition)))) {
            //myWriter.write("Sorry extreme");
            path.clear();
            idPath.clear();
            testSchema2(schema);
        } else if (nextComplexDefinition == null) {
            if (idPath.contains(parentId)) {
                path.remove(complexDefinition); // not sure if error would be thrown
                idPath.remove(parentId);
            }
            if (path.size() < 1) {
                stringPath = "schema";
            } else {
                stringPath = String.valueOf(path);
            }

            if (path.size() != idPath.size()) {
                throw new Error("sizes are not same");
            }

            // Update maybe not necessary
            localHasBeenNull = hasBeenNull.get(stringPath);
            if (localHasBeenNull == null) {
                localHasBeenNull = new ArrayList<>();
            }
            localHasBeenNull.add(complexDefinition);
            hasBeenNull.put(stringPath, localHasBeenNull);
            cycleDefinition((PrismContainerDefinition) path.get(path.size() - 1), schema, idPath.get(idPath.size() - 1));
        } else {
            //myWriter.write("halo2");
            if (!path.contains(complexDefinition)) {
                path.add(complexDefinition);
                idPath.add(parentId);
                //testPathWriter.write("\n" + "path (add): " + path + " idPath: " + idPath);
            }
            int idBefore = id;

            writeDefinition(nextComplexDefinition, complexDefinition, parentId, false);
            String nextDef = nextComplexDefinition.getItemName().getLocalPart() + idBefore;
            stringToDef.put(nextDef, nextComplexDefinition); //todo different definitions with same name
            callWriteDefinition(nextDef, nextComplexDefinition);
            cycleDefinition(nextComplexDefinition, schema, nextId); // Not sure if error would be thrown
        }
    }

    Definition testDefinition = null;

    public void callWriteDefinition(String definitionString, PrismContainerDefinition definition) throws IOException {
        if (definition.getItemName().getLocalPart().equals("configured")) {
            testDefinition = definition;
            testFileWriter.write("\nSOMTU" + path);
        }
//        if (testDefinition != null && path.contains(testDefinition)) {
//            testFileWriter.write("\nSUBDEF" + definition);
//        }

        boolean pathInSchema = true;

        for (Definition definition2 : path) {
            if (definition2.getDiagrams() == null) {
                pathInSchema = false;
                break;
            } else {
                boolean isName = false;
                for (ItemDiagramSpecification spec : definition2.getDiagrams()) {
                    if (spec.getName().equals(diagramName)) {
                        isName = true;
                        break;
                    }
                }
                if (!isName) {
                    pathInSchema = false;
                    break;
                }
            }
        }

        if (definition.getDiagrams() != null && pathInSchema) {
            for (ItemDiagramSpecification spec : definition.getDiagrams()) {
                if (spec.getName().equals(diagramName)) {
                    for (Definition subDefinition : selectedDefinitions) {
                        if ((path.contains(subDefinition) || subDefinition == stringToDef.get(definitionString)) && path.indexOf(subDefinition) == mapOfRefDefLevels.get(subDefinition)) { //todo if there will be more than one selected def in path
                            mapOfSelectedDefinitionsParents.put(definition, subDefinition);
                            writeDefinition2(subDefinition, definitionString, path.size());
                        }
                    }
                    break;
                }
            }
        }
    }

    public void testSchema2(PrismSchema schema) throws IOException, SchemaException {

        boolean end = false;

        PrismContainerDefinition complexType = null;

        if (index >= schema.getDefinitions(PrismContainerDefinition.class).size()) {
            end = true;
        } else {
            complexType = schema.getDefinitions(PrismContainerDefinition.class).get(index);
            index++;
        }

        if (end) {
            writeSelectedDefinitions();
            myWriter.write("\n" + charConfig + "];");
            myWriter.close();
            testFileWriter.close();
        } else {

            HashMap<Definition, Integer> defsAndIds = written.get("schema");

            if (defsAndIds == null || !defsAndIds.containsKey(complexType)) {
                int idBefore = id;
                writeDefinition(complexType,null,null, true);
                stringToDef.put(complexType.getItemName().getLocalPart() + idBefore, complexType);
            }

            // Update array.
            defsAndIds = written.get("schema");

            cycleDefinition(complexType, schema, defsAndIds.get(complexType));
        }
    }

    List<PrismContainerDefinition<?>> listOfUserSubdefinitions = new ArrayList<>();

    public void writeDefinition(PrismContainerDefinition<?> definition,
            PrismContainerDefinition<?> parentDefinition, Integer parentId, boolean isSchema) throws IOException, SchemaException {

        boolean expanded = false;
        boolean isPresent = false;
        if (definition.getDiagrams() != null) {
            user = definition;
            int index = 0;
            boolean nameIsPresent = true;
            while (index < definition.getDiagrams().size() && !definition.getDiagrams().get(index).getName().equals(diagramName)) {
                if (index == definition.getDiagrams().size() - 1) {
                    nameIsPresent = false;
                }
                index++;
            }
            if (nameIsPresent) {
                if (definition.getDiagrams().get(index).getForm() == (DiagramElementFormType.parse("expanded"))) {
                    expanded = true;
                }
                isPresent = true;
            }
        }

        String definitionString = definition.getItemName().getLocalPart() + id;
        PrismContainerDefinition<?> refDefinition = null; // todo read from schema
        HashMap<PrismContainerDefinition<?>, ArrayList<String>> mapRefsOfTargets = new HashMap<>();
        // todo as arrays
        String ref = "";
        ArrayList<PrismContainerDefinition<?>> targetDefinitionArray = new ArrayList<>();
        HashMap<String, PrismContainerDefinition<?>> mapOfTargetDefs = new HashMap<>();
        for (ItemDefinition<?> itemDef : definition.getDefinitions()) {
            if (itemDef instanceof PrismReferenceDefinition) {
                QName targetTypeName = ((PrismReferenceDefinition) itemDef).getTargetTypeName();
                String name = itemDef.getItemName().getLocalPart();

                List<ItemDiagramSpecification> diagrams = itemDef.getDiagrams();

                if (diagrams != null) {
                    for (ItemDiagramSpecification spec : diagrams) {
                        if (spec.getName().equals(diagramName)) {
                            PrismContainerDefinition<?> targetDefinition = schemaRegistry.findContainerDefinitionByType(targetTypeName);
                            targetDefinitionArray.add(targetDefinition);
                            ArrayList<String> refsOfTarget = mapRefsOfTargets.get(targetDefinition);
                            if (refsOfTarget == null) {
                                refsOfTarget = new ArrayList<>();
                            }
                            refsOfTarget.add(name);
                            mapRefsOfTargets.put(targetDefinition, refsOfTarget);
                            break;
                            //testFileWriter.write("\n" + itemDef.getDiagrams().get(0).getName() + "\ttarget" + targetTypeName + "\trefName" + name);
                        }
                    }
                }

//                else if (!refs.contains(OBJECT_TYPE_QNAME)){
//                    refs.add(OBJECT_TYPE_QNAME);
//                }
            }
        }

        int maxValue = 1;

        if (path.contains(definition)) {
           maxValue = 2;
        }

        if (isPresent && path.size() < maxValue) {
            if (selectedDefinitions != null && !selectedDefinitions.contains(definition)) {
                selectedDefinitions.add(definition);
                mapOfRefDefLevels.put(definition, path.size());
                charConfig += definition.getItemName().getLocalPart() + ", ";
            }
        }

        for (PrismContainerDefinition<?> targetDefinition : targetDefinitionArray) {

            ArrayList<String> refs = mapRefsOfTargets.get(targetDefinition);

            String refDefinitionString = null;
            mapOfOriginalDefs.put(targetDefinition, definitionString);

            HashMap<Definition, ArrayList<String>> refsMap = new HashMap<>(); //tam moze byt viac definicii
            refsMap.put(targetDefinition, refs);
            mapOfRefs.put(targetDefinition, refsMap);

            Definition parentDef = null;

            if (isPresent) {
                mapOfParents.put(definitionString, definition);
                parentDef = definition;
            } else {
                for (Definition def : selectedDefinitions) {
                    //PrismContainerDefinition<?> selectedDefinition = (PrismContainerDefinition<?>) stringToDef.get(def);
                    if (def != null && (path.contains(def) || definition == def)) { // todo maybe add: && path.indexOf(subDefinition) == mapOfRefDefLevels.get(subDefinitionString)
                        mapOfParents.put(definitionString, def);
                        parentDef = def;
                    }
                }
            }

            if (selectedDefinitions.contains(targetDefinition) && parentDef != null && selectedDefinitions.indexOf(parentDef) > selectedDefinitions.indexOf(targetDefinition)) {
                //Collections.swap(selectedDefinitions, selectedDefinitions.indexOf(targetDefinition), selectedDefinitions.indexOf(parentDef));
                selectedDefinitions.remove(parentDef);
                selectedDefinitions.add(selectedDefinitions.indexOf(targetDefinition), parentDef);
            }

        }

        if (isPresent) {
            List<? extends Definition> list = definition.getDefinitions();
            for (Definition def : list) {
                if (def instanceof PrismContainerDefinition) {
                    listOfUserSubdefinitions.add((PrismContainerDefinition<?>) def);
                    //myWriter.write("\n" + def);
                }
            }

        }
        String valueOfPath = "";

        if (isSchema) {
            valueOfPath = "schema";
        } else {
            valueOfPath = String.valueOf(path);
        }

        HashMap<Definition, Integer> mapOfIds = written.get(valueOfPath);
        if (mapOfIds == null) {
            mapOfIds = new HashMap<>();
        }
        mapOfIds.put(definition, id);
        written.put(valueOfPath, mapOfIds);
        id++;
    }

    public void writeDefinition2(Definition referencedDef, String definition, Integer pathSize) {
        ArrayList<String> subTypes = mapOfSubtypes.get(referencedDef);
        HashMap<String, Integer> levelsMap = mapOfLevels.get(referencedDef);
        if (subTypes == null) {
            subTypes = new ArrayList<>();
        }
        subTypes.add(definition);
        if (levelsMap == null) {
            levelsMap = new HashMap<>();
        }
        levelsMap.put(definition, pathSize);
        mapOfLevels.put(referencedDef, levelsMap); //no need
        mapOfSubtypes.put(referencedDef, subTypes); //no need

    }

    public void writeSelectedDefinitions() throws IOException, SchemaException {
        // todo maybe more files if there is more selected definitions
        for (Definition def : selectedDefinitions) {
            boolean isInDiagram = false;
            for (ItemDiagramSpecification spec : def.getDiagrams()) {
                if (spec.getName() != null && spec.getName().equals(diagramName)) {
                    if (spec.getSubitemInclusion() == null || !spec.getSubitemInclusion().toString().equals("EXCLUDE")) {
                        isInDiagram = true;
                        //testFileWriter.write("\n" + itemDef.getDiagrams().get(0).getName() + "\ttarget" + targetTypeName + "\trefName" + name);
                    }
                }
            }

            String description = "";
            String defURL = BASE_URL + def.getTypeName().getLocalPart() + ".html";
            ArrayList<String> subDefinitions = mapOfSubtypes.get(def);
            String properties = "";

            if (isInDiagram && subDefinitions != null) {
                HashMap<String, Integer> levelsMap = mapOfLevels.get(def);
                HashMap<String, Integer> mapOfNotClosedUls = new HashMap<>();
                Integer parentLevel = null;
                Integer notClosedUls = 0;

                properties = writeProperties(def,null);

                if (properties.equals("")) {
                    description += "<ul>";
                }

                for (int i = 0; i < subDefinitions.size(); i++) {
                    String subDefinition = subDefinitions.get(i);
                    Integer level = levelsMap.get(subDefinition);
                    Definition subDefinitionDef = stringToDef.get(subDefinition);
                    //myWriter.write("\nHALLOOO" + subDefinition + level);
                    String subDefinitionName = subDefinition.replaceAll("\\d", "");

                    if (parentLevel == null) {
                        notClosedUls++;
                        description += "<li><ul><li><h1><a href=\"" + BASE_URL + subDefinitionDef.getTypeName().getLocalPart() + ".html\">" + subDefinitionName + "</a></h1></li>"; // + "level" + level + "ncu" + notClosedUls + "parentlevelisnull"
                        //myWriter.write("\nnotCUls " + notClosedUls + "Definition:" + subDefinition.getItemName().getLocalPart());
                        mapOfNotClosedUls.put(subDefinition, notClosedUls);
                    } else if (parentLevel == level) {
                        description += "</ul><li><ul><li><h1><a href=\"" + BASE_URL + subDefinitionDef.getTypeName().getLocalPart() + ".html\">" + subDefinitionName + "</a></h1></li>"; // "level" + level + "ncu" + notClosedUls + "parentissame" +
                        //myWriter.write("\nnotCUls " + notClosedUls + "Definition:" + subDefinition.getItemName().getLocalPart());
                        mapOfNotClosedUls.put(subDefinition, notClosedUls);
                    } else if (parentLevel < level) {
                        notClosedUls++;
                        description += "<li><ul><li><h1><a href=\"" + BASE_URL + subDefinitionDef.getTypeName().getLocalPart() + ".html\">" + subDefinitionName + "</a></h1></li>"; //  + "level" + level + "ncu" + notClosedUls + "parent is smaller"
                        //myWriter.write("\nnotCUls " + notClosedUls + "Definition:" + subDefinition.getItemName().getLocalPart());
                        mapOfNotClosedUls.put(subDefinition, notClosedUls);
                    }
                    //todo is cycling
                    else if (parentLevel > level) {
                        String nParentDefinition = null;
                        Integer nParentLevel = parentLevel;
                        Integer neededNotClosedUls = notClosedUls;
                        for (int j = i - 1; j >= 0 && !nParentLevel.equals(level); j--) {
                            nParentDefinition = subDefinitions.get(j);
                            //myWriter.write("\n nparent" + nParentDefinition.getItemName().getLocalPart() + "j" + j);
                            neededNotClosedUls = mapOfNotClosedUls.get(nParentDefinition);
                            nParentLevel = levelsMap.get(nParentDefinition);
                            //myWriter.write("\nparentlevel" + String.valueOf(levelsMap.get(nParentDefinition)) + "myLevel" + level);
                        }

                        //myWriter.write("\nparent" + nParentDefinition + "needed" + neededNotClosedUls + "notClosedUls" + notClosedUls);

                        //description += nParentDefinition;

                        if (neededNotClosedUls == null || neededNotClosedUls > notClosedUls || nParentLevel != level) {
                            myWriter.write("\ncouz" + subDefinition);
                        } else {
                            while (notClosedUls != (neededNotClosedUls)) {
                                description += "</ul></li>";
                                notClosedUls--;
                            }
                        }

                        description += "</ul></li><li><ul><li><h1><a href=\"" + BASE_URL + subDefinitionDef.getTypeName().getLocalPart() + ".html\">" + subDefinitionName + "</a></h1></li>"; // "level" + level + "ncu" + notClosedUls + "parent is higher" +
                        mapOfNotClosedUls.put(subDefinition, notClosedUls);

                    }
                    String propertiesOfSubType = writeProperties(subDefinitionDef,null);
                    if (propertiesOfSubType.length() > 4) {
                        description += propertiesOfSubType.substring(4);
                    }
                    parentLevel = level;
                }
            } else if (isInDiagram){
                properties = writeProperties(def,null);
            }

            String originalDefString = mapOfOriginalDefs.get(def);
            Definition originalDef = stringToDef.get(originalDefString);
            PrismContainerDefinition<?> parent = (PrismContainerDefinition<?>) mapOfParents.get(originalDefString);

            if (originalDef != null) { //refs != null

                ArrayList<ArrayList<String>> refsArrays = new ArrayList<>(mapOfRefs.get(def).values());
                ArrayList<String> refs = refsArrays.get(0);

                myWriter.write("\n" + parent.getItemName().getLocalPart() + def.getTypeName().getLocalPart() + "Refs = {");
                myWriter.write("\n\tparent: " + ((PrismContainerDefinition<?>)mapOfSelectedDefinitionsParents.get(parent)).getItemName().getLocalPart() + ","); // todo make as parent selected def

                //String refsInnerHTML = "<div class = \"ref\"><h1>source:<br>" + originalDef.replaceAll("\\d", "") + "</h1><ul>"; // todo make decision if source would be displayed

                String refsInnerHTML = "<div class = \"ref\">";

                for (String ref : refs) {
                    refsInnerHTML += "<li><a href=\"" + BASE_URL + originalDef.getTypeName().getLocalPart() + ".html#item-" + ref + "\">" + ref + "<a></li>";
                }

                myWriter.write("\n\tinnerHTML: '" + refsInnerHTML + "</ul></div>',\n}");

                charConfig += parent.getItemName().getLocalPart() + def.getTypeName().getLocalPart() + "Refs, ";
            }

            myWriter.write("\n" + ((PrismContainerDefinition) def).getItemName().getLocalPart() + " = {");

            myWriter.write("\n\tconnectors: {\n"
                    + "        style: {\n"
                    + "            'arrow-end': 'none'\n"
                    + "        }\n"
                    + "    },");

            if (originalDef != null) {
                myWriter.write("\n\tparent: " + parent.getItemName().getLocalPart() + def.getTypeName().getLocalPart() + "Refs,");
            }

            myWriter.write("\n\tinnerHTML: '<h1><a href =\"" + defURL + "\">" + ((PrismContainerDefinition<?>) def).getItemName().getLocalPart() + "</a></h1>" + properties + "<div class = \"\">" + description + "</ul></div>'\n}");
        }
    }

    public String writeProperties(Definition definition, PrismContainerDefinition<?> parentDefinition) throws IOException, SchemaException {  //, PrismContainerDefinition parentDefinition, Integer parentId
//        fileWriter.write("\n" + definition.getItemName().getLocalPart() + id + " = {");
//        fileWriter.write("\n\t" + "parent: " + parentDefinition.getItemName().getLocalPart() + parentId + ",");
//        fileWriter.write("\n\t" + "collapsed: true,");
//        fileWriter.write("\n\t" + "text: {");
//        fileWriter.write("\n\t\t" + "title: '" + definition.getItemName().getLocalPart() +"',");
        if (definition != null) {

            for (Definition definition1 : ((PrismContainerDefinition<?>) definition).getDefinitions()) {
                if ("resource".equals(((PrismContainerDefinition<?>) definition).getItemName().getLocalPart())
                    && "OperationalStateType".equals(definition1.getTypeName().getLocalPart())) {
                    System.out.println("Hi");
                }
            }

            boolean expanded = false;
            if (definition.getDiagrams() != null) {
                int index = 0;
                boolean nameIsPresent = true;
                while (index < definition.getDiagrams().size() && !definition.getDiagrams().get(index).getName().equals(diagramName)) {
                    if (index == definition.getDiagrams().size() - 1) {
                        nameIsPresent = false;
                    }
                    index++;
                }
                if (nameIsPresent) {
                    if (definition.getDiagrams().get(index).getForm() == (DiagramElementFormType.parse("expanded"))) {
                        expanded = true;
                    }
                    //myWriter.write(String.valueOf(definition.getDiagrams().get(index).getForm()));
                    //myWriter.write(String.valueOf(definition.getDiagrams().get(index).getName()));
                }
            }

            String description = "";
            List<String> usedDefinitions = new ArrayList<>();

            if (parentDefinition != null) {
                List<? extends Definition> parentDefinitions = parentDefinition.getDefinitions();
                if (parentDefinitions != null) {
                    for (Definition definition2 : parentDefinitions) {
                        if (definition2 instanceof PrismPropertyDefinition) {
                            PrismPropertyDefinition<?> definitionUsed = (PrismPropertyDefinition<?>) definition2;
                            //description += "<li class = \"parentDef\">" + definitionUsed.getItemName().getLocalPart() + "\\n" + "</li>";
                            usedDefinitions.add(definitionUsed.getItemName().getLocalPart());
                        }
                    }
                }

            }

            List<? extends Definition> definitions;

            try {
                definitions = ((ComplexTypeDefinition)definition).getDefinitions();
            } catch (Exception e) {
                definitions = ((PrismContainerDefinition<?>)definition).getDefinitions();
            }

            for (Definition definition2: definitions) {
                if (definition2 instanceof PrismPropertyDefinition && definition2.getDiagrams() != null) {
                    PrismPropertyDefinition<?> definitionUsed = (PrismPropertyDefinition<?>) definition2;
                    if (!usedDefinitions.contains(definitionUsed.getItemName().getLocalPart())) {
                        description += "<li><a href=\"" + BASE_URL + definition.getTypeName().getLocalPart() + ".html#item-" + definitionUsed.getItemName().getLocalPart() + "\">" + definitionUsed.getItemName().getLocalPart() + "\\n" + "</a></li>"; // deleted " + definitionUsed.getItemName().getLocalPart() + "<br/>Type: " +
                    } else {
                        description += "<li class = \"parentDef\"><a href=\"" + BASE_URL + definition.getTypeName().getLocalPart() + ".html#item-" + definitionUsed.getItemName().getLocalPart() + "\">" + definitionUsed.getItemName().getLocalPart() + "</a></li>";
                    }
                }
            }

            String propertyClass = "";

            if (expanded) {
                propertyClass = "propertiesExpanded";
            } else {
                propertyClass = "properties";
            }

            propertyClass = "propertiesExpanded"; //todo

//            if (definition.getTypeClass() != null) {
//                propertiesClassMap.put(definition.getTypeClass(), "<div class = \"" + propertyClass + "\"><ul>" + description + "</ul></div>',"); //todo odstanit v druhom <p class = "title">Properties<br/></p>
//            }

            //myWriter.write("\n\t},\n\tinnerHTML: '<h1>" + definition.getItemName().getLocalPart() + "</h1><div class = \"" + propertyClass + "\"><p class = \"title\">Properties<br/></p><ul>" + description + "</ul></div>'\n}");
//        myWriter.write("\n\t\t" + "desc: '" + description +"'\n\t}\n}");
            //fileWriter.write("\n\t}\n}");
            //if (!description.equals("")) {
//            fileWriter.write("\n" + definition.getItemName().getLocalPart() + id + "properties = {");
//            fileWriter.write("\n\t" + "parent: " +  definition.getItemName().getLocalPart() + id + ",");
//            fileWriter.write("\n\t" + "text: 'Properties:',");
//            fileWriter.write("\n\t" + "innerHTML: '<p class = \"title\">Properties<br/></p><ul>" + description + "</ul>'\n}");
//            charConfig += definition.getItemN
//            ame().getLocalPart() + id + "properties, ";
            //}

//        String description = "";
//        List<? extends Definition> definitions = definition.getDefinitions();
//        for (Definition definition2: definitions) {
//            if (definition2 instanceof PrismPropertyDefinition) {
//                description += definition2.getTypeName().getLocalPart();
//            }
//        }
//        myWriter.write("\n\t" + "text: '" + description +"'\n}");
//        charConfig += definition.getItemName().getLocalPart() + id + ", ";
            //id++;
            if (description.equals("")) {
                return "";
            } else {
                return "<ul><div class = \"" + propertyClass + "\"><ul>" + description + "</ul></div>";
            }
        } else {
            return "";
        }

    }
}

