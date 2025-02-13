/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.context;

import static com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition.getNoiseRolesObjects;
import static com.evolveum.midpoint.schema.util.FocusTypeUtil.createArchetypeAssignment;
import static com.evolveum.midpoint.schema.util.FocusTypeUtil.createTargetAssignment;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import com.evolveum.midpoint.ninja.action.mining.generator.GeneratorOptions;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.ninja.action.mining.generator.object.InitialObjectsDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Utility methods for generating rbac data.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public class RbacGeneratorUtils {

    /**
     * Retrieves a randomly selected location org.
     *
     * @return The randomly selected location org.
     */
    protected static @NotNull InitialObjectsDefinition.LocationOrg getRandomLocationOrg() {
        InitialObjectsDefinition.LocationOrg[] roles = InitialObjectsDefinition.LocationOrg.values();
        Random random = new Random();
        return roles[random.nextInt(roles.length)];
    }

    /**
     * Retrieves a randomly selected plankton abstract role.
     *
     * @return The randomly selected plankton abstract role.
     */
    protected static @NotNull InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole getRandomPlanktonRole() {
        InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole[] roles = InitialObjectsDefinition
                .PlanktonApplicationBusinessAbstractRole.values();
        Random random = new Random();
        return roles[random.nextInt(roles.length)];
    }

    protected static @NotNull InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole getRandomNoiseRole() {
        InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole[] roles = InitialObjectsDefinition
                .NoiseApplicationBusinessAbstractRole.values();
        Random random = new Random();
        return roles[random.nextInt(roles.length)];
    }

    /**
     * Retrieves a list of randomly selected plankton abstract roles.
     *
     * @param minRoles The minimum number of roles to select.
     * @param generatorOptions The generator options to consider.
     * @return A list of randomly selected plankton abstract roles.
     */
    protected static @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getRandomPlanktonRoles(
            int minRoles, GeneratorOptions generatorOptions) {

        if (generatorOptions.isPlanktonDisable()) {
            return new ArrayList<>();
        }

        int maxRoles = InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole.values().length;

        Random random = new Random();
        int numRoles = minRoles + random.nextInt(maxRoles - minRoles + 1);

        Set<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> selectedRoles = EnumSet.noneOf(
                InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole.class);

        while (selectedRoles.size() < numRoles) {
            InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomRole = getRandomPlanktonRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }

    protected static @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getRandomPlanktonRoles(
            int minRoles, int maxRoles, GeneratorOptions generatorOptions) {

        if (generatorOptions.isPlanktonDisable()) {
            return new ArrayList<>();
        }

        int numRoles = minRoles + new Random().nextInt(maxRoles - minRoles + 1);

        Set<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> selectedRoles = EnumSet.noneOf(
                InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole.class);

        while (selectedRoles.size() < numRoles) {
            InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomRole = getRandomPlanktonRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }

    protected static @NotNull List<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> getRandomPlanktonRoles(
            int minRoles) {

        int maxRoles = InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole.values().length;

        Random random = new Random();
        int numRoles = minRoles + random.nextInt(maxRoles - minRoles + 1);

        Set<InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole> selectedRoles = EnumSet.noneOf(
                InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole.class);

        while (selectedRoles.size() < numRoles) {
            InitialObjectsDefinition.PlanktonApplicationBusinessAbstractRole randomRole = getRandomPlanktonRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }

    protected static @NotNull List<InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole> getRandomNoiseRoles(
            int minRoles) {

        int maxRoles = InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole.values().length;

        Random random = new Random();
        int numRoles = minRoles + random.nextInt(maxRoles - minRoles + 1);

        Set<InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole> selectedRoles = EnumSet.noneOf(
                InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole.class);

        while (selectedRoles.size() < numRoles) {
            InitialObjectsDefinition.NoiseApplicationBusinessAbstractRole randomRole = getRandomNoiseRole();
            selectedRoles.add(randomRole);
        }

        return new ArrayList<>(selectedRoles);
    }

    /**
     * Retrieves a randomly selected job title from the provided list, including an empty string.
     *
     * @return The randomly selected job title.
     */
    public static @NotNull String getRandomlyJobTitlesWithNone() {
        List<String> jobTitles = List.of("engineer", "analyst", "reviewer", "editor", "");
        Random random = new Random();
        return jobTitles.get(random.nextInt(jobTitles.size()));
    }

    /**
     * Retrieves a randomly selected job title from the provided list, excluding an empty string.
     *
     * @return The randomly selected job title.
     */
    public static @NotNull String getRandomlyJobTitles() {
        List<String> jobTitles = List.of("engineer", "analyst", "reviewer", "editor");
        Random random = new Random();
        return jobTitles.get(random.nextInt(jobTitles.size()));
    }

    /**
     * Creates an assignment of type OrgType with the provided OID.
     *
     * @param oid The OID of the organization.
     * @return The created assignment.
     */
    public static @NotNull AssignmentType createOrgAssignment(@NotNull String oid) {
        ObjectReferenceType ref = new ObjectReferenceType()
                .oid(oid)
                .type(OrgType.COMPLEX_TYPE);

        return createTargetAssignment(ref);
    }

    /**
     * Creates an assignment of type RoleType with the provided OID.
     *
     * @param oid The OID of the role object.
     * @return The created assignment.
     */
    protected static @NotNull AssignmentType createRoleAssignment(@NotNull String oid) {
        ObjectReferenceType ref = new ObjectReferenceType()
                .oid(oid)
                .type(RoleType.COMPLEX_TYPE);

        return createTargetAssignment(ref);
    }

    /**
     * Creates an assignment of type ArchetypeType with the provided OID.
     *
     * @param user The user for which to create the assignment.
     * @param archetypeOid The OID of the archetype object.
     */
    public static void setUpArchetypeUser(@NotNull UserType user, @NotNull String archetypeOid) {
        AssignmentType targetAssignment = createArchetypeAssignment(archetypeOid);
        user.getAssignment().add(targetAssignment);
        user.getArchetypeRef().add(new ObjectReferenceType().oid(archetypeOid).type(ArchetypeType.COMPLEX_TYPE));
    }

    /**
     * Retrieves a list of PrismObjects representing business RoleType assignments for the specified AssignmentHolderType.
     *
     * @param object The AssignmentHolderType for which to retrieve business role assignments.
     * @param repository The RepositoryService used to retrieve objects from the repository.
     * @param result The OperationResult to track the operation's result.
     * @return A list of PrismObjects representing the business RoleType assignments.
     * @throws SchemaException If an error related to schema occurs.
     * @throws ObjectNotFoundException If an object cannot be found in the repository.
     */
    protected static @NotNull List<PrismObject<RoleType>> getBusinessRolesOidAssignment(
            @NotNull AssignmentHolderType object,
            @NotNull RepositoryService repository,
            @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<AssignmentType> assignments = object.getAssignment();

        List<PrismObject<RoleType>> list = new ArrayList<>();
        for (AssignmentType assignment : assignments) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                if (targetRef.getType().equals(RoleType.COMPLEX_TYPE)) {
                    String assignmentRefOid = targetRef.getOid();
                    PrismObject<RoleType> roleTypeObject = repository.getObject(
                            RoleType.class, assignmentRefOid, null, result);
                    RoleType role = roleTypeObject.asObjectable();
                    List<ObjectReferenceType> archetypeRef = role.getArchetypeRef();
                    if (archetypeRef != null && !archetypeRef.isEmpty()) {
                        ObjectReferenceType objectReferenceType = archetypeRef.get(0);
                        String refOid = objectReferenceType.getOid();
                        if (refOid.equals("00000000-0000-0000-0000-000000000321")) {
                            list.add(roleTypeObject);
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * Loads names from a CSV file located at the specified path.
     * <p>
     * NOTE: names are expected to be in the column with the specified header at "name" value.
     *
     * @param csvPath The path to the CSV file.
     * @return A set containing the loaded names.
     * @throws IOException If an I/O error occurs while reading the CSV file.
     */
    protected static @NotNull Set<String> loadNamesFromCSV(@NotNull String csvPath) throws IOException {
        Set<String> names = new HashSet<>();

        try (Reader reader = new FileReader(csvPath);
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            int nameColumnIndex = csvParser.getHeaderMap().get("name");
            for (CSVRecord record : csvParser) {
                String name = record.get(nameColumnIndex).trim();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        }

        return names;
    }

    /**
     * Retrieves aux role with a chance specified chance.
     *
     * @param auxRole The business role to consider.
     * @param chance The chance of the role being returned.
     * @return The randomly selected location business role.
     */
    public static InitialObjectsDefinition.@Nullable AuxInitialBusinessRole getRandomAuxWithChance(
            @NotNull InitialObjectsDefinition.AuxInitialBusinessRole auxRole, int chance) {
        Random random = new Random();
        if (random.nextInt(100) < chance) {
            return auxRole;
        } else {
            return null;
        }
    }

    /**
     * Resolves auxiliary roles for a user.
     * <p>
     * This method resolves auxiliary roles for a user based on specified criteria.
     *
     * @param user The user to resolve auxiliary roles for.
     */
    static void resolveAuxRoles(@NotNull UserType user) {
        InitialObjectsDefinition.AuxInitialBusinessRole randomAuxWithChanceNomad = getRandomAuxWithChance(
                InitialObjectsDefinition.AuxInitialBusinessRole.NOMAD, 1);
        InitialObjectsDefinition.AuxInitialBusinessRole randomAuxWithChanceVpn = getRandomAuxWithChance(
                InitialObjectsDefinition.AuxInitialBusinessRole.VPN_REMOTE_ACCESS, 30);

        if (randomAuxWithChanceNomad != null) {
            user.getAssignment().add(createRoleAssignment(randomAuxWithChanceNomad.getOidValue()));
        }

        if (randomAuxWithChanceVpn != null) {
            user.getAssignment().add(createRoleAssignment(randomAuxWithChanceVpn.getOidValue()));
        }
    }

    /**
     * Determines whether a role should be forgotten based on a given chance.
     * The chance is a percentage value between 0 and 100.
     * A random number between 0 and 100 is generated and if it's less than the given chance, the method returns true.
     *
     * @param chance The chance (percentage) of forgetting a role.
     * @return True if the role should be forgotten, false otherwise.
     */
    public static boolean isForgetRole(int chance) {
        Random random = new Random();
        return random.nextInt(100) < chance;
    }

    /**
     * Retrieves an additional noise role based on a given chance.
     * The chance is a percentage value between 0 and 100.
     * A random number between 0 and 100 is generated and if it's less than the given chance,
     * a noise role is selected randomly from the list of noise roles.
     * If the chance condition is not met or there are no noise roles, the method returns null.
     *
     * @param chance The chance (percentage) of getting an additional noise role.
     * @return A RoleType object representing the additional noise role, or null if no role is selected.
     */
    public static @Nullable RoleType getAdditionNoiseRole(int chance) {
        Random random = new Random();
        boolean b = random.nextInt(100) < chance;
        List<RoleType> noiseRolesObjects = getNoiseRolesObjects();

        if (b && !noiseRolesObjects.isEmpty()) {
            int randomIndex = random.nextInt(noiseRolesObjects.size());
            return noiseRolesObjects.get(randomIndex);
        } else {
            return null;
        }
    }

    public static boolean isCandidate(int chance) {
        Random random = new Random();
        return random.nextInt(100) < chance;
    }

    public static int getProbabilityPoint() {
        Random random = new Random();
        return random.nextInt(100) + 1;
    }
}
