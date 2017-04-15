package com.tmiyamon.bundler;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

class BundlerElement {
    public final TypeElement originalElement;
    public final BundlerConstructorElement constructor;
    public final List<BundlerFieldElement> fields;
    public final String packageName;
    public final String bundlerClassName;
    public final String originalClassName;

    public BundlerElement(
            TypeElement originalElement,
            BundlerConstructorElement constructor,
            List<BundlerFieldElement> fields,
            String bundlerClassName,
            String originalClassName,
            String packageName
    ) {
        this.originalElement = originalElement;
        this.constructor = constructor;
        this.fields = fields;
        this.bundlerClassName = bundlerClassName;
        this.originalClassName = originalClassName;
        this.packageName = packageName;
    }

    public ClassName getBundlerClassName() {
        return ClassName.get(packageName, bundlerClassName);
    }
    public ClassName getOriginalClassName() {
        return ClassName.get(packageName, originalClassName);
    }

    public static BundlerElement parse(Element element, Env env) {
        validateBundlerType(element);

        final TypeElement typeElement = MoreElements.asType(element);

        final List<BundlerFieldElement> fields = new ArrayList<>();
        final List<BundlerConstructorElement> constructors = new ArrayList<>();

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                fields.add(BundlerFieldElement.parse(env, MoreElements.asVariable(enclosedElement)));
            }

            if (enclosedElement.getKind() == ElementKind.CONSTRUCTOR) {
                constructors.add(BundlerConstructorElement.parse(MoreElements.asExecutable(enclosedElement), env));
            }
        }

        validateConstructorsCount(constructors);

        final BundlerConstructorElement constructor = constructors.get(0);
        final String packageName = env.getPackageName(typeElement);
        final String bundlerClassName = "Bundler" + element.getSimpleName().toString();
        final String originalClassName = element.getSimpleName().toString();

        return new BundlerElement(
                typeElement,
                constructor,
                fields,
                bundlerClassName,
                originalClassName,
                packageName
        );
    }

    public boolean hasGetterOf(BundlerFieldElement field) {
        for (Element element : originalElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement executable = MoreElements.asExecutable(element);
                if (executable.getSimpleName().toString().equals(field.getExpectedGetterName())
                        && executable.getReturnType().equals(field.fieldType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasSetterOf(BundlerFieldElement field) {
        for (Element element : originalElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement executable = MoreElements.asExecutable(element);
                if (executable.getSimpleName().toString().equals(field.getExpectedSetterName())
                        && executable.getParameters().size() == 1
                        && executable.getParameters().get(0).equals(field.fieldType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public GetterType getGetterTypeOf(BundlerFieldElement field) {
        if (field.isPublic()) {
            return GetterType.FIELD;
        } else if (hasGetterOf(field)) {
            return GetterType.GETTER;
        }
        throw new IllegalArgumentException(field.fieldName + " must be public or have public getter method");
    }

    public SetterType getSetterTypeOf(BundlerFieldElement field) {
        if (constructor.isParametersMatchToFields(this)) {
            return SetterType.CONSTRUCTOR;
        } else if (field.isPublic()) {
            return SetterType.FIELD;
        } else if (hasSetterOf(field)) {
            return SetterType.SETTER;
        }
        throw new IllegalArgumentException(field.fieldName + " must be public, have public setter method or be initialized in constructor");
    }

    private static void validateBundlerType(Element element) {
        if (element.getKind() != ElementKind.CLASS) {
            throw new IllegalArgumentException("You can annotate only class with @Bundler");
        }
    }

    private static void validateConstructorsCount(List<BundlerConstructorElement> constructors) {
        if (constructors.size() != 1) {
            throw new IllegalArgumentException("@Bundler must have only one constructor");
        }
    }

    enum GetterType {
        FIELD, GETTER
    }

    enum SetterType {
        FIELD, SETTER, CONSTRUCTOR
    }
}
