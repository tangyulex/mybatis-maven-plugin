package com.tyty.mybatis.generator.plugin;

import org.codehaus.plexus.util.StringUtils;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.VisitableElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JpaVersionPlugin extends PluginAdapter {

    @Override
    public boolean validate(List<String> warnings) {
        // this plugin is always valid
        return true;
    }

    @Override
    public boolean sqlMapInsertElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {

        String s = "#{jpaVersion,jdbcType=INTEGER}";

        List<VisitableElement> elements = element.getElements();
        for (VisitableElement ve : elements) {
            if (!(ve instanceof XmlElement)) {
                continue;
            }
            if (StringUtils.contains(ve.toString(), s)) {

            }
        }

        return super.sqlMapInsertElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapInsertSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        List<VisitableElement> elements = element.getElements();
        // 找到两个trim节点
        XmlElement trim1 = null;
        XmlElement trim2 = null;
        for (VisitableElement ve : elements) {
            if (!(ve instanceof XmlElement) || !"trim".equals(((XmlElement) ve).getName())) {
                continue;
            }
            XmlElement xe = (XmlElement) ve;
            if (!"trim".equals(xe.getName())) {
                continue;
            }
            if (trim1 == null) {
                trim1 = xe;
            } else {
                trim2 = xe;
                break;
            }
        }
        if (trim1 == null || trim2 == null) {
            return true;
        }

        // 处理第一个trim节点
        XmlElement finalTrim = trim2;
        findJpaVersionElement4InsertSelective(trim1).ifPresent(xe -> {
            xe.getAttributes().set(0, new Attribute("test", "true"));
            // 处理第二个trim节点
            findJpaVersionElement4InsertSelective(finalTrim).ifPresent(xe2 -> {
                xe2.getAttributes().set(0, new Attribute("test", "true"));
                xe2.getElements().set(0, new TextElement("0,"));
            });
        });

        return true;
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleSelectiveElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByExampleWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleWithBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByExampleWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByExampleWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByPrimaryKeySelectiveElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByPrimaryKeyWithBLOBsElementGenerated(element, introspectedTable);
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        return super.sqlMapUpdateByPrimaryKeyWithoutBLOBsElementGenerated(element, introspectedTable);
    }

    /**
     * 找到insertSelective的jpaVersion节点
     */
    private Optional<XmlElement> findJpaVersionElement4InsertSelective(XmlElement trimElement) {
        for (VisitableElement visitableElement : trimElement.getElements()) {
            if (!(visitableElement instanceof XmlElement)) {
                continue;
            }
            XmlElement xe = (XmlElement) visitableElement;
            if (!"if".equals(xe.getName())) {
                continue;
            }
            // 将jpaVersion的判断条件调整为true
            String value = xe.getAttributes().get(0).getValue();
            if (value.matches(".*[ .]*jpaVersion != null .*|.*[ .]*jpaVersion != null")) {
                return Optional.of(xe);
            }
        }
        return Optional.empty();
    }

    public static void main(String[] args) {
        System.out.println(".jpaVersion != null".matches("\\.jpaVersion != null"));
        System.out.println("jpaVersion != null".matches(".*[ .]*jpaVersion != null .*|.*[ .]*jpaVersion != null"));
    }
}
