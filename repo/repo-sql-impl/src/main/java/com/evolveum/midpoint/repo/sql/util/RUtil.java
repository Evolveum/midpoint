/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RSimpleEmbeddedReference;

import com.google.common.base.Strings;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.Query;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.SerializationOptions;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.ROperationResult;
import com.evolveum.midpoint.repo.sql.data.common.ROperationResultFull;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceType;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public final class RUtil {

    public static final int COLUMN_LENGTH_QNAME = 157;

    public static final String QNAME_DELIMITER = "#";

    /**
     * This constant is used for oid column size in database.
     */
    public static final int COLUMN_LENGTH_OID = 36;

    private static final int DB_OBJECT_NAME_MAX_LENGTH = 30;

    private static final Map<Enum<?>, SchemaEnum<?>> ENUM_MAPPINGS = new ConcurrentHashMap<>();

    private RUtil() {
        throw new AssertionError("utility class can't be instantiated");
    }

    public static void revive(Objectable object)
            throws DtoTranslationException {
        try {
            PrismContext.get().adopt(object);
        } catch (SchemaException ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static <T> Set<T> listToSet(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return new HashSet<>(list);
    }

    public static Set<RPolyString> listPolyToSet(List<PolyStringType> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        Set<RPolyString> set = new HashSet<>();
        for (PolyStringType str : list) {
            set.add(RPolyString.copyFromJAXB(str));
        }
        return set;
    }

    public static List<ObjectReferenceType> toObjectReferenceTypeList(
            Set<? extends RObjectReference<?>> set) {

        if (set == null || set.isEmpty()) {
            return new ArrayList<>();
        }

        List<ObjectReferenceType> list = new ArrayList<>();
        for (RObjectReference<?> str : set) {
            ObjectReferenceType ort = new ObjectReferenceType();
            RObjectReference.copyToJAXB(str, ort);
            list.add(ort);
        }
        return list;
    }

    public static <T extends RObject> Set<RObjectReference<T>> toRObjectReferenceSet(
            List<ObjectReferenceType> list, RObject owner, RReferenceType refOwner, RelationRegistry relationRegistry) {
        return CollectionUtils.emptyIfNull(list)
                .stream()
                .<RObjectReference<T>>map(
                        ref -> RUtil.jaxbRefToRepo(ref, owner, refOwner, relationRegistry))
                .collect(Collectors.toSet());
    }

    public static <T extends RObject> RObjectReference<T> jaxbRefToRepo(ObjectReferenceType reference,
            RObject owner, RReferenceType refType, RelationRegistry relationRegistry) {
        if (reference == null) {
            return null;
        }
        Objects.requireNonNull(owner, "Owner of reference must not be null.");
        Objects.requireNonNull(refType, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RObjectReference<T> repoRef = new RObjectReference<>();
        repoRef.setReferenceType(refType);
        repoRef.setOwner(owner);
        RObjectReference.copyFromJAXB(reference, repoRef, relationRegistry);

        return repoRef;
    }

    public static RSimpleEmbeddedReference jaxbRefToEmbeddedRepoRef(ObjectReferenceType jaxb,
            RelationRegistry relationRegistry) {
        if (jaxb == null) {
            return null;
        }
        RSimpleEmbeddedReference ref = new RSimpleEmbeddedReference();
        RSimpleEmbeddedReference.fromJaxb(jaxb, ref, relationRegistry);

        return ref;
    }

    public static Integer getIntegerFromString(String val) {
        if (val == null || !val.matches("[0-9]+")) {
            return null;
        }

        return Integer.parseInt(val);
    }

    public static void copyResultFromJAXB(
            QName itemName, OperationResultType jaxb, ROperationResult repo) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");

        if (jaxb == null) {
            repo.setStatus(null);
            if (repo instanceof ROperationResultFull) {
                ((ROperationResultFull) repo).setFullResult(null);
            }

            return;
        }

        repo.setStatus(getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
        if (repo instanceof ROperationResultFull) {
            try {
                // Operation result can contain some "wild" objects within traces (e.g. AuditEventRecord).
                // Also invalid XML characters may be present here.
                // So let's be a little bit tolerant here. This is consistent with the new (native) repo implementation.
                SerializationOptions options = new SerializationOptions()
                        .serializeUnsupportedTypesAsString(true)
                        .escapeInvalidCharacters(true);
                // TODO MID-6303 should this be affected by configured fullObjectFormat?
                String full = PrismContext.get().xmlSerializer().options(options).serializeRealValue(jaxb, itemName);
                byte[] data = RUtil.getBytesFromSerializedForm(full, true);
                ((ROperationResultFull) repo).setFullResult(data);
            } catch (Exception ex) {
                throw new DtoTranslationException(ex.getMessage(), ex);
            }
        }
    }

    public static String computeChecksum(byte[]... objects) {
        try {
            List<InputStream> list = new ArrayList<>();
            for (byte[] data : objects) {
                if (data == null) {
                    continue;
                }
                list.add(new ByteArrayInputStream(data));
            }
            SequenceInputStream sis = new SequenceInputStream(Collections.enumeration(list));

            return DigestUtils.md5Hex(sis);
        } catch (IOException ex) {
            throw new SystemException(ex);
        }
    }

    public static <C, T extends SchemaEnum<C>> T getRepoEnumValue(C object, Class<T> type) {
        if (object == null) {
            return null;
        }
        T[] values = type.getEnumConstants();
        for (T value : values) {
            if (object.equals(value.getSchemaValue())) {
                return value;
            }
        }

        throw new IllegalArgumentException(
                "Unknown value '%s' of type '%s', can't translate to '%s'.".formatted(object, object.getClass(), type));
    }

    /*
     *  Beware: this method provides results different from QNameUtil.qnameToUri for namespaces ending with "/":
     *  E.g. for http://x/ plus auditor:
     *    - this one: http://x/#auditor
     *    - QNameUtil: http://x/auditor
     *  Normally it should not be a problem, because repo may use any QName representation it wants ... but it might
     *  be confusing in some situations.
     */
    @NotNull
    public static String qnameToString(QName qname) {
        StringBuilder sb = new StringBuilder();
        if (qname != null) {
            sb.append(qname.getNamespaceURI());
        }
        sb.append(QNAME_DELIMITER);
        if (qname != null) {
            sb.append(qname.getLocalPart());
        }

        return sb.toString();
    }

    public static ItemName stringToQName(String text) {
        if (Strings.isNullOrEmpty(text)) {
            return null;
        }

        int index = text.lastIndexOf(QNAME_DELIMITER);
        String namespace = StringUtils.left(text, index);
        String localPart = StringUtils.right(text, text.length() - index - 1);

        if (Strings.isNullOrEmpty(localPart)) {
            return null;
        }

        return new ItemName(namespace, localPart);
    }

    public static Long toLong(Integer i) {
        if (i == null) {
            return null;
        }

        return i.longValue();
    }

    public static Integer toInteger(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Couldn't cast value to Integer " + l);
        }

        return l.intValue();
    }

    public static String getDebugString(RObject object) {
        StringBuilder sb = new StringBuilder();
        if (object.getName() != null) {
            sb.append(object.getName().getOrig());
        } else {
            sb.append("null");
        }
        sb.append('(').append(object.getOid()).append(')');

        return sb.toString();
    }

    public static String getTableName(Class<?> hqlType, EntityManager entityManager) {
        EntityManagerFactory factory = entityManager.getEntityManagerFactory();
        MappingMetamodel model = (MappingMetamodel) factory.getMetamodel();
        EntityPersister ep = model.getEntityDescriptor(hqlType); // model.entityPersister(hqlType);
        if (ep instanceof Joinable joinable) {
            return joinable.getTableName();
        }

        throw new SystemException("Couldn't get table name for class " + hqlType.getName());
    }

    public static byte[] getBytesFromSerializedForm(String serializedForm, boolean compress) {
        if (serializedForm == null) {
            return null;
        }

        try {
            if (!compress) {
                return serializedForm.getBytes(StandardCharsets.UTF_8);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(serializedForm.getBytes(StandardCharsets.UTF_8));
                gzip.close(); // explicit close writes any remaining data
                return out.toByteArray();
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't save full object, reason: " + ex.getMessage(), ex);
        }
    }

    public static String getSerializedFormFromBytes(byte[] array) {
        return getSerializedFormFromBytes(array, false);
    }

    public static String getSerializedFormFromBytes(byte[] array, boolean useUtf16) {
        if (array == null) {
            return null;
        }

        // auto-detecting gzipped array (starts with 1f 8b)
        final int head = (array[0] & 0xff) | ((array[1] << 8) & 0xff00);
        if (GZIPInputStream.GZIP_MAGIC != head) {
            /*
             * UTF-16 is relevant only for older SQL Server deployments.
             * If we are trying to read audit delta or fullResult which aren't compressed,
             * it is likely data before 3.8 release.
             * These data couldn't be migrated from nvarchar(max) to varbinary(max) without breaking
             * encoding as SQL Server doesn't support UTF8 and uses UCS-2 (UTF-16) encoding.
             */
            Charset ch = useUtf16 ? detectCharsetIfUtf16Suggested(array) : StandardCharsets.UTF_8;
            return new String(array, ch);
        }

        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(array))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copy(gzip, out);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new SystemException("Couldn't read data from full object column, reason: " + ex.getMessage(), ex);
        }
    }

    private static Charset detectCharsetIfUtf16Suggested(byte[] bytes) {
        if (bytes != null && bytes.length > 2) {
            // UTF 16 has either 0 bytes or BOM (0xfeff or 0xfffe depending on endianness)
            if (bytes[0] == 0 || bytes[0] == (byte) 0xfe && bytes[1] == (byte) 0xff) {
                return StandardCharsets.UTF_16BE;
            }
            if (bytes[1] == 0 || bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xfe) {
                return StandardCharsets.UTF_16LE;
            }
        }

        return StandardCharsets.UTF_8;
    }

    public static String fixDBSchemaObjectNameLength(String input) {
        if (input == null || input.length() <= DB_OBJECT_NAME_MAX_LENGTH) {
            return input;
        }

        String result = input;
        String[] array = input.split("_");
        for (int i = 0; i < array.length; i++) {
            int length = array[i].length();
            String lengthStr = Integer.toString(length);

            if (length < lengthStr.length()) {
                continue;
            }

            array[i] = array[i].charAt(0) + lengthStr;

            result = StringUtils.join(array, "_");
            if (result.length() < DB_OBJECT_NAME_MAX_LENGTH) {
                break;
            }
        }

        return result;
    }

    public static void executeStatement(Connection connection, String sql) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.execute(sql);
        } finally {
            if (stmt != null && !stmt.isClosed()) {
                stmt.close();
            }
        }
    }

    public static Object getRepoEnumValue(Object key) {
        var mapped = ENUM_MAPPINGS.get(key);
        return mapped != null ? mapped : key;
    }

    public static <C extends Enum<C>> void register(SchemaEnum<C> value) {
        if (value.getSchemaValue() != null) {
            ENUM_MAPPINGS.put(value.getSchemaValue(), value);
        }
    }

    public static <T> T getSingleResultOrNull(Query query) {
        List<?> results = query.getResultList();
        if (results.isEmpty()) {
            return null;
        }

        if (results.size() > 1) {
            throw new NonUniqueResultException("Expected single result, but got " + results.size());
        }

        return (T) results.get(0);
    }
}
