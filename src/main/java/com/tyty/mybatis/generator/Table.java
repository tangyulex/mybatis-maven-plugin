package com.tyty.mybatis.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Table
 *
 * @author lextang
 */
public class Table {

    private String tableName;
    private String remarks;
    private List<Map<String, Object>> primaryKeys = new ArrayList<>();
    private List<Map<String, Object>> columns = new ArrayList<>();

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<Map<String, Object>> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<Map<String, Object>> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<Map<String, Object>> getColumns() {
        return columns;
    }

    public void setColumns(List<Map<String, Object>> columns) {
        this.columns = columns;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
