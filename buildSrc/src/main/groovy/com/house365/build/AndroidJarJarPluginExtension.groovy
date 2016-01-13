/*
 * Copyright (C) 2015 House365. All rights reserved.
 */

package com.house365.build

/**
 * User: Ralf Wondratschek
 */
class AndroidJarJarPluginExtension {

    Set<String> rules
    Set<String> srcExcludes

    boolean skipManifest

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        AndroidJarJarPluginExtension that = (AndroidJarJarPluginExtension) o

        if (skipManifest != that.skipManifest) return false
        if (rules != that.rules) return false
        if (srcExcludes != that.srcExcludes) return false

        return true
    }

    int hashCode() {
        int result
        result = (rules != null ? rules.hashCode() : 0)
        result = 31 * result + (srcExcludes != null ? srcExcludes.hashCode() : 0)
        result = 31 * result + (skipManifest ? 1 : 0)
        return result
    }


    @Override
    public String toString() {
        return "AndroidJarJarPluginExtension{" +
                "rules=" + rules +
                ", srcExcludes=" + srcExcludes +
                ", skipManifest=" + skipManifest +
                '}';
    }
}
