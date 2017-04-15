package com.tmiyamon.bundler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import static com.squareup.javapoet.TypeSpec.classBuilder;

public class BundlerProcessingStep implements BasicAnnotationProcessor.ProcessingStep {

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
        final ClassName bundlerClassName = bundler.getBundlerClassName();

        TypeSpec.Builder typeSpecBuilder = classBuilder(bundlerClassName)
                .addModifiers(Modifier.PUBLIC);

        for (BundlerFieldElement field : bundler.fields) {
            emitField(field, typeSpecBuilder);
        }

        typeSpecBuilder
                .addMethod(buildCreateBundle(bundler))
                .addMethod(buildCreateBundleWithFields(bundler))
                .addMethod(buildCreateIntent(bundler))
                .addMethod(buildCreateIntentWithFields(bundler))
                .addMethod(buildApply(bundler))
                .addMethod(buildApplyWithField(bundler))
                .addMethod(buildApplyBundle(bundler))
                .addMethod(buildApplyBundleWithField(bundler))
                .addMethod(buildParse(bundler))
                .addMethod(buildParseIntent(bundler));
        ;

        JavaFile.builder(bundlerClassName.packageName(), typeSpecBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(env.getFiler());
    }


    private void emitField(BundlerFieldElement field, TypeSpec.Builder typeSpecBuilder)  throws IOException {
        final String operation = field.getOperation(env);
        final String keyName = field.bundleKeyName;
        final String keyValue = field.bundleKeyValue;
        final TypeName valueType = TypeName.get(field.fieldType);

        FieldSpec bundleKey = FieldSpec.builder(TypeName.get(getStringType()), keyName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", keyValue)
                .build();

        MethodSpec putOperation = MethodSpec.methodBuilder(field.getPutValueToBundleMethodName())
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .addParameter(ParameterSpec.builder(valueType, "value").build())
                .returns(TypeName.VOID)
                .addStatement("bundle.put$N($N, $N)", operation, keyName, "value")
                .build();

        MethodSpec getOperation = MethodSpec.methodBuilder(field.getGetValueFromBundleMethodName())
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

    /**
     * public static Bundle createBundle(T model)
     * @param bundler
     * @return
     */
    private MethodSpec buildCreateBundle(BundlerElement bundler) {
        return MethodSpec.methodBuilder("createBundle")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(bundler.originalElement.asType()), "model").build())
                .returns(TypeName.get(getBundleType()))
                .addStatement("return apply(new Bundle(), model)")
                .build();
    }

    /**
     * public static Bundle createBundle(varargs)
     * @param bundler
     * @return
     */
    private MethodSpec buildCreateBundleWithFields(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createBundle")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(getBundleType()));

        for (BundlerFieldElement field : bundler.fields) {
            builder.addParameter(TypeName.get(field.fieldType), field.fieldName);
        }

        return builder
                .addStatement("return apply(new Bundle(), $L)", bundler.joinedFieldNames())
                .build();
    }

