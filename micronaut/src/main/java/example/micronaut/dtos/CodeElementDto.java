package example.micronaut.dtos;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@ReflectiveAccess
@Introspected
public class CodeElementDto {
    private String className;
    private String decompiledCode;

    public CodeElementDto(String className, String decompiledCode) {
        this.className = className;
        this.decompiledCode = decompiledCode;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDecompiledCode() {
        return decompiledCode;
    }

    public void setDecompiledCode(String decompiledCode) {
        this.decompiledCode = decompiledCode;
    }
}
