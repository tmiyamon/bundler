package com.tmiyamon.bundler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.TypeSpec.classBuilder;

public class BundlerProcessingStep implements BasicAnnotationProcessor.ProcessingStep {
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

    private final Env env;

    public BundlerProcessingStep(Env env) {
        this.env = env;
    }

    @Override
    public Set<? extends Class<? extends Annotation>> annotations() {
        return Collections.singleton(Bundler.class);
    }

    @Override
    public Set<Element> process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
        Set<Element> delayed = new HashSet<>();
        for (Element bundler : elementsByAnnotation.values()) {
            try {
                BundlerElement bundleElement = BundlerElement.parse(bundler, env);
                emitBundleClass(bundleElement);
            }  catch (Exception e) {
                e.printStackTrace();
                env.printError(bundler, "Internal processor error:\n %s", e.getMessage());
            }
        }
        return delayed;
    }

    private void emitBundleClass(BundlerElement bundler) throws IOException {
        TypeSpec.Builder typeSpecBuilder = classBuilder(bundler.className);

        for (BundlerFieldElement field : bundler.fields) {
            emitField(field, typeSpecBuilder);
        }

        JavaFile.builder(bundler.packageName, typeSpecBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(env.getFiler());
    }

    private void emitField(BundlerFieldElement field, TypeSpec.Builder typeSpecBuilder)  throws IOException {
        final String operation = getOperation(field);
        final String keyName = field.getBundleKeyName();
        final String keyValue = field.getBundleKeyValue();
        final TypeName valueType = TypeName.get(field.bundleValueType);

        FieldSpec bundleKey = FieldSpec.builder(TypeName.get(getStringType()), keyName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", keyValue)
                .build();

        MethodSpec putOperation = MethodSpec.methodBuilder(field.getPutMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .addParameter(ParameterSpec.builder(valueType, "value").build())
                .returns(TypeName.VOID)
                .addStatement("bundle.put$N($N, $N)", operation, keyName, "value")
                .build();

        MethodSpec getOperation = MethodSpec.methodBuilder(field.getGetMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .returns(valueType)
                .addStatement("return ($L) bundle.get$N($N)", valueType.toString(), operation, keyName)
                .build();

        MethodSpec putExtraOperation = MethodSpec.methodBuilder(field.getPutMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .addParameter(ParameterSpec.builder(valueType, "value").build())
                .returns(TypeName.VOID)
                .addStatement("bundle.put$N($N, $N)", operation, keyName, "value")
                .build();

        MethodSpec getExtraOperation = MethodSpec.methodBuilder(field.getGetMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .returns(valueType)
                .addStatement("return ($L) bundle.get$N($N)", valueType.toString(), operation, keyName)
                .build();

        typeSpecBuilder
                .addField(bundleKey)
                .addMethod(getOperation)
                .addMethod(putOperation);
    }

    protected String getOperation(BundlerFieldElement field) {
        String op = ARGUMENT_TYPES.get(field.getRawTypeName());
        if (op != null) {
            if (field.isArray()) {
                return op + "Array";
            } else {
                return op;
            }
        }

        TypeMirror type = field.bundleValueType;
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
            TypeMirror tm = getArrayListType(arrayListTypes[i]);
            if (env.getTypes().isAssignable(type, tm)) {
                return arrayListOps[i];
            }
        }

        if (env.getTypes().isAssignable(type,
                getWildcardType(ArrayList.class.getName(), "android.os.Parcelable"))) {
            return "ParcelableArrayList";
        }
        TypeMirror sparseParcelableArray =
                getWildcardType("android.util.SparseArray", "android.os.Parcelable");

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
    private TypeMirror getWildcardType(String type, String elementType) {
        TypeElement arrayList = env.getElements().getTypeElement(type);
        TypeMirror elType = env.getElements().getTypeElement(elementType).asType();
        return env.getTypes()
                .getDeclaredType(arrayList, env.getTypes().getWildcardType(elType, null));
    }

    private TypeMirror getArrayListType(String elementType) {
        TypeElement arrayList = env.getElements().getTypeElement("java.util.ArrayList");
        TypeMirror elType = env.getElements().getTypeElement(elementType).asType();
        return env.getTypes().getDeclaredType(arrayList, elType);
    }

    private TypeMirror getTypeFromString(String fullClassName)  {
        return env.getElements().getTypeElement(fullClassName).asType();
    }

    private TypeMirror getBundleType() {
        return getTypeFromString("android.os.Bundle");
    }

    private TypeMirror getStringType() {
        return getTypeFromString("java.lang.String");
    }
}
