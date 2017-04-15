package com.tmiyamon.bundler;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;

class BundlerConstructorElement {
    public final ExecutableElement originalElement;
    public final boolean isNoParametersConstructor;

    public BundlerConstructorElement(ExecutableElement originalElement, boolean isNoParametersConstructor) {
        this.originalElement = originalElement;
        this.isNoParametersConstructor = isNoParametersConstructor;
    }

    public boolean isParametersMatchToFields(BundlerElement bundler) {
        int fieldsCount = bundler.fields.size();
        if (fieldsCount != originalElement.getParameters().size()) {
            return false;
        }

        for (int i = 0; i < fieldsCount; i++) {
            BundlerFieldElement field = bundler.fields.get(i);
            VariableElement variable = originalElement.getParameters().get(i);

            if (!field.fieldName.equals(variable.getSimpleName().toString()) || field.fieldType != variable.asType()) {
                return false;
            }
        }

        return true;
    }

    public String buildParametersString() {
        if (isNoParametersConstructor) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (VariableElement variable : originalElement.getParameters()) {
            builder.append(variable.asType().toString()).append(variable.getSimpleName().toString()).append(",");
        }

        return builder.substring(0, builder.length()-1);
    }


    public static BundlerConstructorElement parse(ExecutableElement element, Env env) {
        final boolean isNoParametersConstructor = element.getParameters().isEmpty();

        return new BundlerConstructorElement(element, isNoParametersConstructor);
    }
}
