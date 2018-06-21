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

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.any.*;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignmentReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedNamedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metamodel.internal.MetamodelImpl;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.tuple.IdentifierProperty;
import org.hibernate.tuple.entity.EntityMetamodel;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

/**
 * @author lazyman
 */
public final class RUtil {

    /**
     * Currently set in ctx-session.xml as constant, used for batch inserts (e.g. in OrgClosureManager)
     */
    public static final int JDBC_BATCH_SIZE = 20;

    /**
     * This constant is used for mapping type for {@link javax.persistence.Lob}
     * fields. {@link org.hibernate.type.MaterializedClobType} was not working
     * properly with PostgreSQL, causing TEXT types (clobs) to be saved not in
     * table row but somewhere else and it always messed up UTF-8 encoding
     */
    public static final String LOB_STRING_TYPE = "org.hibernate.type.MaterializedClobType"; //todo is it working correctly with postgresql [lazyman]

    public static final int COLUMN_LENGTH_QNAME = 157;

    public static final String QNAME_DELIMITER = "#";

    /**
     * This constant is used for oid column size in database.
     */
    public static final int COLUMN_LENGTH_OID = 36;

    /**
     * This namespace is used for wrapping xml parts of objects during save to
     * database.
     */
    public static final String NS_SQL_REPO = "http://midpoint.evolveum.com/xml/ns/fake/sqlRepository-1.xsd";
    public static final String SQL_REPO_OBJECT = "sqlRepoObject";
    public static final QName CUSTOM_OBJECT = new QName(NS_SQL_REPO, SQL_REPO_OBJECT);

    private static final Trace LOGGER = TraceManager.getTrace(RUtil.class);

    private static final int DB_OBJECT_NAME_MAX_LENGTH = 30;

    private RUtil() {
    }

    public static <T extends Objectable> void revive(Objectable object, PrismContext prismContext)
            throws DtoTranslationException {
        try {
            prismContext.adopt(object);
        } catch (SchemaException ex) {
            throw new DtoTranslationException(ex.getMessage(), ex);
        }
    }

    public static Element createFakeParentElement() {
        return DOMUtil.createElement(DOMUtil.getDocument(), CUSTOM_OBJECT);
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

    public static List<ObjectReferenceType> safeSetReferencesToList(Set<? extends RObjectReference> set, PrismContext prismContext) {

        if (set == null || set.isEmpty()) {
            return new ArrayList<>();
        }

        List<ObjectReferenceType> list = new ArrayList<>();
        for (RObjectReference str : set) {
            ObjectReferenceType ort = new ObjectReferenceType();
            RObjectReference.copyToJAXB(str, ort);
            list.add(ort);
        }
        return list;
    }

    public static Set safeListReferenceToSet(List<ObjectReferenceType> list, PrismContext prismContext,
                                             RObject owner, RReferenceOwner refOwner) {
        Set<RObjectReference> set = new HashSet<>();
        if (list == null || list.isEmpty()) {
            return set;
        }

        for (ObjectReferenceType ref : list) {
            RObjectReference rRef = RUtil.jaxbRefToRepo(ref, prismContext, owner, refOwner);
            if (rRef != null) {
                set.add(rRef);
            }
        }
        return set;
    }

    public static RObjectReference jaxbRefToRepo(ObjectReferenceType reference, PrismContext prismContext,
                                                 RObject owner, RReferenceOwner refOwner) {
        if (reference == null) {
            return null;
        }
        Validate.notNull(owner, "Owner of reference must not be null.");
        Validate.notNull(refOwner, "Reference owner of reference must not be null.");
        Validate.notEmpty(reference.getOid(), "Target oid reference must not be null.");

        RObjectReference repoRef = new RObjectReference();
        repoRef.setReferenceType(refOwner);
        repoRef.setOwner(owner);
        RObjectReference.copyFromJAXB(reference, repoRef);

        return repoRef;
    }

    public static REmbeddedReference jaxbRefToEmbeddedRepoRef(ObjectReferenceType jaxb,
                                                              PrismContext prismContext) {
        if (jaxb == null) {
            return null;
        }
        REmbeddedReference ref = new REmbeddedReference();
        REmbeddedReference.copyFromJAXB(jaxb, ref);

        return ref;
    }

    public static REmbeddedNamedReference jaxbRefToEmbeddedNamedRepoRef(ObjectReferenceType jaxb) {
        if (jaxb == null) {
            return null;
        }
        REmbeddedNamedReference ref = new REmbeddedNamedReference();
        REmbeddedNamedReference.copyFromJAXB(jaxb, ref);

        return ref;
    }

    public static Integer getIntegerFromString(String val) {
        if (val == null || !val.matches("[0-9]+")) {
            return null;
        }

        return Integer.parseInt(val);
    }

    /**
     * This method is used to override "hasIdentifierMapper" in EntityMetaModels
     * of entities which have composite id and class defined for it. It's
     * workaround for bug as found in forum
     * https://forum.hibernate.org/viewtopic.php?t=978915&highlight=
     *
     * @param sessionFactory
     */
    public static void fixCompositeIDHandling(SessionFactory sessionFactory) {
        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectDeltaOperation.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROrgClosure.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, ROExtLong.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentExtension.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtDate.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtPolyString.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAExtLong.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RObjectReference.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignmentReference.class);

        fixCompositeIdentifierInMetaModel(sessionFactory, RAssignment.class);
        fixCompositeIdentifierInMetaModel(sessionFactory, RTrigger.class);
        for (RObjectType type : ClassMapper.getKnownTypes()) {
            fixCompositeIdentifierInMetaModel(sessionFactory, type.getClazz());
        }
    }

