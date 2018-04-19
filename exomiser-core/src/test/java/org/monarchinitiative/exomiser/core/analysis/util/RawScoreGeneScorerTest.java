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
package org.monarchinitiative.exomiser.core.analysis.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.mendel.ModeOfInheritance;
import de.charite.compbio.jannovar.pedigree.Disease;
import de.charite.compbio.jannovar.pedigree.PedPerson;
import de.charite.compbio.jannovar.pedigree.Pedigree;
import de.charite.compbio.jannovar.pedigree.Sex;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.GenotypeType;
import htsjdk.variant.variantcontext.VariantContext;
import org.junit.Test;
import org.monarchinitiative.exomiser.core.filters.FilterResult;
import org.monarchinitiative.exomiser.core.filters.FilterType;
import org.monarchinitiative.exomiser.core.model.Gene;
import org.monarchinitiative.exomiser.core.model.GeneScore;
import org.monarchinitiative.exomiser.core.model.SampleIdentifier;
import org.monarchinitiative.exomiser.core.model.VariantEvaluation;
import org.monarchinitiative.exomiser.core.prioritisers.MockPriorityResult;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityType;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.monarchinitiative.exomiser.core.analysis.util.TestAlleleFactory.*;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class RawScoreGeneScorerTest {

    private static final FilterResult PASS_FREQUENCY = FilterResult.pass(FilterType.FREQUENCY_FILTER);
    private static final FilterResult FAIL_FREQUENCY = FilterResult.fail(FilterType.FREQUENCY_FILTER);
    private static final FilterResult PASS_PATHOGENICITY = FilterResult.pass(FilterType.PATHOGENICITY_FILTER);
    private static final FilterResult FAIL_PATHOGENICITY = FilterResult.fail(FilterType.PATHOGENICITY_FILTER);

    private Gene newGene(VariantEvaluation... variantEvaluations) {
        Gene gene = new Gene("TEST1", 1234);
        Arrays.stream(variantEvaluations).forEach(gene::addVariant);
        return gene;
    }

    private VariantEvaluation failFreq() {
        return VariantEvaluation.builder(1, 1, "A", "T")
                .variantEffect(VariantEffect.MISSENSE_VARIANT)
                .filterResults(FAIL_FREQUENCY)
                .build();
    }

    private VariantEvaluation passAllFrameShift() {
        return VariantEvaluation.builder(1, 2, "A", "T")
                .variantEffect(VariantEffect.FRAMESHIFT_VARIANT)
                .filterResults(PASS_FREQUENCY, PASS_PATHOGENICITY)
                .build();
    }

    private VariantEvaluation passAllMissense() {
        return VariantEvaluation.builder(1, 3, "A", "T")
                .variantEffect(VariantEffect.MISSENSE_VARIANT)
                .filterResults(PASS_FREQUENCY, PASS_PATHOGENICITY)
                .build();
    }

    private VariantEvaluation passAllSynonymous() {
        return VariantEvaluation.builder(1, 4, "A", "T")
                .variantEffect(VariantEffect.SYNONYMOUS_VARIANT)
                .filterResults(PASS_FREQUENCY, PASS_PATHOGENICITY)
                .build();
    }

    private List<GeneScore> scoreGene(Gene gene, ModeOfInheritance modeOfInheritance, int sampleId) {
        return scoreGene(gene, modeOfInheritance, SampleIdentifier.of("sample", sampleId), Pedigree.constructSingleSamplePedigree("sample"));
    }

    private List<GeneScore> scoreGene(Gene gene, ModeOfInheritance modeOfInheritance, SampleIdentifier probandSample, Pedigree pedigree) {
        InheritanceModeAnnotator inheritanceModeAnnotator = new InheritanceModeAnnotator(pedigree, InheritanceModeOptions.defaultForModes(modeOfInheritance));
        RawScoreGeneScorer instance = new RawScoreGeneScorer(probandSample, inheritanceModeAnnotator);
        return instance.scoreGene().apply(gene);
    }

    private List<GeneScore> scoreGene(Gene gene, InheritanceModeOptions inheritanceModeOptions, int sampleId) {
        return scoreGene(gene, inheritanceModeOptions, SampleIdentifier.of("sample", sampleId), Pedigree.constructSingleSamplePedigree("sample"));
    }

    private List<GeneScore> scoreGene(Gene gene, InheritanceModeOptions inheritanceModeOptions, SampleIdentifier probandSample) {
        return scoreGene(gene, inheritanceModeOptions, probandSample, Pedigree.constructSingleSamplePedigree("sample"));
    }

    private List<GeneScore> scoreGene(Gene gene, InheritanceModeOptions inheritanceModeOptions, SampleIdentifier probandSample, Pedigree pedigree) {
        InheritanceModeAnnotator inheritanceModeAnnotator = new InheritanceModeAnnotator(pedigree, inheritanceModeOptions);
        RawScoreGeneScorer instance = new RawScoreGeneScorer(probandSample, inheritanceModeAnnotator);
        return instance.scoreGene().apply(gene);
    }

    @Test
    public void testScoreGeneWithoutPriorityResultsOrVariants_UNINITIALIZED() {
        Gene gene = newGene();
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithoutPriorityResultsOrVariants_AUTOSOMAL_DOMINANT() {
        Gene gene = newGene();
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_DOMINANT, 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_DOMINANT)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithoutPriorityResultsOrVariants_AUTOSOMAL_RECESSIVE() {
        Gene gene = newGene();
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSingleFailedVariant_UNINITIALIZED() {
        Gene gene = newGene(failFreq());
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.ANY)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSingleFailedVariant_AUTOSOMAL_DOMINANT() {
        Gene gene = newGene(failFreq());
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_DOMINANT, 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_DOMINANT)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSingleFailedVariant_AUTOSOMAL_RECESSIVE() {
        Gene gene = newGene(failFreq());
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSinglePassedVariant_UNINITIALIZED() {
        VariantEvaluation passAllFrameshift = passAllFrameShift();
        Gene gene = newGene(passAllFrameshift);
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        float variantScore = passAllFrameshift.getVariantScore();

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.ANY)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSinglePassedVariant_AUTOSOMAL_DOMINANT() {
        VariantEvaluation passAllFrameshift = passAllFrameShift();
        passAllFrameshift.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT));

        Gene gene = newGene(passAllFrameshift);
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_DOMINANT, 0);

        float variantScore = passAllFrameshift.getVariantScore();

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_DOMINANT)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSinglePassedVariant_AUTOSOMAL_RECESSIVE_HOM_ALT() {
        List<Allele> alleles = buildAlleles("A", "T");

        //Classical recessive inheritance mode
        Genotype proband = buildUnPhasedSampleGenotype("Cain", alleles.get(1), alleles.get(1));
        assertThat(proband.getType(), equalTo(GenotypeType.HOM_VAR));

        Genotype mother = buildUnPhasedSampleGenotype("Eve", alleles.get(0), alleles.get(1));
        assertThat(mother.getType(), equalTo(GenotypeType.HET));

        Genotype father = buildUnPhasedSampleGenotype("Adam", alleles.get(1), alleles.get(0));
        assertThat(father.getType(), equalTo(GenotypeType.HET));

        VariantContext variantContext = buildVariantContext(1, 12345, alleles, proband, mother, father);
        System.out.println("Built variant context " + variantContext);
        System.out.println("Proband sample 0 has genotype " + variantContext.getGenotype(0).getGenotypeString());

        PedPerson probandPerson = new PedPerson("Family", "Cain", "Adam", "Eve", Sex.MALE, Disease.AFFECTED, new ArrayList<>());
        PedPerson motherPerson = new PedPerson("Family", "Eve", "0", "0", Sex.FEMALE, Disease.UNAFFECTED, new ArrayList<>());
        PedPerson fatherPerson = new PedPerson("Family", "Adam", "0", "0", Sex.MALE, Disease.UNAFFECTED, new ArrayList<>());
        Pedigree pedigree = buildPedigree(probandPerson, motherPerson, fatherPerson);

        VariantEvaluation probandHomAlt = filteredVariant(1, 12345, "A", "T", FilterResult.pass(FilterType.FREQUENCY_FILTER), variantContext, VariantEffect.MISSENSE_VARIANT);
        probandHomAlt.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        Gene gene = newGene(probandHomAlt);

        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, SampleIdentifier.of(probandPerson.getName(), 0), pedigree);

        float variantScore = probandHomAlt.getVariantScore();

        assertThat(probandHomAlt.contributesToGeneScore(), is(true));

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(probandHomAlt))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSinglePassedVariant_AUTOSOMAL_RECESSIVE_HET() {
        VariantEvaluation passAllFrameShift = passAllFrameShift();
        Gene gene = newGene(passAllFrameShift);
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0);

        //A single het allele can't be compatible with AR
        assertThat(passAllFrameShift.contributesToGeneScore(), is(false));

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(0f)
                .phenotypeScore(0f)
                .combinedScore(0f)
                .contributingVariants(Collections.emptyList())
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithSinglePassedAndSingleFailedVariantOnlyPassedVariantIsConsidered() {
        VariantEvaluation passAllFrameshift = passAllFrameShift();

        Gene gene = newGene(passAllFrameshift, failFreq());
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        float variantScore = passAllFrameshift.getVariantScore();

        assertThat(passAllFrameshift.contributesToGeneScore(), is(true));

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.ANY)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithTwoPassedVariants_UNINITIALIZED_inheritance() {
        VariantEvaluation passAllMissense = passAllMissense();
        VariantEvaluation passAllFrameshift = passAllFrameShift();

        Gene gene = newGene(passAllFrameshift, passAllMissense);
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        float variantScore = passAllFrameshift.getVariantScore();

        assertThat(passAllFrameshift.contributesToGeneScore(), is(true));

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.ANY)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithTwoPassedVariants_AUTOSOMAL_DOMINANT_inheritance() {
        VariantEvaluation passAllMissense = passAllMissense();
        passAllMissense.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT));

        VariantEvaluation passAllFrameshift = passAllFrameShift();
        passAllFrameshift.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT));

        Gene gene = newGene(passAllFrameshift, passAllMissense);

        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_DOMINANT, 0);

        float variantScore = passAllFrameshift.getVariantScore();

        assertThat(passAllFrameshift.contributesToGeneScore(), is(true));
        assertThat(passAllFrameshift.contributesToGeneScoreUnderMode(ModeOfInheritance.AUTOSOMAL_DOMINANT), is(true));

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_DOMINANT)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithTwoPassedVariants_X_DOMINANT_inheritance() {
        VariantEvaluation passAllMissense = passAllMissense();
        passAllMissense.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.X_DOMINANT));

        VariantEvaluation passAllFrameshift = passAllFrameShift();
        passAllFrameshift.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.X_DOMINANT));

        Gene gene = newGene(passAllFrameshift, passAllMissense);

        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.X_DOMINANT, 0);

        float variantScore = passAllFrameshift.getVariantScore();

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.X_DOMINANT)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                //REALLY? This isn't X-linked
                .contributingVariants(ImmutableList.of(passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithTwoPassedVariants_AUTOSOMAL_RECESSIVE_inheritance() {
        VariantEvaluation passAllMissense = passAllMissense();
        passAllMissense.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        VariantEvaluation passAllFrameshift = passAllFrameShift();
        passAllFrameshift.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        Gene gene = newGene(passAllMissense, passAllFrameshift);
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0);

        float variantScore = (passAllFrameshift.getVariantScore() + passAllMissense.getVariantScore()) / 2f;

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllMissense, passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithThreePassedVariants_AUTOSOMAL_RECESSIVE_inheritance() {
        VariantEvaluation passAllMissense = passAllMissense();
        passAllMissense.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        VariantEvaluation passAllSynonymous = passAllSynonymous();
        passAllSynonymous.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        VariantEvaluation passAllFrameshift = passAllFrameShift();
        passAllFrameshift.setCompatibleInheritanceModes(EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE));

        Gene gene = newGene(passAllMissense, passAllSynonymous, passAllFrameshift);
        List<GeneScore> geneScores = scoreGene(gene, ModeOfInheritance.AUTOSOMAL_RECESSIVE, 0);

        float variantScore = (passAllFrameshift.getVariantScore() + passAllMissense.getVariantScore()) / 2f;
        assertThat(passAllFrameshift.contributesToGeneScore(), is(true));
        assertThat(passAllFrameshift.contributesToGeneScoreUnderMode(ModeOfInheritance.AUTOSOMAL_RECESSIVE), is(true));

        assertThat(passAllMissense.contributesToGeneScore(), is(true));
        assertThat(passAllMissense.contributesToGeneScoreUnderMode(ModeOfInheritance.AUTOSOMAL_RECESSIVE), is(true));

        assertThat(passAllSynonymous.contributesToGeneScore(), is(false));
        assertThat(passAllSynonymous.contributesToGeneScoreUnderMode(ModeOfInheritance.AUTOSOMAL_RECESSIVE), is(false));


        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.AUTOSOMAL_RECESSIVE)
                .variantScore(variantScore)
                .phenotypeScore(0f)
                .combinedScore(variantScore / 2)
                .contributingVariants(ImmutableList.of(passAllMissense, passAllFrameshift))
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testGenesAreRankedAccordingToScore() {
        Gene first = new Gene("FIRST", 1111);
        first.addVariant(passAllFrameShift());
        first.addPriorityResult(new MockPriorityResult(PriorityType.HIPHIVE_PRIORITY, first.getEntrezGeneID(), first.getGeneSymbol(), 1d));

        Gene middle = new Gene("MIDDLE", 2222);
        middle.addVariant(passAllMissense());
        middle.addPriorityResult(new MockPriorityResult(PriorityType.HIPHIVE_PRIORITY, middle.getEntrezGeneID(), middle.getGeneSymbol(), 1d));

        Gene last = new Gene("LAST", 3333);
        last.addVariant(passAllSynonymous());
        last.addPriorityResult(new MockPriorityResult(PriorityType.HIPHIVE_PRIORITY, last.getEntrezGeneID(), last.getGeneSymbol(), 1d));

        List<Gene> genes = Lists.newArrayList(last, first, middle);
        Collections.shuffle(genes);

        RawScoreGeneScorer instance = new RawScoreGeneScorer(SampleIdentifier.of("sample", 0), new InheritanceModeAnnotator(Pedigree.constructSingleSamplePedigree("Nemo"), InheritanceModeOptions.empty()));
        instance.scoreGenes(genes);

        genes.forEach(System.out::println);

        assertThat(genes.indexOf(first), equalTo(0));
        assertThat(genes.indexOf(middle), equalTo(1));
        assertThat(genes.indexOf(last), equalTo(2));
    }

    ///Priority and Combined score tests
    @Test
    public void testCalculateCombinedScoreFromUnoptimisedPrioritiser() {
        Gene gene = newGene();
        gene.addPriorityResult(new MockPriorityResult(PriorityType.OMIM_PRIORITY, gene.getEntrezGeneID(), gene.getGeneSymbol(), 1d));

        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        GeneScore expected = GeneScore.builder()
                .geneIdentifier(gene.getGeneIdentifier())
                .modeOfInheritance(ModeOfInheritance.ANY)
                .variantScore(0f)
                .phenotypeScore(1f)
                .combinedScore(0.5f)
                .build();

        assertThat(geneScores, equalTo(ImmutableList.of(expected)));
    }

    @Test
    public void testScoreGeneWithoutPriorityResultsOrVariantsAllInheritanceModes() {
        Gene gene = newGene();
        EnumSet<ModeOfInheritance> inheritanceModes = EnumSet.of(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE, ModeOfInheritance.X_DOMINANT, ModeOfInheritance.X_RECESSIVE, ModeOfInheritance.MITOCHONDRIAL);
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.defaults(), 0);

        List<GeneScore> expected = inheritanceModes.stream()
                .map(mode ->  GeneScore.builder()
                    .geneIdentifier(gene.getGeneIdentifier())
                    .modeOfInheritance(mode)
                    .variantScore(0f)
                    .phenotypeScore(0f)
                    .combinedScore(0f)
                    .contributingVariants(Collections.emptyList())
                    .build())
                .collect(toList());

        assertThat(geneScores, equalTo(expected));
    }

    @Test
    public void testScoreGeneWithoutPriorityResultsOrVariantsOrInheritanceModes() {
        Gene gene = newGene();
        List<GeneScore> geneScores = scoreGene(gene, InheritanceModeOptions.empty(), 0);

        List<GeneScore> expected = ImmutableList.of(
                GeneScore.builder()
                        .geneIdentifier(gene.getGeneIdentifier())
                        .modeOfInheritance(ModeOfInheritance.ANY)
                        .variantScore(0f)
                        .phenotypeScore(0f)
                        .combinedScore(0f)
                        .contributingVariants(Collections.emptyList())
                        .build()
        );

        assertThat(geneScores, equalTo(expected));
    }

}
