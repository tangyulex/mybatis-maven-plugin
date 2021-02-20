package com.tyty.mybatis.generator.plugin;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.Field;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import java.util.ArrayList;
import java.util.List;

/**
 * 添加属性名常量
 */
public class DescribePropertyPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    private List<Field> fields = new ArrayList<>();
    //private List<IntrospectedColumn> columns = new ArrayList<>();

    @Override
    public boolean modelFieldGenerated(Field field, TopLevelClass topLevelClass, IntrospectedColumn introspectedColumn, IntrospectedTable introspectedTable, ModelClassType modelClassType) {
        fields.add(field);
        //columns.add(introspectedColumn);
        return true;
    }

    @Override
    public boolean modelBaseRecordClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        /*InnerClass innerClass = new InnerClass("PropertyName");
        innerClass.setVisibility(JavaVisibility.PUBLIC);
        innerClass.setStatic(true);
        innerClass.setFinal(true);*/
        //Iterator<IntrospectedColumn> columnIterator = columns.iterator();
        for (Field field : fields) {
            /*Field fieldName = new Field(columnIterator.next().getActualColumnName().toUpperCase()
                    , FullyQualifiedJavaType.getStringInstance());*/
            Field fieldName = new Field("__" + field.getName()
                    , FullyQualifiedJavaType.getStringInstance());
            fieldName.setVisibility(JavaVisibility.PUBLIC);
            fieldName.setStatic(true);
            fieldName.setFinal(true);
            fieldName.setTransient(true);
            fieldName.setInitializationString("\"" + field.getName() + "\"");
            topLevelClass.addField(fieldName);
            //innerClass.addField(fieldName);
        }
        //topLevelClass.addInnerClass(innerClass);
        fields.clear();
        //columns.clear();
        return true;
    }
}
