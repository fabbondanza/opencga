/*
 * Copyright 2015-2016 OpenCB
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

package org.opencb.opencga.catalog.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.catalog.db.api.JobDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.exceptions.CatalogIOException;
import org.opencb.opencga.catalog.io.CatalogIOManager;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.catalog.utils.FileScanner;
import org.opencb.opencga.core.common.UriUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.opencb.opencga.catalog.monitor.executors.AbstractExecutor.JOB_STATUS_FILE;

/*
 * Created by jacobo on 4/11/14.
 *
 *  Scans the temporal output directory from a job to find all generated files.
 *  Modifies the job status to set the output and endTime.
 *  If the job was type:INDEX, modify the index status.
 */

public class ExecutionOutputRecorder {

    private static Logger logger = LoggerFactory.getLogger(ExecutionOutputRecorder.class);
    private final CatalogManager catalogManager;
    private CatalogIOManager ioManager;
    private final String sessionId;
    private final boolean calculateChecksum = false;    //TODO: Read from config file
    private final FileScanner.FileScannerPolicy fileScannerPolicy = FileScanner.FileScannerPolicy.DELETE; //TODO: Read from config file
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExecutionOutputRecorder(CatalogManager catalogManager, String sessionId) {
        this.catalogManager = catalogManager;
        this.sessionId = sessionId;
    }

    @Deprecated
    public void recordJobOutputAndPostProcess(Job job, boolean jobFailed) throws CatalogException {

    }

    public void recordJobOutputAndPostProcess(Job job, String status) throws CatalogException, IOException, URISyntaxException {
        /** Modifies the job to set the output and endTime. **/
        URI uri = UriUtils.createUri(catalogManager.getConfiguration().getTempJobsDir());
        Path tmpOutdirPath = Paths.get(uri.getPath()).resolve("J_" + job.getId());
//        Path tmpOutdirPath = Paths.get(catalogManager.getCatalogConfiguration().getTempJobsDir(), "J_" + job.getId());
        this.ioManager = catalogManager.getCatalogIOManagerFactory().get(tmpOutdirPath.toUri());
        recordJobOutput(job, tmpOutdirPath);
        updateJobStatus(job, new Job.JobStatus(status));
    }

    @Deprecated
    public void recordJobOutput(Job job) {
    }

    /**
     * Scans the temporal output folder for the job and adds all the output files to catalog.
     *
     * @param job job.
     * @param tmpOutdirPath Temporal output directory path.
     * @throws CatalogException catalogException.
     * @throws IOException ioException.
     */
    public void recordJobOutput(Job job, Path tmpOutdirPath) throws CatalogException, IOException {
        logger.debug("Moving data from temporary folder to catalog folder...");

        // Delete job.status file
        Path path = Paths.get(tmpOutdirPath.toString(), JOB_STATUS_FILE);
        if (path.toFile().exists()) {
            logger.info("Deleting " + JOB_STATUS_FILE + " file: {}", path.toUri());
            try {
                ioManager.deleteFile(path.toUri());
            } catch (CatalogIOException e) {
                logger.error("Could not delete " + JOB_STATUS_FILE + " file");
                throw e;
            }
        }

        URI tmpOutDirUri = tmpOutdirPath.toUri();
        /* Scans the output directory from a job or index to find all files. **/
        logger.debug("Scan the temporal output directory ({}) from a job to find all generated files.", tmpOutDirUri);
        File outDir;
        try {
            outDir = catalogManager.getFileManager().get(job.getOutDirId(), new QueryOptions(), sessionId).getResult().get(0);
        } catch (CatalogException e) {
            logger.error("Cannot find file {}. Error: {}", job.getOutDirId(), e.getMessage());
            throw e;
        }

        FileScanner fileScanner = new FileScanner(catalogManager);
        List<File> files;
        try {
            logger.info("Scanning files from {} to move to {}", outDir.getPath(), tmpOutdirPath);
            files = fileScanner.scan(outDir, tmpOutDirUri, fileScannerPolicy, calculateChecksum, true, uri -> true, job.getId(), sessionId);
        } catch (IOException e) {
            logger.warn("IOException when scanning temporal directory. Error: {}", e.getMessage());
            throw e;
        } catch (CatalogException e) {
            logger.warn("CatalogException when scanning temporal directory. Error: {}", e.getMessage());
            throw e;
        }
        List<Long> fileIds = files.stream().map(File::getId).collect(Collectors.toList());
        if (!ioManager.exists(tmpOutDirUri)) {
            logger.warn("Output folder doesn't exist");
            return;
        }
        List<URI> uriList;
        try {
            uriList = ioManager.listFiles(tmpOutDirUri);
        } catch (CatalogIOException e) {
            logger.warn("Could not obtain the URI of the files within the directory {}", tmpOutDirUri);
            logger.error(e.getMessage());
            throw e;
        }
        if (uriList.isEmpty()) {
            try {
                ioManager.deleteDirectory(tmpOutDirUri);
            } catch (CatalogIOException e) {
                if (ioManager.exists(tmpOutDirUri)) {
                    logger.error("Could not delete empty directory {}. Error: {}", tmpOutDirUri, e.getMessage());
                    throw e;
                }
            }
        } else {
            logger.error("Error processing job output. Temporal job out dir is not empty. " + uriList);
        }

        ObjectMap parameters = new ObjectMap();
        parameters.put(JobDBAdaptor.QueryParams.OUTPUT.key(), fileIds);
        parameters.put(JobDBAdaptor.QueryParams.END_TIME.key(), System.currentTimeMillis());
        try {
            catalogManager.modifyJob(job.getId(), parameters, this.sessionId);
        } catch (CatalogException e) {
            logger.error("Critical error. Could not update job output files from job {} with output {}. Error: {}", job.getId(),
                    fileIds.toArray(), e.getMessage());
            throw e;
        }

        //TODO: "input" files could be modified by the tool. Have to be scanned, calculate the new Checksum and
    }

    public void updateJobStatus(Job job, Job.JobStatus jobStatus) throws CatalogException {
        if (jobStatus != null) {
            if (jobStatus.getName().equalsIgnoreCase(Job.JobStatus.DONE)) {
                jobStatus.setName(Job.JobStatus.READY);
                jobStatus.setMessage("The job has finished");
            } else if (jobStatus.getName().equalsIgnoreCase(Job.JobStatus.ERROR)) {
                jobStatus.setName(Job.JobStatus.ERROR);
                jobStatus.setMessage("The job finished with an error");
            } else {
                logger.error("This block should never be executed. Accepted status in " + JOB_STATUS_FILE + " file are DONE and ERROR");
                jobStatus.setName(Job.JobStatus.ERROR);
                jobStatus.setMessage("The finished with an unexpected error");
            }
//            ObjectMap params = new ObjectMap(JobDBAdaptor.QueryParams.STATUS.key(), jobStatus);
//            catalogManager.getJobManager().update(job.getId(), params, new QueryOptions(), sessionId);
            catalogManager.getJobManager().setStatus(Long.toString(job.getId()), jobStatus.getName(), jobStatus.getMessage(), sessionId);
        } else {
            logger.error("This code should never be executed.");
            throw new CatalogException("Job status = null");
        }
    }

    @Deprecated
    public void postProcessJob(Job job, boolean jobFailed) throws CatalogException {
    }

}
