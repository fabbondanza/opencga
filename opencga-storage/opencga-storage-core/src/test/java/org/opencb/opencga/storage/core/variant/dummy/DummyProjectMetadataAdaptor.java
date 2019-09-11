package org.opencb.opencga.storage.core.variant.dummy;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.commons.datastore.core.result.WriteResult;
import org.opencb.opencga.storage.core.exceptions.StorageEngineException;
import org.opencb.opencga.storage.core.metadata.adaptors.ProjectMetadataAdaptor;
import org.opencb.opencga.storage.core.metadata.models.ProjectMetadata;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created on 02/05/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class DummyProjectMetadataAdaptor implements ProjectMetadataAdaptor {
    private static ProjectMetadata projectMetadata;
    private static Map<String, Integer> counters = new HashMap<>();

    @Override
    public long lockProject(long lockDuration, long timeout) throws InterruptedException, TimeoutException {
        return 0;
    }

    @Override
    public void unLockProject(long lockId) {
    }

    @Override
    public synchronized QueryResult<ProjectMetadata> getProjectMetadata() {
        final QueryResult<ProjectMetadata> result = new QueryResult<>("");
        if (projectMetadata == null) {
            projectMetadata = new ProjectMetadata("hsapiens", "grch37", 1);
        }
        result.setResult(Collections.singletonList(projectMetadata.copy()));
        return result;
    }

    @Override
    public synchronized WriteResult updateProjectMetadata(ProjectMetadata projectMetadata, boolean updateCounters) {
        DummyProjectMetadataAdaptor.projectMetadata = projectMetadata;
        return new WriteResult();
    }

    @Override
    public synchronized int generateId(Integer studyId, String idType) throws StorageEngineException {
        return counters.compute(idType + (studyId == null ? "" : ("_" + studyId)),
                (key, value) -> value == null ? 1 : value + 1);
    }

    private static final AtomicInteger NUM_PRINTS = new AtomicInteger();

    public static void writeAndClear(Path path) {
        ObjectMapper objectMapper = new ObjectMapper().configure(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS, true);
        String name = "project_metadata_" + NUM_PRINTS.incrementAndGet() + ".json";
        try (OutputStream os = new FileOutputStream(path.resolve(name).toFile())) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(os, projectMetadata);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        clear();
    }

    public static void clear() {
        projectMetadata = null;
        counters.clear();
    }


}
