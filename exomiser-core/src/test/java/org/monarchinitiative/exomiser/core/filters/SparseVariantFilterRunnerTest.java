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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.monarchinitiative.exomiser.core.filters;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.monarchinitiative.exomiser.core.genome.VariantDataService;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
@RunWith(MockitoJUnitRunner.class)
public class SparseVariantFilterRunnerTest {

    @InjectMocks
    private SparseVariantFilterRunner instance;

    @Mock
    private VariantDataService variantDataService;

    @Mock
    private VariantEffectFilter targetFilter;
    @Mock
    private QualityFilter qualityFilter;
    @Mock
    private FrequencyFilter frequencyFilter;
    @Mock
    private PathogenicityFilter pathogenicityFilter;

    private VariantEvaluation passesAllFilters;
    private VariantEvaluation failsAllFilters;
    private VariantEvaluation passesQualityFrequencyFilter;
    private VariantEvaluation passesTargetQualityFilter;

    private List<VariantEvaluation> variantEvaluations;

    @Before
    public void setUp() {

        passesAllFilters = VariantEvaluation.builder(1, 1, "A", "T").build();
        failsAllFilters = VariantEvaluation.builder(2, 2, "A", "T").build();
        passesQualityFrequencyFilter = VariantEvaluation.builder(3, 3, "A", "T").build();
        passesTargetQualityFilter = VariantEvaluation.builder(4, 4, "A", "T").build();

        makeVariantEvaluations();

        setUpFrequencyMocks();
        setUpPathogenicityMocks();
        setUpQualityMocks();
        setUpTargetMocks();

    }

    private void makeVariantEvaluations() {
        variantEvaluations = new ArrayList<>();
        variantEvaluations.add(passesTargetQualityFilter);
        variantEvaluations.add(passesQualityFrequencyFilter);
        variantEvaluations.add(failsAllFilters);
        variantEvaluations.add(passesAllFilters);
    }

    private void setUpFrequencyMocks() {
        Mockito.when(frequencyFilter.getFilterType()).thenReturn(FilterType.FREQUENCY_FILTER);

        FilterResult passFrequencyResult = new PassFilterResult(FilterType.FREQUENCY_FILTER);
        FilterResult failFrequencyResult = new FailFilterResult(FilterType.FREQUENCY_FILTER);

        Mockito.when(frequencyFilter.runFilter(passesAllFilters)).thenReturn(passFrequencyResult);
        Mockito.when(frequencyFilter.runFilter(passesTargetQualityFilter)).thenReturn(failFrequencyResult);
    }

    private void setUpPathogenicityMocks() {
        Mockito.when(pathogenicityFilter.getFilterType()).thenReturn(FilterType.PATHOGENICITY_FILTER);

        FilterResult pass = new PassFilterResult(FilterType.PATHOGENICITY_FILTER);
        FilterResult fail = new FailFilterResult(FilterType.PATHOGENICITY_FILTER);

        Mockito.when(pathogenicityFilter.runFilter(passesAllFilters)).thenReturn(pass);
    }

    private void setUpQualityMocks() {
        Mockito.when(qualityFilter.getFilterType()).thenReturn(FilterType.QUALITY_FILTER);

        FilterResult passQualityResult = new PassFilterResult(FilterType.QUALITY_FILTER);
        FilterResult failQualityResult = new FailFilterResult(FilterType.QUALITY_FILTER);

        Mockito.when(qualityFilter.runFilter(passesAllFilters)).thenReturn(passQualityResult);
        Mockito.when(qualityFilter.runFilter(failsAllFilters)).thenReturn(failQualityResult);
        Mockito.when(qualityFilter.runFilter(passesQualityFrequencyFilter)).thenReturn(passQualityResult);
        Mockito.when(qualityFilter.runFilter(passesTargetQualityFilter)).thenReturn(passQualityResult);
    }

