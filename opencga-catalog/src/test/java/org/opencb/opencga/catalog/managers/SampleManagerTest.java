package org.opencb.opencga.catalog.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.Test;
import org.opencb.biodata.models.pedigree.IndividualProperty;
import org.opencb.commons.datastore.core.DataResult;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.opencga.catalog.db.api.DBIterator;
import org.opencb.opencga.catalog.db.api.ProjectDBAdaptor;
import org.opencb.opencga.catalog.db.api.SampleDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogDBException;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.utils.CatalogAnnotationsValidatorTest;
import org.opencb.opencga.catalog.utils.Constants;
import org.opencb.opencga.catalog.utils.ParamUtils;
import org.opencb.opencga.core.models.AclParams;
import org.opencb.opencga.core.models.common.AnnotationSet;
import org.opencb.opencga.core.models.common.Status;
import org.opencb.opencga.core.models.individual.Individual;
import org.opencb.opencga.core.models.individual.IndividualAclEntry;
import org.opencb.opencga.core.models.individual.IndividualUpdateParams;
import org.opencb.opencga.core.models.project.Project;
import org.opencb.opencga.core.models.sample.*;
import org.opencb.opencga.core.models.study.GroupUpdateParams;
import org.opencb.opencga.core.models.study.Study;
import org.opencb.opencga.core.models.study.Variable;
import org.opencb.opencga.core.models.study.VariableSet;
import org.opencb.opencga.core.models.summaries.FeatureCount;
import org.opencb.opencga.core.models.summaries.VariableSetSummary;
import org.opencb.opencga.core.models.user.Account;
import org.opencb.opencga.core.response.OpenCGAResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.opencb.opencga.catalog.db.api.SampleDBAdaptor.QueryParams.ANNOTATION;

public class SampleManagerTest extends AbstractManagerTest {

