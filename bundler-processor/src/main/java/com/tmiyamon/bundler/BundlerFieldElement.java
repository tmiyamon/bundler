package com.tmiyamon.bundler;

import com.google.common.base.CaseFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class BundlerFieldElement {
    public final String bundleValueName;
    public final TypeMirror bundleValueType;
    public final VariableElement variableElement;


    private static final Map<String, String> ARGUMENT_TYPES = new HashMap<String, String>(20);
    static {
        ARGUMENT_TYPES.put("java.lang.String", "String");
        ARGUMENT_TYPES.put("int", "Int");
        ARGUMENT_TYPES.put("java.lang.Integer", "Int");
        ARGUMENT_TYPES.put("long", "Long");
        ARGUMENT_TYPES.put("java.lang.Long", "Long");
        ARGUMENT_TYPES.put("double", "Double");
        ARGUMENT_TYPES.put("java.lang.Double", "Double");
        ARGUMENT_TYPES.put("short", "Short");
        ARGUMENT_TYPES.put("java.lang.Short", "Short");
        ARGUMENT_TYPES.put("float", "Float");
        ARGUMENT_TYPES.put("java.lang.Float", "Float");
        ARGUMENT_TYPES.put("byte", "Byte");
        ARGUMENT_TYPES.put("java.lang.Byte", "Byte");
        ARGUMENT_TYPES.put("boolean", "Boolean");
        ARGUMENT_TYPES.put("java.lang.Boolean", "Boolean");
        ARGUMENT_TYPES.put("char", "Char");
        ARGUMENT_TYPES.put("java.lang.Character", "Char");
        ARGUMENT_TYPES.put("java.lang.CharSequence", "CharSequence");
        ARGUMENT_TYPES.put("android.os.Bundle", "Bundle");
        ARGUMENT_TYPES.put("android.os.Parcelable", "Parcelable");
    }

    private BundlerFieldElement(String bundleValueName, VariableElement variableElement) {
        this.bundleValueName = bundleValueName;
        this.bundleValueType = variableElement.asType();
        this.variableElement = variableElement;
    }

    public static BundlerFieldElement parse(Env env, VariableElement element) {
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

    public String getOperation(Env env) {
        String op = ARGUMENT_TYPES.get(getRawTypeName());
        if (op != null) {
            if (isArray()) {
                return op + "Array";
            } else {
                return op;
            }
        }

        TypeMirror type = bundleValueType;
        String[] arrayListTypes = new String[] {
                String.class.getName(),
                Integer.class.getName(),
                CharSequence.class.getName()
        };
        String[] arrayListOps = new String[] {
                "StringArrayList",
                "IntegerArrayList",
                "CharSequenceArrayList"
        };
        for (int i = 0; i < arrayListTypes.length; i++) {
            TypeMirror tm = getArrayListType(env, arrayListTypes[i]);
            if (env.getTypes().isAssignable(type, tm)) {
                return arrayListOps[i];
            }
        }

        if (env.getTypes().isAssignable(type,
                getWildcardType(env, ArrayList.class.getName(), "android.os.Parcelable"))) {
            return "ParcelableArrayList";
        }
        TypeMirror sparseParcelableArray =
                getWildcardType(env, "android.util.SparseArray", "android.os.Parcelable");

        if (env.getTypes().isAssignable(type, sparseParcelableArray)) {
            return "SparseParcelableArray";
        }

        if (env.getTypes().isAssignable(type, env.getElements().getTypeElement("android.os.Parcelable").asType())) {
            return "Parcelable";
        }

        if (env.getTypes().isAssignable(type, env.getElements().getTypeElement(Serializable.class.getName()).asType())) {
            return "Serializable";
        }

        return null;
    }
    private TypeMirror getWildcardType(Env env, String type, String elementType) {
        TypeElement arrayList = env.getElements().getTypeElement(type);
        TypeMirror elType = env.getElements().getTypeElement(elementType).asType();
        return env.getTypes()
                .getDeclaredType(arrayList, env.getTypes().getWildcardType(elType, null));
    }

    private TypeMirror getArrayListType(Env env, String elementType) {
        TypeElement arrayList = env.getElements().getTypeElement("java.util.ArrayList");
        TypeMirror elType = env.getElements().getTypeElement(elementType).asType();
        return env.getTypes().getDeclaredType(arrayList, elType);
    }

    private static String buildOperationName(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, bundleValueName);
    }

    private static String buildKeyName(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, bundleValueName);
    }
}
