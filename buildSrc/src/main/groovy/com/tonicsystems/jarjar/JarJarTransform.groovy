/*
 * Copyright (C) 2015 House365. All rights reserved.
 */


package com.tonicsystems.jarjar

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.house365.build.AndroidJarJarPlugin
import com.house365.build.AndroidJarJarPluginExtension
import com.tonicsystems.jarjar.util.StandaloneJarProcessor
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.reflect.FieldUtils
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
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
    private boolean isLibrary
    private BaseExtension android

    public JarJarTransform(org.gradle.api.Project project) {
        println "JarJarTransform.JarJarTransform " + isLibrary
        this.isLibrary = isLibrary
        this.project = project
        android = project.hasProperty("android") ? project.android : null
        if (android == null) {
            throw new ProjectConfigurationException("Only use shade for android library", null)
        }
        if (android instanceof LibraryExtension) {
            isLibrary = true
        } else if (android instanceof AppExtension) {
            isLibrary = false
        }
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

    @NonNull
    @Override
    public Set<Scope> getScopes() {
        if (isLibrary) {
            return Sets.immutableEnumSet(Scope.PROJECT, Scope.PROJECT_LOCAL_DEPS);
        }
        return TransformManager.SCOPE_FULL_PROJECT;
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
        if (config == null || config.rules == null || config.rules.size() <= 0)
            return
        config.rules.each { String rule ->
            stringBuilder.append(rule).append("\r\n");
        }
        List<PatternElement> rules = RulesFileParser.parse(stringBuilder.toString());

        boolean verbose = logger.isEnabled(LogLevel.DEBUG);
        boolean skipManifest = config.skipManifest

        BaseVariant variant = getCurrentVariant(outputProvider);

        BaseVariantData baseVariantData = variant.variantData;

        File intermediatesDir = baseVariantData.getScope().getGlobalScope().getIntermediatesDir()
        HashMap<File, String> destNameMap = new HashMap<>();
        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                String name = directoryInput.getFile().getName();
                boolean isParent = checkIsParent(directoryInput.getFile(), intermediatesDir)
                if (isParent) {
                    name = intermediatesDir.toURI().relativize(directoryInput.getFile().getParentFile().toURI()).getPath().replace("/", "-") + name;
                }
                while (destNameMap.containsValue(name)) {
                    name = directoryInput.getFile().getParentFile().getName() + "-" + name
                }
                destNameMap.put(directoryInput.getFile(), name)

            }
            for (JarInput jarInput : input.jarInputs) {
                String name = FilenameUtils.getBaseName(jarInput.getFile().getName());
                boolean isParent = checkIsParent(jarInput.getFile(), intermediatesDir)
                if (isParent) {
                    name = intermediatesDir.toURI().relativize(jarInput.getFile().getParentFile().toURI()).getPath().replace("/", "-") + name;
                } else if (jarInput.getFile().getParentFile().getName().matches("^\\w{38,}\$")) {
                    File parentFile = jarInput.getFile().getParentFile().getParentFile()
                    name = parentFile.getParentFile().getParentFile().getName() + "-" + parentFile.getParentFile().getName() + "-" + parentFile.getName()
                }
                while (destNameMap.containsValue(name)) {
                    name = jarInput.getFile().getParentFile().getName() + "-" + name
                }
                destNameMap.put(jarInput.getFile(), name)
            }
        }

        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File outJar = outputProvider.getContentLocation(destNameMap.get(directoryInput.getFile()), getOutputTypes(), getScopes(),
                        Format.JAR);
                com.android.utils.FileUtils.mkdirs(outJar.getParentFile());
                deleteIfExists(outJar);
                println "Form : " + directoryInput.getFile()
                println "  to : " + outJar
                outJar.createNewFile();

                MainProcessor mainProcessor = new MainProcessor(rules, verbose, skipManifest);
                DirectoryProcessor.run(directoryInput.getFile(), outJar, mainProcessor)
            }

            for (JarInput jarInput : input.jarInputs) {
                File outJar = outputProvider.getContentLocation(destNameMap.get(jarInput.getFile()), getOutputTypes(), getScopes(),
                        Format.JAR);
                com.android.utils.FileUtils.mkdirs(outJar.getParentFile());
                deleteIfExists(outJar);
                println "Form : " + jarInput.getFile()
                println "  to : " + outJar
                outJar.createNewFile();
                MainProcessor mainProcessor = new MainProcessor(rules, verbose, skipManifest);
                StandaloneJarProcessor.run(jarInput.getFile(), outJar, mainProcessor)
            }
        }
    }

    BaseVariant getCurrentVariant(TransformOutputProvider outputProvider) {
        File rootLoaction = FieldUtils.readField(outputProvider, "rootLocation", true)
        return getCurrentVariant(rootLoaction);
    }

    BaseVariant getCurrentVariant(File file) {
        def variants
        if (android instanceof LibraryExtension) {
            LibraryExtension libraryExtension = (LibraryExtension) android
            variants = libraryExtension.libraryVariants
        } else if (android instanceof AppExtension) {
            AppExtension appExtension = (AppExtension) android
            variants = appExtension.applicationVariants
        }

        if (variants != null) {
            for (BaseVariant variant : variants) {
                BaseVariantData variantData = variant.variantData
                GlobalScope globalScope = variantData.getScope().getGlobalScope();
                File parentFile = new File(globalScope.getIntermediatesDir(), "/transforms/" + this.getName() + "/" +
                        variantData.getVariantConfiguration().getDirName())
                if (checkIsParent(file, parentFile))
                    return variant;
            }
        }
        return null
    }

    boolean checkIsParent(File child, File possibleParent) {
        return child.getAbsolutePath().startsWith(possibleParent.getAbsolutePath());
    }

}
