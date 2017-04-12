package com.tmiyamon.bundler;

import com.google.common.base.CaseFormat;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class BundlerFieldElement {
    public final String bundleValueName;
    public final TypeMirror bundleValueType;
    public final ExecutableElement executableElement;

    private BundlerFieldElement(String bundleValueName, ExecutableElement executableElement) {
        this.bundleValueName = bundleValueName;
        this.bundleValueType = executableElement.getReturnType();
        this.executableElement = executableElement;
    }

    public static BundlerFieldElement parse(Env env, ExecutableElement element) {
        if (!element.getParameters().isEmpty()) {
            throw new IllegalArgumentException("Method %s should have no arguments");
        }

        final String fieldName = element.getSimpleName().toString();
        return new BundlerFieldElement(fieldName, element);
    }

    public String getRawTypeName() {
        if (isArray()) {
            return ((ArrayType) bundleValueType).getComponentType().toString();
        }
        return bundleValueType.toString();
    }

    public boolean isArray() {
        return bundleValueType.getKind() == TypeKind.ARRAY;
    }

    public boolean isPrimitive() {
        return bundleValueType.getKind().isPrimitive();
    }

    public TypeMirror getComponentTypeIfArray() {
        if (isArray()) {
            return ((ArrayType) bundleValueType).getComponentType();
        }
        return null;
    }

    public String getGetMethodName() {
        return "get" + buildOperationName(bundleValueName);
    }

    public String getPutMethodName() {
        return "put" + buildOperationName(bundleValueName);
    }

    public String getBundleKeyName() {
        return "ARG_" + buildKeyName(bundleValueName);
    }
    public String getBundleKeyValue() {
        return getBundleKeyName();
    }

    private static String buildOperationName(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, bundleValueName);
    }

    private static String buildKeyName(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, bundleValueName);
    }
}
