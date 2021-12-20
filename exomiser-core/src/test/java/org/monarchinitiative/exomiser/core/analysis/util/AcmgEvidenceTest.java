/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2021 Queen Mary University of London.
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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.monarchinitiative.exomiser.core.analysis.util.AcmgCriterion.*;

public class AcmgEvidenceTest {

    @Test
    void testEmptyBuilder() {
        assertThat(AcmgEvidence.builder().build(), equalTo(AcmgEvidence.empty()));
    }

    @Test
    void testEmptyStaticFactory() {
        assertThat(AcmgEvidence.of(Map.of()), equalTo(AcmgEvidence.empty()));
    }

    @Test
    void testStaticFactoryDefaultEvidence() {
        AcmgEvidence instance = AcmgEvidence.of(Map.of(PVS1, PVS1.evidence(), PS1, PS1.evidence()));
        assertThat(instance.toString(), equalTo("[PVS1, PS1]"));
    }

    @Test
    public void testToStringDefaultEvidence() {
        AcmgEvidence instance = AcmgEvidence.builder().add(PVS1).add(PS1).build();
        assertThat(instance.toString(), equalTo("[PVS1, PS1]"));
    }

    @Test
    public void testToStringModifiedEvidence() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.STRONG)
                .add(PS1, Evidence.MODERATE)
                .add(PP3, Evidence.VERY_STRONG)
                .build();
        assertThat(instance.toString(), equalTo("[PVS1_Strong, PS1_Moderate, PP3_VeryStrong]"));
    }

    @Test
    public void testGetEvidenceOverwriteInputInBuilder() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.STRONG)
                .add(PVS1)
                .build();
        assertThat(instance.criterionEvidence(PVS1), equalTo(PVS1.evidence()));
    }

    @Test
    public void testGetEvidenceDefaultValue() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1)
                .build();
        assertThat(instance.criterionEvidence(PVS1), equalTo(PVS1.evidence()));
    }

    @Test
    public void testGetEvidenceModifiedValue() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.MODERATE)
                .build();
        assertThat(instance.criterionEvidence(PVS1), equalTo(Evidence.MODERATE));
    }

    @Test
    public void testGetEvidenceNoValue() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.MODERATE)
                .build();
        assertThat(instance.criterionEvidence(PM3), equalTo(null));
    }

    @Test
    public void testContainsEvidence() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.MODERATE)
                .build();
        assertThat(instance.hasCriterion(PVS1), equalTo(true));
        assertThat(instance.hasCriterion(PM3), equalTo(false));
    }

    @Test
    public void testBuilderContainsEvidence() {
        AcmgEvidence.Builder instance = AcmgEvidence.builder()
                .add(PVS1, Evidence.MODERATE);
        assertThat(instance.contains(PVS1), equalTo(true));
        assertThat(instance.contains(PM3), equalTo(false));
    }

    @Test
    public void testSizeWhenEmpty() {
        AcmgEvidence instance = AcmgEvidence.builder().build();
        assertThat(instance.size(), equalTo(0));
    }

    @Test
    public void testSizeWithElement() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1)
                .build();
        assertThat(instance.size(), equalTo(1));
    }

    @Test
    public void testIsEmpty() {
        AcmgEvidence instance = AcmgEvidence.builder().build();
        assertThat(instance.isEmpty(), equalTo(true));
    }

    @Test
    public void testNotEmpty() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1)
                .build();
        assertThat(instance.isEmpty(), equalTo(false));
    }

    @Test
    void testPvs() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PVS1)
                .add(PM1, Evidence.VERY_STRONG)
                .build();
        assertThat(instance.pvs(), equalTo(2));
    }

    @Test
    void testPs() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PS1)
                .build();
        assertThat(instance.ps(), equalTo(1));
    }

    @Test
    void testPm() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PM1)
                .build();
        assertThat(instance.pm(), equalTo(1));
    }

    @Test
    void testPp() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(PP1)
                .build();
        assertThat(instance.pp(), equalTo(1));
    }

    @Test
    void testBa() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(BA1)
                .build();
        assertThat(instance.ba(), equalTo(1));
    }

    @Test
    void testBs() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(BS1)
                .build();
        assertThat(instance.bs(), equalTo(1));
    }

    @Test
    void testBp() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .add(BP1)
                .build();
        assertThat(instance.bp(), equalTo(1));
    }

    @Test
    void testEmptyCounts() {
        AcmgEvidence instance = AcmgEvidence.builder()
                .build();
        assertThat(instance.pvs(), equalTo(0));
        assertThat(instance.ps(), equalTo(0));
        assertThat(instance.pm(), equalTo(0));
        assertThat(instance.pp(), equalTo(0));
        assertThat(instance.ba(), equalTo(0));
        assertThat(instance.bs(), equalTo(0));
        assertThat(instance.bp(), equalTo(0));
    }
}