    private void setUpTargetMocks() {
        Mockito.when(targetFilter.getFilterType()).thenReturn(FilterType.VARIANT_EFFECT_FILTER);

        FilterResult passTargetResult = new PassFilterResult(FilterType.VARIANT_EFFECT_FILTER);
        FilterResult failTargetResult = new FailFilterResult(FilterType.VARIANT_EFFECT_FILTER);

        Mockito.when(targetFilter.runFilter(passesAllFilters)).thenReturn(passTargetResult);
        Mockito.when(targetFilter.runFilter(failsAllFilters)).thenReturn(failTargetResult);
        Mockito.when(targetFilter.runFilter(passesQualityFrequencyFilter)).thenReturn(failTargetResult);
        Mockito.when(targetFilter.runFilter(passesTargetQualityFilter)).thenReturn(passTargetResult);
    }

    @Test
    public void testRunOnlyReturnsVariantPassingAllFilters() {
        List<VariantFilter> filters = new ArrayList<>();
        filters.add(targetFilter);
        filters.add(qualityFilter);
        filters.add(frequencyFilter);
        filters.add(pathogenicityFilter);

        List<VariantEvaluation> expResult = new ArrayList<>();
        expResult.add(passesAllFilters);

        List<VariantEvaluation> result = instance.run(filters, variantEvaluations);
        assertThat(result, equalTo(expResult));
        assertThat(passesAllFilters.passedFilters(), is(true));
        assertThat(passesAllFilters.passedFilter(FilterType.VARIANT_EFFECT_FILTER), is(true));
        assertThat(passesAllFilters.passedFilter(FilterType.QUALITY_FILTER), is(true));
        assertThat(passesAllFilters.passedFilter(FilterType.FREQUENCY_FILTER), is(true));
        assertThat(passesAllFilters.passedFilter(FilterType.PATHOGENICITY_FILTER), is(true));
        //filters not run should return false 
        assertThat(passesAllFilters.passedFilter(FilterType.ENTREZ_GENE_ID_FILTER), is(false));
    }

    @Test
    public void testRunWithNoFiltersReturnsOriginalVariantEvaluations() {
        List<VariantFilter> filters = Collections.emptyList();

        List<VariantEvaluation> result = instance.run(filters, variantEvaluations);
        assertThat(result, equalTo(variantEvaluations));
    }

    @Test
    public void testRunWithOneFilterOnlyReturnsVariantPassingAllFilters() {

        VariantFilter filterToPass = qualityFilter;

        List<VariantEvaluation> expResult = new ArrayList<>();
        expResult.add(passesTargetQualityFilter);
        expResult.add(passesQualityFrequencyFilter);
        expResult.add(passesAllFilters);

        List<VariantEvaluation> result = instance.run(filterToPass, variantEvaluations);

        assertThat(result, equalTo(expResult));
        for (VariantEvaluation variantEvaluation : result) {
            assertPassedFilterAndFailedAllOthers(variantEvaluation, filterToPass);
        }

    }

    @Test
    public void testRunWithOneFilterUsingStreamOnlyReturnsVariantPassingAllFilters() {

        VariantFilter filterToPass = qualityFilter;

        List<VariantEvaluation> expResult = new ArrayList<>();
        expResult.add(passesTargetQualityFilter);
        expResult.add(passesQualityFrequencyFilter);
        expResult.add(passesAllFilters);

        List<VariantEvaluation> result = variantEvaluations.stream()
                .filter(variantEvaluation -> {
                    instance.run(filterToPass, variantEvaluation);
                    return variantEvaluation.passedFilters();
                })
                .collect(toList());

        assertThat(result, equalTo(expResult));
        for (VariantEvaluation variantEvaluation : result) {
            assertPassedFilterAndFailedAllOthers(variantEvaluation, filterToPass);
        }

    }

    private void assertPassedFilterAndFailedAllOthers(VariantEvaluation variantEvaluation, VariantFilter filterToPass) {
        assertThat(variantEvaluation.passedFilters(), is(true));
        assertThat(variantEvaluation.passedFilter(filterToPass.getFilterType()), is(true));
        //filters not run should return false
        assertThat(variantEvaluation.passedFilter(FilterType.VARIANT_EFFECT_FILTER), is(false));
        assertThat(variantEvaluation.passedFilter(FilterType.FREQUENCY_FILTER), is(false));
        assertThat(variantEvaluation.passedFilter(FilterType.PATHOGENICITY_FILTER), is(false));
        assertThat(variantEvaluation.passedFilter(FilterType.ENTREZ_GENE_ID_FILTER), is(false));
    }
}
