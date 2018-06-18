/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2018 Queen Mary University of London.
 * Copyright (c) 2012-2016 Charité Universitätsmedizin Berlin and Genome Research Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.monarchinitiative.exomiser.core.analysis.util;

import org.junit.Test;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.model.SimpleVariantCoordinates;
import org.monarchinitiative.exomiser.core.model.TopologicalDomain;
import org.monarchinitiative.exomiser.core.model.VariantCoordinates;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class ChromosomalRegionIndexTest {

    private ChromosomalRegionIndex<TopologicalDomain> instance;

    private final VariantCoordinates variant = new SimpleVariantCoordinates(GenomeAssembly.HG19, 1, 50, "A", "T");

    private void createInstance(TopologicalDomain... tad) {
        instance = new ChromosomalRegionIndex<>(Arrays.asList(tad));
    }

    @Test
    public void testGetTadsContainingVariantSingleTad() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 100, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsContainingVariant(variant), equalTo(Collections.singletonList(tad)));
        assertThat(instance.hasRegionContainingVariant(variant), is(true));
    }

    @Test
    public void testGetTadsContainingVariantSingleTadVariantNotInTad() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 10, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsContainingVariant(variant), equalTo(Collections.emptyList()));
        assertThat(instance.hasRegionContainingVariant(variant), is(false));
    }

    @Test
    public void testGetTadsContainingVariantSingleTadVariantNotInChromosomeIndex() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 10, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsContainingVariant(new SimpleVariantCoordinates(GenomeAssembly.HG19, 100, 50, "A", "T")), equalTo(Collections
                .emptyList()));
    }

    @Test
    public void testGetTadsContainingPositionPositionOneBeforeStartOfRegion() {
        TopologicalDomain tad = new TopologicalDomain(1, 10, 12, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsOverlappingPosition(1, 9), equalTo(Collections.emptyList()));
    }

    @Test
    public void testGetTadsContainingPositionPositionAtStartOfRegion() {
        TopologicalDomain tad = new TopologicalDomain(1, 10, 12, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsOverlappingPosition(1, 10), equalTo(Collections.singletonList(tad)));
    }

    @Test
    public void testGetTadsContainingPositionPositionMiddleOfRegion() {
        TopologicalDomain tad = new TopologicalDomain(1, 10, 12, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsOverlappingPosition(1, 11), equalTo(Collections.singletonList(tad)));
    }

    @Test
    public void testGetTadsContainingPositionPositionAtEndOfRegion() {
        TopologicalDomain tad = new TopologicalDomain(1, 10, 12, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsOverlappingPosition(1, 12), equalTo(Collections.singletonList(tad)));
    }

    @Test
    public void testGetTadsContainingPositionPositionOneAfterEndOfRegion() {
        TopologicalDomain tad = new TopologicalDomain(1, 10, 12, new HashMap<>());
        createInstance(tad);

        assertThat(instance.getRegionsOverlappingPosition(1, 13), equalTo(Collections.emptyList()));
    }

    @Test
    public void testGetTadsContainingVariantTwoNonOverlappingTads() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 100, new HashMap<>());
        TopologicalDomain tad1 = new TopologicalDomain(2, 200, 300, new HashMap<>());
        createInstance(tad, tad1);

        assertThat(instance.getRegionsContainingVariant(variant), equalTo(Collections.singletonList(tad)));
    }

    @Test
    public void testGetTadsContainingVariantTwoOverlappingTadsVariantInBoth() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 100, new HashMap<>());
        TopologicalDomain tad1 = new TopologicalDomain(1, 25, 75, new HashMap<>());
        createInstance(tad, tad1);

        assertThat(instance.getRegionsContainingVariant(variant), equalTo(Arrays.asList(tad, tad1)));
    }

    @Test
    public void testGetTadsContainingVariantTwoOverlappingTadsVariantInOne() {
        TopologicalDomain tad = new TopologicalDomain(1, 1, 100, new HashMap<>());
        TopologicalDomain tad1 = new TopologicalDomain(1, 75, 200, new HashMap<>());
        createInstance(tad, tad1);

        assertThat(instance.getRegionsContainingVariant(variant), equalTo(Collections.singletonList(tad)));
    }

}