    private static void fixCompositeIdentifierInMetaModel(SessionFactory sessionFactory, Class clazz) {
        ClassMetadata classMetadata = sessionFactory.getClassMetadata(clazz);
        if (classMetadata instanceof AbstractEntityPersister) {
            AbstractEntityPersister persister = (AbstractEntityPersister) classMetadata;
            EntityMetamodel model = persister.getEntityMetamodel();
            IdentifierProperty identifier = model.getIdentifierProperty();

            try {
                Field field = IdentifierProperty.class.getDeclaredField("hasIdentifierMapper");
                field.setAccessible(true);
                field.set(identifier, true);
                field.setAccessible(false);
            } catch (Exception ex) {
                throw new SystemException("Attempt to fix entity meta model with hack failed, reason: "
                        + ex.getMessage(), ex);
            }
        }
    }

    public static void copyResultFromJAXB(QName itemName, OperationResultType jaxb,
                                          OperationResult repo, PrismContext prismContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");

        if (jaxb == null) {
            repo.setStatus(null);
            if (repo instanceof OperationResultFull) {
                ((OperationResultFull) repo).setFullResult(null);
            }

            return;
        }

        repo.setStatus(getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
        if (repo instanceof OperationResultFull) {
            try {
                String full = prismContext.xmlSerializer().serializeRealValue(jaxb, itemName);
                byte[] data = RUtil.getByteArrayFromXml(full, true);
                ((OperationResultFull) repo).setFullResult(data);
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

    public static <T extends SchemaEnum> T getRepoEnumValue(Object object, Class<T> type) {
        if (object == null) {
            return null;
        }
        Object[] values = type.getEnumConstants();
        for (Object value : values) {
            T schemaEnum = (T) value;
            if (schemaEnum.getSchemaValue().equals(object)) {
                return schemaEnum;
            }
        }

        throw new IllegalArgumentException("Unknown value '" + object + "' of type '" + object.getClass()
                + "', can't translate to '" + type + "'.");
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

    public static QName stringToQName(String text) {
        if (StringUtils.isEmpty(text)) {
            return null;
        }

        int index = text.lastIndexOf(QNAME_DELIMITER);
        String namespace = StringUtils.left(text, index);
        String localPart = StringUtils.right(text, text.length() - index - 1);

        if (StringUtils.isEmpty(localPart)) {
            return null;
        }

        return new QName(namespace, localPart);
    }

    public static Long toLong(Short s) {
        if (s == null) {
            return null;
        }

        return s.longValue();
    }

    public static Long toLong(Integer i) {
        if (i == null) {
            return null;
        }

        return i.longValue();
    }

    public static Short toShort(Long l) {
        if (l == null) {
            return null;
        }

        if (l > Short.MAX_VALUE || l < Short.MIN_VALUE) {
            throw new IllegalArgumentException("Couldn't cast value to short " + l);
        }

        return l.shortValue();
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

    public static String getTableName(Class hqlType, Session session) {
        SessionFactory factory = session.getSessionFactory();
        MetamodelImpl model = (MetamodelImpl) factory.getMetamodel();
        EntityPersister ep = model.entityPersister(hqlType);
        if (ep instanceof Joinable) {
            Joinable joinable = (Joinable) ep;
            return joinable.getTableName();
        }

        throw new SystemException("Couldn't get table name for class " + hqlType.getName());
    }

    public static byte[] getByteArrayFromXml(String xml, boolean compress) {
        if (xml == null) {
            return null;
        }

        byte[] array;

        GZIPOutputStream gzip = null;
        try {
            if (compress) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                gzip = new GZIPOutputStream(out);
                gzip.write(xml.getBytes(StandardCharsets.UTF_8.name()));
                gzip.close();
                out.close();

                array = out.toByteArray();
            } else {
                array = xml.getBytes(StandardCharsets.UTF_8.name());
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't save full xml object, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(gzip);
        }

        return array;
    }

    public static String getXmlFromByteArray(byte[] array, boolean compressed) {
        return getXmlFromByteArray(array, compressed, false);
    }

    public static String getXmlFromByteArray(byte[] array, boolean compressed, boolean useUtf16) {
        if (array == null) {
            return null;
        }

        String xml;

        GZIPInputStream gzip = null;
        try {
            if (compressed) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    gzip = new GZIPInputStream(new ByteArrayInputStream(array));
                    IOUtils.copy(gzip, out);
                    xml = new String(out.toByteArray(), StandardCharsets.UTF_8);
                } catch (ZipException ex) {
                    LOGGER.debug("Byte array should represent compressed (gzip) string, but: {}", ex.getMessage());

                    // utf-16 will be used only under specific conditions
                    // - we're using SQL Server
                    // - we are trying to read audit delta or fullResult which aren't compressed - data before 3.8 release
                    // These data couldn't be migrated from nvarchar(max) to varbinary(max) without breaking encoding as
                    // SQL Server doesn't support utf8 and uses ucs2 encoding (compatible to read with utf16)
                    Charset ch = useUtf16 ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_8;
                    xml = new String(array, ch);
                }
            } else {
                xml = new String(array, StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            throw new SystemException("Couldn't read data from full object column, reason: " + ex.getMessage(), ex);
        } finally {
            IOUtils.closeQuietly(gzip);
        }

        return xml;
    }

    public static OrgFilter findOrgFilter(ObjectQuery query) {
        return query != null ? findOrgFilter(query.getFilter()) : null;
    }

    public static OrgFilter findOrgFilter(ObjectFilter filter) {
        if (filter == null) {
            return null;
        }

        if (filter instanceof OrgFilter) {
            return (OrgFilter) filter;
        }

        if (filter instanceof LogicalFilter) {
            LogicalFilter logical = (LogicalFilter) filter;
            for (ObjectFilter f : logical.getConditions()) {
                OrgFilter o = findOrgFilter(f);
                if (o != null) {
                    return o;
                }
            }
        }

        return null;
    }

    public static String trimString(String message, int size) {
        if (message == null || message.length() <= size) {
            return message;
        }
        return message.substring(0, size - 4) + "...";
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
}