    @Test
    public void testSampleVersioning() throws CatalogException {
        Query query = new Query(ProjectDBAdaptor.QueryParams.USER_ID.key(), "user");
        String projectId = catalogManager.getProjectManager().get(query, null, token).first().getId();

        catalogManager.getSampleManager().create(studyFqn,
                new Sample().setId("testSample").setDescription("description"), null, token);
        catalogManager.getSampleManager().update(studyFqn, "testSample", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);
        catalogManager.getSampleManager().update(studyFqn, "testSample", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        catalogManager.getProjectManager().incrementRelease(projectId, token);
        // We create something to have a gap in the release
        catalogManager.getSampleManager().create(studyFqn, new Sample().setId("dummy"), null, token);

        catalogManager.getProjectManager().incrementRelease(projectId, token);
        catalogManager.getSampleManager().update(studyFqn, "testSample", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        catalogManager.getSampleManager().update(studyFqn, "testSample",
                new SampleUpdateParams().setDescription("new description"), null, token);

        // We want the whole history of the sample
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample")
                .append(Constants.ALL_VERSIONS, true);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(4, sampleDataResult.getNumResults());
        assertEquals("description", sampleDataResult.getResults().get(0).getDescription());
        assertEquals("description", sampleDataResult.getResults().get(1).getDescription());
        assertEquals("description", sampleDataResult.getResults().get(2).getDescription());
        assertEquals("new description", sampleDataResult.getResults().get(3).getDescription());

        // We want the last version of release 1
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample")
                .append(SampleDBAdaptor.QueryParams.SNAPSHOT.key(), 1);
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(3, sampleDataResult.first().getVersion());

        // We want the last version of release 2 (must be the same of release 1)
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample")
                .append(SampleDBAdaptor.QueryParams.SNAPSHOT.key(), 2);
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(3, sampleDataResult.first().getVersion());

        // We want the last version of the sample
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample");
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(4, sampleDataResult.first().getVersion());

        // We want the version 2 of the sample
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample")
                .append(SampleDBAdaptor.QueryParams.VERSION.key(), 2);
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(2, sampleDataResult.first().getVersion());

        // We want the version 1 of the sample
        query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "testSample")
                .append(SampleDBAdaptor.QueryParams.VERSION.key(), 1);
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(1, sampleDataResult.first().getVersion());

        DataResult<Sample> testSample = catalogManager.getSampleManager()
                .get(studyFqn, Collections.singletonList("testSample"), new Query(Constants.ALL_VERSIONS, true), null, false, token);
        assertEquals(4, testSample.getResults().size());
    }

    @Test
    public void updateProcessingField() throws CatalogException {
        catalogManager.getSampleManager().create(studyFqn,
                new Sample().setId("testSample").setDescription("description"), null, token);

        SampleProcessing processing = new SampleProcessing("product", "preparationMethod", "extractionMethod", "labSampleId", "quantity",
                "date", Collections.emptyMap());
        catalogManager.getSampleManager().update(studyFqn, "testSample",
                new SampleUpdateParams().setProcessing(processing), new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        DataResult<Sample> testSample = catalogManager.getSampleManager().get(studyFqn, "testSample", new QueryOptions(), token);
        assertEquals("product", testSample.first().getProcessing().getProduct());
        assertEquals("preparationMethod", testSample.first().getProcessing().getPreparationMethod());
        assertEquals("extractionMethod", testSample.first().getProcessing().getExtractionMethod());
        assertEquals("labSampleId", testSample.first().getProcessing().getLabSampleId());
        assertEquals("quantity", testSample.first().getProcessing().getQuantity());
        assertEquals("date", testSample.first().getProcessing().getDate());
        assertTrue(testSample.first().getProcessing().getAttributes().isEmpty());
    }

    @Test
    public void updateCollectionField() throws CatalogException {
        catalogManager.getSampleManager().create(studyFqn,
                new Sample().setId("testSample").setDescription("description"), null, token);

        SampleCollection collection = new SampleCollection("tissue", "organ", "quantity", "method", "date", Collections.emptyMap());
        ObjectMap params = new ObjectMap(SampleDBAdaptor.QueryParams.COLLECTION.key(), collection);
        catalogManager.getSampleManager().update(studyFqn, "testSample",
                new SampleUpdateParams().setCollection(collection), new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        DataResult<Sample> testSample = catalogManager.getSampleManager().get(studyFqn, "testSample", new QueryOptions(), token);
        assertEquals("tissue", testSample.first().getCollection().getTissue());
        assertEquals("organ", testSample.first().getCollection().getOrgan());
        assertEquals("quantity", testSample.first().getCollection().getQuantity());
        assertEquals("method", testSample.first().getCollection().getMethod());
        assertEquals("date", testSample.first().getCollection().getDate());
        assertTrue(testSample.first().getCollection().getAttributes().isEmpty());
    }

    @Test
    public void testCreateSample() throws CatalogException {
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("HG007"), null,
                token);
        assertEquals(1, sampleDataResult.getNumResults());
    }

//    @Test
//    public void testUpdateSampleStats() throws CatalogException {
//        catalogManager.getSampleManager().create(studyFqn, new Sample().setId("HG007"), null, sessionIdUser);
//        DataResult<Sample> update = catalogManager.getSampleManager().update(studyFqn, "HG007",
//                new ObjectMap(SampleDBAdaptor.QueryParams.STATS.key(), new ObjectMap("one", "two")), new QueryOptions(), sessionIdUser);
//        assertEquals(1, update.first().getStats().size());
//        assertTrue(update.first().getStats().containsKey("one"));
//        assertEquals("two", update.first().getStats().get("one"));
//
//        update = catalogManager.getSampleManager().update(studyFqn, "HG007",
//                new ObjectMap(SampleDBAdaptor.QueryParams.STATS.key(), new ObjectMap("two", "three")), new QueryOptions(), sessionIdUser);
//        assertEquals(2, update.first().getStats().size());
//    }

    @Test
    public void testCreateSampleWithDotInName() throws CatalogException {
        String name = "HG007.sample";
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().create(studyFqn, new Sample().setId(name), null,
                token);
        assertEquals(name, sampleDataResult.first().getId());
    }

    @Test
    public void testAnnotate() throws CatalogException {
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("NAME", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("AGE", "", Variable.VariableType.INTEGER, "", false, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("HEIGHT", "", Variable.VariableType.DOUBLE, "", false, false, Collections.emptyList(), 0, "",
                "", null, Collections.emptyMap()));
        variables.add(new Variable("MAP", "", Variable.VariableType.OBJECT, new HashMap<>(), false, false, Collections.emptyList(), 0, "", "", null,
                Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("NAME", "Joe");
        annotations.put("AGE", 25);
        annotations.put("HEIGHT", 180);
        annotations.put("MAP", new ObjectMap("unknownKey1", "value1").append("unknownKey2", 42));

        catalogManager.getSampleManager().update(studyFqn, s_1, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);

        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, s_1,
                new QueryOptions(QueryOptions.INCLUDE, Constants.ANNOTATION_SET_NAME + ".annotation1"), token);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

//        DataResult<AnnotationSet> annotationSetDataResult = catalogManager.getSampleManager().getAnnotationSet(s_1,
//                studyFqn, "annotation1", sessionIdUser);
//        assertEquals(1, annotationSetQueryResult.getNumResults());
        Map<String, Object> map = sampleDataResult.first().getAnnotationSets().get(0).getAnnotations();
        assertEquals(4, map.size());

        assertEquals("Joe", map.get("NAME"));
        assertEquals(25, map.get("AGE"));
        assertEquals(180.0, map.get("HEIGHT"));
        assertEquals(2, ((Map) map.get("MAP")).size());
        assertEquals("value1", ((Map) map.get("MAP")).get("unknownKey1"));
        assertEquals(42, ((Map) map.get("MAP")).get("unknownKey2"));
    }

    @Test
    public void testDynamicAnnotationsCreation() throws CatalogException, IOException {
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("a", "a", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                Collections.emptySet(), Collections.emptyMap()));
        variables.add(new Variable("a1", "a1", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                                Collections.emptySet(), Collections.emptyMap()))),
                Collections.emptyMap()));
        variables.add(new Variable("a2", "a2", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.OBJECT, null, true, false, null, 0, "", "",
                                new HashSet<>(Arrays.asList(
                                        new Variable("c", "c", "", Variable.VariableType.MAP_STRING, null, true, true, null, 0, "", "",
                                                Collections.emptySet(), Collections.emptyMap()))),
                                Collections.emptyMap()))),
                Collections.emptyMap()));
        variables.add(new Variable("a3", "a3", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                                new HashSet<>(Arrays.asList(
                                        new Variable("c", "c", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                                                Collections.emptySet(), Collections.emptyMap()))),
                                Collections.emptyMap()))),
                Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        InputStream inputStream = this.getClass().getClassLoader().getResource("annotation_sets/complete_annotation.json").openStream();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectMap annotations = objectMapper.readValue(inputStream, ObjectMap.class);

        catalogManager.getSampleManager().update(studyFqn, s_1, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);

        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, s_1,
                new QueryOptions(QueryOptions.INCLUDE, Constants.ANNOTATION_SET_NAME + ".annotation1"), token);

        assertEquals(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(annotations),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(sampleDataResult.first().getAnnotationSets().get(0).getAnnotations()));

        inputStream = this.getClass().getClassLoader().getResource("annotation_sets/incomplete_annotation.json").openStream();
        objectMapper = new ObjectMapper();
        annotations = objectMapper.readValue(inputStream, ObjectMap.class);

        thrown.expect(CatalogException.class);
        thrown.expectMessage("Missing required variable");
        catalogManager.getSampleManager().update(studyFqn, s_2, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
    }

    @Test
    public void testDynamicAnnotationsSearch() throws CatalogException, IOException {
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("a", "a", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                Collections.emptySet(), Collections.emptyMap()));
        variables.add(new Variable("a1", "a1", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                                Collections.emptySet(), Collections.emptyMap()))),
                Collections.emptyMap()));
        variables.add(new Variable("a2", "a2", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.OBJECT, null, true, false, null, 0, "", "",
                                new HashSet<>(Arrays.asList(
                                        new Variable("c", "c", "", Variable.VariableType.MAP_STRING, null, true, true, null, 0, "", "",
                                                Collections.emptySet(), Collections.emptyMap()))),
                                Collections.emptyMap()))),
                Collections.emptyMap()));
        variables.add(new Variable("a3", "a3", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                new HashSet<>(Arrays.asList(
                        new Variable("b", "b", "", Variable.VariableType.OBJECT, null, true, true, null, 0, "", "",
                                new HashSet<>(Arrays.asList(
                                        new Variable("c", "c", "", Variable.VariableType.MAP_STRING, null, true, false, null, 0, "", "",
                                                Collections.emptySet(), Collections.emptyMap()))),
                                Collections.emptyMap()))),
                Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        InputStream inputStream = this.getClass().getClassLoader().getResource("annotation_sets/complete_annotation.json").openStream();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectMap annotations = objectMapper.readValue(inputStream, ObjectMap.class);

        catalogManager.getSampleManager().update(studyFqn, s_1, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);

        Query query = new Query(Constants.ANNOTATION, "a3.b.c.z=z2;a2.b.c.z=z3");
        assertEquals(0 , catalogManager.getSampleManager().count(studyFqn, query, token).getNumMatches());

        query = new Query(Constants.ANNOTATION, "a3.b.c.z=z2;a2.b.c.z=z");
        OpenCGAResult<Sample> result = catalogManager.getSampleManager().search(studyFqn, query, null, token);
        assertEquals(1, result.getNumMatches());
        assertEquals(s_1, result.first().getId());
    }

    @Test
    public void searchSamples() throws CatalogException {
        catalogManager.getStudyManager().createGroup(studyFqn, "myGroup", "myGroup", Arrays.asList("user2", "user3"), token);
        catalogManager.getStudyManager().createGroup(studyFqn, "myGroup2", "myGroup2", Arrays.asList("user2", "user3"), token);
        catalogManager.getStudyManager().updateAcl(Arrays.asList(studyFqn), "@myGroup",
                new Study.StudyAclParams("", AclParams.Action.SET, null), token);

        catalogManager.getSampleManager().updateAcl(studyFqn, Arrays.asList("s_1"), "@myGroup", new Sample.SampleAclParams("VIEW",
                AclParams.Action.SET, null, null, null), token);

        DataResult<Sample> search = catalogManager.getSampleManager().search(studyFqn, new Query(), new QueryOptions(),
                sessionIdUser2);
        assertEquals(1, search.getNumResults());
    }

    @Test
    public void testDeleteAnnotationset() throws CatalogException, JsonProcessingException {
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("var_name", "", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("AGE", "", "", Variable.VariableType.INTEGER, "", false, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("HEIGHT", "", "", Variable.VariableType.DOUBLE, "", false, false, Collections.emptyList(), 0, "",
                "", null, Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        ObjectMap annotations = new ObjectMap()
                .append("var_name", "Joe")
                .append("AGE", 25)
                .append("HEIGHT", 180);
        AnnotationSet annotationSet = new AnnotationSet("annotation1", vs1.getId(), annotations);
        AnnotationSet annotationSet1 = new AnnotationSet("annotation2", vs1.getId(), annotations);

        DataResult<Sample> update = catalogManager.getSampleManager().update(studyFqn, s_1, new SampleUpdateParams()
                .setAnnotationSets(Arrays.asList(annotationSet, annotationSet1)), QueryOptions.empty(), token);
        assertEquals(1, update.getNumUpdated());

        Sample sample = catalogManager.getSampleManager().get(studyFqn, s_1, QueryOptions.empty(), token).first();
        assertEquals(3, sample.getAnnotationSets().size());

        catalogManager.getSampleManager().removeAnnotationSet(studyFqn, s_1, "annotation1", QueryOptions.empty(), token);
        update = catalogManager.getSampleManager()
                .removeAnnotationSet(studyFqn, s_1, "annotation2", QueryOptions.empty(), token);
        assertEquals(1, update.getNumUpdated());

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, QueryOptions.empty(), token).first();
        assertEquals(1, sample.getAnnotationSets().size());

        thrown.expect(CatalogDBException.class);
        thrown.expectMessage("not found");
        catalogManager.getSampleManager().removeAnnotationSet(studyFqn, s_1, "non_existing", QueryOptions.empty(), token);
    }

    @Test
    public void testSearchAnnotation() throws CatalogException, JsonProcessingException {
        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("var_name", "", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("AGE", "", "", Variable.VariableType.INTEGER, "", false, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        variables.add(new Variable("HEIGHT", "", "", Variable.VariableType.DOUBLE, "", false, false, Collections.emptyList(), 0, "",
                "", null, Collections.emptyMap()));
        variables.add(new Variable("OTHER", "", "", Variable.VariableType.OBJECT, null, false, false, null, 1, "", "", null,
                Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        ObjectMap annotations = new ObjectMap()
                .append("var_name", "Joe")
                .append("AGE", 25)
                .append("HEIGHT", 180);
        AnnotationSet annotationSet = new AnnotationSet("annotation1", vs1.getId(), annotations);

        catalogManager.getSampleManager().update(studyFqn, s_1, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(annotationSet)), QueryOptions.empty(), token);

        Query query = new Query(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=25");
        DataResult<Sample> annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(),
                token);
        assertEquals(1, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=23");
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(0, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=25;variableSet!=" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(1, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=25;variableSet!==" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(0, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=25;variableSet==" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(1, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "var_name=Joe;" + vs1.getId() + ":AGE=25;variableSet===" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(0, annotDataResult.getNumResults());

        VariableSet vs = catalogManager.getStudyManager().getVariableSet(studyFqn, "vs", null, token).first();
        query.put(Constants.ANNOTATION, "variableSet===" + vs.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(7, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "variableSet!=" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(9, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "variableSet!==" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(8, annotDataResult.getNumResults());

        query.put(Constants.ANNOTATION, "variableSet=" + vs1.getId());
        annotDataResult = catalogManager.getSampleManager().search(studyFqn, query,
                new QueryOptions(QueryOptions.INCLUDE, Constants.VARIABLE_SET + "." + vs1.getId()), token);
        assertEquals(1, annotDataResult.getNumResults());
        assertEquals(1, annotDataResult.first().getAnnotationSets().size());
        assertEquals(vs1.getId(), annotDataResult.first().getAnnotationSets().get(0).getVariableSetId());
    }

    @Test
    public void testProjections() throws CatalogException {
        VariableSet variableSet = catalogManager.getStudyManager().getVariableSet("1000G:phase1", "vs", null, token).first();

        Query query = new Query(Constants.ANNOTATION, "variableSet===" + variableSet.getId());
        QueryOptions options = new QueryOptions(QueryOptions.INCLUDE, "annotationSets");
        DataResult<Sample> annotDataResult = catalogManager.getSampleManager().search(studyFqn, query, options,
                token);
        assertEquals(8, annotDataResult.getNumResults());

        for (Sample sample : annotDataResult.getResults()) {
            assertEquals(null, sample.getId());
            assertTrue(!sample.getAnnotationSets().isEmpty());
        }
    }

    @Test
    public void testAnnotateMulti() throws CatalogException {
        String sampleId = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("NAME", "NAME", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();


        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("NAME", "Luke");

        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations = new HashMap<>();
        annotations.put("NAME", "Lucas");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(2, sampleDataResult.first().getAnnotationSets().size());

        assertTrue(Arrays.asList("annotation1", "annotation2")
                .containsAll(sampleDataResult.first().getAnnotationSets().stream().map(AnnotationSet::getId).collect(Collectors.toSet())));
    }

    @Test
    public void testAnnotateUnique() throws CatalogException {
        String sampleId = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("NAME", "NAME", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", true, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();


        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("NAME", "Luke");

        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("NAME", "Lucas");
        thrown.expect(CatalogException.class);
        thrown.expectMessage("unique");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
    }

    @Test
    public void testAnnotateIndividualUnique() throws CatalogException {
        String individualId = catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("INDIVIDUAL_1"),
                new QueryOptions(), token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("NAME", "NAME", "", Variable.VariableType.TEXT, "", true, false, Collections.emptyList(), 0, "", "",
                null, Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", true, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.INDIVIDUAL), token).first();


        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("NAME", "Luke");
        catalogManager.getIndividualManager().update(studyFqn, individualId, new IndividualUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Individual> individualDataResult = catalogManager.getIndividualManager().get(studyFqn, individualId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, individualDataResult.first().getAnnotationSets().size());

        annotations.put("NAME", "Lucas");
        thrown.expect(CatalogException.class);
        thrown.expectMessage("unique");
        catalogManager.getIndividualManager().update(studyFqn, individualId, new IndividualUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
    }

    @Test
    public void testAnnotateIncorrectType() throws CatalogException {
        String sampleId = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("NUM", "NUM", "", Variable.VariableType.DOUBLE, "", true, false, null, 0, "", "", null,
                Collections.emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();


        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("NUM", "5");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("NUM", "6.8");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(2, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("NUM", "five polong five");
        thrown.expect(CatalogException.class);
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation3", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
    }

    @Test
    public void testAnnotateRange() throws CatalogException {
        String sampleId = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("RANGE_NUM", "RANGE_NUM", "", Variable.VariableType.DOUBLE, "", true, false, Arrays.asList("1:14",
                "16:22", "50:"), 0, "", "", null, Collections.<String, Object>emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("RANGE_NUM", "1");  // 1:14
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("RANGE_NUM", "14"); // 1:14
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(2, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("RANGE_NUM", "20");  // 16:20
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation3", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(3, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("RANGE_NUM", "100000"); // 50:
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation4", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(4, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("RANGE_NUM", "14.1");
        thrown.expect(CatalogException.class);
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation5", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
    }

    @Test
    public void testAnnotateCategorical() throws CatalogException {
        String sampleId = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        List<Variable> variables = new ArrayList<>();
        variables.add(new Variable("COOL_NAME", "COOL_NAME", "", Variable.VariableType.CATEGORICAL, "", true, false, Arrays.asList("LUKE",
                "LEIA", "VADER", "YODA"), 0, "", "", null, Collections.<String, Object>emptyMap()));
        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null, variables,
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        Map<String, Object> actionMap = new HashMap<>();
        actionMap.put(AnnotationSetManager.ANNOTATION_SETS, ParamUtils.UpdateAction.ADD);
        QueryOptions options = new QueryOptions(Constants.ACTIONS, actionMap);

        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("COOL_NAME", "LUKE");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                options, token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("COOL_NAME", "LEIA");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation2", vs1.getId(), annotations))),
                options, token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(2, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("COOL_NAME", "VADER");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation3", vs1.getId(), annotations))),
                options, token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(3, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("COOL_NAME", "YODA");
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation4", vs1.getId(), annotations))),
                options, token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(4, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("COOL_NAME", "SPOCK");
        thrown.expect(CatalogException.class);
        catalogManager.getSampleManager().update(studyFqn, sampleId, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation5", vs1.getId(), annotations))),
                options, token);
    }

    @Test
    public void testAnnotateNested() throws CatalogException {
        String sampleId1 = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"),
                new QueryOptions(), token).first().getId();
        String sampleId2 = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_2"),
                new QueryOptions(), token).first().getId();

        VariableSet vs1 = catalogManager.getStudyManager().createVariableSet(studyFqn, "vs1", "vs1", false, false, "", null,
                Collections.singletonList(CatalogAnnotationsValidatorTest.nestedObject),
                Collections.singletonList(VariableSet.AnnotableDataModels.SAMPLE), token).first();

        HashMap<String, Object> annotations = new HashMap<>();
        annotations.put("nestedObject", new ObjectMap()
                .append("stringList", Arrays.asList("li", "lu"))
                .append("object", new ObjectMap()
                        .append("string", "my value")
                        .append("numberList", Arrays.asList(2, 3, 4))));
        catalogManager.getSampleManager().update(studyFqn, sampleId1, new SampleUpdateParams()
                    .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId1,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        annotations.put("nestedObject", new ObjectMap()
                .append("stringList", Arrays.asList("lo", "lu"))
                .append("object", new ObjectMap()
                        .append("string", "stringValue")
                        .append("numberList", Arrays.asList(3, 4, 5))));
        catalogManager.getSampleManager().update(studyFqn, sampleId2, new SampleUpdateParams()
                        .setAnnotationSets(Collections.singletonList(new AnnotationSet("annotation1", vs1.getId(), annotations))),
                QueryOptions.empty(), token);
        sampleDataResult = catalogManager.getSampleManager().get(studyFqn, sampleId2,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(1, sampleDataResult.first().getAnnotationSets().size());

        List<Sample> samples;
        Query query = new Query(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=li");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(1, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(1, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=LL");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(0, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,li,LL");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(2, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.object.string=my value");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(1, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;" + vs1.getId()
                + ":nestedObject.object.string=my value");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(1, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;" + vs1.getId()
                + ":nestedObject.object.numberList=7");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(0, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;"
                + vs1.getId() + ":nestedObject.object.numberList=3");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(2, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;" + vs1.getId()
                + ":nestedObject.object.numberList=5;" + vs1.getId() + ":nestedObject.object.string=stringValue");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(1, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;" + vs1.getId()
                + ":nestedObject.object.numberList=2,5");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(2, samples.size());

        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":nestedObject.stringList=lo,lu,LL;" + vs1.getId()
                + ":nestedObject.object.numberList=0");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(0, samples.size());


        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), vs1.getId() + ":unexisting=lo,lu,LL");
        thrown.expect(CatalogException.class);
        thrown.expectMessage("does not exist");
        catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
    }

//    @Test
//    public void testQuerySampleAnnotationFail1() throws CatalogException {
//        Query query = new Query();
//        query.put(SampleDBAdaptor.QueryParams.ANNOTATION.key() + ":nestedObject.stringList", "lo,lu,LL");
//
//        thrown.expect(CatalogDBException.class);
//        thrown.expectMessage("annotation:nestedObject does not exist");
//        DataResult<Sample> search = catalogManager.getSampleManager().search(studyFqn, query, null, sessionIdUser);
//        catalogManager.getAllSamples(studyId, query, null, sessionIdUser).getResults();
//    }

//    @Test
//    public void testQuerySampleAnnotationFail2() throws CatalogException {
//        Query query = new Query();
//        query.put(CatalogSampleDBAdaptor.QueryParams.ANNOTATION.key(), "nestedObject.stringList:lo,lu,LL");
//
//        thrown.expect(CatalogDBException.class);
//        thrown.expectMessage("Wrong annotation query");
//        catalogManager.getAllSamples(studyId, query, null, sessionIdUser).getResults();
//    }

    @Test
    public void testGroupByAnnotations() throws Exception {
        AbstractManager.MyResourceId vs1 = catalogManager.getStudyManager().getVariableSetId("vs", studyFqn, token);

        DataResult queryResult = catalogManager.getSampleManager().groupBy(studyFqn, new Query(),
                Collections.singletonList(Constants.ANNOTATION + ":" + vs1.getResourceId() + ":annot1:PHEN"), QueryOptions.empty(),
                token);

        assertEquals(3, queryResult.getNumResults());
        for (Document document : (List<Document>) queryResult.getResults()) {
            Document id = (Document) document.get("_id");
            List<String> value = ((ArrayList<String>) id.values().iterator().next());

            List<String> items = (List<String>) document.get("items");

            if (value.isEmpty()) {
                assertEquals(4, items.size());
                assertTrue(items.containsAll(Arrays.asList("s_6", "s_7", "s_8", "s_9")));
            } else if ("CONTROL".equals(value.get(0))) {
                assertEquals(3, items.size());
                assertTrue(items.containsAll(Arrays.asList("s_1", "s_3", "s_4")));
            } else if ("CASE".equals(value.get(0))) {
                assertEquals(2, items.size());
                assertTrue(items.containsAll(Arrays.asList("s_2", "s_5")));
            } else {
                fail("It should not get into this condition");
            }
        }
    }

    @Test
    public void testIteratorSamples() throws CatalogException {
        Query query = new Query();

        DBIterator<Sample> iterator = catalogManager.getSampleManager().iterator(studyFqn, query, null, token);
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(9, count);
    }

    @Test
    public void testQuerySamples() throws CatalogException {
        VariableSet variableSet = catalogManager.getStudyManager().getVariableSet(studyFqn, "vs", QueryOptions.empty(), token).first();

        List<Sample> samples;
        Query query = new Query();

        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(9, samples.size());

        query = new Query(ANNOTATION.key(), Constants.VARIABLE_SET + "=" + variableSet.getId());
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(8, samples.size());

        query = new Query(ANNOTATION.key(), Constants.ANNOTATION_SET_NAME + "=annot2");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(3, samples.size());

        query = new Query(ANNOTATION.key(), Constants.ANNOTATION_SET_NAME + "=noExist");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(0, samples.size());

        query = new Query(ANNOTATION.key(), variableSet.getId() + ":NAME=s_1,s_2,s_3");
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(3, samples.size());

        query = new Query(ANNOTATION.key(), variableSet.getId() + ":AGE>30;" + Constants.VARIABLE_SET + "=" + variableSet.getId());
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(3, samples.size());

        query = new Query(ANNOTATION.key(), variableSet.getId() + ":AGE>30;" + Constants.VARIABLE_SET + "=" + variableSet.getId());
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(3, samples.size());

        query = new Query(ANNOTATION.key(), variableSet.getId() + ":AGE>30;" + variableSet.getId() + ":ALIVE=true;"
                + Constants.VARIABLE_SET + "=" + variableSet.getId());
        samples = catalogManager.getSampleManager().search(studyFqn, query, null, token).getResults();
        assertEquals(2, samples.size());
    }

    @Test
    public void testUpdateAnnotation() throws CatalogException {
        Sample sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        AnnotationSet annotationSet = sample.getAnnotationSets().get(0);

        Individual ind = new Individual()
                .setId("INDIVIDUAL_1")
                .setSex(IndividualProperty.Sex.UNKNOWN);
        ind.setAnnotationSets(Collections.singletonList(annotationSet));
        ind = catalogManager.getIndividualManager().create(studyFqn, ind, QueryOptions.empty(), token).first();

        // First update
        annotationSet.getAnnotations().put("NAME", "SAMPLE1");
        annotationSet.getAnnotations().put("AGE", 38);
        annotationSet.getAnnotations().put("EXTRA", "extra");
        annotationSet.getAnnotations().remove("HEIGHT");

        // Update annotation set
        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), annotationSet.getId(),
                annotationSet.getAnnotations(), ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true),
                token);
        catalogManager.getSampleManager().updateAnnotations(studyFqn, s_1, annotationSet.getId(), annotationSet.getAnnotations(),
                ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        Consumer<AnnotationSet> check = as -> {
            Map<String, Object> auxAnnotations = as.getAnnotations();

            assertEquals(5, auxAnnotations.size());
            assertEquals("SAMPLE1", auxAnnotations.get("NAME"));
            assertEquals(38, auxAnnotations.get("AGE"));
            assertEquals("extra", auxAnnotations.get("EXTRA"));
        };

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        ind = catalogManager.getIndividualManager().get(studyFqn, ind.getId(), null, token).first();
        check.accept(sample.getAnnotationSets().get(0));
        check.accept(ind.getAnnotationSets().get(0));

        // Call again to the update to check that nothing changed
        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), annotationSet.getId(),
                annotationSet.getAnnotations(), ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true),
                token);
        check.accept(ind.getAnnotationSets().get(0));

        // Update mandatory annotation
        annotationSet.getAnnotations().put("NAME", "SAMPLE 1");
        annotationSet.getAnnotations().remove("EXTRA");

        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), annotationSet.getId(),
                annotationSet.getAnnotations(), ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true),
                token);
        catalogManager.getSampleManager().updateAnnotations(studyFqn, s_1, annotationSet.getId(), annotationSet.getAnnotations(),
                ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        check = as -> {
            Map<String, Object> auxAnnotations = as.getAnnotations();

            assertEquals(4, auxAnnotations.size());
            assertEquals("SAMPLE 1", auxAnnotations.get("NAME"));
            assertEquals(false, auxAnnotations.containsKey("EXTRA"));
        };

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        ind = catalogManager.getIndividualManager().get(studyFqn, ind.getId(), null, token).first();
        check.accept(sample.getAnnotationSets().get(0));
        check.accept(ind.getAnnotationSets().get(0));

        // Update non-mandatory annotation
        annotationSet.getAnnotations().put("EXTRA", "extra");
        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), annotationSet.getId(),
                annotationSet.getAnnotations(), ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true),
                token);
        catalogManager.getSampleManager().updateAnnotations(studyFqn, s_1, annotationSet.getId(), annotationSet.getAnnotations(),
                ParamUtils.CompleteUpdateAction.SET, new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        check = as -> {
            Map<String, Object> auxAnnotations = as.getAnnotations();

            assertEquals(5, auxAnnotations.size());
            assertEquals("SAMPLE 1", auxAnnotations.get("NAME"));
            assertEquals("extra", auxAnnotations.get("EXTRA"));
        };

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        ind = catalogManager.getIndividualManager().get(studyFqn, ind.getId(), null, token).first();
        check.accept(sample.getAnnotationSets().get(0));
        check.accept(ind.getAnnotationSets().get(0));

        // Update non-mandatory annotation
        Map<String, Object> annotationUpdate = new ObjectMap("EXTRA", "extraa");
        // Action now is ADD, we only want to change that annotation
        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), annotationSet.getId(), annotationUpdate,
                ParamUtils.CompleteUpdateAction.ADD, new QueryOptions(Constants.INCREMENT_VERSION, true), token);
        catalogManager.getSampleManager().updateAnnotations(studyFqn, s_1, annotationSet.getId(), annotationUpdate,
                ParamUtils.CompleteUpdateAction.ADD, new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        check = as -> {
            Map<String, Object> auxAnnotations = as.getAnnotations();

            assertEquals(5, auxAnnotations.size());
            assertEquals("SAMPLE 1", auxAnnotations.get("NAME"));
            assertEquals("extraa", auxAnnotations.get("EXTRA"));
        };

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        ind = catalogManager.getIndividualManager().get(studyFqn, ind.getId(), null, token).first();
        check.accept(sample.getAnnotationSets().get(0));
        check.accept(ind.getAnnotationSets().get(0));

        thrown.expect(CatalogException.class);
        thrown.expectMessage("not found");
        catalogManager.getIndividualManager().updateAnnotations(studyFqn, ind.getId(), "blabla", annotationUpdate,
                ParamUtils.CompleteUpdateAction.ADD, new QueryOptions(Constants.INCREMENT_VERSION, true), token);
    }

    @Test
    public void testUpdateAnnotationFail() throws CatalogException {
        Sample sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        AnnotationSet annotationSet = sample.getAnnotationSets().get(0);

        thrown.expect(CatalogException.class); //Can not delete required fields
        thrown.expectMessage("required variable");
        catalogManager.getSampleManager().removeAnnotations(studyFqn, s_1, annotationSet.getId(), Collections.singletonList("NAME"),
                QueryOptions.empty(), token);
    }

    @Test
    public void testDeleteAnnotation() throws CatalogException {
        // We add one of the non mandatory annotations

        // First update
        catalogManager.getSampleManager().updateAnnotations(studyFqn, s_1, "annot1", new ObjectMap("EXTRA", "extra"),
                ParamUtils.CompleteUpdateAction.ADD, QueryOptions.empty(), token);

        Sample sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        AnnotationSet annotationSet = sample.getAnnotationSets().get(0);
        assertEquals("extra", annotationSet.getAnnotations().get("EXTRA"));

        // Now we remove that non mandatory annotation
        catalogManager.getSampleManager().removeAnnotations(studyFqn, s_1, annotationSet.getId(), Collections.singletonList("EXTRA"),
                QueryOptions.empty(), token);

        sample = catalogManager.getSampleManager().get(studyFqn, s_1, null, token).first();
        annotationSet = sample.getAnnotationSets().get(0);
        assertTrue(!annotationSet.getAnnotations().containsKey("EXTRA"));

        // Now we attempt to remove one mandatory annotation
        thrown.expect(CatalogException.class); //Can not delete required fields
        thrown.expectMessage("required variable");
        catalogManager.getSampleManager().removeAnnotations(studyFqn, s_1, annotationSet.getId(), Collections.singletonList("AGE"),
                QueryOptions.empty(), token);
    }

    @Test
    public void testDeleteAnnotationSet() throws CatalogException {
        catalogManager.getSampleManager().removeAnnotationSet(studyFqn, s_1, "annot1", QueryOptions.empty(), token);

        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().get(studyFqn, s_1,
                new QueryOptions(QueryOptions.INCLUDE, SampleDBAdaptor.QueryParams.ANNOTATION_SETS.key()), token);
        assertEquals(0, sampleDataResult.first().getAnnotationSets().size());
    }

    @Test
    public void getVariableSetSummary() throws CatalogException {
        VariableSet variableSet = catalogManager.getStudyManager().getVariableSet(studyFqn, "vs", null, token).first();

        DataResult<VariableSetSummary> variableSetSummary = catalogManager.getStudyManager()
                .getVariableSetSummary(studyFqn, variableSet.getId(), token);

        assertEquals(1, variableSetSummary.getNumResults());
        VariableSetSummary summary = variableSetSummary.first();

        assertEquals(5, summary.getSamples().size());

        // PHEN
        int i;
        for (i = 0; i < summary.getSamples().size(); i++) {
            if ("PHEN".equals(summary.getSamples().get(i).getName())) {
                break;
            }
        }
        List<FeatureCount> annotations = summary.getSamples().get(i).getAnnotations();
        assertEquals("PHEN", summary.getSamples().get(i).getName());
        assertEquals(2, annotations.size());

        for (i = 0; i < annotations.size(); i++) {
            if ("CONTROL".equals(annotations.get(i).getName())) {
                break;
            }
        }
        assertEquals("CONTROL", annotations.get(i).getName());
        assertEquals(5, annotations.get(i).getCount());

        for (i = 0; i < annotations.size(); i++) {
            if ("CASE".equals(annotations.get(i).getName())) {
                break;
            }
        }
        assertEquals("CASE", annotations.get(i).getName());
        assertEquals(3, annotations.get(i).getCount());

    }

    @Test
    public void testModifySample() throws CatalogException {
        String sampleId1 = catalogManager.getSampleManager()
                .create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(), token).first().getId();
        String individualId = catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("Individual1"),
                new QueryOptions(), token).first().getId();

        DataResult<Sample> updateResult = catalogManager.getSampleManager()
                .update(studyFqn, sampleId1, new SampleUpdateParams().setIndividualId(individualId), null, token);
        assertEquals(1, updateResult.getNumUpdated());

        Sample sample = catalogManager.getSampleManager().get(studyFqn, sampleId1, QueryOptions.empty(), token).first();
        assertEquals(individualId, sample.getIndividualId());
    }

    @Test
    public void testGetSampleAndIndividualWithPermissionsChecked() throws CatalogException {
        String sampleId1 = catalogManager.getSampleManager()
                .create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(), token).first().getId();
        String individualId = catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("Individual1"),
                new QueryOptions(), token).first().getId();

        DataResult<Sample> updateResult = catalogManager.getSampleManager()
                .update(studyFqn, sampleId1, new SampleUpdateParams().setIndividualId(individualId), null, token);
        assertEquals(1, updateResult.getNumUpdated());

        Sample sample = catalogManager.getSampleManager().get(studyFqn, sampleId1, QueryOptions.empty(), token).first();
        assertEquals(individualId, sample.getIndividualId());

        catalogManager.getSampleManager().updateAcl(studyFqn, Collections.singletonList("SAMPLE_1"), "user2",
                new Sample.SampleAclParams(SampleAclEntry.SamplePermissions.VIEW.name(), AclParams.Action.SET, null, null, null),
                token);

        sample = catalogManager.getSampleManager().get(studyFqn, "SAMPLE_1", new QueryOptions("lazy", false), sessionIdUser2).first();
        assertEquals(null, sample.getAttributes().get("individual"));

        catalogManager.getSampleManager().updateAcl(studyFqn, Collections.singletonList("SAMPLE_1"), "user2",
                new Sample.SampleAclParams(SampleAclEntry.SamplePermissions.VIEW.name(), AclParams.Action.SET, null, null, null, true),
                token);
        sample = catalogManager.getSampleManager().get(studyFqn, "SAMPLE_1", new QueryOptions("lazy", false), sessionIdUser2).first();
        assertEquals(individualId, ((Individual) sample.getAttributes().get("OPENCGA_INDIVIDUAL")).getId());
        assertEquals(sampleId1, sample.getId());

        sample = catalogManager.getSampleManager().search(studyFqn, new Query("individual", "Individual1"), new QueryOptions("lazy", false), sessionIdUser2).first();
        assertEquals(individualId, ((Individual) sample.getAttributes().get("OPENCGA_INDIVIDUAL")).getId());
        assertEquals(sampleId1, sample.getId());

    }

    @Test
    public void searchSamplesByIndividual() throws CatalogException {
        catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("Individual1")
                .setSamples(Arrays.asList(new Sample().setId("sample1"), new Sample().setId("sample2"))), new QueryOptions(), token);

        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().search(studyFqn,
                new Query(SampleDBAdaptor.QueryParams.INDIVIDUAL.key(), "Individual1"), QueryOptions.empty(), token);

        assertEquals(2, sampleDataResult.getNumResults());

        sampleDataResult = catalogManager.getSampleManager().search(studyFqn,
                new Query().append(SampleDBAdaptor.QueryParams.INDIVIDUAL.key(), "Individual1")
                        .append(SampleDBAdaptor.QueryParams.ID.key(), "sample1"), QueryOptions.empty(), token);
        assertEquals(1, sampleDataResult.getNumResults());

        catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("Individual2"), new QueryOptions(), token);
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn,
                new Query().append(SampleDBAdaptor.QueryParams.INDIVIDUAL.key(), "Individual2"), QueryOptions.empty(), token);
        assertEquals(0, sampleDataResult.getNumResults());
    }

    @Test
    public void searchSamplesDifferentVersions() throws CatalogException {
        catalogManager.getSampleManager().create(studyFqn, new Sample().setId("sample1"), QueryOptions.empty(), token);
        catalogManager.getSampleManager().create(studyFqn, new Sample().setId("sample2"), QueryOptions.empty(), token);
        catalogManager.getSampleManager().create(studyFqn, new Sample().setId("sample3"), QueryOptions.empty(), token);

        // Generate 4 versions of sample1
        catalogManager.getSampleManager().update(studyFqn, "sample1", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);
        catalogManager.getSampleManager().update(studyFqn, "sample1", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);
        catalogManager.getSampleManager().update(studyFqn, "sample1", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        // Generate 3 versions of sample2
        catalogManager.getSampleManager().update(studyFqn, "sample2", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);
        catalogManager.getSampleManager().update(studyFqn, "sample2", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        // Generate 1 versions of sample3
        catalogManager.getSampleManager().update(studyFqn, "sample3", new SampleUpdateParams(),
                new QueryOptions(Constants.INCREMENT_VERSION, true), token);

        Query query = new Query()
                .append(SampleDBAdaptor.QueryParams.ID.key(), "sample1,sample2,sample3")
                .append(SampleDBAdaptor.QueryParams.VERSION.key(), "3,2,1");
        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(3, sampleDataResult.getNumResults());
        for (Sample sample : sampleDataResult.getResults()) {
            switch (sample.getId()) {
                case "sample1":
                    assertEquals(3, sample.getVersion());
                    break;
                case "sample2":
                    assertEquals(2, sample.getVersion());
                    break;
                case "sample3":
                    assertEquals(1, sample.getVersion());
                    break;
                default:
                    fail("One of the three samples above should always be present");
            }
        }

        query.put(SampleDBAdaptor.QueryParams.VERSION.key(), "2");
        sampleDataResult = catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
        assertEquals(3, sampleDataResult.getNumResults());
        sampleDataResult.getResults().forEach(
                s -> assertEquals(2, s.getVersion())
        );

        query.put(SampleDBAdaptor.QueryParams.VERSION.key(), "1,2");
        thrown.expect(CatalogException.class);
        thrown.expectMessage("size of the array");
        catalogManager.getSampleManager().search(studyFqn, query, QueryOptions.empty(), token);
    }

    @Test
    public void getSharedProject() throws CatalogException, IOException {
        catalogManager.getUserManager().create("dummy", "dummy", "asd@asd.asd", "dummy", "", 50000L,
                Account.Type.GUEST, null);
        catalogManager.getStudyManager().updateGroup(studyFqn, "@members", ParamUtils.UpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("dummy")), token);

        String token = catalogManager.getUserManager().login("dummy", "dummy");
        DataResult<Project> queryResult = catalogManager.getProjectManager().getSharedProjects("dummy", QueryOptions.empty(), token);
        assertEquals(1, queryResult.getNumResults());

        catalogManager.getStudyManager().updateGroup(studyFqn, "@members", ParamUtils.UpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("*")), this.token);
        queryResult = catalogManager.getProjectManager().getSharedProjects("*", QueryOptions.empty(), null);
        assertEquals(1, queryResult.getNumResults());
    }

    @Test
    public void smartResolutorStudyAliasFromAnonymousUser() throws CatalogException {
        catalogManager.getStudyManager().updateGroup(studyFqn, "@members", ParamUtils.UpdateAction.ADD,
                new GroupUpdateParams(Collections.singletonList("*")), token);
        Study study = catalogManager.getStudyManager().resolveId(studyFqn, "*");
        assertTrue(study != null);
    }

    @Test
    public void testCreateSampleWithIndividual() throws CatalogException {
        String individualId = catalogManager.getIndividualManager().create(studyFqn, new Individual().setId("Individual1"),
                new QueryOptions(), token).first().getId();
        String sampleId1 = catalogManager.getSampleManager().create(studyFqn, new Sample()
                        .setId("SAMPLE_1")
                        .setIndividualId(individualId),
                new QueryOptions(), token).first().getId();

        DataResult<Individual> individualDataResult = catalogManager.getIndividualManager().get(studyFqn, individualId,
                QueryOptions.empty(), token);
        assertEquals(sampleId1, individualDataResult.first().getSamples().get(0).getId());

        // Create sample linking to individual based on the individual name
        String sampleId2 = catalogManager.getSampleManager().create(studyFqn, new Sample()
                        .setId("SAMPLE_2")
                        .setIndividualId("Individual1"),
                new QueryOptions(), token).first().getId();

        individualDataResult = catalogManager.getIndividualManager().get(studyFqn, individualId, QueryOptions.empty(), token);
        assertEquals(2, individualDataResult.first().getSamples().size());
        assertTrue(individualDataResult.first().getSamples().stream().map(Sample::getId).collect(Collectors.toSet()).containsAll(
                Arrays.asList(sampleId1, sampleId2)
        ));
    }

    @Test
    public void testModifySampleBadIndividual() throws CatalogException {
        String sampleId1 = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getId();

        thrown.expect(CatalogException.class);
        thrown.expectMessage("not found");
        catalogManager.getSampleManager().update(studyFqn, sampleId1, new SampleUpdateParams().setIndividualId("ind"), null, token);
    }

    @Test
    public void testDeleteSample() throws CatalogException {
        long sampleUid = catalogManager.getSampleManager().create(studyFqn, new Sample().setId("SAMPLE_1"), new QueryOptions(),
                token).first().getUid();

        Query query = new Query(SampleDBAdaptor.QueryParams.ID.key(), "SAMPLE_1");
        DataResult delete = catalogManager.getSampleManager().delete("1000G:phase1", query, null, token);
        assertEquals(1, delete.getNumDeleted());

        query = new Query()
                .append(SampleDBAdaptor.QueryParams.UID.key(), sampleUid)
                .append(SampleDBAdaptor.QueryParams.DELETED.key(), true);

        DataResult<Sample> sampleDataResult = catalogManager.getSampleManager().search("1000G:phase1", query, new QueryOptions(), token);
//        DataResult<Sample> sample = catalogManager.getSample(sampleId, new QueryOptions(), sessionIdUser);
        assertEquals(1, sampleDataResult.getNumResults());
        assertEquals(Status.DELETED, sampleDataResult.first().getStatus().getName());
    }

    @Test
    public void testAssignPermissionsWithPropagationAndNoIndividual() throws CatalogException {
        Sample sample = new Sample().setId("sample");
        catalogManager.getSampleManager().create(studyFqn, sample, QueryOptions.empty(), token);

        DataResult<Map<String, List<String>>> dataResult = catalogManager.getSampleManager().updateAcl(studyFqn,
                Arrays.asList("sample"), "user2", new Sample.SampleAclParams("VIEW", AclParams.Action.SET, null, null, null, true),
                token);
        assertEquals(1, dataResult.getNumResults());
        assertEquals(1, dataResult.first().size());
        assertEquals(1, dataResult.first().get("user2").size());
        assertTrue(dataResult.first().get("user2").contains(SampleAclEntry.SamplePermissions.VIEW.name()));
    }

    // Two samples, one related to one individual and the other does not have any individual associated
    @Test
    public void testAssignPermissionsWithPropagationWithIndividualAndNoIndividual() throws CatalogException {
        Individual individual = new Individual().setId("individual").setSamples(Collections.singletonList(new Sample().setId("sample")));
        catalogManager.getIndividualManager().create(studyFqn, individual, QueryOptions.empty(), token);

        Sample sample2 = new Sample().setId("sample2");
        catalogManager.getSampleManager().create(studyFqn, sample2, QueryOptions.empty(), token);

        DataResult<Map<String, List<String>>> dataResult = catalogManager.getSampleManager().updateAcl(studyFqn,
                Arrays.asList("sample", "sample2"), "user2", new Sample.SampleAclParams("VIEW", AclParams.Action.SET, null, null, null,
                        true), token);
        assertEquals(2, dataResult.getNumResults());
        assertEquals(1, dataResult.first().size());
        assertEquals(1, dataResult.first().get("user2").size());
        assertTrue(dataResult.getResults().get(0).get("user2").contains(SampleAclEntry.SamplePermissions.VIEW.name()));
        assertTrue(dataResult.getResults().get(1).get("user2").contains(SampleAclEntry.SamplePermissions.VIEW.name()));

        DataResult<Map<String, List<String>>> individualAcl = catalogManager.getIndividualManager().getAcls(studyFqn,
                Collections.singletonList("individual"), "user2", false, token);
        assertEquals(1, individualAcl.getNumResults());
        assertEquals(1, individualAcl.first().size());
        assertEquals(1, individualAcl.first().get("user2").size());
        assertTrue(individualAcl.first().get("user2").contains(IndividualAclEntry.IndividualPermissions.VIEW.name()));
    }

}