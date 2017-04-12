package com.tmiyamon.bundler;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.service.AutoService;

import java.util.Arrays;

import javax.annotation.processing.Processor;
import javax.lang.model.SourceVersion;

@AutoService(Processor.class)
public class BundlerProcessor extends BasicAnnotationProcessor {
    @Override
    protected Iterable<? extends ProcessingStep> initSteps() {
        return Arrays.asList(
                new BundlerProcessingStep(Env.fromProcessingEnvironment(processingEnv))
        );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
