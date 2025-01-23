package example.micronaut.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@ReflectiveAccess
@Introspected
public class JavaClassDto {
    private static final String URL_PREFIX = "show/";

    private String className;
    private String showCodeUrl;
    private String javaClassName;

    public JavaClassDto() {
    }

    public JavaClassDto(String className) {
        this.className = className;
        this.showCodeUrl = URL_PREFIX + className.replace("/", "-");
        this.javaClassName = classNameToJavaFormate(className);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getShowCodeUrl() {
        return showCodeUrl;
    }

    public void setShowCodeUrl(String showCodeUrl) {
        this.showCodeUrl = showCodeUrl;
    }

    public String getJavaClassName() {
        return javaClassName;
    }

    public void setJavaClassName(String javaClassName) {
        this.javaClassName = javaClassName;
    }

    private static String classNameToJavaFormate(String className) {
        return className.replace("-", ".").replace("/", ".");
    }
}
