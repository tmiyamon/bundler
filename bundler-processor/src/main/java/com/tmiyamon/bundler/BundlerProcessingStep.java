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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
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
                .addModifiers(Modifier.PUBLIC)
                .superclass(bundler.getOriginalClassName());

        for (ExecutableElement constructor: bundler.constrcutors) {
            typeSpecBuilder.addMethod(buildConstructor(constructor));
        }

        for (BundlerFieldElement field : bundler.fields) {
            emitField(bundler, field, typeSpecBuilder);
        }

        typeSpecBuilder
                .addMethod(buildToBundleMethod(bundler))
                .addMethod(buildToIntentMethod(bundler))
                .addMethod(buildFromBundleMethod(bundler))
                .addMethod(buildFromIntentMethod(bundler))
                .addMethod(buildWithIntentMethod(bundler));

        JavaFile.builder(bundlerClassName.packageName(), typeSpecBuilder.build())
                .skipJavaLangImports(true)
                .build()
                .writeTo(env.getFiler());
    }

    private void emitField(BundlerElement bundler, BundlerFieldElement field, TypeSpec.Builder typeSpecBuilder)  throws IOException {
        final String operation = field.getOperation(env);
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

        typeSpecBuilder
                .addField(bundleKey)
                .addMethod(getOperation)
                .addMethod(putOperation);
    }

    private MethodSpec buildConstructor(ExecutableElement constructor) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(constructor.getModifiers());

        StringBuilder superArgs = new StringBuilder();
        for (VariableElement variable: constructor.getParameters()) {
            constructorBuilder
                    .addParameter(TypeName.get(variable.asType()), variable.getSimpleName().toString());
            superArgs.append(variable.getSimpleName()).append(",");
        }

        constructorBuilder.addStatement("super($N)", superArgs.substring(0, superArgs.length()-1));

        return constructorBuilder.build();
    }

    private MethodSpec buildFromBundleMethod(BundlerElement bundler) {
        MethodSpec.Builder fromBundleBuilder = MethodSpec.methodBuilder("fromBundle")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getBundleType()), "bundle").build())
                .returns(bundler.getOriginalClassName())
                .addStatement("$L bundleModel = new $L()", bundler.getBundlerClassName().toString(), bundler.getBundlerClassName().toString());

        for (BundlerFieldElement field : bundler.fields) {
            fromBundleBuilder.addStatement("bundleModel.$N = $N(bundle)", field.bundleValueName, field.getGetMethodName());
        }

        return fromBundleBuilder.addStatement("return bundleModel").build();
    }

    private MethodSpec buildFromIntentMethod(BundlerElement bundler) {
        return MethodSpec.methodBuilder("fromIntent")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getIntentType()), "intent").build())
                .returns(bundler.getOriginalClassName())
                .addStatement("return fromBundle(intent.getExtras())")
                .build();
    }

    private MethodSpec buildToBundleMethod(BundlerElement bundler) {
        MethodSpec.Builder toBundleBuilder = MethodSpec.methodBuilder("toBundle")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(getBundleType()))
                .addStatement("Bundle bundle = new Bundle()");

        for (BundlerFieldElement field : bundler.fields) {
            toBundleBuilder.addStatement("$N(bundle, this.$N)", field.getPutMethodName(), field.bundleValueName);
        }

        return toBundleBuilder.addStatement("return bundle").build();
    }

    private MethodSpec buildToIntentMethod(BundlerElement bundler) {
        return MethodSpec.methodBuilder("toIntent")
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(getIntentType()))
                .addStatement("Intent intent = new Intent()")
                .addStatement("intent.putExtras(this.toBundle())")
                .addStatement("return intent")
                .build();
    }

    private MethodSpec buildWithIntentMethod(BundlerElement bundler) {
        return MethodSpec.methodBuilder("withIntent")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ParameterSpec.builder(TypeName.get(getIntentType()), "intent").build())
                .returns(TypeName.get(getIntentType()))
                .addStatement("intent.putExtras(this.toBundle())")
                .addStatement("return intent")
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
}
