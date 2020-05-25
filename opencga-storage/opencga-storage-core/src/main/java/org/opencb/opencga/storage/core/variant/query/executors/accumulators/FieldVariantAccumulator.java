package org.opencb.opencga.storage.core.variant.query.executors.accumulators;

import org.opencb.commons.datastore.core.FacetField;

import java.util.Collections;
import java.util.List;

public abstract class FieldVariantAccumulator<T> {
    private FieldVariantAccumulator<T> nestedFieldAccumulator;

    protected FieldVariantAccumulator(FieldVariantAccumulator<T> nestedFieldAccumulator) {
        this.nestedFieldAccumulator = nestedFieldAccumulator;
    }

    public FieldVariantAccumulator<T> setNestedFieldAccumulator(FieldVariantAccumulator<T> nestedFieldAccumulator) {
        this.nestedFieldAccumulator = nestedFieldAccumulator;
        return this;
    }

    /**
     * Get field name.
     * @return Field name
     */
    public abstract String getName();

    /**
     * Prepare (if required) the list of buckets for this field.
     * @return predefined list of buckets.
     */
    public FacetField createField() {
        return new FacetField(getName(), 0, prepareBuckets());
    }

    /**
     * Prepare (if required) the list of buckets for this field.
     * @return predefined list of buckets.
     */
    public final List<FacetField.Bucket> prepareBuckets() {
        List<FacetField.Bucket> valueBuckets = prepareBuckets1();
        for (FacetField.Bucket bucket : valueBuckets) {
            if (nestedFieldAccumulator != null) {
                bucket.setFacetFields(Collections.singletonList(nestedFieldAccumulator.createField()));
            }
        }
        return valueBuckets;
    }

    protected final FacetField.Bucket addBucket(FacetField field, String value) {
        FacetField.Bucket bucket;
        bucket = new FacetField.Bucket(value, 0, null);
        if (nestedFieldAccumulator != null) {
            bucket.setFacetFields(Collections.singletonList(nestedFieldAccumulator.createField()));
        }
        field.getBuckets().add(bucket);
        return bucket;
    }

    protected abstract List<FacetField.Bucket> prepareBuckets1();

    public void cleanEmptyBuckets(FacetField field) {
        field.getBuckets().removeIf(bucket -> bucket.getCount() == 0);
        if (nestedFieldAccumulator != null) {
            for (FacetField.Bucket bucket : field.getBuckets()) {
                nestedFieldAccumulator.cleanEmptyBuckets(bucket.getFacetFields().get(0));
            }
        }
    }

    /**
     * Accumulate variant in the given field.
     * @param field   Field
     * @param variant Variant
     */
    public final void accumulate(FacetField field, T variant) {
        List<FacetField.Bucket> buckets = getBuckets(field, variant);
        if (buckets == null || buckets.isEmpty()) {
            return;
        }
        field.addCount(1);
        for (FacetField.Bucket bucket : buckets) {
            bucket.addCount(1);
            if (nestedFieldAccumulator != null) {
                nestedFieldAccumulator.accumulate(bucket.getFacetFields().get(0), variant);
            }
        }
    }

    protected abstract List<FacetField.Bucket> getBuckets(FacetField field, T variant);
}
