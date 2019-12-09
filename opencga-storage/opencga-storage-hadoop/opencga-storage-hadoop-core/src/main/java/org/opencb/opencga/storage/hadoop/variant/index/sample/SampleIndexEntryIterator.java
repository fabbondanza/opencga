package org.opencb.opencga.storage.hadoop.variant.index.sample;

import org.opencb.biodata.models.variant.Variant;
import org.opencb.opencga.storage.hadoop.variant.index.annotation.AnnotationIndexEntry;

import java.util.Iterator;

/**
 * Iterate through the variants of a SampleIndexEntry.
 *
 * Multiple implementations to allow different ways to walk through the sample index entry.
 */
public interface SampleIndexEntryIterator extends Iterator<Variant> {

    /**
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();

    /**
     * @return the index of the element that would be returned by a
     * subsequent call to {@code next}.
     */
    int nextIndex();

    /**
     * @return the non intergenic index of the element that would be returned by a
     * subsequent call to {@code next}.
     */
    int nextNonIntergenicIndex();

    /**
     * @return the genotype of the next element.
     */
    String nextGenotype();

    boolean hasFileIndex();

    /**
     * @return the file index value of the next element.
     */
    byte nextFileIndex();

    boolean hasParentsIndex();

    /**
     * @return the parents index value of the next element.
     */
    byte nextParentsIndex();

    /**
     * @return the AnnotationIndexEntry of the next element.
     */
    AnnotationIndexEntry nextAnnotationIndexEntry();

    /**
     * Skip next element. Avoid conversion.
     */
    void skip();

    /**
     * @return next variant
     */
    Variant next();

    int getApproxSize();
}
