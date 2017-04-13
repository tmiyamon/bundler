package com.tmiyamon.bundler;

import com.google.auto.common.MoreElements;
import com.squareup.javapoet.ClassName;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

class BundlerElement {
    public final TypeElement originalElement;
    public final List<BundlerFieldElement> fields;
    public final String packageName;
    public final String bundlerClassName;
    public final String originalClassName;

    public BundlerElement(
            TypeElement originalElement,
            List<BundlerFieldElement> fields,
            String bundlerClassName,
            String originalClassName,
            String packageName
    ) {
        this.originalElement = originalElement;
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
        if (element.getKind() != ElementKind.CLASS) {
            throw new IllegalArgumentException("You can annotate only class with @BundleModel");
        }

        final TypeElement typeElement = (TypeElement) element;

        List<BundlerFieldElement> fields = new ArrayList<>();
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                fields.add(BundlerFieldElement.parse(env, MoreElements.asVariable(enclosedElement)));
            }
        }

        final String packageName = env.getPackageName(typeElement);
        final String bundlerClassName = "Bundler" + element.getSimpleName().toString();
        final String originalClassName = element.getSimpleName().toString();

        return new BundlerElement(
                typeElement,
                fields,
                bundlerClassName,
                originalClassName,
                packageName
        );
    }
}
