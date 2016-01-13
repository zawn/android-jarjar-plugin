/*
 * Copyright (C) 2015 House365. All rights reserved.
 */

package com.house365.build

import com.android.build.gradle.BaseExtension
import com.tonicsystems.jarjar.JarJarTransform
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Created by FanLei on 2016/1/11.
 */
public class AndroidJarJarPlugin implements Plugin<Project> {
    protected Logger logger;
    private BaseExtension android

    @Override
    void apply(Project project) {
        logger = Logging.getLogger(this.getClass());
        android = project.hasProperty("android") ? project.android : null
        if (android == null) {
            throw new ProjectConfigurationException("Only use shade for android library", null)
        }
        project.getExtensions().create("jarjar", AndroidJarJarPluginExtension.class);
        android.registerTransform(new JarJarTransform(project))
    }

    public static AndroidJarJarPluginExtension getExtension(Project project) {
        final AndroidJarJarPluginExtension config = project.jarjar
        return config
    }
}
