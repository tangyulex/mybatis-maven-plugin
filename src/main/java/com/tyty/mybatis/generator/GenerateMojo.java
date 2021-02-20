package com.tyty.mybatis.generator;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.mysql.cj.MysqlType;
import com.tyty.mybatis.generator.plugin.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.config.*;
import org.mybatis.generator.internal.DefaultShellCallback;
import org.mybatis.generator.internal.util.StringUtility;
import org.mybatis.generator.plugins.EqualsHashCodePlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.DatabaseMetaData;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "")
    private String[] sources;

    @Parameter(defaultValue = "target")
    private String mapperLocation;

    @Parameter(defaultValue = "target")
    private String mapperXmlLocation;

    @Parameter(defaultValue = "pkg")
    private String targetPackage;

    @Parameter
    private String rootClass;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        try {
            Configuration config = makeConfiguration();
            DefaultShellCallback callback = new DefaultShellCallback(true);
            List<String> warnings = new ArrayList<>();
            MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config, callback, warnings);
            myBatisGenerator.generate(null);
            warnings.forEach(getLog()::warn);
        } catch (Exception e) {
            throw new MojoExecutionException("生成失败", e);
        }
    }

    public Configuration makeConfiguration() throws IOException {

        Map<String, Table> tables = new HashMap<>();
        for (String source : sources) {
            byte[] bytes = Files.readAllBytes(Paths.get(source));
            String content = new String(bytes, StandardCharsets.UTF_8);
            tables.putAll(parseTables(content));
        }

        Configuration configuration = new Configuration();
        /*
        conditional 这是默认值，这个模型与hierarchical模型相似,除了如果一个实体类只包含一个字段,则不会单独生成此实体类。因此,如果一个表的主键只有一个字段,那么不会为该字段生成单独的实体类,会将该字段合并到基本实体类中。
        flat 该模型为每一张表只生成一个实体类。这个实体类包含表中的所有字段。一般使用这个模型就够了。
        hierarchical 如果表有主键,那么该模型会产生一个单独的主键实体类,如果表还有BLOB字段，则会为表生成一个包含所有BLOB字段的单独的实体类,然后为所有其他的字段生成一个单独的实体类。MBG会在所有生成的实体类之间维护一个继承关系。
        显然这个模型比较复杂。
        */
        Context context = new Context(ModelType.FLAT, tables);
        configuration.addContext(context);

        context.setId("MySQLTables");
        //context.setTargetRuntime("MyBatis3DynamicSql");
        context.setTargetRuntime("MyBatis3");

        // PLUGINS
        addPluginConfiguration(context, ToStringPlugin.class.getName());
        /*addPluginConfiguration(context, ToStringPlugin.class.getName());*/
        addPluginConfiguration(context, JpaVersionPlugin.class.getName());
        addPluginConfiguration(context, EqualsHashCodePlugin.class.getName());
        addPluginConfiguration(context, PaginationPlugin.class.getName());
        addPluginConfiguration(context, OverIsMergeablePlugin.class.getName());
        addPluginConfiguration(context, DescribePropertyPlugin.class.getName());
        PluginConfiguration pluginConfiguration = addPluginConfiguration(context, SerializablePlugin.class.getName());
        parseProperty(pluginConfiguration, "suppressJavaInterface", "false");
        /*
        // 如果要使用这个插件，得用自定义的ToStringPlugin去掉静态属性
        addPluginConfiguration(context, DescribePropertyPlugin.class.getName());
        */

        CommentGeneratorConfiguration commentGeneratorConfiguration = new CommentGeneratorConfiguration();
        commentGeneratorConfiguration.setConfigurationType(CustomCommentGenerator.class.getName());
        context.setCommentGeneratorConfiguration(commentGeneratorConfiguration);
        parseProperty(commentGeneratorConfiguration, PropertyRegistry.COMMENT_GENERATOR_SUPPRESS_DATE, "false");
        parseProperty(commentGeneratorConfiguration, PropertyRegistry.COMMENT_GENERATOR_SUPPRESS_ALL_COMMENTS, "false");
        parseProperty(commentGeneratorConfiguration, PropertyRegistry.COMMENT_GENERATOR_ADD_REMARK_COMMENTS, "true");
        parseProperty(commentGeneratorConfiguration, PropertyRegistry.COMMENT_GENERATOR_DATE_FORMAT, "yyyy年MM月dd日");

        // POJO
        JavaModelGeneratorConfiguration javaModelGeneratorConfiguration = new JavaModelGeneratorConfiguration();
        context.setJavaModelGeneratorConfiguration(javaModelGeneratorConfiguration);
        javaModelGeneratorConfiguration.setTargetPackage(targetPackage + ".entity");
        javaModelGeneratorConfiguration.setTargetProject(mapperLocation);
        parseProperty(javaModelGeneratorConfiguration, PropertyRegistry.MODEL_GENERATOR_TRIM_STRINGS, "true");
        if (StringUtility.stringHasValue(rootClass)) {
            parseProperty(javaModelGeneratorConfiguration, PropertyRegistry.ANY_ROOT_CLASS, rootClass);
        }

        JavaTypeResolverConfiguration javaTypeResolverConfiguration = new JavaTypeResolverConfiguration();
        context.setJavaTypeResolverConfiguration(javaTypeResolverConfiguration);
        parseProperty(javaTypeResolverConfiguration, PropertyRegistry.TYPE_RESOLVER_FORCE_BIG_DECIMALS, "false");

        // XML
        SqlMapGeneratorConfiguration sqlMapGeneratorConfiguration = new SqlMapGeneratorConfiguration();
        context.setSqlMapGeneratorConfiguration(sqlMapGeneratorConfiguration);
        sqlMapGeneratorConfiguration.setTargetPackage("mybatis-mappers");
        sqlMapGeneratorConfiguration.setTargetProject(mapperXmlLocation);
        parseProperty(sqlMapGeneratorConfiguration, PropertyRegistry.ANY_ENABLE_SUB_PACKAGES, "true");

        // MAPPER
        JavaClientGeneratorConfiguration javaClientGeneratorConfiguration = new JavaClientGeneratorConfiguration();
        context.setJavaClientGeneratorConfiguration(javaClientGeneratorConfiguration);
        javaClientGeneratorConfiguration.setConfigurationType("XMLMAPPER");
        javaClientGeneratorConfiguration.setTargetPackage(targetPackage + ".mapper");
        javaClientGeneratorConfiguration.setTargetProject(mapperLocation);
        parseProperty(javaClientGeneratorConfiguration, PropertyRegistry.ANY_ENABLE_SUB_PACKAGES, "true");

        // TABLES
        tables.forEach((s, tb) -> {
            TableConfiguration tc = new TableConfiguration(context);
            tc.setTableName(tb.getTableName());
            tb.getColumns().stream().filter(col -> "YES".equals(col.get("IS_AUTOINCREMENT"))).findFirst().ifPresent(col -> {
                GeneratedKey gk = new GeneratedKey((String) col.get("COLUMN_NAME"), "MySql", true, null);
                tc.setGeneratedKey(gk);
            });
            context.addTableConfiguration(tc);
        });

        // 只是为了过校验
        JDBCConnectionConfiguration jdbcConnectionConfiguration = new JDBCConnectionConfiguration();
        jdbcConnectionConfiguration.setConnectionURL("x");
        jdbcConnectionConfiguration.setPassword("x");
        jdbcConnectionConfiguration.setUserId("x");
        jdbcConnectionConfiguration.setDriverClass("x");
        context.setJdbcConnectionConfiguration(jdbcConnectionConfiguration);

        return configuration;
    }

    private PluginConfiguration addPluginConfiguration(Context context, String configurationType) {
        PluginConfiguration pluginConfiguration = new PluginConfiguration();
        pluginConfiguration.setConfigurationType(configurationType);
        context.addPluginConfiguration(pluginConfiguration);
        return pluginConfiguration;
    }

    private Map<String, Table> parseTables(String content) {
        SQLStatementParser parser = new MySqlStatementParser(content);
        return parser.parseStatementList().stream()
                .filter(s -> s instanceof SQLCreateTableStatement)
                .map(cs -> mapToTable((SQLCreateTableStatement) cs))
                .collect(Collectors.toMap(Table::getTableName, table -> table));
    }

    private void parseProperty(PropertyHolder propertyHolder, String name, String value) {
        propertyHolder.addProperty(name, value);
    }

    public MavenProject getProject() {
        return project;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    private static Table mapToTable(SQLCreateTableStatement cs) {
        String tbName = cs.getName().getSimpleName();
        Table tb = new Table();
        tb.setTableName(tbName);

        // comment
        SQLExpr comment = cs.getComment();
        if (comment instanceof SQLCharExpr) {
            tb.setRemarks(((SQLCharExpr) comment).getText());
        }

        // columns
        List<Map<String, Object>> columns = cs.getTableElementList().stream()
                .filter(se -> se instanceof SQLColumnDefinition)
                .map((SQLTableElement se) -> toMap((SQLColumnDefinition) se, tbName))
                .collect(Collectors.toList());
        tb.setColumns(columns);

        // primaryKey
        Optional.ofNullable(cs.findPrimaryKey())
                .map(SQLUniqueConstraint::getColumns)
                .ifPresent(pks -> {
                    for (int i = 0; i < pks.size(); i++) {
                        SQLSelectOrderByItem pk = pks.get(i);
                        Map<String, Object> col = new HashMap<>();
                        col.put("COLUMN_NAME", pk.getExpr().toString());
                        col.put("KEY_SEQ", (short) (i + 1));
                        tb.getPrimaryKeys().add(col);
                    }
                });
        return tb;

    }

    private static Map<String, Object> toMap(SQLColumnDefinition sd, String tbName) {
        Map<String, Object> col = new HashMap<>();
        col.put("TABLE_NAME", tbName);
        col.put("DATA_TYPE", MysqlType.getByName(sd.getDataType().getName()).getJdbcType());
        List<SQLExpr> args = sd.getDataType().getArguments();
        col.put("COLUMN_SIZE", 0);
        col.put("DECIMAL_DIGITS", 0);
        if (args != null && args.size() > 0) {
            col.put("COLUMN_SIZE", Integer.parseInt(args.get(0).toString()));
            if (args.size() > 1) {
                col.put("DECIMAL_DIGITS", Integer.parseInt(args.get(1).toString()));
            }
        }
        col.put("COLUMN_NAME", sd.getName().getSimpleName());

        List<SQLColumnConstraint> constraints = sd.getConstraints();
        col.put("NULLABLE", DatabaseMetaData.columnNullableUnknown);
        if (constraints != null) {
            if (constraints.stream().anyMatch(s -> s instanceof SQLNotNullConstraint)) {
                col.put("NULLABLE", DatabaseMetaData.columnNoNulls);
            } else if (constraints.stream().anyMatch(s -> s instanceof SQLNullConstraint)) {
                col.put("NULLABLE", DatabaseMetaData.columnNullable);
            }
        }

        String sdStr = sd.toString();
        if (sd.getComment() instanceof SQLCharExpr) {
            String text = ((SQLCharExpr) sd.getComment()).getText();
            col.put("REMARKS", text);
            sdStr = sdStr.replaceAll("'" + text + "'|\"" + text + "\"", "");
        }
        if (sd.getDefaultExpr() instanceof SQLCharExpr) {
            col.put("COLUMN_DEF", ((SQLCharExpr) sd.getDefaultExpr()).getText());
        }
        if (sdStr.contains("AUTO_INCREMENT")) {
            col.put("IS_AUTOINCREMENT", "YES");
        }
        //col.put("IS_GENERATEDCOLUMN", "YES");
        return col;
    }
}






