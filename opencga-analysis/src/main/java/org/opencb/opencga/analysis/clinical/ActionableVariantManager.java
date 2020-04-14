/*
 * Copyright 2015-2020 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.analysis.clinical;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantBuilder;
import org.opencb.commons.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ActionableVariantManager {
    // Folder where actionable variant files are located, multiple assemblies are supported, i.e.: one variant actionable file per assembly
    // File name format: actionableVariants_xxx.txt[.gz] where xxx = assembly in lower case
    private Path path;

    // We keep a Map for each assembly with a Map of variant IDs with the phenotype list
    private static Map<String, Map<String, List<String>>> actionableVariants = null;

    public ActionableVariantManager(Path path) {
        this.path = path;
    }

    public Map<String, List<String>> getActionableVariants(String assembly) throws IOException {
        // Lazy loading
        if (actionableVariants == null) {
            actionableVariants = loadActionableVariants();
        }

        if (actionableVariants.containsKey(assembly)) {
            return actionableVariants.get(assembly);
        }
        return null;
    }


    private Map<String, Map<String, List<String>>> loadActionableVariants() throws IOException {
        // Load actionable variants for each assembly, if present
        // First, read all actionableVariants filenames, actionableVariants_xxx.txt[.gz] where xxx = assembly in lower case
        Map<String, Map<String, List<String>>> actionableVariantsByAssembly = new HashMap<>();
        File folder = path.toFile();
        File[] files = folder.listFiles();
        if (files != null && ArrayUtils.isNotEmpty(files)) {
            for (File file : files) {
                if (file.isFile() && file.getName().startsWith("actionableVariants_")) {
                    // Split by _ and .  to fetch assembly
                    String[] split = file.getName().split("[_\\.]");
                    if (split.length > 1) {
                        String assembly = split[1].toLowerCase();
                        actionableVariantsByAssembly.put(assembly, loadActionableVariants(file));
                    }
                }
            }
        }
        return actionableVariantsByAssembly;
    }

    /**
     * Returns a Map of variant IDs with a alist of phenotypes.
     * @param file OpenCGA home installation folder
     * @return Map of variant IDs with a alist of phenotypes
     * @throws IOException If file is not found
     */
    private Map<String, List<String>> loadActionableVariants(File file) throws IOException {

//        System.out.println("ActionableVariantManager: path = " + file.toString());

        Map<String, List<String>> actionableVariants = new HashMap<>();

        if (file != null && file.exists()) {
            BufferedReader bufferedReader = FileUtils.newBufferedReader(file.toPath());
            List<String> lines = bufferedReader.lines().collect(Collectors.toList());
            for (String line : lines) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] split = line.split("\t");
                if (split.length > 4) {
                    List<String> phenotypes = new ArrayList<>();
                    if (split.length > 5 && StringUtils.isNotEmpty(split[5])) {
                        phenotypes.addAll(Arrays.asList(split[5].split(";")));
                    }
                    try {
                        String ref = split[3];
                        if (ref.equals("-") || ref.equals("na")) {
                            ref = ".";
                        }
                        String alt = split[4];
                        if (alt.equals("-") || alt.equals("na")) {
                            alt = ".";
                        }
                        Variant variant = new VariantBuilder(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]), ref, alt)
                                .build();
                        actionableVariants.put(variant.toString(), phenotypes);
                    } catch (NumberFormatException e) {
                        // Skip this variant
                        System.err.println("Skip actionable variant: " + line + "\nCause: " + e.getMessage());
                    }
                } else {
                    // Skip this variant
                    System.err.println("Skip actionable variant, invalid format: " + line);
                }
            }
        }

//        System.out.println("ActionableVariantManager: size = " + actionableVariants.size());

        return actionableVariants;
    }
}
