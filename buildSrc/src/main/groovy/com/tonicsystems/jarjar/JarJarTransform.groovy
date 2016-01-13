/*
 * Copyright (C) 2015 House365. All rights reserved.
 */


package com.tonicsystems.jarjar

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.house365.build.AndroidJarJarPlugin
import com.house365.build.AndroidJarJarPluginExtension
import com.sun.media.sound.SoftTuning
import com.tonicsystems.jarjar.util.StandaloneJarProcessor
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.android.utils.FileUtils.deleteIfExists

/**
 * Prints out some information and copies inputs to outputs.
 */
public class JarJarTransform extends Transform {
    private Project project
    private final Logger logger;

    public JarJarTransform(org.gradle.api.Project project) {
        this.project = project
        this.logger = Logging.getLogger(JarJarTransform.class);
    }

    @Override
    public String getName() {
        return "jarJar";
    }

    @Override
    public Set<ContentType> getInputTypes() {
        return ImmutableSet.<ContentType> of(QualifiedContent.DefaultContentType.CLASSES);
    }

    @Override
    public Set<Scope> getScopes() {
        return Sets.immutableEnumSet(Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS);
    }


    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs,
                          Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        StringBuilder stringBuilder = new StringBuilder();
        AndroidJarJarPluginExtension config = AndroidJarJarPlugin.getExtension(project)
        config.rules.each { String rule ->
            stringBuilder.append(rule).append("\r\n");
        }
        List<PatternElement> rules = RulesFileParser.parse(stringBuilder.toString());

        boolean verbose = logger.isEnabled(LogLevel.DEBUG);
        boolean skipManifest = config.skipManifest

        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File outJar = outputProvider.getContentLocation(directoryInput.getFile().getName(), getOutputTypes(), getScopes(),
                        Format.JAR);
                com.android.utils.FileUtils.mkdirs(outJar.getParentFile());
                deleteIfExists(outJar);
                println "Form : "+directoryInput.getFile()
                println "  to : "+outJar
                outJar.createNewFile();

                MainProcessor mainProcessor = new MainProcessor(rules, verbose, skipManifest);
                DirectoryProcessor.run(directoryInput.getFile(), outJar, mainProcessor)
            }

            for (JarInput jarInput : input.jarInputs) {
                File outJar = outputProvider.getContentLocation(FilenameUtils.getBaseName(jarInput.getFile().getName()), getOutputTypes(), getScopes(),
                        Format.JAR);
                com.android.utils.FileUtils.mkdirs(outJar.getParentFile());
                deleteIfExists(outJar);
                println "Form : "+jarInput.getFile()
                println "  to : "+outJar
                outJar.createNewFile();
                MainProcessor mainProcessor = new MainProcessor(rules, verbose, skipManifest);
                StandaloneJarProcessor.run(jarInput.getFile(), outJar, mainProcessor)
            }
        }
    }
}
