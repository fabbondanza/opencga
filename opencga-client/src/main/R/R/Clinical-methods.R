
# WARNING: AUTOGENERATED CODE
#
#    This code was generated by a tool.
#    Autogenerated on: 2020-06-01 00:45:37
#    
#    Manual changes to this file may cause unexpected behavior in your application.
#    Manual changes to this file will be overwritten if the code is regenerated.


# ##############################################################################
#' ClinicalClient methods
#' @include AllClasses.R
#' @include AllGenerics.R
#' @include commons.R

#' @description This function implements the OpenCGA calls for managing Analysis - Clinical.

#' The following table summarises the available *actions* for this client:
#'
#' | endpointName | Endpoint WS | parameters accepted |
#' | -- | :-- | --: |
#' | updateAcl | /{apiVersion}/analysis/clinical/acl/{members}/update | study, members[*], action[*], body[*] |
#' | create | /{apiVersion}/analysis/clinical/create | study, body[*] |
#' | searchInterpretation | /{apiVersion}/analysis/clinical/interpretation/search | include, exclude, limit, skip, sort, study, id, description, software, analyst, comment, status, creationDate, version, panel |
#' | runInterpreterCancerTiering | /{apiVersion}/analysis/clinical/interpreter/cancerTiering/run | study, jobId, jobDescription, jobDependsOn, jobTags, body[*] |
#' | runInterpreterTeam | /{apiVersion}/analysis/clinical/interpreter/team/run | study, jobId, jobDescription, jobDependsOn, jobTags, body[*] |
#' | runInterpreterTiering | /{apiVersion}/analysis/clinical/interpreter/tiering/run | study, jobId, jobDescription, jobDependsOn, jobTags, body[*] |
#' | runInterpreterZetta | /{apiVersion}/analysis/clinical/interpreter/zetta/run | study, jobId, jobDescription, jobDependsOn, jobTags, body[*] |
#' | search | /{apiVersion}/analysis/clinical/search | include, exclude, limit, skip, count, study, id, type, priority, status, creationDate, modificationDate, dueDate, description, family, proband, sample, analystAssignee, disorder, flags, deleted, release, attributes |
#' | actionableVariant | /{apiVersion}/analysis/clinical/variant/actionable | study, sample |
#' | queryVariant | /{apiVersion}/analysis/clinical/variant/query | include, exclude, limit, skip, count, approximateCount, approximateCountSamplingSize, savedFilter, id, region, type, study, file, filter, qual, fileData, sample, sampleData, sampleAnnotation, cohort, cohortStatsRef, cohortStatsAlt, cohortStatsMaf, cohortStatsMgf, cohortStatsPass, missingAlleles, missingGenotypes, score, family, familyDisorder, familySegregation, familyMembers, familyProband, gene, ct, xref, biotype, proteinSubstitution, conservation, populationFrequencyAlt, populationFrequencyRef, populationFrequencyMaf, transcriptFlag, geneTraitId, go, expression, proteinKeyword, drug, functionalScore, clinicalSignificance, customAnnotation, panel, trait |
#' | acl | /{apiVersion}/analysis/clinical/{clinicalAnalyses}/acl | clinicalAnalyses[*], study, member, silent |
#' | delete | /{apiVersion}/analysis/clinical/{clinicalAnalyses}/delete | study, clinicalAnalyses |
#' | update | /{apiVersion}/analysis/clinical/{clinicalAnalyses}/update | clinicalAnalyses, study, body[*] |
#' | info | /{apiVersion}/analysis/clinical/{clinicalAnalysis}/info | include, exclude, clinicalAnalysis, study, deleted |
#' | updateInterpretation | /{apiVersion}/analysis/clinical/{clinicalAnalysis}/interpretation/update | study, clinicalAnalysis, body[*] |
#' | updateQualityControl | /{apiVersion}/analysis/clinical/{clinicalAnalysis}/qualityControl/update | study, clinicalAnalysis, body[*] |
#' | updateSecondaryInterpretations | /{apiVersion}/analysis/clinical/{clinicalAnalysis}/secondaryInterpretations/update | clinicalAnalysis, study, action, body[*] |
#'
#' @md
#' @seealso \url{http://docs.opencb.org/display/opencga/Using+OpenCGA} and the RESTful API documentation
#' \url{http://bioinfo.hpc.cam.ac.uk/opencga-prod/webservices/}
#' [*]: Required parameter
#' @export

