package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import com.google.common.collect.Iterables;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreConfig;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreDBType;
import org.assertj.core.util.Arrays;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CoreFiberRepositoryFunction {
    private static DateFormat timestampFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public static Object createResultObject(Map<Class, Map<Object, Object>> tableInstanceIdMapCache, Connection connection, Class returnClass, Class elementType, CoreTableInfo tableInfo, ResultSet resultSet, Integer size) throws Exception {
        Object result = null;
        if (CoreFiberGenericResultSetContainer.class.isAssignableFrom(returnClass)) {
            result = new CoreFiberGenericResultSetContainer();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            List<String> columnNameDBList = new LinkedList<>();
            for (int count = 1; count <= resultSetMetaData.getColumnCount(); count++) {
                columnNameDBList.add(resultSetMetaData.getColumnName(count));
            }
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int count = 0; count < columnNameDBList.size(); count++) {
                    String columnName = columnNameDBList.get(count);
                    Object value = resultSet.getObject(count + 1);
                    row.put(columnName, value);
                }
                ((CoreFiberGenericResultSetContainer)result).getResult().add(row);
            }
            return result;
        }
        List<LinkedHashMap<String, Object>> resultHolder = new ArrayList<>();
        Map<Class, Map<String, List<Object>>> foreignKeyIdMap = new HashMap<>();
        if (tableInstanceIdMapCache == null) {
            tableInstanceIdMapCache = new HashMap<>();
        }
        if ((tableInfo != null) && (tableInstanceIdMapCache.get(tableInfo.getTableClass()) == null)) {
            tableInstanceIdMapCache.put(tableInfo.getTableClass(), new HashMap<>());
        }

        List<String> columnNameDBList = getColumnNameDBList(resultSet);
        List<String> columnNameJAVAList = getColumnNameJAVAList(tableInfo, columnNameDBList);
        Integer currentSize = 0;
        while (((size == null) || ((size != null) && (currentSize < size))) && (resultSet.next())) {
            LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
            for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
                Object value = resultSet.getObject(columnNameDBList.get(countColumn));
                entry.put(columnNameDBList.get(countColumn), value);
            }
            currentSize++;
            resultHolder.add(entry);
        }
        if (Iterable.class.isAssignableFrom(returnClass)) {
            if (!List.class.isAssignableFrom(returnClass)) {
                throw new Exception("Return type of '" + returnClass.getName() + "' is not supported yet");
            }
            result = new ArrayList<>();
            if (elementType == null) {
                if (columnNameJAVAList.size() == 1) {
                    for (int count = 0; count < resultHolder.size(); count++) {
                        ((ArrayList) result).add(resultHolder.get(count).entrySet().iterator().next().getValue());
                    }
                } else if (columnNameJAVAList.size() > 1) {
                    for (int count = 0; count < resultHolder.size(); count++) {
                        Object entry = tableInfo.getTableClass().newInstance();
                        int countColumn = 0;
                        for (Map.Entry<String, Object> mapEntry : resultHolder.get(count).entrySet()) {
                            tableFieldValueAssignment(
                                    tableInfo,
                                    foreignKeyIdMap,
                                    tableInfo.getTableClass(),
                                    entry,
                                    mapEntry,
                                    columnNameDBList.get(countColumn),
                                    columnNameJAVAList.get(countColumn)
                            );
                            countColumn++;
                        }
                        Field idField = tableInfo.getTableClass().getDeclaredField(tableInfo.getIdNameJava());
                        idField.setAccessible(true);
                        Object idValue = idField.get(entry);
                        tableInstanceIdMapCache.get(tableInfo.getTableClass()).put(idValue, entry);
                        ((ArrayList) result).add(entry);
                    }
                    tableFieldManyToOneAssignment(tableInstanceIdMapCache, connection, tableInfo, (List) result, foreignKeyIdMap);
                }
            } else if (Map.class.isAssignableFrom(elementType)) {
                for (int count = 0; count < resultHolder.size(); count++) {
                    ((ArrayList) result).add(resultHolder.get(count));
                }
            } else {
                for (int count = 0; count < resultHolder.size(); count++) {
                    Object entry = elementType.newInstance();
                    int countColumn = 0;
                    for (Map.Entry<String, Object> mapEntry : resultHolder.get(count).entrySet()) {
                        tableFieldValueAssignment(
                                tableInfo,
                                foreignKeyIdMap,
                                tableInfo.getTableClass(),
                                entry,
                                mapEntry,
                                columnNameDBList.get(countColumn),
                                columnNameJAVAList.get(countColumn)
                        );
                        countColumn++;
                    }
                    Field idField = tableInfo.getTableClass().getDeclaredField(tableInfo.getIdNameJava());
                    idField.setAccessible(true);
                    Object idValue = idField.get(entry);
                    tableInstanceIdMapCache.get(tableInfo.getTableClass()).put(idValue, entry);
                    ((ArrayList) result).add(entry);
                }
                tableFieldManyToOneAssignment(tableInstanceIdMapCache, connection, tableInfo, (List) result, foreignKeyIdMap);
            }
        } else {
            if (resultHolder.size() > 1) {
                throw new Exception("Query returned more than 1 result. Query's size = '" + resultHolder.size() + "'");
            } else if (resultHolder.size() == 1) {
                if (elementType == null) {
                    if (columnNameJAVAList.size() == 1) {
                        result = resultHolder.get(0).entrySet().iterator().next().getValue();
                    } else if (columnNameJAVAList.size() > 1) {
                        Object entry = tableInfo.getTableClass().newInstance();
                        int countColumn = 0;
                        for (Map.Entry<String, Object> mapEntry : resultHolder.get(0).entrySet()) {
                            tableFieldValueAssignment(
                                    tableInfo,
                                    foreignKeyIdMap,
                                    tableInfo.getTableClass(),
                                    entry,
                                    mapEntry,
                                    columnNameDBList.get(countColumn),
                                    columnNameJAVAList.get(countColumn)
                            );
                            countColumn++;
                        }
                        Field idField = tableInfo.getTableClass().getDeclaredField(tableInfo.getIdNameJava());
                        idField.setAccessible(true);
                        Object idValue = idField.get(entry);
                        tableInstanceIdMapCache.get(tableInfo.getTableClass()).put(idValue, entry);
                        List<Object> instanceContainer = new LinkedList<>();
                        instanceContainer.add(entry);
                        tableFieldManyToOneAssignment(tableInstanceIdMapCache, connection, tableInfo, instanceContainer, foreignKeyIdMap);
                        result = entry;
                    }
                } else {
                    Object entry = elementType.newInstance();
                    int countColumn = 0;
                    for (Map.Entry<String, Object> mapEntry : resultHolder.get(0).entrySet()) {
                        tableFieldValueAssignment(
                                tableInfo,
                                foreignKeyIdMap,
                                tableInfo.getTableClass(),
                                entry,
                                mapEntry,
                                columnNameDBList.get(countColumn),
                                columnNameJAVAList.get(countColumn)
                        );
                        countColumn++;
                    }
                    Field idField = tableInfo.getTableClass().getDeclaredField(tableInfo.getIdNameJava());
                    idField.setAccessible(true);
                    Object idValue = idField.get(entry);
                    tableInstanceIdMapCache.get(tableInfo.getTableClass()).put(idValue, entry);
                    List<Object> instanceContainer = new LinkedList<>();
                    instanceContainer.add(entry);
                    tableFieldManyToOneAssignment(tableInstanceIdMapCache, connection, tableInfo, instanceContainer, foreignKeyIdMap);
                    ((ArrayList) result).add(entry);
                }
            }
        }
        return result;
    }

    private static void tableFieldValueAssignment(
            CoreTableInfo tableInfo,
            Map<Class, Map<String, List<Object>>> foreignKeyIdMap,
            Class tableClass,
            Object tableInstance,
            Map.Entry<String, Object> dbFieldValueMap,
            String columnNameDB,
            String columnNameJava
    ) throws Exception {
        if ((tableInfo.getManyToOneDBColumnMapTableClass().containsKey(columnNameDB)) ||
                (tableInfo.getManyToOneDBUpperColumnMapTableClass().containsKey(columnNameDB))) {
            Class fieldTableClass = tableInfo.getManyToOneDBColumnMapTableClass().get(columnNameDB);
            if (fieldTableClass == null) {
                fieldTableClass = tableInfo.getManyToOneDBUpperColumnMapTableClass().get(columnNameDB);
            }
            if (foreignKeyIdMap.get(fieldTableClass) == null) {
                foreignKeyIdMap.put(fieldTableClass, new HashMap<>());
            }
            if (foreignKeyIdMap.get(fieldTableClass).get(columnNameDB) == null) {
                foreignKeyIdMap.get(fieldTableClass).put(columnNameDB, new LinkedList<>());
            }
            foreignKeyIdMap.get(fieldTableClass).get(columnNameDB).add(dbFieldValueMap.getValue());
        } else {
            Field javaField = tableClass.getDeclaredField(columnNameJava);
            javaField.setAccessible(true);

            if (dbFieldValueMap.getValue() == null) {
                javaField.set(tableInstance, dbFieldValueMap.getValue());
            } else if ((BigDecimal.class.isAssignableFrom(javaField.getType()))) {
                javaField.set(tableInstance, new BigDecimal(String.valueOf(dbFieldValueMap.getValue())));
            } else if (((Number.class.isAssignableFrom(javaField.getType())) && (Number.class.isAssignableFrom(dbFieldValueMap.getValue().getClass())))) {
                Method numberValueOf = javaField.getType().getMethod("valueOf", String.class);
                javaField.set(tableInstance, numberValueOf.invoke(null, String.valueOf(dbFieldValueMap.getValue())));
            } else if (javaField.getType().isEnum()) {
                Class enumClass = javaField.getType();
                Method valueOfMethod = enumClass.getMethod("valueOf", String.class);
                javaField.set(tableInstance, valueOfMethod.invoke(null, dbFieldValueMap.getValue()));
            } else if ((dbFieldValueMap.getValue() != null) && (Clob.class.isAssignableFrom(dbFieldValueMap.getValue().getClass()))) {
                InputStream in = ((Clob)dbFieldValueMap.getValue()).getAsciiStream();
                String clobAsString = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
                javaField.set(tableInstance, clobAsString);
            }
            else if (Date.class.isAssignableFrom(javaField.getType())) {
                Timestamp dbDate = (Timestamp) dbFieldValueMap.getValue();
                ZonedDateTime gmt = dbDate.toInstant().atZone(ZoneId.systemDefault());
                ZonedDateTime zdt = gmt.withZoneSameLocal(ZoneOffset.UTC);
                ZonedDateTime lzdt = zdt.withZoneSameInstant(ZoneId.systemDefault());
                java.sql.Timestamp timestamp = Timestamp.valueOf(lzdt.toLocalDateTime());
                javaField.set(tableInstance, new Date(timestamp.getTime()));
            }
            else {
                javaField.set(tableInstance, dbFieldValueMap.getValue());
            }
        }
    }

    private static void tableFieldManyToOneAssignment(
            Map<Class, Map<Object, Object>> tableInstanceIdMapCache,
            Connection connection,
            CoreTableInfo tableInfo,
            List<Object> tableInstanceList,
            Map<Class, Map<String, List<Object>>> foreignKeyIdMap
    ) throws Exception {
        if (tableInstanceList.size() == 0) {
            return;
        }
        Iterator entry = tableInfo.getManyToOneDBColumnMapField().entrySet().iterator();
        Iterator entryUpper = tableInfo.getManyToOneDBUpperColumnMapField().entrySet().iterator();
        for (int count = 0; count < tableInfo.getManyToOneDBColumnMapField().size(); count++) {
            Map.Entry<String, Field> entryMap = (Map.Entry<String, Field>) entry.next();
            Map.Entry<String, Field> entryUpperMap = (Map.Entry<String, Field>) entryUpper.next();

            Class fieldClass = entryMap.getValue().getType();
            Map<String, List<Object>> fkIdMap = foreignKeyIdMap.get(fieldClass);
            List<Object> fkIdValueList = fkIdMap.get(entryMap.getKey());
            if (fkIdValueList == null) {
                fkIdValueList = fkIdMap.get(entryUpperMap.getKey());
            }
            Map<Object, Object> fkIdObjectMap = tableInstanceIdMapCache.get(fieldClass);
            if (fkIdObjectMap == null) {
                tableInstanceIdMapCache.put(fieldClass, new HashMap<>());
                fkIdObjectMap = tableInstanceIdMapCache.get(fieldClass);
                Field manyToOneField = entryMap.getValue();
                CoreTableInfo fieldTableInfo = CoreConfig.tableInfoMap.get(manyToOneField.getType());
                String queryForeignKeyTable = fieldTableInfo.getQuerySelect() + " WHERE (" + fieldTableInfo.getIdNameDB() + " " + getTransformedInClause(fieldTableInfo.getIdClass());
                queryForeignKeyTable += ")";
                PreparedStatement fiberPreparedStatement = null;
                ResultSet foreignKeyResultSet = null;

                try {
                    List<Object> fkIdUniqueValueList = fkIdValueList.stream().filter(e -> e != null).distinct().collect(Collectors.toList());

                    fiberPreparedStatement = connection.prepareStatement(queryForeignKeyTable);
                    CoreFiberRepositoryFunction.assignPreparedStatement(connection, fiberPreparedStatement, 1, fkIdUniqueValueList);
                    foreignKeyResultSet = fiberPreparedStatement.executeQuery();
                    List<Object> foreignKeyObjectList = (List) CoreFiberRepositoryFunction.createResultObject(tableInstanceIdMapCache, connection, List.class, fieldTableInfo.getTableClass(), fieldTableInfo, foreignKeyResultSet, null);
                    for (int countForeignKeyObject = 0; countForeignKeyObject < foreignKeyObjectList.size(); countForeignKeyObject++) {
                        Field foreignKeyIdField = fieldTableInfo.getTableClass().getDeclaredField(fieldTableInfo.getIdNameJava());
                        foreignKeyIdField.setAccessible(true);
                        Object foreignKeyId = foreignKeyIdField.get(foreignKeyObjectList.get(countForeignKeyObject));
                        fkIdObjectMap.put(foreignKeyId, foreignKeyObjectList.get(countForeignKeyObject));
                    }
                } finally {
                    if (foreignKeyResultSet != null) {
                        foreignKeyResultSet.close();
                    }
                    if (fiberPreparedStatement != null) {
                        fiberPreparedStatement.close();
                    }
                }
            }
            Field manyToOneField = entryMap.getValue();
            for (int countTableInstance = 0; countTableInstance < tableInstanceList.size(); countTableInstance++) {
                Object fkId = fkIdValueList.get(countTableInstance);
                Object fkObject = fkIdObjectMap.get(fkId);
                manyToOneField.set(tableInstanceList.get(countTableInstance), fkObject);
            }
        }
    }

    public static String getTypeSql(Class type) throws Exception {
        //Add new types here when necessary
        if (String.class.isAssignableFrom(type)) {
            return "varchar";
        }
        if ((Date.class.isAssignableFrom(type)) ||
                (ZonedDateTime.class.isAssignableFrom(type))) {
            return "timestamp without time zone";
        }
        if ((boolean.class.isAssignableFrom(type)) ||
                (Boolean.class.isAssignableFrom(type))) {
            return "boolean";
        }
        if ((long.class.isAssignableFrom(type)) ||
                (Long.class.isAssignableFrom(type)) ||
                (int.class.isAssignableFrom(type)) ||
                (Integer.class.isAssignableFrom(type)) ||
                (BigInteger.class.isAssignableFrom(type))) {
            return "bigint";
        }
        if (BigDecimal.class.isAssignableFrom(type)) {
            return "numeric";
        }
        if (Byte.class.isAssignableFrom(type)) {
            if (CoreDBType.H2.equals(CoreConfig.databaseType)) {
                return "BINARY";
            }
            if (CoreDBType.POSTGRES.equals(CoreConfig.databaseType)) {
                return "bytea";
            }
        }
        throw new Exception("Unsupported Type '" + type.getName() + "'");
    }

    public static void prepareRowForInsert(Connection connection, PreparedStatement preparedStatement, CoreTableInfo tableInfo, Object row, Boolean withId) throws Exception {
        //Add support different field types here.
        int parameterIndex = 1;
        for (int countField = 0; countField < tableInfo.getColumnNameJAVAList().size(); countField++) {
            String columnNameDB = tableInfo.getColumnNameDBList().get(countField);
            String columnNameJAVA = tableInfo.getColumnNameJAVAList().get(countField);
            Field field = row.getClass().getDeclaredField(columnNameJAVA);
            field.setAccessible(true);
            Boolean isFieldManyToOne = (field.getAnnotation(ManyToOne.class) != null);
            Boolean isFieldOneToMany = (field.getAnnotation(OneToMany.class) != null);
            if (isFieldManyToOne) {
                Object parentTableObject = field.get(row);
                if (parentTableObject == null) {
                    assignPreparedStatement(connection, preparedStatement, parameterIndex, null);
                } else {
                    CoreTableInfo parentTableType = CoreConfig.tableInfoMap.get(field.getType());
                    Field parentTableIdField = parentTableType.getTableClass().getDeclaredField(parentTableType.getIdNameJava());
                    parentTableIdField.setAccessible(true);
                    Object parentTableObjectIdValue = parentTableIdField.get(parentTableObject);
                    assignPreparedStatement(connection, preparedStatement, parameterIndex, parentTableObjectIdValue);
                }
                parameterIndex++;
            } else if (withId) {
                Object value = field.get(row);
                assignPreparedStatement(connection, preparedStatement, parameterIndex, value);
                parameterIndex++;
            } else if (!(tableInfo.getIdNameDB().equals(columnNameDB))) {
                Object value = field.get(row);
                assignPreparedStatement(connection, preparedStatement, parameterIndex, value);
                parameterIndex++;
            }
        }
    }

    public static void prepareRowForUpdate(Connection connection, PreparedStatement preparedStatement, CoreTableInfo tableInfo, Object row) throws Exception {
        //Add support different field types here.
        int parameterIndex = 1;
        for (int countField = 0; countField < tableInfo.getColumnNameJAVAList().size(); countField++) {
            String columnNameJAVA = tableInfo.getColumnNameJAVAList().get(countField);
            Field field = row.getClass().getDeclaredField(columnNameJAVA);
            field.setAccessible(true);
            Boolean isFieldManyToOne = (field.getAnnotation(ManyToOne.class) != null);
            Boolean isFieldOneToMany = (field.getAnnotation(OneToMany.class) != null);
            if (isFieldManyToOne) {
                Object parentTableObject = field.get(row);
                if (parentTableObject == null) {
                    assignPreparedStatement(connection, preparedStatement, parameterIndex, null);
                } else {
                    CoreTableInfo parentTableType = CoreConfig.tableInfoMap.get(field.getType());
                    Field parentTableIdField = parentTableType.getTableClass().getDeclaredField(parentTableType.getIdNameJava());
                    parentTableIdField.setAccessible(true);
                    Object parentTableObjectIdValue = parentTableIdField.get(parentTableObject);
                    assignPreparedStatement(connection, preparedStatement, parameterIndex, parentTableObjectIdValue);
                }
                parameterIndex++;
            } else if (!columnNameJAVA.equals(tableInfo.getIdNameJava())) {
                Object value = field.get(row);
                assignPreparedStatement(connection, preparedStatement, parameterIndex, value);
                parameterIndex++;
            }
        }
        // last assignment = id (where id = ?)
        Field field = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
        field.setAccessible(true);
        Object value = field.get(row);
        assignPreparedStatement(connection, preparedStatement, parameterIndex, value);
    }

    public static void prepareRowForDelete(Connection connection, PreparedStatement preparedStatement, CoreTableInfo tableInfo, Object idValue) throws Exception {
        assignPreparedStatement(connection, preparedStatement, 1, idValue);
    }

    public static void assignPreparedStatement(Connection connection, PreparedStatement preparedStatement, int index, Object value) throws Exception {
        if (value == null) {
            preparedStatement.setObject(index, null);
        } else if (Iterable.class.isAssignableFrom(value.getClass())) {
            Iterator iterator = ((Iterable) value).iterator();
            if (iterator.hasNext()) {
                Object[] arrayValue = Iterables.toArray((Iterable) value, Object.class);
                java.sql.Array arraySqlValue = null;
                if (CoreDBType.H2.equals(CoreConfig.databaseType)) {
                    arrayValue = java.util.Arrays.stream(arrayValue).map(Object::toString).toArray();
                    arraySqlValue = connection.createArrayOf(CoreFiberRepositoryFunction.getTypeSql(String.class), arrayValue);
                } else {
                    Object firstValue = iterator.next();
                    arraySqlValue = connection.createArrayOf(CoreFiberRepositoryFunction.getTypeSql(firstValue.getClass()), arrayValue);
                }
                preparedStatement.setArray(index, arraySqlValue);
            } else {
                preparedStatement.setArray(index, null);
            }
        } else if (value.getClass().isArray()) {
            if (Array.getLength(value) > 0) {
                List<Object> arrayAsList = Arrays.asList(value);
                Object[] arrayValue = arrayAsList.stream().toArray();
                java.sql.Array arraySqlValue = null;
                if (CoreDBType.H2.equals(CoreConfig.databaseType)) {
                    arrayValue = java.util.Arrays.stream(arrayValue).map(Object::toString).toArray();
                    arraySqlValue = connection.createArrayOf(CoreFiberRepositoryFunction.getTypeSql(String.class), arrayValue);
                } else {
                    Object firstValue = arrayValue[0];
                    arraySqlValue = connection.createArrayOf(CoreFiberRepositoryFunction.getTypeSql(firstValue.getClass()), arrayValue);
                }
                preparedStatement.setArray(index, arraySqlValue);
            } else {
                preparedStatement.setArray(index, null);
            }
        } else if (value instanceof Enum) {
            preparedStatement.setString(index, String.valueOf(((Enum) value).name()));
        } else if (value instanceof Date) {
            Date date = (Date)value;
            ZonedDateTime zdt = date.toInstant().atZone(ZoneId.systemDefault());
            ZonedDateTime gmt = zdt.withZoneSameInstant(ZoneOffset.UTC);
            java.sql.Timestamp timestamp = Timestamp.valueOf(gmt.toLocalDateTime());
            preparedStatement.setTimestamp(index, timestamp);
//            java.sql.Timestamp timestamp = new java.sql.Timestamp(((Date) value).getTime());
//            preparedStatement.setTimestamp(index, timestamp);
        } else if (value instanceof ZonedDateTime) {
            java.sql.Timestamp timestamp = new java.sql.Timestamp(Date.from(((ZonedDateTime) value).toInstant()).getTime());
            preparedStatement.setTimestamp(index, timestamp, CoreConfig.timestampTimeZone);
        } else {
            preparedStatement.setObject(index, value);
        }
    }

    public static Class getTableClass(Class repositoryClass) {
        Type[] types = ((ParameterizedType) repositoryClass.getGenericInterfaces()[0]).getActualTypeArguments();
        Class tableClass = (Class) types[0];
        return tableClass;
    }

    public static List<String> getColumnNameDBList(ResultSet resultSet) throws Exception {
        List<String> result = new ArrayList<>();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        for (int count = 1; count <= resultSetMetaData.getColumnCount(); count++) {
            String columnName = resultSetMetaData.getColumnName(count);
            result.add(columnName);
        }
        return result;
    }

    public static List<String> getColumnNameJAVAList(CoreTableInfo tableInfo, List<String> columnNameDBList) throws Exception {
        List<String> result = new ArrayList<>();
        if (tableInfo != null) {
            for (int count = 0; count < columnNameDBList.size(); count++) {
                int indexDbColumn = tableInfo.getColumnNameDBList().indexOf(columnNameDBList.get(count));
                if (indexDbColumn == -1) {
                    indexDbColumn = tableInfo.getColumnNameDBUpperList().indexOf(columnNameDBList.get(count));
                }
                if (indexDbColumn == -1) {
                    columnNameDBList.remove(count);
                    count--;
                    continue;
                }
                String javaColumnName = tableInfo.getColumnNameJAVAList().get(indexDbColumn);
                result.add(javaColumnName);
            }
        }
        return result;
    }

    public static String getValueDb(Object value) throws Exception {
        if (value instanceof Enum) {
            return "'" + String.valueOf(value) + "'";
        }
        if (value instanceof Date) {
            return "'" + timestampFormatter.format(value) + "'";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        throw new Exception("Unsupported field type: " + value.getClass().getName());
    }

    public static String getNameDb2Java(String dbName) {
        String columnNameDB = dbName.toLowerCase();
        StringBuilder columnNameJAVA = new StringBuilder();
        for (int countChar = 0; countChar < columnNameDB.length(); countChar++) {
            char currentChar = columnNameDB.charAt(countChar);
            if (currentChar == '_') {
                char nextChar = columnNameDB.charAt(countChar + 1);
                nextChar = Character.toUpperCase(nextChar);
                columnNameJAVA.append(nextChar);
                //skip 1 char index
                countChar++;
            } else {
                columnNameJAVA.append(currentChar);
            }

        }
        return columnNameJAVA.toString();
    }

    public static String getNameJava2Db(String javaName) {
        StringBuilder columnNameDB = new StringBuilder();
        for (int countChar = 0; countChar < javaName.length(); countChar++) {
            char currentChar = javaName.charAt(countChar);
            if ((currentChar >= 'A') && (currentChar <= 'Z')) {
                String converted = "_" + Character.toLowerCase(currentChar);
                columnNameDB.append(converted);
            } else {
                columnNameDB.append(currentChar);
            }

        }
        return columnNameDB.toString();
    }

    public static void assignGeneratedIdToRowObject(Connection connection, CoreTableInfo tableInfo, List<Object> rowObjectList, int indexRowList, ResultSet generatedIdResultSet, int batchSize) throws Exception {
        List executeResultList = (List) CoreFiberRepositoryFunction.createResultObject(null, connection, List.class, null, tableInfo, generatedIdResultSet, null);
        Object firstElement = executeResultList.get(0);
        if (Number.class.isAssignableFrom(firstElement.getClass())) {
            int indexGeneratedIdList = 0;
            for (int count = 0; (indexRowList < rowObjectList.size()) & (count < batchSize); count++) {
                Object row = rowObjectList.get(indexRowList);
                Field idField = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
                idField.setAccessible(true);
                idField.set(row, executeResultList.get(indexGeneratedIdList));
                indexRowList++;
                indexGeneratedIdList++;
            }
        } else {
            rowObjectList.clear();
            rowObjectList.addAll(executeResultList);
        }
    }

    public static Map<String, String> getColumnTypeSqlMapUpper(ResultSet resultSet) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        int columnCount = 1;
        for (int count = 0; count < resultSetMetaData.getColumnCount(); count++) {
            String columnName = resultSetMetaData.getColumnName(columnCount);
            String columnType = resultSetMetaData.getColumnTypeName(columnCount);
            result.put(columnName.toUpperCase(), columnType);
            columnCount++;
        }
        return result;
    }

    public static String getTransformedInClause(Class arrayClass) throws Exception {
        String result = "IN (?)";
        if (CoreDBType.H2.equals(CoreConfig.databaseType)) {
            result = " IN (SELECT * FROM TABLE(x " + CoreFiberRepositoryFunction.getTypeSql(arrayClass) + " = ?))";
        } else if (CoreDBType.POSTGRES.equals(CoreConfig.databaseType)) {
            result = " = ANY(?)";
        }
        return result;
    }
}
