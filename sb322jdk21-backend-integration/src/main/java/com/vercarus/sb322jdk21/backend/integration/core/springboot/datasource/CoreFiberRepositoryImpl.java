package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreConfig;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.GeneratedValue;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class CoreFiberRepositoryImpl implements InvocationHandler {

    private Class targetSubclass;

    public CoreFiberRepositoryImpl(Class targetSubclass) {
        this.targetSubclass = targetSubclass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class repositoryClass = (Class) targetSubclass;
        Long strandId = Thread.currentThread().threadId();
        Connection connection = null;
        FiberConnectionReaderMap connectionReaderMap = null;
        PreparedStatement fiberPreparedStatement = null;
        Object result = null;
        try {
            Class tableClass = CoreConfig.repositoryTableMap.get(repositoryClass);
            CoreTableInfo tableInfo = CoreConfig.tableInfoMap.get(tableClass);
            if (tableInfo == null) {
                throw new Exception("Could not retrieve table (" + tableClass.getName() + ") mapping info. Please check the value of 'CoreDataSourceConfig.dbutilPackage'.");
            }
            Class elementType = null;
            Annotation queryElementTypeAnnotation = method.getAnnotation(QueryElementType.class);
            if (queryElementTypeAnnotation != null) {
                Class queryElementTypeAnnotationType = queryElementTypeAnnotation.annotationType();
                Method queryElementTypeAnnotationValueMethod = queryElementTypeAnnotationType.getMethod("value");
                elementType = (Class) queryElementTypeAnnotationValueMethod.invoke(queryElementTypeAnnotation);
            }
            List<Object> rowsForInsert = new LinkedList<>();
            List<Object> rowsForUpdate = new LinkedList<>();
            List<Object> rowsForDelete = new LinkedList<>();
            String methodName = method.getName();
            StringBuilder queryBuilder = new StringBuilder();
            Class returnClass = method.getReturnType();

            List<String> queryDbColumnNameList = new ArrayList<>();
            if (methodName.toLowerCase().startsWith("find")) {
                Annotation queryAnnotation = method.getAnnotation(Query.class);
                if (queryAnnotation == null) {
                    queryBuilder.append(tableInfo.getQuerySelect());
                    Parameter[] parameters = method.getParameters();
                    if (method.getParameterCount() > 0) {
                        queryBuilder.append(" WHERE (");
                    }
                    for (int countParameter = 0; countParameter < parameters.length; countParameter++) {
                        String parameterName = parameters[countParameter].getName();
                        Class parameterType = parameters[countParameter].getType();
                        Class subParameterType = null;
                        if (Iterable.class.isAssignableFrom(parameterType)) {
                            if (((Iterable) args[0]).iterator().hasNext()) {
                                subParameterType = ((Iterable) args[0]).iterator().next().getClass();
                            }
                        }
                        String dbColumnName = null;
                        if (tableInfo.getColumnNameJAVAList().contains(parameterName)) {
                            dbColumnName = CoreFiberRepositoryFunction.getNameJava2Db(parameterName);
                            int columnIndex = tableInfo.getColumnNameJAVAList().indexOf(dbColumnName);
                        } else {
                            if ((tableInfo.getIdClass().equals(parameterType)) || (tableInfo.getIdClass().equals(subParameterType))) {
                                //Assuming it is Id based on variable Type
                                dbColumnName = tableInfo.getIdNameDB();
                            } else {
                                String exceptionMessage = "Unable to automatically map '" + parameterName + "' of type='" + parameters[countParameter].getType().getName() + "'";
                                if (subParameterType != null) {
                                    exceptionMessage += ", sub-type = '" + subParameterType + "'";
                                }
                                exceptionMessage += ".\nPlease base the 'parameter name' from the table class 'variable name' or use @Query to define the query statement manually.";
                                throw new Exception(exceptionMessage);
                            }
                        }
                        if (Iterable.class.isAssignableFrom(parameterType)) {
                            queryBuilder.append(dbColumnName + " " + CoreFiberRepositoryFunction.getTransformedInClause(subParameterType) + ",");
                        } else {
                            queryBuilder.append(dbColumnName + " = ?,");
                        }
                        queryDbColumnNameList.add(dbColumnName);
                    }
                    if (method.getParameterCount() > 0) {
                        queryBuilder.setLength(queryBuilder.length() - 1);
                    }
                    if (method.getParameterCount() > 0) {
                        queryBuilder.append(")");
                    }
                } else {
                    Class queryAnnotationType = queryAnnotation.annotationType();
                    Method queryAnnotationValueMethod = queryAnnotationType.getMethod("value");
                    queryBuilder.append((String) queryAnnotationValueMethod.invoke(queryAnnotation));
                }
            } else if (("saveAll".equals(methodName)) && (method.getParameterCount() == 1)) {
                Iterable rowCollection = (Iterable) args[0];
                Iterator iterator = rowCollection.iterator();
                while (iterator.hasNext()) {
                    Object row = iterator.next();
                    Field idField = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
                    idField.setAccessible(true);
                    Annotation generatedValueAnnotation = idField.getAnnotation(GeneratedValue.class);
                    Object idValue = idField.get(row);
                    if (generatedValueAnnotation != null) {
                        if (idValue == null) {
                            rowsForInsert.add(row);
                        } else {
                            rowsForUpdate.add(row);
                        }
                    } else {
                        rowsForInsert.add(row);
                        rowsForUpdate.add(row);
                    }
                }
            } else if (("save".equals(methodName)) && (method.getParameterCount() == 1)) {
                Object row = args[0];
                Field idField = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
                idField.setAccessible(true);
                Annotation generatedValueAnnotation = idField.getAnnotation(GeneratedValue.class);
                Object idValue = idField.get(row);
                if (generatedValueAnnotation != null) {
                    if (idValue == null) {
                        rowsForInsert.add(row);
                    } else {
                        rowsForUpdate.add(row);
                    }
                } else {
                    rowsForInsert.add(row);
                    rowsForUpdate.add(row);
                }
            } else if (("deleteAll".equals(methodName)) && (method.getParameterCount() == 1)) {
                Iterable rowCollection = (Iterable) args[0];
                Iterator iterator = rowCollection.iterator();
                while (iterator.hasNext()) {
                    Object row = iterator.next();
                    Field idField = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
                    idField.setAccessible(true);
                    Object idValue = idField.get(row);
                    rowsForDelete.add(idValue);
                }
            } else if (("delete".equals(methodName)) && (method.getParameterCount() == 1)) {
                Object row = args[0];
                Field idField = row.getClass().getDeclaredField(tableInfo.getIdNameJava());
                idField.setAccessible(true);
                Object idValue = idField.get(row);
                rowsForDelete.add(idValue);
            } else if (("deleteById".equals(methodName)) && (method.getParameterCount() == 1)) {
                Object idValue = args[0];
                rowsForDelete.add(idValue);
            } else {
                // use the function's @Query Annotation
                Annotation queryAnnotation = method.getAnnotation(Query.class);
                if (queryAnnotation == null) {
                    throw new Exception("@Query is required for functions that is not 'find'");
                }
                Class queryAnnotationType = queryAnnotation.annotationType();
                Method queryAnnotationValueMethod = queryAnnotationType.getMethod("value");
                queryBuilder.append((String) queryAnnotationValueMethod.invoke(queryAnnotation));
            }
            String query = queryBuilder.toString();
            if (query.toLowerCase().startsWith("select")) {
                connectionReaderMap = CoreConfig.fiberConnectionGet_reader();
                connection = connectionReaderMap.getFiberConnection();
            } else {
                connection = CoreConfig.fiberConnectionGet();
            }
            if (query.toUpperCase().contains("IN (?)")) {
                List<Class> arrayTypeList = new LinkedList<>();
                for (int countArgs = 0; countArgs < args.length; countArgs++) {
                    if (Iterable.class.isAssignableFrom(args[countArgs].getClass())) {
                        Iterator iter = ((Iterable) args[countArgs]).iterator();
                        if (iter.hasNext()) {
                            arrayTypeList.add(iter.next().getClass());
                        } else {
                            arrayTypeList.add(String.class);
                        }
                    } else if (args[countArgs].getClass().isArray()) {
                        if (Array.getLength(args[countArgs]) > 0) {
                            arrayTypeList.add(Array.get(args[countArgs], 0).getClass());
                        } else {
                            arrayTypeList.add(String.class);
                        }
                    }
                }
                int countArrayType = 0;
                Properties clientInfo = connection.getClientInfo();
                while (query.toUpperCase().contains("IN (?)")) {
                    String queryUpper = query.toUpperCase();
                    query = query.substring(0, queryUpper.indexOf("IN (?)"))
                            + CoreFiberRepositoryFunction.getTransformedInClause(arrayTypeList.get(countArrayType))
                            + query.substring(queryUpper.indexOf("IN (?)") + 6);
                    countArrayType++;
                }
            }
            if (query.toUpperCase().startsWith("SELECT ")) {
                fiberPreparedStatement = connection.prepareStatement(query);
                if (args != null) {
                    int parameterIndex = 1;
                    for (int countArgs = 0; countArgs < args.length; countArgs++) {
                        CoreFiberRepositoryFunction.assignPreparedStatement(connection, fiberPreparedStatement, parameterIndex, args[countArgs]);
                        parameterIndex++;
                    }
                }
                ResultSet resultSet = null;
                try {
                    resultSet = fiberPreparedStatement.executeQuery();
                    result = CoreFiberRepositoryFunction.createResultObject(null, connection, returnClass, elementType, tableInfo, resultSet, null);
                } finally {
                    if (resultSet != null) {
                        resultSet.close();
                    }
                }
            } else if ((rowsForInsert.size() > 0) || (rowsForUpdate.size() > 0)) {
                if (rowsForInsert.size() > 0) {
                    Boolean withId = (rowsForUpdate.size() > 0);
                    if (withId) {
                        fiberPreparedStatement = connection.prepareStatement(tableInfo.getQueryInsert(), Statement.RETURN_GENERATED_KEYS);
                    } else {
                        fiberPreparedStatement = connection.prepareStatement(tableInfo.getQueryInsertNoId(), Statement.RETURN_GENERATED_KEYS);
                    }
                    for (int countRows = 0; countRows < rowsForInsert.size(); countRows += CoreConfig.batchSize) {
                        for (int countSubRows = countRows; countSubRows < countRows + CoreConfig.batchSize & countSubRows < rowsForInsert.size(); countSubRows++) {
                            Object row = rowsForInsert.get(countSubRows);
                            CoreFiberRepositoryFunction.prepareRowForInsert(connection, fiberPreparedStatement, tableInfo, row, withId);
                            fiberPreparedStatement.addBatch();
                        }
                        fiberPreparedStatement.executeBatch();
                        connection.commit();
                        ResultSet generatedIdResultSet = null;
                        try {
                            generatedIdResultSet = fiberPreparedStatement.getGeneratedKeys();
                            CoreFiberRepositoryFunction.assignGeneratedIdToRowObject(connection, tableInfo, rowsForInsert, countRows, generatedIdResultSet, CoreConfig.batchSize);
                            if ((rowsForInsert.size() == 1) && (!Iterable.class.isAssignableFrom(returnClass))) {
                                result = rowsForInsert.get(0);
                            } else {
                                result = rowsForInsert;
                            }
                        } catch (Exception e) {
                            //Suppress non-auto generated id duplicate error for insertion
                            if (rowsForUpdate.size() == 0) {
                                throw e;
                            }
                        } finally {
                            if (generatedIdResultSet != null) {
                                generatedIdResultSet.close();
                            }
                        }
                    }
                }
                if (rowsForUpdate.size() > 0) {
                    fiberPreparedStatement = connection.prepareStatement(tableInfo.getQueryUpdate());
                    for (int countRows = 0; countRows < rowsForUpdate.size(); countRows += CoreConfig.batchSize) {
                        for (int countSubRows = countRows; countSubRows < countRows + CoreConfig.batchSize & countSubRows < rowsForUpdate.size(); countSubRows++) {
                            Object row = rowsForUpdate.get(countSubRows);
                            CoreFiberRepositoryFunction.prepareRowForUpdate(connection, fiberPreparedStatement, tableInfo, row);
                            fiberPreparedStatement.addBatch();
                        }
                        fiberPreparedStatement.executeBatch();
                        connection.commit();
                        if ((rowsForUpdate.size() == 1) && (!Iterable.class.isAssignableFrom(returnClass))) {
                            result = rowsForUpdate.get(0);
                        } else {
                            result = rowsForUpdate;
                        }
                    }
                }
            } else if (rowsForDelete.size() != 0) {
                fiberPreparedStatement = connection.prepareStatement(tableInfo.getQueryDelete());
                for (int countRows = 0; countRows < rowsForDelete.size(); countRows += CoreConfig.batchSize) {
                    for (int countSubRows = countRows; countSubRows < countRows + CoreConfig.batchSize & countSubRows < rowsForDelete.size(); countSubRows++) {
                        Object idValue = rowsForDelete.get(countSubRows);
                        CoreFiberRepositoryFunction.prepareRowForDelete(connection, fiberPreparedStatement, tableInfo, idValue);
                        fiberPreparedStatement.addBatch();
                    }
                    fiberPreparedStatement.executeBatch();
                    connection.commit();
                }
            } else if (query.length() > 0) {
                fiberPreparedStatement = connection.prepareStatement(query);
                int parameterIndex = 1;
                if (args != null) {
                    for (int countArgs = 0; countArgs < args.length; countArgs++) {
                        CoreFiberRepositoryFunction.assignPreparedStatement(connection, fiberPreparedStatement, parameterIndex, args[countArgs]);
                        parameterIndex++;
                    }
                }
                fiberPreparedStatement.execute();
                connection.commit();
            }
            return result;
        } finally {
            if (fiberPreparedStatement != null) {
                fiberPreparedStatement.close();
            }
            if (connection != null) {
                if (connectionReaderMap != null) {
                    CoreConfig.fiberConnectionClose_reader(connectionReaderMap);
                } else {
                    CoreConfig.fiberConnectionClose(connection);
                }
            }
        }
    }
}