setMethod("clinicalClient", "OpencgaR", function(OpencgaR, clinicalAnalyses, clinicalAnalysis, members, endpointName, params=NULL, ...) {
    switch(endpointName,

        #' @section Endpoint /{apiVersion}/analysis/clinical/acl/{members}/update:
        #' Update the set of permissions granted for the member.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param members Comma separated list of user or group IDs.
        #' @param action Action to be performed [ADD, SET, REMOVE or RESET].
        #' @param data JSON containing the parameters to add ACLs.
        updateAcl=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical/acl",
                subcategoryId=members, action="update", params=params, httpMethod="POST", as.queryParam=c("action"),
                ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/create:
        #' Create a new clinical analysis.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param data JSON containing clinical analysis information.
        create=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=NULL, action="create", params=params, httpMethod="POST", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/interpretation/search:
        #' Clinical interpretation analysis.
        #' @param include Fields included in the response, whole JSON path must be provided.
        #' @param exclude Fields excluded in the response, whole JSON path must be provided.
        #' @param limit Number of results to be returned.
        #' @param skip Number of results to skip.
        #' @param sort Sort the results.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param id Interpretation ID.
        #' @param description Description.
        #' @param software Software.
        #' @param analyst Analyst.
        #' @param comment Comments.
        #' @param status Status.
        #' @param creationDate Creation date.
        #' @param version Version.
        #' @param panel List of panels.
        searchInterpretation=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/interpretation", subcategoryId=NULL, action="search", params=params,
                httpMethod="GET", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/interpreter/cancerTiering/run:
        #' Run cancer tiering interpretation analysis.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param jobId Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
        #' @param jobDescription Job description.
        #' @param jobDependsOn Comma separated list of existing job IDs the job will depend on.
        #' @param jobTags Job tags.
        #' @param data Cancer tiering interpretation analysis params.
        runInterpreterCancerTiering=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/interpreter/cancerTiering", subcategoryId=NULL, action="run", params=params,
                httpMethod="POST", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/interpreter/team/run:
        #' Run TEAM interpretation analysis.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param jobId Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
        #' @param jobDescription Job description.
        #' @param jobDependsOn Comma separated list of existing job IDs the job will depend on.
        #' @param jobTags Job tags.
        #' @param data TEAM interpretation analysis params.
        runInterpreterTeam=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/interpreter/team", subcategoryId=NULL, action="run", params=params,
                httpMethod="POST", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/interpreter/tiering/run:
        #' Run tiering interpretation analysis.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param jobId Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
        #' @param jobDescription Job description.
        #' @param jobDependsOn Comma separated list of existing job IDs the job will depend on.
        #' @param jobTags Job tags.
        #' @param data Tiering interpretation analysis params.
        runInterpreterTiering=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/interpreter/tiering", subcategoryId=NULL, action="run", params=params,
                httpMethod="POST", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/interpreter/zetta/run:
        #' Run Zetta interpretation analysis.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param jobId Job ID. It must be a unique string within the study. An ID will be autogenerated automatically if not provided.
        #' @param jobDescription Job description.
        #' @param jobDependsOn Comma separated list of existing job IDs the job will depend on.
        #' @param jobTags Job tags.
        #' @param data Zetta interpretation analysis params.
        runInterpreterZetta=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/interpreter/zetta", subcategoryId=NULL, action="run", params=params,
                httpMethod="POST", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/search:
        #' Clinical analysis search.
        #' @param include Fields included in the response, whole JSON path must be provided.
        #' @param exclude Fields excluded in the response, whole JSON path must be provided.
        #' @param limit Number of results to be returned.
        #' @param skip Number of results to skip.
        #' @param count Get the total number of results matching the query. Deactivated by default.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param id Clinical analysis ID.
        #' @param type Clinical analysis type.
        #' @param priority Priority.
        #' @param status Clinical analysis status.
        #' @param creationDate Creation date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        #' @param modificationDate Modification date. Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805.
        #' @param dueDate Due date (Format: yyyyMMddHHmmss. Examples: >2018, 2017-2018, <201805...).
        #' @param description Description.
        #' @param family Family.
        #' @param proband Proband.
        #' @param sample Proband sample.
        #' @param analystAssignee Clinical analyst assignee.
        #' @param disorder Disorder ID or name.
        #' @param flags Flags.
        #' @param deleted Boolean to retrieve deleted entries.
        #' @param release Release value.
        #' @param attributes Text attributes (Format: sex=male,age>20 ...).
        search=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=NULL, action="search", params=params, httpMethod="GET", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/variant/actionable:
        #' Fetch actionable clinical variants.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param sample Sample ID.
        actionableVariant=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/variant", subcategoryId=NULL, action="actionable", params=params,
                httpMethod="GET", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/variant/query:
        #' Fetch clinical variants.
        #' @param include Fields included in the response, whole JSON path must be provided.
        #' @param exclude Fields excluded in the response, whole JSON path must be provided.
        #' @param limit Number of results to be returned.
        #' @param skip Number of results to skip.
        #' @param count Get the total number of results matching the query. Deactivated by default.
        #' @param approximateCount Get an approximate count, instead of an exact total count. Reduces execution time.
        #' @param approximateCountSamplingSize Sampling size to get the approximate count. Larger values increase accuracy but also increase execution time.
        #' @param savedFilter Use a saved filter at User level.
        #' @param id List of IDs, these can be rs IDs (dbSNP) or variants in the format chrom:start:ref:alt, e.g. rs116600158,19:7177679:C:T.
        #' @param region List of regions, these can be just a single chromosome name or regions in the format chr:start-end, e.g.: 2,3:100000-200000.
        #' @param type List of types, accepted values are SNV, MNV, INDEL, SV, CNV, INSERTION, DELETION, e.g. SNV,INDEL.
        #' @param study Filter variants from the given studies, these can be either the numeric ID or the alias with the format user@project:study.
        #' @param file Filter variants from the files specified. This will set includeFile parameter when not provided.
        #' @param filter Specify the FILTER for any of the files. If 'file' filter is provided, will match the file and the filter. e.g.: PASS,LowGQX.
        #' @param qual Specify the QUAL for any of the files. If 'file' filter is provided, will match the file and the qual. e.g.: >123.4.
        #' @param fileData Filter by file data (i.e. INFO column from VCF file). [{file}:]{key}{op}{value}[,;]* . If no file is specified, will use all files from "file" filter. e.g. AN>200 or file_1.vcf:AN>200;file_2.vcf:AN<10 . Many INFO fields can be combined. e.g. file_1.vcf:AN>200;DB=true;file_2.vcf:AN<10.
        #' @param sample Filter variants by sample genotype. This will automatically set 'includeSample' parameter when not provided. This filter accepts multiple 3 forms: 1) List of samples: Samples that contain the main variant. Accepts AND (;) and OR (,) operators.  e.g. HG0097,HG0098 . 2) List of samples with genotypes: {sample}:{gt1},{gt2}. Accepts AND (;) and OR (,) operators.  e.g. HG0097:0/0;HG0098:0/1,1/1 . Unphased genotypes (e.g. 0/1, 1/1) will also include phased genotypes (e.g. 0|1, 1|0, 1|1), but not vice versa. When filtering by multi-allelic genotypes, any secondary allele will match, regardless of its position e.g. 1/2 will match with genotypes 1/2, 1/3, 1/4, .... Genotype aliases accepted: HOM_REF, HOM_ALT, HET, HET_REF, HET_ALT and MISS  e.g. HG0097:HOM_REF;HG0098:HET_REF,HOM_ALT . 3) Sample with segregation mode: {sample}:{segregation}. Only one sample accepted.Accepted segregation modes: [ monoallelic, monoallelicIncompletePenetrance, biallelic, biallelicIncompletePenetrance, XlinkedBiallelic, XlinkedMonoallelic, Ylinked, MendelianError, DeNovo, CompoundHeterozygous ]. Value is case insensitive. e.g. HG0097:DeNovo Sample must have parents defined and indexed. .
        #' @param sampleData Filter by any SampleData field from samples. [{sample}:]{key}{op}{value}[,;]* . If no sample is specified, will use all samples from "sample" or "genotype" filter. e.g. DP>200 or HG0097:DP>200,HG0098:DP<10 . Many FORMAT fields can be combined. e.g. HG0097:DP>200;GT=1/1,0/1,HG0098:DP<10.
        #' @param sampleAnnotation Selects some samples using metadata information from Catalog. e.g. age>20;phenotype=hpo:123,hpo:456;name=smith.
        #' @param cohort Select variants with calculated stats for the selected cohorts.
        #' @param cohortStatsRef Reference Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4.
        #' @param cohortStatsAlt Alternate Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4.
        #' @param cohortStatsMaf Minor Allele Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4.
        #' @param cohortStatsMgf Minor Genotype Frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL<=0.4.
        #' @param cohortStatsPass Filter PASS frequency: [{study:}]{cohort}[<|>|<=|>=]{number}. e.g. ALL>0.8.
        #' @param missingAlleles Number of missing alleles: [{study:}]{cohort}[<|>|<=|>=]{number}.
        #' @param missingGenotypes Number of missing genotypes: [{study:}]{cohort}[<|>|<=|>=]{number}.
        #' @param score Filter by variant score: [{study:}]{score}[<|>|<=|>=]{number}.
        #' @param family Filter variants where any of the samples from the given family contains the variant (HET or HOM_ALT).
        #' @param familyDisorder Specify the disorder to use for the family segregation.
        #' @param familySegregation Filter by segregation mode from a given family. Accepted values: [ monoallelic, monoallelicIncompletePenetrance, biallelic, biallelicIncompletePenetrance, XlinkedBiallelic, XlinkedMonoallelic, Ylinked, MendelianError, DeNovo, CompoundHeterozygous ].
        #' @param familyMembers Sub set of the members of a given family.
        #' @param familyProband Specify the proband child to use for the family segregation.
        #' @param gene List of genes, most gene IDs are accepted (HGNC, Ensembl gene, ...). This is an alias to 'xref' parameter.
        #' @param ct List of SO consequence types, e.g. missense_variant,stop_lost or SO:0001583,SO:0001578.
        #' @param xref List of any external reference, these can be genes, proteins or variants. Accepted IDs include HGNC, Ensembl genes, dbSNP, ClinVar, HPO, Cosmic, ...
        #' @param biotype List of biotypes, e.g. protein_coding.
        #' @param proteinSubstitution Protein substitution scores include SIFT and PolyPhen. You can query using the score {protein_score}[<|>|<=|>=]{number} or the description {protein_score}[~=|=]{description} e.g. polyphen>0.1,sift=tolerant.
        #' @param conservation Filter by conservation score: {conservation_score}[<|>|<=|>=]{number} e.g. phastCons>0.5,phylop<0.1,gerp>0.1.
        #' @param populationFrequencyAlt Alternate Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01.
        #' @param populationFrequencyRef Reference Population Frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01.
        #' @param populationFrequencyMaf Population minor allele frequency: {study}:{population}[<|>|<=|>=]{number}. e.g. 1kG_phase3:ALL<0.01.
        #' @param transcriptFlag List of transcript annotation flags. e.g. CCDS, basic, cds_end_NF, mRNA_end_NF, cds_start_NF, mRNA_start_NF, seleno.
        #' @param geneTraitId List of gene trait association id. e.g. "umls:C0007222" , "OMIM:269600".
        #' @param go List of GO (Gene Ontology) terms. e.g. "GO:0002020".
        #' @param expression List of tissues of interest. e.g. "lung".
        #' @param proteinKeyword List of Uniprot protein variant annotation keywords.
        #' @param drug List of drug names.
        #' @param functionalScore Functional score: {functional_score}[<|>|<=|>=]{number} e.g. cadd_scaled>5.2 , cadd_raw<=0.3.
        #' @param clinicalSignificance Clinical significance: benign, likely_benign, likely_pathogenic, pathogenic.
        #' @param customAnnotation Custom annotation: {key}[<|>|<=|>=]{number} or {key}[~=|=]{text}.
        #' @param panel Filter by genes from the given disease panel.
        #' @param trait List of traits, based on ClinVar, HPO, COSMIC, i.e.: IDs, histologies, descriptions,...
        queryVariant=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL,
                subcategory="clinical/variant", subcategoryId=NULL, action="query", params=params, httpMethod="GET",
                as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalyses}/acl:
        #' Returns the acl of the clinical analyses. If member is provided, it will only return the acl for the member.
        #' @param clinicalAnalyses Comma separated list of clinical analysis IDs or names up to a maximum of 100.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param member User or group ID.
        #' @param silent Boolean to retrieve all possible entries that are queried for, false to raise an exception whenever one of the entries looked for cannot be shown for whichever reason.
        acl=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=clinicalAnalyses, action="acl", params=params, httpMethod="GET", as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalyses}/delete:
        #' Delete clinical analyses.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param clinicalAnalyses Comma separated list of clinical analysis IDs or names up to a maximum of 100.
        delete=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=clinicalAnalyses, action="delete", params=params, httpMethod="DELETE",
                as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalyses}/update:
        #' Update clinical analysis attributes.
        #' @param clinicalAnalyses Comma separated list of clinical analysis IDs.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param data JSON containing clinical analysis information.
        update=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=clinicalAnalyses, action="update", params=params, httpMethod="POST", as.queryParam=NULL,
                ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalysis}/info:
        #' Clinical analysis info.
        #' @param include Fields included in the response, whole JSON path must be provided.
        #' @param exclude Fields excluded in the response, whole JSON path must be provided.
        #' @param clinicalAnalysis Comma separated list of clinical analysis IDs or names up to a maximum of 100.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param deleted Boolean to retrieve deleted entries.
        info=fetchOpenCGA(object=OpencgaR, category="analysis", categoryId=NULL, subcategory="clinical",
                subcategoryId=clinicalAnalysis, action="info", params=params, httpMethod="GET", as.queryParam=NULL,
                ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalysis}/interpretation/update:
        #' Update interpretation fields of primary interpretation.
        #' @param study [[user@]project:]study ID.
        #' @param clinicalAnalysis Clinical analysis ID.
        #' @param data JSON containing clinical interpretation information.
        updateInterpretation=fetchOpenCGA(object=OpencgaR, category="analysis/clinical", categoryId=clinicalAnalysis,
                subcategory="interpretation", subcategoryId=NULL, action="update", params=params, httpMethod="POST",
                as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalysis}/qualityControl/update:
        #' Update quality control fields of clinical analysis.
        #' @param study [[user@]project:]study ID.
        #' @param clinicalAnalysis Clinical analysis ID.
        #' @param data JSON containing quality control information.
        updateQualityControl=fetchOpenCGA(object=OpencgaR, category="analysis/clinical", categoryId=clinicalAnalysis,
                subcategory="qualityControl", subcategoryId=NULL, action="update", params=params, httpMethod="POST",
                as.queryParam=NULL, ...),

        #' @section Endpoint /{apiVersion}/analysis/clinical/{clinicalAnalysis}/secondaryInterpretations/update:
        #' Add or remove secondary interpretations to/from a Clinical Analysis.
        #' @param clinicalAnalysis Clinical analysis ID.
        #' @param study Study [[user@]project:]study where study and project can be either the ID or UUID.
        #' @param action Action to be performed if the array of interpretations is being updated.
        #' @param data JSON containing clinical analysis information.
        updateSecondaryInterpretations=fetchOpenCGA(object=OpencgaR, category="analysis/clinical",
                categoryId=clinicalAnalysis, subcategory="secondaryInterpretations", subcategoryId=NULL,
                action="update", params=params, httpMethod="POST", as.queryParam=NULL, ...),
    )
})