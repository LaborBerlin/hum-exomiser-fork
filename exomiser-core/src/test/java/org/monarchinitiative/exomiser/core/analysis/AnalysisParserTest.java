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
package org.monarchinitiative.exomiser.core.analysis;

import com.google.common.collect.ImmutableMap;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.mendel.ModeOfInheritance;
import de.charite.compbio.jannovar.mendel.SubModeOfInheritance;
import org.junit.Before;
import org.junit.Test;
import org.monarchinitiative.exomiser.core.analysis.AnalysisParser.AnalysisFileNotFoundException;
import org.monarchinitiative.exomiser.core.analysis.AnalysisParser.AnalysisParserException;
import org.monarchinitiative.exomiser.core.analysis.util.InheritanceModeOptions;
import org.monarchinitiative.exomiser.core.filters.*;
import org.monarchinitiative.exomiser.core.genome.*;
import org.monarchinitiative.exomiser.core.model.GeneticInterval;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.monarchinitiative.exomiser.core.prioritisers.HiPhiveOptions;
import org.monarchinitiative.exomiser.core.prioritisers.NoneTypePriorityFactoryStub;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityFactory;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityType;
import org.monarchinitiative.exomiser.core.writers.OutputFormat;
import org.monarchinitiative.exomiser.core.writers.OutputSettings;

