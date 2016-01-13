/*
 * Copyright (C) 2015 House365. All rights reserved.
 */

package com.tonicsystems.jarjar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by ZhangZhenli on 2016/1/13.
 */
public class DirectoryProcessor {

    public static void run(File from, File to, com.tonicsystems.jarjar.util.JarProcessor proc) throws java.io.IOException {
        run(from, to, proc, false);
    }

    public static void run(File from, File to, com.tonicsystems.jarjar.util.JarProcessor proc, boolean ignoreDuplicates) throws java.io.IOException {


        final File tmpTo = File.createTempFile("jarjar", ".jar");
        JarOutputStream out = new JarOutputStream(new FileOutputStream(tmpTo));
        java.util.Set<String> entries = new java.util.HashSet<String>();
        try {
            com.tonicsystems.jarjar.util.EntryStruct struct = new com.tonicsystems.jarjar.util.EntryStruct();
            java.util.Collection<File> listFiles = FileUtils.listFiles(from, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
            for (File file : listFiles) {
                struct.name = file.getName();
                struct.time = file.lastModified();
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                FileInputStream fileInputStream = new FileInputStream(file);
                IOUtils.copy(fileInputStream, baos);
                struct.data = baos.toByteArray();
                if (proc.process(struct)) {
                    if (entries.add(struct.name)) {
                        java.util.jar.JarEntry entry = new JarEntry(struct.name);
                        entry.setTime(struct.time);
                        entry.setCompressedSize(-1);
                        out.putNextEntry(entry);
                        out.write(struct.data);
                    } else if (struct.name.endsWith("/")) {
                        // TODO(chrisn): log
                    } else if (!ignoreDuplicates) {
                        throw new IllegalArgumentException("Duplicate jar entries: " + struct.name);
                    }
                }
                try {
                    fileInputStream.close();
                } catch (java.io.IOException e) {
                }
            }

        } finally {
            try {
                out.close();
            } catch (java.io.IOException e) {
            }
        }

        // delete the empty directories
        copyZipWithoutEmptyDirectories(tmpTo, to);
        tmpTo.delete();

    }

    /**
     * Create a copy of an zip file without its empty directories.
     *
     * @param inputFile
     * @param outputFile
     * @throws IOException
     */
    public static void copyZipWithoutEmptyDirectories(final File inputFile, final File outputFile) throws IOException {

        final ZipFile inputZip = new ZipFile(inputFile);
        final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(outputFile));
        try {
            // read a the entries of the input zip file and sort them
            final java.util.Enumeration<? extends ZipEntry> e = inputZip.entries();
            final java.util.ArrayList<ZipEntry> sortedList = new java.util.ArrayList<ZipEntry>();
            while (e.hasMoreElements()) {
                final ZipEntry entry = e.nextElement();
                // META-INF/ doesn't need a directory entry
                if (!"META-INF/".equals(entry.getName())) {
                    sortedList.add(entry);
                }
            }

            java.util.Collections.sort(sortedList, new java.util.Comparator<ZipEntry>() {
                public int compare(ZipEntry o1, ZipEntry o2) {
                    String n1 = o1.getName(), n2 = o2.getName();
                    if (metaOverride(n1, n2)) {
                        return -1;
                    }
                    if (metaOverride(n2, n1)) {
                        return 1;
                    }
                    return n1.compareTo(n2);
                }

                // make sure that META-INF/MANIFEST.MF is always the very first entry
                private boolean metaOverride(String n1, String n2) {
                    return (n1.startsWith("META-INF/") && !n2.startsWith("META-INF/"))
                            || (n1.equals("META-INF/MANIFEST.MF") && !n2.equals(n1));
                }
            });

            // treat them again and write them in output, wenn they not are empty directories
            for (int i = sortedList.size() - 1; i >= 0; i--) {
                final ZipEntry inputEntry = sortedList.get(i);
                final String name = inputEntry.getName();
                final boolean isEmptyDirectory;
                if (inputEntry.isDirectory()) {
                    if (i == sortedList.size() - 1) {
                        // no item afterwards; it was an empty directory
                        isEmptyDirectory = true;
                    } else {
                        final String nextName = sortedList.get(i + 1).getName();
                        isEmptyDirectory = !nextName.startsWith(name);
                    }
                } else {
                    isEmptyDirectory = false;
                }

                if (isEmptyDirectory) {
                    sortedList.remove(i);
                }
            }

            // finally write entries in normal order
            for (int i = 0; i < sortedList.size(); i++) {
                final ZipEntry inputEntry = sortedList.get(i);
                final ZipEntry outputEntry = new ZipEntry(inputEntry);
                outputStream.putNextEntry(outputEntry);
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                final java.io.InputStream is = inputZip.getInputStream(inputEntry);
                org.apache.commons.io.IOUtils.copy(is, baos);
                is.close();
                outputStream.write(baos.toByteArray());
            }
        } finally {
            outputStream.close();
            inputZip.close();
        }

    }
}
