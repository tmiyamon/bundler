package com.tmiyamon.bundler;

import com.google.auto.common.MoreElements;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

class BundlerElement {
    public final TypeElement originalElement;
    public final List<BundlerFieldElement> fields;
    public final String packageName;
    public final String className;

    public BundlerElement(TypeElement originalElement, List<BundlerFieldElement> fields, String className, String packageName) {
        this.originalElement = originalElement;
        this.fields = fields;
        this.className = className;
        this.packageName = packageName;
    }

    public static BundlerElement parse(Element element, Env env) {
        if (element.getKind() != ElementKind.INTERFACE) {
            throw new IllegalArgumentException("You can annotate only interface with @Bundler");
        }

        final TypeElement typeElement = (TypeElement) element;

        List<BundlerFieldElement> fields = new ArrayList<>();
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                fields.add(BundlerFieldElement.parse(env, MoreElements.asExecutable(enclosedElement)));
            }
        }

        final String packageName = env.getPackageName(typeElement);
        final String className = "Bundler" + element.getSimpleName().toString();

        return new BundlerElement(typeElement, fields, className, packageName);
    }
}
