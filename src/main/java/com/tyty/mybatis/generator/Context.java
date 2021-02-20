package com.tyty.mybatis.generator;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.JavaTypeResolver;
import org.mybatis.generator.api.ProgressCallback;
import org.mybatis.generator.config.ModelType;
import org.mybatis.generator.config.TableConfiguration;
import org.mybatis.generator.internal.ObjectFactory;
import org.mybatis.generator.internal.db.DatabaseIntrospector;

import java.lang.reflect.Field;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

import static org.mybatis.generator.internal.util.StringUtility.composeFullyQualifiedTableName;
import static org.mybatis.generator.internal.util.messages.Messages.getString;

public class Context extends org.mybatis.generator.config.Context {

    private Map<String, Table> tables;

    public Context(ModelType defaultModelType, Map<String, Table> tables) {
        super(defaultModelType);
        this.tables = tables;
    }

    @Override
    public void introspectTables(ProgressCallback callback, List<String> warnings, Set<String> fullyQualifiedTableNames) throws SQLException, InterruptedException {

        List<IntrospectedTable> introspectedTables = getIntrospectedTables();

        JavaTypeResolver javaTypeResolver = ObjectFactory
                .createJavaTypeResolver(this, warnings);

        callback.startTask(getString("Progress.0")); //$NON-NLS-1$

        DatabaseIntrospector databaseIntrospector = new DatabaseIntrospector(
                this, createDatabaseMetaData(), javaTypeResolver, warnings);

        for (TableConfiguration tc : getTableConfigurations()) {
            String tableName = composeFullyQualifiedTableName(tc.getCatalog(), tc
                    .getSchema(), tc.getTableName(), '.');

            if (fullyQualifiedTableNames != null
                    && fullyQualifiedTableNames.size() > 0
                    && !fullyQualifiedTableNames.contains(tableName)) {
                continue;
            }

            if (!tc.areAnyStatementsEnabled()) {
                warnings.add(getString("Warning.0", tableName)); //$NON-NLS-1$
                continue;
            }

            callback.startTask(getString("Progress.1", tableName)); //$NON-NLS-1$
            List<IntrospectedTable> tables = databaseIntrospector
                    .introspectTables(tc);

            if (tables != null) {
                introspectedTables.addAll(tables);
            }

            callback.checkCancel();
        }
    }

    private List<IntrospectedTable> getIntrospectedTables() {
        List<IntrospectedTable> introspectedTables = new ArrayList<>();
        try {
            Field field = org.mybatis.generator.config.Context.class.getDeclaredField("introspectedTables");
            field.setAccessible(true);
            field.set(this, introspectedTables);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return introspectedTables;
    }

    private DatabaseMetaData createDatabaseMetaData() {

        return new DatabaseMetaDataAdapter() {

            @Override
            public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
                Table tb = tables.get(table);
                List<Map<String, Object>> primaryKeys = tb.getPrimaryKeys();
                return new ResultSetAdapter(primaryKeys);
            }

            @Override
            public String getSearchStringEscape() throws SQLException {
                return "";
            }

            @Override
            public boolean storesUpperCaseIdentifiers() throws SQLException {
                return false;
            }

            @Override
            public boolean storesLowerCaseIdentifiers() throws SQLException {
                return false;
            }

            @Override
            public ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern) throws SQLException {
                Table tb = tables.get(tableNamePattern);
                return new ResultSetAdapter(tb.getColumns()) {
                    @Override
                    public ResultSetMetaData getMetaData() throws SQLException {
                        return new ResultSetMetaDataAdapter() {
                            @Override
                            public int getColumnCount() throws SQLException {
                                return 1;
                            }

                            @Override
                            public String getColumnName(int column) throws SQLException {
                                return "IS_AUTOINCREMENT";
                            }
                        };
                    }
                };
            }

            @Override
            public ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types) throws SQLException {
                Table table = tables.get(tableNamePattern);
                List<Map<String, Object>> rows = new ArrayList<>();
                Map<String, Object> row = new HashMap<>();
                row.put("REMARKS", table.getRemarks());
                rows.add(row);
                return new ResultSetAdapter(rows);
            }
        };
    }
}
