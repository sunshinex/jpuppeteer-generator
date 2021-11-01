package jpuppeteer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.util.*;

@Mojo(name = "gen")
public class GenMojo extends AbstractMojo {

    private static final String ENUM_CLASS = "CDPEnum";

    private static final String CRLF = "\r\n";

    @Parameter
    private File baseDir;

    @Parameter
    private String pkg;

    @Parameter
    private String connectionClassName;

    @Parameter
    private File browserProtocol;

    @Parameter
    private File jsProtocol;

    private Map<String, Type> typeMap = new HashMap<>();

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Type.pkg = pkg;
            getLog().info("parse browser protocol");
            Document browserDoc = readProtocol(browserProtocol);
            getLog().info("parse javascript protocol");
            Document jsDoc = readProtocol(jsProtocol);
            if (!browserDoc.version.equals(jsDoc.version)) {
                throw new MojoFailureException("browser and js protocol not match");
            }
            //生成枚举接口
            createEnumInterface();
            browserDoc.domains.addAll(jsDoc.domains);
            getLog().info("protocol version " + browserDoc.version.major + "." + browserDoc.version.minor);
            //处理types的引用
            for (Domain domain : browserDoc.domains) {
                if (isNotEmpty(domain.types)) {
                    for (Type type : domain.types) {
                        type.domain = domain;
                        typeMap.put(domain.domain + "." + type.id, type);
                    }
                }
            }
            Map<String, Type> events = new LinkedHashMap<>();
            for (Domain domain : browserDoc.domains) {
                //生成type
                if (isNotEmpty(domain.types)) {
                    for (Type type : domain.types) {
                        createType(domain, type);
                    }
                }
                //生成接口
                createCommand(domain);
                //生成事件
                events.putAll(createEvent(domain));
            }
            createEventEnum(events);
            getLog().info("generate success");
        } catch (MojoExecutionException | MojoFailureException e0) {
            throw e0;
        } catch (Throwable cause) {
            throw new MojoExecutionException(cause.getMessage(), cause);
        }
    }

    private static boolean isNotEmpty(Collection collection) {
        return collection != null && collection.size() > 0;
    }

    private static boolean isEmpty(Collection collection) {
        return !isNotEmpty(collection);
    }

    private void writeFile(String filename, StringBuffer sb) throws Exception {
        getLog().info("write file:" + filename);
        File java = new File(filename);
        if (java.exists()) {
            java.delete();
        }
        java.createNewFile();
        OutputStream os = new FileOutputStream(java);
        os.write(sb.toString().getBytes("UTF-8"));
        os.flush();
        os.close();
    }

    private void createEnumInterface() throws Exception {
        String pkg = this.pkg;
        String dirName = baseDir + "/" + pkg.replace(".", "/");
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("package ");
        sb.append(pkg);
        sb.append(";");
        sb.append(CRLF);
        sb.append(CRLF);

        sb.append("public interface ");
        sb.append(ENUM_CLASS);
        sb.append(" {");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    String value();");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("}");

        writeFile(dirName + "/" + ENUM_CLASS + ".java", sb);
    }

    public void createEventEnum(Map<String, Type> values) throws Exception {
        String pkg = this.pkg;
        String dirName = baseDir + "/" + pkg.replace(".", "/");
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String enumClassName = "CDPEventType";
        StringBuffer sb = new StringBuffer();
        sb.append("package ");
        sb.append(pkg);
        sb.append(";");
        sb.append(CRLF);
        sb.append(CRLF);

        sb.append("public enum ");
        sb.append(enumClassName);
        sb.append(" {");
        sb.append(CRLF);
        sb.append(CRLF);
        for(Map.Entry<String, Type> entry : values.entrySet()) {
            Type clazz = entry.getValue();
            String name = entry.getKey().toUpperCase().replaceAll("[^a-zA-Z0-9_]", "_");
            if (clazz != null && StringUtils.isNotEmpty(clazz.description)) {
                sb.append("    /**");
                sb.append(CRLF);
                sb.append("    * ");
                sb.append(clazz.description.replaceAll("(\r\n|\n|\r)", " "));
                sb.append(CRLF);
                sb.append("    * @see ");
                sb.append(clazz.getPackage() + "." + clazz.id);
                sb.append(CRLF);
                sb.append("    */");
                sb.append(CRLF);
            }
            sb.append("    ");
            sb.append(name);
            sb.append("(\"");
            sb.append(entry.getKey());
            sb.append("\"");
            sb.append(", ");
            if (clazz != null) {
                sb.append(clazz.getPackage() + "." + clazz.id);
                sb.append(".class");
            } else {
                sb.append("null");
            }
            sb.append(")");
            sb.append(",");
            sb.append(CRLF);
            sb.append(CRLF);
        }
        sb.append("    ;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    private String name;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    private Class clazz;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    ");
        sb.append(enumClassName);
        sb.append("(String name, Class clazz) {");
        sb.append(CRLF);
        sb.append("        this.name = name;");
        sb.append(CRLF);
        sb.append("        this.clazz = clazz;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    public String getName() {");
        sb.append(CRLF);
        sb.append("        return name;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    public Class getClazz() {");
        sb.append(CRLF);
        sb.append("        return clazz;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    public static ");
        sb.append(enumClassName);
        sb.append(" findByName(String name) {");
        sb.append(CRLF);
        sb.append("        for(");
        sb.append(enumClassName);
        sb.append(" val : values()) {");
        sb.append(CRLF);
        sb.append("            if (val.name.equals(name)) return val;");
        sb.append(CRLF);
        sb.append("        }");
        sb.append(CRLF);
        sb.append("        return null;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append("}");

        writeFile(dirName + "/" + enumClassName + ".java", sb);
    }

    public Map<String, Type> createEvent(Domain domain) throws Exception {
        if (isEmpty(domain.events)) {
            return new HashMap<>();
        }
        //生成class
        Map<String, Type> values = new LinkedHashMap<>();
        for(Event event : domain.events) {
            if (isNotEmpty(event.parameters)) {
                Type param = new Type();
                param.id = StringUtils.removeEndIgnoreCase(StringUtils.capitalize(event.name), "event") + "Event";
                param.description = event.description;
                param.properties = event.parameters;
                param.type = TypeType.OBJECT;
                param.domain = domain;
                createType(domain, param);
                values.put(domain.domain + "." + event.name, param);
            } else {
                values.put(domain.domain + "." + event.name, null);
            }
        }
        return values;
    }

    public void createCommand(Domain domain) throws Exception {
        String pkg = this.pkg + ".domain";
        String dirName = baseDir + "/" + pkg.replace(".", "/");
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        StringBuffer sb = new StringBuffer();

        sb.append("package ");
        sb.append(pkg);
        sb.append(";");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("/**");
        if (domain.experimental) {
            sb.append(CRLF);
            sb.append("* experimental");
        }
        sb.append(CRLF);
        sb.append("*/");
        if (domain.deprecated) {
            sb.append(CRLF);
            sb.append("@java.lang.Deprecated");
        }
        sb.append(CRLF);
        sb.append("public class ");
        sb.append(domain.domain);
        sb.append(" {");
        sb.append(CRLF);
        sb.append(CRLF);
        //属性及构造方法

        String varName;
        sb.append("    private ");
        sb.append(connectionClassName);
        sb.append(" connection;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    public ");
        sb.append(domain.domain);
        sb.append("(");
        sb.append(connectionClassName);
        sb.append(" connection");
        sb.append(") {");
        sb.append(CRLF);
        sb.append("        this.connection = connection;");
        sb.append(CRLF);
        sb.append("    }");
        varName = "connection";
        //接口方法
        for(Command command : domain.commands) {
            sb.append(CRLF);
            sb.append(CRLF);
            sb.append("    /**");
            if (StringUtils.isNotEmpty(command.description)) {
                sb.append(CRLF);
                sb.append("    * ");
                sb.append(command.description.replaceAll("(\r\n|\n|\r)", " "));
            }
            if (command.experimental) {
                sb.append(CRLF);
                sb.append("    * experimental");
            }
            sb.append(CRLF);
            sb.append("    */");
            if (command.deprecated) {
                sb.append(CRLF);
                sb.append("    @java.lang.Deprecated");
            }
            sb.append(CRLF);
            sb.append("    public ");
            Type ret = null;
            Type param = null;
            if (isEmpty(command.returns)) {
                sb.append("io.netty.util.concurrent.Future");
            } else {
                //生成返回值结构体
                ret = new Type();
                ret.id = StringUtils.capitalize(command.name) + "Response";
                ret.properties = command.returns;
                ret.type = TypeType.OBJECT;
                ret.domain = domain;
                createType(domain, ret);
                sb.append("io.netty.util.concurrent.Future<");
                sb.append(ret.getPackage() + "." + ret.id);
                sb.append(">");
            }
            sb.append(" ");
            sb.append(command.name);
            sb.append("(");
            //处理参数
            if (isNotEmpty(command.parameters)) {
                param = new Type();
                param.id = StringUtils.capitalize(command.name) + "Request";
                param.properties = command.parameters;
                param.type = TypeType.OBJECT;
                param.domain = domain;
                createType(domain, param);
                sb.append(param.getPackage() + "." + param.id + " request");
            }
            sb.append(")");
            sb.append(" {");
            sb.append(CRLF);

            if (isNotEmpty(command.returns)) {
                sb.append("        return ");
                sb.append(varName);
                sb.append(".send(");
                sb.append("\"");
                sb.append(domain.domain);
                sb.append(".");
                sb.append(command.name);
                sb.append("\"");
                sb.append(", ");
                if (isNotEmpty(command.parameters)) {
                    sb.append("request, ");
                } else {
                    sb.append("null, ");
                }
                sb.append(ret.getPackage() + "." + ret.id + ".class");
                sb.append(")");
            } else {
                sb.append("        return ");
                sb.append(varName);
                sb.append(".send(");
                sb.append("\"");
                sb.append(domain.domain);
                sb.append(".");
                sb.append(command.name);
                sb.append("\"");
                sb.append(", ");
                if (isNotEmpty(command.parameters)) {
                    sb.append("request");
                } else {
                    sb.append("null");
                }
                sb.append(")");
            }
            sb.append(";");
            sb.append(CRLF);
            sb.append("    }");
            sb.append(CRLF);
        }
        sb.append(CRLF);
        sb.append("}");

        writeFile(dirName + "/" + domain.domain + ".java", sb);
    }

    public static Document readProtocol(File file) throws Exception {
        InputStream is = new FileInputStream(file);
        StringBuffer sb = new StringBuffer();
        byte[] bytes;
        while (true) {
            bytes = new byte[8192];
            int size = is.read(bytes);
            if (size == -1) {
                break;
            }
            sb.append(new String(bytes, 0, size));
        }
        return JSON.parseObject(sb.toString(), Document.class, Feature.DisableCircularReferenceDetect);
    }

    public void createType(Domain domain, Type type) throws Exception {
        if (!(type.isEnum() || type.isObject())) {
            //不是枚举, 也不是类, 不需要生成
            return;
        }
        String dirName = baseDir + "/" + type.getPackage().replace(".", "/");
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        StringBuffer sb = new StringBuffer();
        sb.append("package ");
        sb.append(type.getPackage());
        sb.append(";");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("/**");
        if (StringUtils.isNotEmpty(type.description)) {
            sb.append(CRLF);
            sb.append("* ");
            sb.append(type.description.replaceAll("(\r\n|\n|\r)", " "));
        }
        if (domain.experimental) {
            sb.append(CRLF);
            sb.append("* experimental");
        }
        sb.append(CRLF);
        sb.append("*/");
        sb.append(CRLF);

        if (type.isEnum()) {
            sb.append(createEnum(domain, type));
        } else {
            sb.append(createObject(domain, type));
        }

        writeFile(dirName + "/" + type.id + ".java", sb);
    }

    public StringBuffer createEnum(Domain domain, Type type) {
        StringBuffer sb = new StringBuffer();
        sb.append("public enum ");
        sb.append(type.id);
        sb.append(" implements ");
        sb.append(this.pkg);
        sb.append(".");
        sb.append(ENUM_CLASS);
        sb.append(" {");
        sb.append(CRLF);
        sb.append(CRLF);
        for(String e : type.enums) {
            String name = e.toUpperCase().replaceAll("[^a-zA-Z0-9_]", "_");
            sb.append("    ");
            sb.append(name);
            sb.append("(\"");
            sb.append(e);
            sb.append("\")");
            sb.append(",");
            sb.append(CRLF);
        }
        sb.append("    ;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    private String value;");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    ");
        sb.append(type.id);
        sb.append("(String value) {");
        sb.append(CRLF);
        sb.append("        this.value = value;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    @Override");
        sb.append(CRLF);
        sb.append("    public String value() {");
        sb.append(CRLF);
        sb.append("        return value;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);
        sb.append("    public static ");
        sb.append(type.id);
        sb.append(" findByValue(String value) {");
        sb.append(CRLF);
        sb.append("        for(");
        sb.append(type.id);
        sb.append(" val : values()) {");
        sb.append(CRLF);
        sb.append("            if (val.value.equals(value)) return val;");
        sb.append(CRLF);
        sb.append("        }");
        sb.append(CRLF);
        sb.append("        return null;");
        sb.append(CRLF);
        sb.append("    }");
        sb.append(CRLF);
        sb.append("}");
        return sb;
    }

    public String getType(Domain domain, Type prop, Type parent) throws Exception {
        if (prop == null) {
            throw new Exception("prop is null");
        }
        if (StringUtils.isNotEmpty(prop.ref)) {
            String[] ref = prop.ref.split("\\.");
            String pkg = ref.length == 2 ? ref[0] : domain.domain;
            String cls = ref.length == 2 ? ref[1] : ref[0];
            if (!typeMap.containsKey(pkg + "." + cls)) {
                throw new Exception("未知的引用");
            }
            return getType(domain, typeMap.get(pkg + "." + cls), prop);
        }
        switch (prop.type) {
            case OBJECT:
                if (StringUtils.isNotEmpty(prop.id) && isNotEmpty(prop.properties)) {
                    return prop.getPackage() + "." + prop.id;
                } else {
                    //对于这种情况的object统一处理为Map
                    return "java.util.Map<String, Object>";
                }
            case ARRAY:
                if (prop.items == null) {
                    throw new Exception("数组必须有items");
                }
                String typeName = "java.util.List<";
                typeName += getType(domain, prop.items, prop);
                typeName += ">";
                return typeName;
            case STRING:
                //此处存在枚举的情况
                if (isNotEmpty(prop.enums)) {
                    if (StringUtils.isEmpty(prop.id)) {
                        //枚举
                        if (StringUtils.isEmpty(parent.id)) {
                            throw new Exception("parent id is null");
                        }
                        Type type = new Type();
                        type.id = StringUtils.capitalize(parent.id) + StringUtils.capitalize(prop.name);
                        type.type = TypeType.STRING;
                        type.enums = prop.enums;
                        type.domain = domain;
                        createType(domain, type);
                        return type.getPackage() + "." + type.id;
                    } else {
                        return prop.getPackage() + "." + prop.id;
                    }
                } else {
                    return "String";
                }
            case ANY:
                return "Object";
            case NUMBER:
                return "java.math.BigDecimal";
            case BOOLEAN:
                return "Boolean";
            case INTEGER:
                return "Integer";
            default:
                throw new Exception("unknown type");
        }
    }

    public StringBuffer createObject(Domain domain, Type type) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("public class ");
        sb.append(type.id);
        sb.append(" {");
        sb.append(CRLF);
        sb.append(CRLF);
        Map<Type, String> typeMap = new HashMap<>();
        for(Type prop : type.properties) {
            sb.append("    /**");
            if (StringUtils.isNotEmpty(prop.description)) {
                sb.append(CRLF);
                sb.append("    * ");
                sb.append(prop.description.replaceAll("(\r\n|\n|\r)", " "));
            }
            String typeName = getType(domain, prop, type);
            typeMap.put(prop, typeName);
            sb.append(CRLF);
            sb.append("    */");
            sb.append(CRLF);
            sb.append("    public final ");
            sb.append(typeName);
            sb.append(" ");
            sb.append(prop.name.equals("this") ? "self" : prop.name);
            sb.append(";");
            sb.append(CRLF);
            sb.append(CRLF);
        }
        //生成constructor
        sb.append("    public ");
        sb.append(type.id);
        sb.append("(");
        boolean hasOptional = false;
        for(int i=0; i<type.properties.size(); i++) {
            Type prop = type.properties.get(i);
            if (prop.optional) {
                hasOptional = true;
            }
            if (i > 0) {
                sb.append(", ");
            }
            String typeName = typeMap.get(prop);
            sb.append(typeName);
            sb.append(" ");
            sb.append(prop.name.equals("this") ? "self" : prop.name);
        }
        sb.append(") {");
        sb.append(CRLF);
        for(Type prop : type.properties) {
            sb.append("        this.");
            sb.append(prop.name.equals("this") ? "self" : prop.name);
            sb.append(" = ");
            sb.append(prop.name.equals("this") ? "self" : prop.name);
            sb.append(";");
            sb.append(CRLF);
        }
        sb.append("    }");
        sb.append(CRLF);
        sb.append(CRLF);

        //生成可选参数的constructor
        if (hasOptional) {
            sb.append("    public ");
            sb.append(type.id);
            sb.append("(");
            int pNum = 0;
            for (Type prop : type.properties) {
                if (prop.optional) {
                    continue;
                }
                if (pNum > 0) {
                    sb.append(", ");
                }
                String typeName = typeMap.get(prop);
                sb.append(typeName);
                sb.append(" ");
                sb.append(prop.name.equals("this") ? "self" : prop.name);
                pNum++;
            }
            sb.append(") {");
            sb.append(CRLF);
            for (Type prop : type.properties) {
                sb.append("        this.");
                sb.append(prop.name.equals("this") ? "self" : prop.name);
                if (prop.optional) {
                    sb.append(" = null");
                } else {
                    sb.append(" = ");
                    sb.append(prop.name.equals("this") ? "self" : prop.name);
                }
                sb.append(";");
                sb.append(CRLF);
            }
            sb.append("    }");
            sb.append(CRLF);
            sb.append(CRLF);
        }
        sb.append("}");
        return sb;
    }

    public static class Document {
        public Version version;
        public List<Domain> domains;
    }

    public static class Version {
        public String major;
        public String minor;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Version)) return false;
            Version version = (Version) o;
            return Objects.equals(major, version.major) &&
                    Objects.equals(minor, version.minor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor);
        }
    }

    public static class Domain {

        public String domain;

        public boolean experimental;

        public boolean deprecated;

        public List<String> dependencies;

        public List<Type> types;

        public List<Command> commands;

        public List<Event> events;

    }

    public static class Event {

        public String name;

        public String description;

        public List<Type> parameters;

    }

    public static class Command {

        public String name;

        public String description;

        public boolean experimental;

        public boolean deprecated;

        public List<Type> parameters;

        public List<Type> returns;

    }

    public static class Type {

        private static volatile String pkg;

        public Domain domain;

        public String id;

        public String name;

        public String description;

        public TypeType type;

        public boolean optional;

        private String ref;

        @JSONField(name = "enum")
        public List<String> enums;

        public Type items;

        public List<Type> properties;

        @JSONField(name = "type")
        public void setType(String type) {
            this.type = TypeType.find(type);
        }

        @JSONField(name = "$ref")
        public void setRef(String ref) {
            this.ref = ref;
        }

        public boolean isEnum() {
            return TypeType.STRING.equals(type) && isNotEmpty(enums);
        }

        public boolean isObject() {
            return TypeType.OBJECT.equals(type) && isNotEmpty(properties);
        }

        public String getPackage() {
            return pkg + "." + (isEnum() ? "constant" : "entity") + "." + domain.domain.toLowerCase();
        }
    }

    public enum TypeType {

        STRING, OBJECT, BOOLEAN, ANY, ARRAY, NUMBER, INTEGER

        ;

        static TypeType find(String value) {
            for(TypeType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }
}
