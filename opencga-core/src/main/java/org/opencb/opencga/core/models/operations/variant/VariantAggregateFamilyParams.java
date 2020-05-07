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

package org.opencb.opencga.core.models.operations.variant;

import org.opencb.opencga.core.tools.ToolParams;

import java.util.List;

public class VariantAggregateFamilyParams extends ToolParams {
    public static final String DESCRIPTION = "Variant aggregate family params.";
    private List<String> samples;
    private boolean resume;

    public VariantAggregateFamilyParams() {
    }

    public VariantAggregateFamilyParams(List<String> samples, boolean resume) {
        this.samples = samples;
        this.resume = resume;
    }

    public boolean isResume() {
        return resume;
    }

    public VariantAggregateFamilyParams setResume(boolean resume) {
        this.resume = resume;
        return this;
    }

    public List<String> getSamples() {
        return samples;
    }

    public VariantAggregateFamilyParams setSamples(List<String> samples) {
        this.samples = samples;
        return this;
    }
}