import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public class AnalysisParserTest {

    private AnalysisParser instance;
    private PriorityFactory priorityFactory;

    private List<AnalysisStep> analysisSteps;

    private List<String> hpoIds;
    private Set<FrequencySource> frequencySources;
    private Set<PathogenicitySource> pathogenicitySources;

    @Before
    public void setUp() {
        priorityFactory = new NoneTypePriorityFactoryStub();
        GenomeAnalysisServiceProvider genomeAnalysisServiceProvider = new GenomeAnalysisServiceProvider(TestFactory.buildDefaultHg19GenomeAnalysisService());
        instance = new AnalysisParser(priorityFactory, genomeAnalysisServiceProvider);

        analysisSteps = new ArrayList<>();
        hpoIds = new ArrayList<>(Arrays.asList("HP:0001156", "HP:0001363", "HP:0011304", "HP:0010055"));
        frequencySources = EnumSet.of(FrequencySource.THOUSAND_GENOMES, FrequencySource.ESP_AFRICAN_AMERICAN, FrequencySource.EXAC_AFRICAN_INC_AFRICAN_AMERICAN);
        pathogenicitySources = EnumSet.of(PathogenicitySource.SIFT, PathogenicitySource.POLYPHEN, PathogenicitySource.MUTATION_TASTER);
    }

    private static String addStepToAnalysis(String step) {
        return String.format("analysis:\n"
                + "    vcf: test.vcf\n"
                + "    genomeAssembly: hg19\n"
                + "    ped:\n"
//                + "    modeOfInheritance: [AUTOSOMAL_DOMINANT]\n"
                + "    inheritanceModes: {\n" +
                "            AUTOSOMAL_DOMINANT: 0.1,\n" +
                "            AUTOSOMAL_RECESSIVE_HOM_ALT: 1.0,\n" +
                "            AUTOSOMAL_RECESSIVE_COMP_HET: 2.0,\n" +
                "            X_DOMINANT: 0.1,\n" +
                "            X_RECESSIVE_HOM_ALT: 1.0,\n" +
                "            X_RECESSIVE_COMP_HET: 2.0,\n" +
                "            MITOCHONDRIAL: 0.2 \n" +
                "      }\n"
                + "    hpoIds: ['HP:0001156', 'HP:0001363', 'HP:0011304', 'HP:0010055']\n"
                + "    analysisMode: PASS_ONLY \n"
                + "    frequencySources: [THOUSAND_GENOMES, ESP_AFRICAN_AMERICAN, EXAC_AFRICAN_INC_AFRICAN_AMERICAN]\n"
                + "    pathogenicitySources: [SIFT, POLYPHEN, MUTATION_TASTER]\n"
                + "    steps: ["
                + "        %s\n"
                + "]", step);
    }

    @Test
    public void testParseAnalysisSteps_NoSteps() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis(""));
        System.out.println(analysis);
        assertThat(analysis.getVcfPath(), equalTo(Paths.get("test.vcf")));
        assertThat(analysis.getPedPath(), nullValue());
        assertThat(analysis.getProbandSampleName(), equalTo(""));
        assertThat(analysis.getInheritanceModeOptions(), equalTo(InheritanceModeOptions.defaults()));
        assertThat(analysis.getHpoIds(), equalTo(hpoIds));
        assertThat(analysis.getAnalysisMode(), equalTo(AnalysisMode.PASS_ONLY));
        assertThat(analysis.getFrequencySources(), equalTo(frequencySources));
        assertThat(analysis.getPathogenicitySources(), equalTo(pathogenicitySources));
        assertThat(analysis.getAnalysisSteps().isEmpty(), is(true));
    }

    @Test(expected = AnalysisParserException.class)
    public void throwsExceptionWhenNoVcfIsSet() {
        instance.parseAnalysis(
                "analysis:\n"
                + "    vcf: \n"
        );
    }

    @Test
    public void testParseAnalysisPedPathSpecified() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    ped: test.ped\n"
                        + "    ");
        assertThat(analysis.getPedPath(), equalTo(Paths.get("test.ped")));
    }

    @Test
    public void testParseAnalysisProbandSampleNameSpecified() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    proband: Bod \n"
                        + "    ");
        assertThat(analysis.getProbandSampleName(), equalTo("Bod"));
    }

    @Test
    public void testParseAnalysis_FullAnalysisMode() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                + "    vcf: test.vcf\n"
                + "    analysisMode: FULL \n"
                + "    ");
        assertThat(analysis.getAnalysisMode(), equalTo(AnalysisMode.FULL));
    }

    @Test
    public void testParseAnalysisNotSettingGenomeBuildReturnsDefault() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    ");
        assertThat(analysis.getGenomeAssembly(), equalTo(GenomeAssembly.defaultBuild()));
    }

    @Test(expected = UnsupportedGenomeAssemblyException.class)
    public void testParseAnalysisThrowsExceptionForUnsupportedGenomeBuild() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    genomeAssembly: hg38\n"
                        + "    ");
    }

    @Test
    public void testParseAnalysisCanSetAlternativeGenomeAssemblyUsingUcscName() {
        AnalysisParser hg19And38SupportedParser = getHg19and38SupportedParser();

        Analysis hg38Analysis = hg19And38SupportedParser.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    genomeAssembly: hg38\n"
                        + "    ");
        assertThat(hg38Analysis.getGenomeAssembly(), equalTo(GenomeAssembly.HG38));

        Analysis hg19Analysis = hg19And38SupportedParser.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    genomeAssembly: hg19\n"
                        + "    ");
        assertThat(hg19Analysis.getGenomeAssembly(), equalTo(GenomeAssembly.HG19));
    }

    private AnalysisParser getHg19and38SupportedParser() {
        GenomeAnalysisService hg19AnalysisService = TestFactory.buildStubGenomeAnalysisService(GenomeAssembly.HG19);
        GenomeAnalysisService hg38AnalysisService = TestFactory.buildStubGenomeAnalysisService(GenomeAssembly.HG38);

        GenomeAnalysisServiceProvider genomeAnalysisServiceProvider = new GenomeAnalysisServiceProvider(hg19AnalysisService, hg38AnalysisService);
        return new AnalysisParser(priorityFactory, genomeAnalysisServiceProvider);
    }

    @Test
    public void testParseAnalysisCanSetGenomeBuildUsingGrcName() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    genomeAssembly: GRCh37\n"
                        + "    ");
        assertThat(analysis.getGenomeAssembly(), equalTo(GenomeAssembly.HG19));
    }

    @Test(expected = GenomeAssembly.InvalidGenomeAssemblyException.class)
    public void testParseAnalysisUnrecognisedGenomeBuild() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    genomeAssembly: invalid\n"
                        + "    ");
    }

    @Test
    public void testParseAnalysisModeOfInheritanceAutosomalDominant() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    inheritanceModes: {\n"
                        + "        AUTOSOMAL_DOMINANT: 0.1 \n"
                        + "}\n"
                        + "    ");
        Map<SubModeOfInheritance, Float> options = ImmutableMap.of(SubModeOfInheritance.AUTOSOMAL_DOMINANT, 0.1f);
        assertThat(analysis.getInheritanceModeOptions(), equalTo(InheritanceModeOptions.of(options)));
    }

    @Test
    public void testParseAnalysisModeOfInheritanceRemovesAny() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    inheritanceModes: {\n"
                        + "        AUTOSOMAL_DOMINANT: 0.1,\n"
                        + "        ANY: 0.1 \n"
                        + "}\n"
                        + "    ");

        Map<SubModeOfInheritance, Float> options = ImmutableMap.of(SubModeOfInheritance.AUTOSOMAL_DOMINANT, 0.1f);
        assertThat(analysis.getInheritanceModeOptions(), equalTo(InheritanceModeOptions.of(options)));
    }

    @Test
    public void testParseAnalysisOldModeOfInheritanceConvertsToInheritanceModes() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    modeOfInheritance: AUTOSOMAL_DOMINANT\n"
                        + "    ");
        assertThat(analysis.getInheritanceModeOptions(), equalTo(InheritanceModeOptions.defaultForModes(ModeOfInheritance.AUTOSOMAL_DOMINANT)));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseAnalysisModeOfInheritanceUserUsesWrongValue() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    modeOfInheritance: AD\n"
                        );
    }

    /**
     * geneScoreMode was removed in commit 2055ac3b36c401569d9b201f43cf23d1f8c6aed2. We're checking that old analysis
     * scripts will still function.
     */
    @Test
    public void testParseAnalysis_DeprecatedGeneScoreModeHasNoEffect() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
                        + "    geneScoreMode: RAW_SCORE\n"
                        );
        Analysis expected = Analysis.builder().vcfPath(Paths.get("test.vcf")).build();
        assertThat(analysis, equalTo(expected));
    }

    @Test
    public void testParseAnalysisStep_UnsupportedFilterAddsNothingToAnalysisSteps() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("wibbleFilter: {}"));
        assertThat(analysis.getAnalysisSteps().isEmpty(), is(true));
    }

    @Test
    public void testParseAnalysisStep_FailedVariantFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("failedVariantFilter: {}"));
        analysisSteps.add(new FailedVariantFilter());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_RegulatoryFeatureFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("regulatoryFeatureFilter: {}"));
        analysisSteps.add(new RegulatoryFeatureFilter());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_IntervalFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("intervalFilter: {interval: 'chr10:122892600-122892700'}"));
        analysisSteps.add(new IntervalFilter(new GeneticInterval(10, 122892600, 122892700)));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_GeneIdFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("genePanelFilter: {geneSymbols: [FGFR1, FGFR2]}"));
        analysisSteps.add(new GeneSymbolFilter(new LinkedHashSet<>(Arrays.asList("FGFR1", "FGFR2"))));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_QualityFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("qualityFilter: {minQuality: 50.0}"));
        analysisSteps.add(new QualityFilter(50.0f));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_VariantEffectFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("variantEffectFilter: {remove: [SYNONYMOUS_VARIANT, INTERGENIC_VARIANT]}"));
        analysisSteps.add(new VariantEffectFilter(EnumSet.of(VariantEffect.SYNONYMOUS_VARIANT, VariantEffect.INTERGENIC_VARIANT)));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseAnalysisStep_VariantEffectFilter_illegalVariantEffect() {
        instance.parseAnalysis(addStepToAnalysis("variantEffectFilter: {remove: [WIBBLE]}"));
    }

    @Test
    public void testParseAnalysisStep_KnownVariantFilterFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("knownVariantFilter: {}"));
        analysisSteps.add(new KnownVariantFilter());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseAnalysisStep_FrequencyFilterNoFrequencySourcesDefined() {
        String script = "analysis:\n"
                + "    vcf: test.vcf\n"
                + "    frequencySources: []\n"
                + "    steps: ["
                + "        frequencyFilter: {maxFrequency: 1.0}\n"
                + "]";
                
        instance.parseAnalysis(script);
    }

    @Test
    public void testParseAnalysisStep_FrequencyFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("frequencyFilter: {maxFrequency: 1.0}"));
        analysisSteps.add(new FrequencyFilter(1.0f));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseAnalysisStep_PathogenicityFilterNoPathSourcesDefined() {
        String script = "analysis:\n"
                + "    vcf: test.vcf\n"
                + "    pathogenicitySources: []\n"
                + "    steps: ["
                + "        pathogenicityFilter: {keepNonPathogenic: false}\n"
                + "]";
                
        instance.parseAnalysis(script);
    }

    @Test
    public void testParseAnalysisStep_PathogenicityFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("pathogenicityFilter: {keepNonPathogenic: false}"));
        analysisSteps.add(new PathogenicityFilter(false));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_PriorityScoreFilter() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("priorityScoreFilter: {priorityType: HIPHIVE_PRIORITY, minPriorityScore: 0.65}"));
        analysisSteps.add(new PriorityScoreFilter(PriorityType.HIPHIVE_PRIORITY, 0.65f));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_InheritanceFilterUndefinedMode() {
        Analysis analysis = instance.parseAnalysis(
                "analysis:\n"
                + "    vcf: test.vcf\n"
//                + "    modeOfInheritance: UNINITIALIZED\n"
                + "    inheritanceModes: {}\n"
                + "    hpoIds: []\n"
                + "    analysisMode: PASS_ONLY \n"
                + "    pathogenicitySources: []\n"
                + "    frequencySources: []\n"
                + "    steps: ["
                + "        inheritanceFilter: {}\n"
                + "]"
        );
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseAnalysisStep_InheritanceFilterUnrecognisedValue() {
        instance.parseAnalysis(
                "analysis:\n"
                        + "    vcf: test.vcf\n"
//                        + "    modeOfInheritance: [WIBBLE!]"
                        + "    inheritanceModes: {WIBBLE: 0.0}\n"
        );
    }

    @Test
    public void testParseAnalysisStep_InheritanceFilterDefinedMode() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("inheritanceFilter: {}"));
        analysisSteps.add(new InheritanceFilter(ModeOfInheritance.AUTOSOMAL_DOMINANT, ModeOfInheritance.AUTOSOMAL_RECESSIVE, ModeOfInheritance.X_DOMINANT, ModeOfInheritance.X_RECESSIVE, ModeOfInheritance.MITOCHONDRIAL));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_OmimPrioritiser() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("omimPrioritiser: {}"));
        analysisSteps.add(priorityFactory.makeOmimPrioritiser());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_HiPhivePrioritiserWithDefaultOptions() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("hiPhivePrioritiser: {}"));
        analysisSteps.add(priorityFactory.makeHiPhivePrioritiser(HiPhiveOptions.DEFAULT));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_HiPhivePrioritiserWithUserDefinedOptions() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("hiPhivePrioritiser: {diseaseId: 'OMIM:101600', candidateGeneSymbol: FGFR2, runParams: 'human,mouse,fish,ppi'}"));
        HiPhiveOptions hiPhiveOptions = HiPhiveOptions.builder().diseaseId("OMIM:101600").candidateGeneSymbol("FGFR2").runParams("human,mouse,fish,ppi").build();
        analysisSteps.add(priorityFactory.makeHiPhivePrioritiser(hiPhiveOptions));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_PhivePrioritiser() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("phivePrioritiser: {}"));
        analysisSteps.add(priorityFactory.makePhivePrioritiser());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_PhenixPrioritiser() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("phenixPrioritiser: {}"));
        analysisSteps.add(priorityFactory.makePhenixPrioritiser());
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisStep_WalkerPrioritiser() {
        Analysis analysis = instance.parseAnalysis(addStepToAnalysis("exomeWalkerPrioritiser: {seedGeneIds: [11111, 22222, 33333]}"));
        analysisSteps.add(priorityFactory.makeExomeWalkerPrioritiser(new ArrayList<>(Arrays.asList(11111, 22222, 33333))));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test
    public void testParseAnalysisFileFromPath() {
        ModeOfInheritance modeOfInheritance = ModeOfInheritance.AUTOSOMAL_DOMINANT;

        Analysis analysis = instance.parseAnalysis(Paths.get("src/test/resources/analysisExample.yml"));
        System.out.println(analysis);
        assertThat(analysis.getVcfPath(), equalTo(Paths.get("test.vcf")));
        assertThat(analysis.getPedPath(), nullValue());
        assertThat(analysis.getHpoIds(), equalTo(hpoIds));
        assertThat(analysis.getInheritanceModeOptions(), equalTo(InheritanceModeOptions.defaultForModes(modeOfInheritance)));
        assertThat(analysis.getFrequencySources(), equalTo(frequencySources));
        assertThat(analysis.getPathogenicitySources(), equalTo(pathogenicitySources));
        analysisSteps.add(new IntervalFilter(new GeneticInterval(10, 123256200, 123256300)));
        analysisSteps.add(new GeneSymbolFilter(new LinkedHashSet<>(Arrays.asList("FGFR1", "FGFR2"))));
        analysisSteps.add(new QualityFilter(50.0f));
        analysisSteps.add(new VariantEffectFilter(EnumSet.of(VariantEffect.SYNONYMOUS_VARIANT)));
        analysisSteps.add(new KnownVariantFilter());
        analysisSteps.add(new FrequencyFilter(1.0f));
        analysisSteps.add(new PathogenicityFilter(false));
        analysisSteps.add(new InheritanceFilter(modeOfInheritance));
        analysisSteps.add(priorityFactory.makeOmimPrioritiser());
        analysisSteps.add(priorityFactory.makeHiPhivePrioritiser(HiPhiveOptions.DEFAULT));
        analysisSteps.add(priorityFactory.makeHiPhivePrioritiser(HiPhiveOptions.builder()
                .diseaseId("OMIM:101600")
                .candidateGeneSymbol("FGFR2")
                .build()));
        analysisSteps.add(new PriorityScoreFilter(PriorityType.HIPHIVE_PRIORITY, 0.7f));
        assertThat(analysis.getAnalysisSteps(), equalTo(analysisSteps));
    }

    @Test(expected = AnalysisFileNotFoundException.class)
    public void testParseAnalysis_NonExistentFile() {
        instance.parseAnalysis(Paths.get("src/test/resources/wibble"));
    }

    @Test(expected = AnalysisFileNotFoundException.class)
    public void testParseOutputSettings_NonExistentFile() {
        instance.parseOutputSettings(Paths.get("src/test/resources/wibble"));
    }

    @Test(expected = AnalysisParserException.class)
    public void testParseOutputSettings_OutputPassVariantsOnlyThrowsExceptionWithNoValue() {
        instance.parseOutputSettings(
                "outputOptions:\n"
                + "    outputPassVariantsOnly: ");
    }

    @Test
    public void testParseOutputSettings_OutputPassVariantsOnly() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                + "    outputPassVariantsOnly: true\n"
                + "    numGenes: 1\n"
                + "    outputPrefix: results/Pfeiffer-hiphive\n"
                + "    outputFormats: [HTML, TSV-GENE, TSV-VARIANT, VCF]\n");
        assertThat(outputSettings.outputPassVariantsOnly(), is(true));
    }

    @Test
    public void testParseOutputSettings_NumGenesToShow() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                + "    outputPassVariantsOnly: true\n"
                + "    numGenes: 1\n"
                + "    outputPrefix: results/Pfeiffer-hiphive\n"
                + "    outputFormats: [HTML, TSV-GENE, TSV-VARIANT, VCF]\n");
        assertThat(outputSettings.getNumberOfGenesToShow(), equalTo(1));
    }

    @Test
    public void testParseOutputSettings_OutputPrefix() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                + "    outputPassVariantsOnly: true\n"
                + "    numGenes: 1\n"
                + "    outputPrefix: results/Pfeiffer-hiphive\n"
                + "    outputFormats: [HTML, TSV-GENE, TSV-VARIANT, VCF]\n");
        assertThat(outputSettings.getOutputPrefix(), equalTo("results/Pfeiffer-hiphive"));
    }

    @Test
    public void testParseOutputSettingsAllSupportedOutputFormats() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                + "    outputPassVariantsOnly: true\n"
                + "    numGenes: 1\n"
                + "    outputPrefix: results/Pfeiffer-hiphive\n"
                + "    outputFormats: [HTML, TSV-GENE, TSV-VARIANT, VCF]\n");
        Set<OutputFormat> outputFormats = EnumSet.of(OutputFormat.HTML, OutputFormat.TSV_GENE, OutputFormat.TSV_VARIANT, OutputFormat.VCF);
        assertThat(outputSettings.getOutputFormats(), equalTo((outputFormats)));
    }

    @Test
    public void testParseOutputSettingsNoOutputFormats() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                        + "    outputPassVariantsOnly: true\n"
                        + "    numGenes: 1\n"
                        + "    outputPrefix: results/Pfeiffer-hiphive\n"
                        + "    outputFormats:\n");
        Set<OutputFormat> outputFormats = EnumSet.noneOf(OutputFormat.class);
        assertThat(outputSettings.getOutputFormats(), equalTo((outputFormats)));
    }

    @Test
    public void testParseOutputSettingsUnsupportedOutputFormatDefaultsToHtml() {
        OutputSettings outputSettings = instance.parseOutputSettings(
                "outputOptions:\n"
                        + "    outputPassVariantsOnly: true\n"
                        + "    numGenes: 1\n"
                        + "    outputPrefix: results/Pfeiffer-hiphive\n"
                        + "    outputFormats: [WIBBLE!]\n");
        Set<OutputFormat> outputFormats = EnumSet.of(OutputFormat.HTML);
        assertThat(outputSettings.getOutputFormats(), equalTo((outputFormats)));
    }

    @Test
    public void testParseOutputSettings() {
        OutputSettings outputSettings = instance.parseOutputSettings(Paths.get("src/test/resources/analysisExample.yml"));
        OutputSettings expected = OutputSettings.builder()
                .outputPassVariantsOnly(false)
                .numberOfGenesToShow(0)
                .outputPrefix("results/Pfeiffer-hiphive")
                .outputFormats(EnumSet.of(OutputFormat.TSV_GENE, OutputFormat.TSV_VARIANT, OutputFormat.VCF, OutputFormat.HTML))
                .build();
        assertThat(outputSettings, equalTo(expected));
    }
}
