package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import lombok.Data;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class CoreTableInfo {
    private Class tableClass;
    private String tableNameDB;
    private String tableNameJAVA;
    private String idNameDB;
    private String idNameJava;
    private Class idClass;
    private List<String> columnNameDBList = new ArrayList<>();
    private List<String> columnNameDBUpperList = new ArrayList<>();
    private List<String> columnNameJAVAList = new ArrayList<>();
    private List<Class> columnClassList = new ArrayList<>();
    private Map<String, Class> manyToOneDBColumnMapTableClass = new LinkedHashMap<>();
    private Map<String, Field> manyToOneDBColumnMapField = new LinkedHashMap<>();
    private Map<String, Class> manyToOneDBUpperColumnMapTableClass = new LinkedHashMap<>();
    private Map<String, Field> manyToOneDBUpperColumnMapField = new LinkedHashMap<>();
    private Map<String, Class> oneToManyDBColumnMapTableClass = new LinkedHashMap<>();
    private Map<String, Field> oneToManyDBColumnMapField = new LinkedHashMap<>();
    private String querySelect;
    private String queryInsert;
    private String queryInsertNoId;
    private String queryUpdate;
    private String queryDelete;
    private String queryMaxId;

    public CoreTableInfo(Class tableClass) throws Exception {
        this.tableClass = tableClass;
        Annotation tableAnnotation = tableClass.getAnnotation(Table.class);
        Class tableAnnotationClass = tableAnnotation.annotationType();
        Method tableAnnotationNameMethod = tableAnnotationClass.getMethod("name");
        this.tableNameDB = (String) tableAnnotationNameMethod.invoke(tableAnnotation);
        this.tableNameJAVA = tableClass.getSimpleName();


        Field[] fields = tableClass.getDeclaredFields();
        for (int count = 0; count < fields.length; count++) {
            Annotation columnAnnotation = fields[count].getAnnotation(Column.class);
            Annotation idAnnotation = fields[count].getAnnotation(Id.class);
            Annotation oneToManyAnnotation = fields[count].getAnnotation(OneToMany.class);
            Annotation manyToOneAnnotation = fields[count].getAnnotation(ManyToOne.class);
            Annotation joinColumnAnnotation = fields[count].getAnnotation(JoinColumn.class);
            String columnNameJava = fields[count].getName();
            if ((columnAnnotation != null) || (idAnnotation != null)) {
                String columnNameDB = CoreFiberRepositoryFunction.getNameJava2Db(columnNameJava);
                if (columnAnnotation != null) {
                    Class columnAnnotationClass = columnAnnotation.annotationType();
                    Method columnAnnotationNameMethod = columnAnnotationClass.getMethod("name");
                    String annotationNameValue = (String)columnAnnotationNameMethod.invoke(columnAnnotation);
                    if (!annotationNameValue.isEmpty()) {
                        columnNameDB = annotationNameValue;
                    }
                }
                columnNameDBList.add(columnNameDB);
                columnNameDBUpperList.add(columnNameDB.toUpperCase());
                columnNameJAVAList.add(columnNameJava);
                columnClassList.add(fields[count].getType());
            }
            if (idAnnotation != null) {
                idNameJava = fields[count].getName();
                idNameDB = CoreFiberRepositoryFunction.getNameJava2Db(idNameJava);
                idClass = fields[count].getType();
            }
            if (oneToManyAnnotation != null) {
                if (!List.class.isAssignableFrom(fields[count].getType())) {
                    throw new Exception(tableClass.getName() + "." + columnNameJava + " is annotated with @OneToMany. Expected listed entries - ('List<Table> " + columnNameJava + "').");
                }
                if (joinColumnAnnotation == null) {
                    throw new Exception(tableClass.getName() + "." + columnNameJava + " is annotated with @OneToMany. Expected @JoinColumn");
                }
                Method joinColumnNameMethod = joinColumnAnnotation.getClass().getMethod("name");
                String joinColumnNameValue = (String)joinColumnNameMethod.invoke(joinColumnAnnotation);
                oneToManyDBColumnMapField.put(joinColumnNameValue, fields[count]);
            }
            if (manyToOneAnnotation != null) {
                if (List.class.isAssignableFrom(fields[count].getType())) {
                    throw new Exception(tableClass.getName() + "." + columnNameJava + " is annotated with @ManyToOne. Expected single entry - ('Table " + columnNameJava + "').");
                }
                if (joinColumnAnnotation == null) {
                    throw new Exception(tableClass.getName() + "." + columnNameJava + " is annotated with @ManyToOne. Expected @JoinColumn");
                }
                Method joinColumnNameMethod = joinColumnAnnotation.getClass().getMethod("name");
                String joinColumnNameValue = (String)joinColumnNameMethod.invoke(joinColumnAnnotation);
                manyToOneDBColumnMapField.put(joinColumnNameValue, fields[count]);
                manyToOneDBUpperColumnMapField.put(joinColumnNameValue.toUpperCase(), fields[count]);
                columnNameDBList.add(joinColumnNameValue);
                columnNameDBUpperList.add(joinColumnNameValue.toUpperCase());
                columnNameJAVAList.add(columnNameJava);
                columnClassList.add(fields[count].getType());
            }
        }

        querySelect = processQuerySelect();
        queryInsert = processQueryInsert();
        queryInsertNoId = processQueryInsertNoId();
        queryUpdate = processQueryUpdate();
        queryDelete = processQueryDelete();
        queryMaxId = processQueryMaxId();
    }

    private String processQuerySelect() {
        return "SELECT * FROM " + tableNameDB;
    }

    private String processQueryMaxId() {
        return "SELECT MAX(" + idNameDB + ") FROM " + tableNameDB;
    }

    private String processQueryInsert() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("INSERT INTO " + tableNameDB + " (");
        for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
            queryBuilder.append(columnNameDBList.get(countColumn) + ",");
        }
        queryBuilder.setLength(queryBuilder.length() - 1);
        queryBuilder.append(") VALUES (");
        for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
            queryBuilder.append("?,");
        }
        queryBuilder.setLength(queryBuilder.length() - 1);
        queryBuilder.append(")");
        return queryBuilder.toString();
    }

    private String processQueryInsertNoId() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("INSERT INTO " + tableNameDB + " (");
        for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
            if (!(columnNameDBList.get(countColumn).equals(idNameDB))) {
                queryBuilder.append(columnNameDBList.get(countColumn) + ",");
            }
        }
        queryBuilder.setLength(queryBuilder.length() - 1);
        queryBuilder.append(") VALUES (");
        for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
            if (!(columnNameDBList.get(countColumn).equals(idNameDB))) {
                queryBuilder.append("?,");
            }
        }
        queryBuilder.setLength(queryBuilder.length() - 1);
        queryBuilder.append(")");
        return queryBuilder.toString();
    }

    private String processQueryUpdate() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("UPDATE " + tableNameDB + " SET ");
        for (int countColumn = 0; countColumn < columnNameDBList.size(); countColumn++) {
            if (!(columnNameDBList.get(countColumn).equals(idNameDB))) {
                queryBuilder.append(columnNameDBList.get(countColumn) + " = ?,");
            }
        }
        queryBuilder.setLength(queryBuilder.length() - 1);
        queryBuilder.append(" WHERE " + idNameDB + " = ?");
        return queryBuilder.toString();
    }

    private String processQueryDelete() {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("DELETE FROM " + tableNameDB + " WHERE " + idNameDB + " = ?");
        return queryBuilder.toString();
    }

    public static void initializeLinkedTable(Map<Class, CoreTableInfo> tableInfoMap) throws Exception {
        for (Map.Entry<Class, CoreTableInfo> entry : tableInfoMap.entrySet()) {
            CoreTableInfo tableInfo = entry.getValue();
            for (Map.Entry<String, Field> oneToManyFieldEntry : tableInfo.oneToManyDBColumnMapField.entrySet()) {
                Field targetField = oneToManyFieldEntry.getValue();
                Class tableClass = tryAssignOneToMany(tableInfoMap, tableInfo.tableClass, targetField);
                tableInfo.oneToManyDBColumnMapTableClass.put(oneToManyFieldEntry.getKey(), tableClass);
            }
            for (Map.Entry<String, Field> manyToOneFieldEntry : tableInfo.manyToOneDBColumnMapField.entrySet()) {
                Field targetField = manyToOneFieldEntry.getValue();
                Class tableClass = tryAssignManyToOne(tableInfoMap, tableInfo.tableClass, targetField);
                tableInfo.manyToOneDBColumnMapTableClass.put(manyToOneFieldEntry.getKey(), tableClass);
                tableInfo.manyToOneDBUpperColumnMapTableClass.put(manyToOneFieldEntry.getKey().toUpperCase(), tableClass);
            }
        }
    }

    private static Class tryAssignOneToMany(Map<Class, CoreTableInfo> tableInfoMap, Class targetTableClass, Field targetField) throws Exception {
        Object targetTableFakeInstance = targetTableClass.newInstance();
        Object targetTableFieldValue = targetField.get(targetTableFakeInstance);
        if (targetTableFieldValue == null) {
            targetField.set(targetTableFakeInstance, new ArrayList<>());
            targetTableFieldValue = targetField.get(targetTableFakeInstance);
        }
        Method targetTableFieldAddMethod = targetField.getType().getMethod("add", Object.class);
        Class result = null;
        for (Map.Entry<Class, CoreTableInfo> entry : tableInfoMap.entrySet()) {
            Class childTableClass = entry.getKey();
            Object fakeChildTableInstance = childTableClass.newInstance();
            try {
                targetTableFieldAddMethod.invoke(targetTableFieldValue, fakeChildTableInstance);
                result = entry.getKey();
                break;
            } catch (Exception e) {
                //ignore
            }
        }
        if (result == null) {
            throw new Exception("Could not associate the table class for '" + targetTableClass.getName() + "." + targetField.getName() + "'");
        }
        return result;
    }

    private static Class tryAssignManyToOne(Map<Class, CoreTableInfo> tableInfoMap, Class targetTableClass, Field targetField) throws Exception {
        targetField.setAccessible(true);
        Object targetTableFakeInstance = targetTableClass.newInstance();
        Class result = null;
        for (Map.Entry<Class, CoreTableInfo> entry : tableInfoMap.entrySet()) {
            Class childTableClass = entry.getKey();
            Object fakeChildTableInstance = childTableClass.newInstance();
            try {
                targetField.set(targetTableFakeInstance, fakeChildTableInstance);
                result = entry.getKey();
                break;
            } catch (Exception e) {
                //ignore
            }
        }
        if (result == null) {
            throw new Exception("Could not associate the table class for '" + targetTableClass.getName() + "." + targetField.getName() + "'");
        }
        return result;
    }
}
