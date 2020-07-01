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

package org.opencb.opencga.analysis.family.qc;

import org.apache.commons.collections.CollectionUtils;
import org.opencb.biodata.models.clinical.qc.RelatednessReport;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.analysis.StorageToolExecutor;
import org.opencb.opencga.analysis.individual.qc.IBDComputation;
import org.opencb.opencga.catalog.db.api.IndividualDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.exceptions.ToolException;
import org.opencb.opencga.core.models.individual.Individual;
import org.opencb.opencga.core.models.sample.Sample;
import org.opencb.opencga.core.response.OpenCGAResult;
import org.opencb.opencga.core.tools.annotations.ToolExecutor;
import org.opencb.opencga.core.tools.variant.FamilyQcAnalysisExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@ToolExecutor(id="opencga-local", tool = FamilyQcAnalysis.ID, framework = ToolExecutor.Framework.LOCAL,
        source = ToolExecutor.Source.STORAGE)
public class FamilyQcLocalAnalysisExecutor extends FamilyQcAnalysisExecutor implements StorageToolExecutor {

    private CatalogManager catalogManager;

    @Override
    public void run() throws ToolException {
        // Sanity check
        if (qualityControl == null) {
            throw new ToolException("Family quality control metrics is null");
        }

        catalogManager = getVariantStorageManager().getCatalogManager();

        switch (qcType) {
            case RELATEDNESS: {
                runRelatedness();
                break;
            }

            default: {
                throw new ToolException("Unknown quality control type: " + qcType);
            }
        }
    }

    private void runRelatedness() throws ToolException {

        if (CollectionUtils.isNotEmpty(qualityControl.getRelatedness())) {
            for (RelatednessReport relatedness : qualityControl.getRelatedness()) {
                if (relatednessMethod.equals(relatedness.getMethod())) {
                    addWarning("Skipping relatedness analysis: it was already computed for method " + relatednessMethod);
                    return;
                }
            }
        }

        // Get list of individual IDs
        List<String> individualIds = family.getMembers().stream().map(m -> m.getId()).collect(Collectors.toList());

        // Populate individual (and sample IDs) from individual IDs
        Query query = new Query(IndividualDBAdaptor.QueryParams.ID.key(), individualIds);
        QueryOptions queryOptions = new QueryOptions(QueryOptions.INCLUDE, "samples");

        List<String> sampleIds = new ArrayList<>();
        try {
            OpenCGAResult<Individual> individualResult = catalogManager.getIndividualManager().search(getStudyId(), query, queryOptions,
                    getToken());
            for (Individual individual : individualResult.getResults()) {
                if (CollectionUtils.isNotEmpty(individual.getSamples())) {
                    for (Sample sample : individual.getSamples()) {
                        if (!sample.isSomatic()) {
                            // We take the first no somatic sample for each individual
                            sampleIds.add(sample.getId());
                            break;
                        }
                    }
                }
            }
        } catch (CatalogException e) {
            throw new ToolException(e);
        }

        if (sampleIds.size() < 2) {
            addWarning("Skipping relatedness analysis: too few samples found (" + sampleIds.size() + ") for that family members;"
                    + " minimum is 2 member samples");
        } else {
            // Run IBD/IBS computation using PLINK in docker
            RelatednessReport report = IBDComputation.compute(getStudyId(), sampleIds, relatednessMaf, getOutDir(),
                    getVariantStorageManager(), getToken());

            // Sanity check
            if (report == null) {
                throw new ToolException("Something wrong when executing relatedness analysis for family " + family.getId());
            }

            // Save results
            qualityControl.getRelatedness().add(report);
        }
    }
}