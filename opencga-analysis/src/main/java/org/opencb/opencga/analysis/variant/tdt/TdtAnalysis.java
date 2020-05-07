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

package org.opencb.opencga.analysis.variant.tdt;

import org.opencb.opencga.analysis.tools.OpenCgaTool;
import org.opencb.opencga.core.models.common.Enums;
import org.opencb.opencga.core.tools.variant.TdtAnalysisExecutor;
import org.opencb.opencga.core.tools.annotations.Tool;
import org.opencb.opencga.core.exceptions.ToolException;


@Tool(id = TdtAnalysis.ID, resource = Enums.Resource.VARIANT)
public class TdtAnalysis extends OpenCgaTool {
    public static final String ID = "tdt";

    private String phenotype;

    @Override
    protected void check() throws ToolException {
        // checks
    }

    @Override
    public void run() throws ToolException {
        step(() -> {
            getToolExecutor(TdtAnalysisExecutor.class)
                    .execute();
        });
    }

}
