package com.jeelvankhede.pixelperfect;

import com.google.common.base.*;
import com.google.common.io.*;
import org.gradle.api.*;
import org.gradle.api.resources.*;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class DimensionGeneratorTask extends DefaultTask {
    private File intermediateDir;
    private String packageNameXOR1;
    private TextResource packageNameXOR2;
    private PixelPerfectPlugin.DimensionContainerExtension extension;
    private final Set<Density> dipsDirs = new HashSet<>();

    @OutputDirectory
    public File getIntermediateDir() {
        return intermediateDir;
    }

    /**
     * Either packageNameXOR1 or packageNameXOR2 must be present, but both must be marked as @Optional or Gradle
     * will throw an exception if one is missing.
     */
    @Input
    @Optional
    public String getPackageNameXOR1() {
        return packageNameXOR1;
    }

    @Input
    @Optional
    public TextResource getPackageNameXOR2() {
        return packageNameXOR2;
    }

    public void setIntermediateDir(File intermediateDir) {
        this.intermediateDir = intermediateDir;
    }

    public void setPackageNameXOR1(String packageNameXOR1) {
        this.packageNameXOR1 = packageNameXOR1;
    }

    public void setPackageNameXOR2(TextResource packageNameXOR2) {
        this.packageNameXOR2 = packageNameXOR2;
    }

    public void setExtension(PixelPerfectPlugin.DimensionContainerExtension extension) {
        this.extension = extension;
    }

    @TaskAction
    public void action() throws IOException {
        if (packageNameXOR1 == null && packageNameXOR2 == null) {
            throw new GradleException("One of packageNameXOR1 or packageNameXOR2 are required:" +
                    " packageNameXOR1: null, packageNameXOR2: null");
        }
        deleteFolder(intermediateDir);
        if (!intermediateDir.mkdirs()) {
            throw new GradleException("Failed to create folder: " + intermediateDir);
        }
        if (!extension.getDpis().isEmpty()) {
            extension.getDpis().forEach(value -> {
                switch (value) {
                    case "ldpi": {
                        dipsDirs.add(new Density("-" + value, 0.75f));
                        break;
                    }
                    case "mdpi": {
                        dipsDirs.add(new Density("-" + value, 1f));
                        break;
                    }
                    case "hdpi": {
                        dipsDirs.add(new Density("-" + value, 1.5f));
                        break;
                    }
                    case "xhdpi": {
                        dipsDirs.add(new Density("-" + value, 2f));
                        break;
                    }
                    case "xxhdpi": {
                        dipsDirs.add(new Density("-" + value, 3f));
                        break;
                    }
                    case "xxxhdpi": {
                        dipsDirs.add(new Density("-" + value, 4f));
                        break;
                    }
                }
            });
        }
        dipsDirs.add(new Density("ldpi", 0.75f));
        dipsDirs.add(new Density("mdpi", 1f));
        dipsDirs.add(new Density("hdpi", 1.5f));
        dipsDirs.add(new Density("xhdpi", 2f));
        dipsDirs.add(new Density("xxhdpi", 3f));
        dipsDirs.add(new Density("xxxhdpi", 4f));
        dipsDirs.forEach(dirValue -> {
            File values = new File(intermediateDir, "values-" + dirValue);
            if (!values.exists() && !values.mkdirs()) {
                throw new GradleException("Failed to create folder: " + values);
            }
            Map<String, String> mappedValues = generateDimensionsMap(1, 100, dirValue.ratio);
            String fileContent = getFileContent(mappedValues);
            System.out.println("file content : \n" + fileContent);
            try {
                Files.asCharSink(new File(values, "dimens.xml"), Charsets.UTF_8)
                        .write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
                throw new GradleException("Failed to write to dimensions file: " + values);
            }
        });
    }

    static Map<String, String> generateDimensionsMap(int startIndex, int endIndex, float dpiRatio) {
        HashMap<String, String> map = new HashMap<>();
        for (int index = startIndex; index <= endIndex; index++) {
            map.put(index+"dp", index * dpiRatio+"dp");
        }
        return map;
    }

    private static String getFileContent(Map<String, String> values) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<resources>\n");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            sb.append("    <dimen name=\"")
                    .append(entry.getKey())
                    .append("\" translatable=\"false\">")
                    .append(entry.getValue())
                    .append("</dimen>\n");
        }
        sb.append("</resources>\n");
        return sb.toString();
    }

    private static void deleteFolder(final File folder) {
        if (!folder.exists()) {
            return;
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    if (!file.delete()) {
                        throw new GradleException("Failed to delete: " + file);
                    }
                }
            }
        }
        if (!folder.delete()) {
            throw new GradleException("Failed to delete: " + folder);
        }
    }

    private static class Density {
        private final String dpi;
        private final Float ratio;

        public Density(String dpi, Float ratio) {
            this.dpi = dpi;
            this.ratio = ratio;
        }

        public String getDpi() {
            return dpi;
        }

        public Float getRatio() {
            return ratio;
        }
    }
}