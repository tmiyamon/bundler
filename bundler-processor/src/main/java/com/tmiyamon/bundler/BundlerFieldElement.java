package com.tmiyamon.bundler;

import com.google.common.base.CaseFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

class BundlerFieldElement {
    public final String fieldName;
    public final TypeMirror fieldType;
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

    private BundlerFieldElement(String fieldName, VariableElement variableElement) {
        this.fieldName = fieldName;
        this.fieldType = variableElement.asType();
        this.variableElement = variableElement;
    }

    public static BundlerFieldElement parse(Env env, VariableElement element) {
        final String fieldName = element.getSimpleName().toString();
        return new BundlerFieldElement(fieldName, element);
    }

    public String getRawTypeName() {
        if (isArray()) {
            return ((ArrayType) fieldType).getComponentType().toString();
        }
        return fieldType.toString();
    }

    public boolean isArray() {
        return fieldType.getKind() == TypeKind.ARRAY;
    }

    public boolean isPrimitive() {
        return fieldType.getKind().isPrimitive();
    }

    public TypeMirror getComponentTypeIfArray() {
        if (isArray()) {
            return ((ArrayType) fieldType).getComponentType();
        }
        return null;
    }

    public String getGetValueFromBundleMethodName() {
        return "get" + fromLowerCamelToUpperCamel(fieldName);
    }

    public String getPutValueToBundleMethodName() {
        return "put" + fromLowerCamelToUpperCamel(fieldName);
    }

    public boolean isPublic() {
        return this.variableElement.getModifiers().contains(Modifier.PUBLIC);
    }

    public String getExpectedGetterName() {
        return "get" + fromLowerCamelToUpperCamel(this.fieldName);
    }

    public String getExpectedSetterName() {
        return "set" + fromLowerCamelToUpperCamel(this.fieldName);
    }

    public String getBundleKeyName() {
        return "ARG_" + fromLowerCamelToUpperUnderscore(fieldName);
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

        TypeMirror type = fieldType;
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

    private static String fromLowerCamelToUpperCamel(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, bundleValueName);
    }

    private static String fromLowerCamelToUpperUnderscore(String bundleValueName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, bundleValueName);
    }
}