    /**
     * public static Intent createIntent(T model)
     * @param bundler
     * @return
     */
    private MethodSpec buildCreateIntent(BundlerElement bundler) {
        return MethodSpec.methodBuilder("createIntent")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(bundler.originalElement.asType()), "model").build())
                .returns(TypeName.get(getIntentType()))
                .addStatement("Intent intent = new Intent()")
                .addStatement("intent.putExtras(createBundle(model))")
                .addStatement("return intent")
                .build();
    }

    /**
     * public static Intent createIntent(varargs)
     * @param bundler
     * @return
     */
    private MethodSpec buildCreateIntentWithFields(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("createIntent")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(getIntentType()))
                .addStatement("Intent intent = new Intent()");

        for (BundlerFieldElement field : bundler.fields) {
            builder.addParameter(TypeName.get(field.fieldType), field.fieldName);
        }

        return builder
                .addStatement("intent.putExtras(createBundle($N))", bundler.joinedFieldNames())
                .addStatement("return intent")
                .build();
    }

    /**
     * public static Intent apply(Intent intent, T model)
     * @param bundler
     * @return
     */
    private MethodSpec buildApply(BundlerElement bundler) {
        return MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getIntentType()), "intent").build())
                .addParameter(ParameterSpec.builder(TypeName.get(bundler.originalElement.asType()), "model").build())
                .returns(TypeName.get(getIntentType()))
                .addStatement("intent.putExtras(createBundle(model))")
                .addStatement("return intent")
                .build();
    }

    /**
     * public static Intent apply(Intent intent, varargs)
     * @param bundler
     * @return
     */
    private MethodSpec buildApplyWithField(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getIntentType()), "intent").build())
                .returns(TypeName.get(getIntentType()));

        for (BundlerFieldElement field : bundler.fields) {
            builder.addParameter(TypeName.get(field.fieldType), field.fieldName);
        }

        return builder
                .addStatement("intent.putExtras(createBundle($N))", bundler.joinedFieldNames())
                .addStatement("return intent")
                .build();
    }

    /**
     * public static Bundle apply(Bundle bundle, T model)
     * @param bundler
     * @return
     */
    private MethodSpec buildApplyBundle(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .addParameter(ParameterSpec.builder(TypeName.get(bundler.originalElement.asType()), "model").build())
                .returns(TypeName.get(getBundleType()));

        List<String> getters = new ArrayList<>();
        for (BundlerFieldElement field : bundler.fields) {
            String getter = buildGetValueFromModelStatement(bundler.getGetterTypeOf(field),field);
            getters.add("model." + getter);
        }

        return builder
                .addStatement("return apply(bundle, $L)", StringUtils.join(getters, ", "))
                .build();
    }

    /**
     * public static Bundle apply(Bundle intent, varargs)
     * @param bundler
     * @return
     */
    private MethodSpec buildApplyBundleWithField(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("apply")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .returns(TypeName.get(getBundleType()));

        for (BundlerFieldElement field : bundler.fields) {
            builder.addParameter(TypeName.get(field.fieldType), field.fieldName)
                    .addStatement("$L(bundle, $L)", field.getPutValueToBundleMethodName(), field.fieldName);
        }

        return builder.addStatement("return bundle").build();
    }

    /**
     * public static T parse(Bundle bundle)
     * @param bundler
     * @return
     */
    private MethodSpec buildParse(BundlerElement bundler) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .returns(bundler.getOriginalClassName());

        boolean useConstructor = bundler.constructor.isParametersMatchToFields(bundler);
        if (useConstructor) {
            Map<String, BundlerFieldElement> fieldIndex = new HashMap<>();
            for (BundlerFieldElement field : bundler.fields) {
                fieldIndex.put(field.fieldName, field);
            }

            StringBuilder modelInitializer = new StringBuilder("new " + bundler.getOriginalClassName().toString() + "(");
            for (VariableElement variable : bundler.constructor.originalElement.getParameters()) {
                BundlerFieldElement correspondingField = fieldIndex.get(variable.getSimpleName().toString());

                modelInitializer
                        .append(correspondingField.getGetValueFromBundleMethodName())
                        .append("(bundle),");
            }
            modelInitializer.deleteCharAt(modelInitializer.length()-1).append(")");

            builder.addStatement("return $L", modelInitializer.toString());
        } else {
            builder.addStatement("$L model = new $L()",
                    bundler.getOriginalClassName().toString(),
                    bundler.getOriginalClassName().toString());

            for (BundlerFieldElement field : bundler.fields) {
                BundlerElement.SetterType setterType = bundler.getSetterTypeOf(field);

                builder.addStatement("model.$L",
                        buildSetValueToModelStatement(setterType, field, field.getGetValueFromBundleMethodName()+"(bundle)")
                );
            }

            builder.addStatement("return model");
        }

        return builder.build();
    }

    /**
     * public static T parse(Intent intent)
     * @param bundler
     * @return
     */
    private MethodSpec buildParseIntent(BundlerElement bundler) {
        return MethodSpec.methodBuilder("parse")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getIntentType()), "intent").build())
                .returns(bundler.getOriginalClassName())
                .addStatement("return parse(intent.getExtras())", TypeName.get(getBundleType()).toString())
                .build();
    }

    private TypeMirror getTypeFromString(String fullClassName)  {
        return env.getElements().getTypeElement(fullClassName).asType();
    }

    private TypeMirror getBundleType() {
        return getTypeFromString("android.os.Bundle");
    }

    private TypeMirror getIntentType() {
        return getTypeFromString("android.content.Intent");
    }

    private TypeMirror getStringType() {
        return getTypeFromString("java.lang.String");
    }


    private String buildGetValueFromModelStatement(BundlerElement.GetterType type, BundlerFieldElement field) {
        switch(type){
            case FIELD:
                return field.fieldName;
            case GETTER:
                return field.getExpectedGetterName() + "()";
        }
        return null;
    }

    private String buildSetValueToModelStatement(BundlerElement.SetterType type, BundlerFieldElement field, String valueStatement) {
        switch(type){
            case FIELD:
                return field.fieldName + " = " + valueStatement;
            case SETTER:
                return field.getExpectedSetterName() + "(" + valueStatement + ")";
        }
        return null;
    }
}
