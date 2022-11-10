/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2020 Queen Mary University of London.
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
package org.monarchinitiative.exomiser.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk7.Jdk7Module;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import de.charite.compbio.jannovar.annotation.VariantEffect;
import de.charite.compbio.jannovar.mendel.ModeOfInheritance;
import org.monarchinitiative.exomiser.core.Exomiser;
import org.monarchinitiative.exomiser.core.analysis.Analysis;
import org.monarchinitiative.exomiser.core.analysis.AnalysisBuilder;
import org.monarchinitiative.exomiser.core.analysis.AnalysisMode;
import org.monarchinitiative.exomiser.core.analysis.AnalysisResults;
import org.monarchinitiative.exomiser.core.analysis.sample.Sample;
import org.monarchinitiative.exomiser.core.analysis.util.InheritanceModeOptions;
import org.monarchinitiative.exomiser.core.analysis.util.PedFiles;
import org.monarchinitiative.exomiser.core.filters.FilterReport;
import org.monarchinitiative.exomiser.core.genome.GenomeAssembly;
import org.monarchinitiative.exomiser.core.genome.VcfFiles;
import org.monarchinitiative.exomiser.core.model.*;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.monarchinitiative.exomiser.core.prioritisers.PriorityType;
import org.monarchinitiative.exomiser.core.prioritisers.service.PriorityService;
import org.monarchinitiative.exomiser.core.writers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;

import static org.monarchinitiative.exomiser.core.prioritisers.PriorityType.*;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
@Controller
public class SubmitJobController {

    private static final Logger logger = LoggerFactory.getLogger(SubmitJobController.class);

    private static final String SUBMIT_PAGE = "submit";
    private static final Set<VariantEffect> NON_EXONIC_VARIANT_EFFECTS = Sets.immutableEnumSet(
            VariantEffect.UPSTREAM_GENE_VARIANT,
            VariantEffect.INTERGENIC_VARIANT,
            VariantEffect.DOWNSTREAM_GENE_VARIANT,
            VariantEffect.CODING_TRANSCRIPT_INTRON_VARIANT,
            VariantEffect.NON_CODING_TRANSCRIPT_INTRON_VARIANT,
            VariantEffect.SYNONYMOUS_VARIANT,
            VariantEffect.SPLICE_REGION_VARIANT,
            VariantEffect.REGULATORY_REGION_VARIANT
    );

    @Autowired
    private Integer maxVariants;
    @Autowired
    private Integer maxGenes;
    @Autowired
    private boolean clinicalInstance;

    @Autowired
    private Exomiser exomiser;
    @Autowired
    private PriorityService priorityService;

    @GetMapping(value = SUBMIT_PAGE)
    public String submit() {
        return SUBMIT_PAGE;
    }

    @PostMapping(value = SUBMIT_PAGE)
    public String submit(
        //input-data
        @RequestParam(value = "vcf") MultipartFile vcfFile,
        @RequestParam(value = "ped", required = false) MultipartFile pedFile,
        @RequestParam(value = "proband", required = false) String proband,
        //Sample Phenotypes
        @RequestParam(value = "disease", required = false) String diseaseId,
        @RequestParam(value = "phenotypes", required = false) List<String> phenotypes,
        //Quality Filtering Parameters
        @RequestParam(value = "min-depth", required = false, defaultValue = "0") Float minimumDepth,
        @RequestParam(value = "min-call-quality", required = false, defaultValue = "0") Float minimumQuality,
        //Data Filtering Parameters
        @RequestParam("keep-non-pathogenic") Boolean keepNonPathogenic,
        @RequestParam("keep-off-target") Boolean keepOffTarget,
        @RequestParam("remove-dbsnp") Boolean removeDbSnp,
        @RequestParam(value = "genetic-interval", required = false, defaultValue = "") String geneticInterval,
        @RequestParam(value = "genes-to-keep", required = false) List<String> genesToFilter,
        //Inheritance Filtering Parameters
        @RequestParam("inheritance") String modeOfInheritance,
        @RequestParam("frequency") String frequency,
        //prioritiser
        @RequestParam("prioritiser") String prioritiser,
        HttpSession session,
        Model model) {

        UUID analysisId = UUID.randomUUID();
        logger.info("Analysis id: {}", analysisId);
        Path vcfPath = createVcfPathFromMultipartFile(vcfFile);
        Path pedPath = createPedPathFromMultipartFile(pedFile);
        //require a mimimum input of a VCF file and a set of HPO terms - these can come from the diseaseId
        if (vcfPath == null) {
            logger.info("User did not submit a VCF - returning to submission page");
            return SUBMIT_PAGE;
        }

        if (phenotypes == null && diseaseId == null) {
            logger.info("User did not provide a disease or phenotype set - returning to submission page");
            return SUBMIT_PAGE;
        }

        if(phenotypes == null) {
            logger.info("No phenotypes provided - trying to use disease phenotypes");
            phenotypes = getDiseasePhenotypes(diseaseId);
        }

        logger.info("Using disease: {}", diseaseId);
        logger.info("Using phenotypes: {}", phenotypes);

        long numVariantsInSample = VcfFiles.readVariantContexts(vcfPath).count();
        if (numVariantsInSample > maxVariants) {
            logger.info("{} contains {} variants - this is more than the allowed maximum of {}."
                    + "Returning user to submit page", vcfPath, numVariantsInSample, maxVariants);
            cleanUpSampleFiles(vcfPath, pedPath);
            model.addAttribute("numVariants", numVariantsInSample);
            return "resubmitWithFewerVariants";
        } else {
            logger.info("{} contains {} variants - within set limit of {}", vcfPath, numVariantsInSample, maxVariants);
        }


        Sample sample = buildSample(vcfPath, pedPath, proband, phenotypes);
        Analysis analysis = buildAnalysis(geneticInterval, minimumQuality, minimumDepth, removeDbSnp, keepOffTarget, keepNonPathogenic, modeOfInheritance, frequency, makeGenesToKeep(genesToFilter), prioritiser);
        AnalysisResults analysisResults = exomiser.run(sample, analysis);
        //2018-06-19: Write results to temporary directory for subsequent download
        writeResultsToFile(analysisId, analysis, analysisResults);


        buildResultsModel(model, analysis, analysisResults);
        logger.info("Returning {} results to user", vcfPath.getFileName());
        cleanUpSampleFiles(vcfPath, pedPath);
        return "results";
    }

    private Sample buildSample(Path vcfPath, Path pedPath, String proband, List<String> phenotypes) {
        return Sample.builder()
                .genomeAssembly(GenomeAssembly.HG19)
                .vcfPath(vcfPath)
                .pedigree((pedPath == null) ? Pedigree.empty() : PedFiles.readPedigree(pedPath))
                .probandSampleName(proband)
                .hpoIds(phenotypes)
                .build();
    }

    private List<String> getDiseasePhenotypes(String diseaseId) {
        if (diseaseId == null || diseaseId.isEmpty()) {
            return Collections.emptyList();
        }
        return priorityService.getHpoIdsForDiseaseId(diseaseId);
    }

    private Analysis buildAnalysis(Path vcfPath, Path pedPath, String proband, List<String> phenotypes, String geneticInterval, Float minimumQuality, Float minimumDepth, Boolean removeDbSnp, Boolean keepOffTarget, Boolean keepNonPathogenic, String modeOfInheritance, String frequency, Set<String> genesToKeep, String prioritiser) {

        //helmuth 2022-11-10: Use local frequency db and external sources
        //AnalysisBuilder analysisBuilder = exomiser.getAnalysisBuilder()
        //        .analysisMode(AnalysisMode.PASS_ONLY)
        //        .inheritanceModes((modeOfInheritance.equalsIgnoreCase("ANY")) ? InheritanceModeOptions.defaults() : InheritanceModeOptions
        //                .defaultForModes(ModeOfInheritance.valueOf(modeOfInheritance)))
        //        .frequencySources(FrequencySource.ALL_EXTERNAL_FREQ_SOURCES)
        //        .pathogenicitySources(EnumSet.of(PathogenicitySource.MUTATION_TASTER, PathogenicitySource.SIFT, PathogenicitySource.POLYPHEN));
        AnalysisBuilder analysisBuilder = exomiser.getAnalysisBuilder()
                .analysisMode(AnalysisMode.FULL)
                .inheritanceModes((modeOfInheritance.equalsIgnoreCase("ANY"))? InheritanceModeOptions.defaults() : InheritanceModeOptions.defaultForModes(ModeOfInheritance.valueOf(modeOfInheritance)))
                .frequencySources(FrequencySource.ALL_LOCAL_AND_EXTERNAL_FREQ_SOURCES)
                .pathogenicitySources(EnumSet.of(PathogenicitySource.MUTATION_TASTER, PathogenicitySource.CADD, PathogenicitySource.SIFT, PathogenicitySource.POLYPHEN));

        //helmuth 2022-11-10: Also capture splicing variants
        analysisBuilder.addVariantEffectFilter(Sets.immutableEnumSet(VariantEffect.SPLICE_REGION_VARIANT));

        addFilters(analysisBuilder, minimumQuality, removeDbSnp, keepOffTarget, keepNonPathogenic, frequency, genesToKeep, geneticInterval);
        //soon these will run by default
        analysisBuilder.addInheritanceFilter();
        analysisBuilder.addOmimPrioritiser();
        //add the users choice of prioritiser
        addPrioritiser(analysisBuilder, prioritiser);

        //log of webform setting
        logger.info("User-specified Quality Filtering Parameters:");
        logger.info("  minimumDepth: {}", minimumDepth);
        logger.info("  minimumQuality: {}", minimumQuality);
        logger.info("User-specified Data Filtering Parameters:");
        logger.info("  modeOfInheritance: {}", modeOfInheritance);
        logger.info("  frequency: {}", frequency);
        logger.info("  keepNonPathogenic: {}", keepNonPathogenic);
        logger.info("  keepOffTarget: {}", keepOffTarget);
        logger.info("  removeDbSnp: {}", removeDbSnp);
        logger.info("  geneticInterval: {}", geneticInterval);
        logger.info("  genesToKeep: {}", genesToKeep);
        logger.info("User-specified Prioritiser:");
        logger.info("  prioritiser: {}", prioritiser);

        return analysisBuilder.build();
    }

    private void addFilters(AnalysisBuilder analysisBuilder, Float minimumQuality, boolean removeDbSnp, boolean keepOffTarget, boolean keepNonPathogenic, String frequency, Set<String> genesToKeep, String geneticInterval) {
        //This is the original Exomiser analysis step order, as found in the SettingsParser
        //Filter for genes:
        if (!genesToKeep.isEmpty()) {
            analysisBuilder.addGeneIdFilter(genesToKeep);
        }
        //Genetic interval:
        if (!geneticInterval.isEmpty()) {
            analysisBuilder.addIntervalFilter(GeneticInterval.parseString(geneticInterval));
        }
        //Keep off-target variants:
        if (!keepOffTarget) {
            analysisBuilder.addVariantEffectFilter(NON_EXONIC_VARIANT_EFFECTS);
        }
        //Minimum variant call quality:
        if (minimumQuality != null && minimumQuality != 0) {
            analysisBuilder.addQualityFilter(minimumQuality);
        }
        //Remove all dbSNP variants:
        if (removeDbSnp) {
            analysisBuilder.addKnownVariantFilter();
        }
        //Maximum minor allele frequency:
        analysisBuilder.addFrequencyFilter(Float.parseFloat(frequency));
        //Keep non-pathogenic variants:
        analysisBuilder.addPathogenicityFilter(keepNonPathogenic);
    }

    private void addPrioritiser(AnalysisBuilder analysisBuilder, String prioritiser) {
        PriorityType priorityType = PriorityType.valueOf(prioritiser);

        if (priorityType == PHENIX_PRIORITY) {
            analysisBuilder.addPhenixPrioritiser();
        }
        else if (priorityType == HIPHIVE_PRIORITY) {
            analysisBuilder.addHiPhivePrioritiser();
        }
        else if (priorityType == PHIVE_PRIORITY) {
            analysisBuilder.addPhivePrioritiser();
        }
    }

    private void writeResultsToFile(UUID analysisId, AnalysisResults analysisResults) {
        Path outputDir = Paths.get(System.getProperty("java.io.tmpdir"), analysisId.toString());
        try {
            Files.createDirectory(outputDir);
        } catch (IOException e) {
            logger.error("Unable to create directory {}", outputDir, e);
        }
        logger.info("Output dir: {}", outputDir);
        String outFileName = outputDir.toString() + "/results";
        OutputSettings outputSettings = OutputSettings.builder()
                .outputContributingVariantsOnly(false)
                .numberOfGenesToShow(50)
                .outputPrefix(outFileName)
                //OutputFormat.HTML causes issues due to thymeleaf templating - don't use!
                .outputFormats(EnumSet.of(OutputFormat.TSV_GENE, OutputFormat.TSV_VARIANT, OutputFormat.VCF, OutputFormat.JSON))
                .build();

        AnalysisResultsWriter.writeToFile(analysisResults, outputSettings);
    }

    private void buildResultsModel(Model model, Analysis analysis, AnalysisResults analysisResults) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        //required for correct output of Path types
        mapper.registerModule(new Jdk7Module());
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true);
        StringWriter jsonSettings = new StringWriter();
        try {
            mapper.writeValue(jsonSettings, analysisResults.getSample());
            mapper.writeValue(jsonSettings, analysisResults.getAnalysis());
        } catch (Exception ex) {
            logger.error("Unable to process JSON settings", ex);
        }
        model.addAttribute("settings", jsonSettings.toString());

        //make the user aware of any unanalysed variants
        List<VariantEvaluation> unAnalysedVarEvals = analysisResults.getUnAnnotatedVariantEvaluations();
        model.addAttribute("unAnalysedVarEvals", unAnalysedVarEvals);

        //write out the filter reports section
        List<FilterReport> filterReports = ResultsWriterUtils.makeFilterReports(analysis, analysisResults);
        model.addAttribute("filterReports", filterReports);

        List<String> sampleNames = analysisResults.getSampleNames();
        String sampleName = "Anonymous";
        if (!sampleNames.isEmpty()) {
            sampleName = sampleNames.get(0);
        }
        model.addAttribute("sampleName", sampleName);
        model.addAttribute("sampleNames", sampleNames);

        //write out the variant type counters
        List<VariantEvaluation> variantEvaluations = analysisResults.getVariantEvaluations();
        List<VariantEffectCount> variantEffectCounters = ResultsWriterUtils.makeVariantEffectCounters(sampleNames, variantEvaluations);
        model.addAttribute("variantTypeCounters", variantEffectCounters);

        List<Gene> sampleGenes = analysisResults.getGenes();
        model.addAttribute("geneResultsTruncated", false);
        int numCandidateGenes = numGenesPassedFilters(sampleGenes);
        if (numCandidateGenes > maxGenes) {
            logger.info("Truncating number of genes returned to {} ", maxGenes);
            model.addAttribute("geneResultsTruncated", true);
            model.addAttribute("numCandidateGenes", numCandidateGenes);
            model.addAttribute("totalGenes", sampleGenes.size());
        }

        List<Gene> passedGenes = ResultsWriterUtils.getMaxPassedGenes(sampleGenes, maxGenes);
        model.addAttribute("genes", passedGenes);
//this will change the links to the relevant resource.
        // For the time being we're going to maintain the original behaviour (UCSC)
        // Need to wire it up through the system or it might be easiest to autodetect this from the transcripts of passed variants.
        // One of UCSC, ENSEMBL or REFSEQ
        var transcriptDb = analysisResults.getContributingVariants().stream()
                .flatMap(variantEvaluation -> variantEvaluation.getTranscriptAnnotations().stream())
                .findFirst()
                .map(TranscriptAnnotation::getAccession)
                .map(value -> {
                    if (value.startsWith("ENST")) {
                        return "ENSEMBL";
                    } else if (value.startsWith("uc")) {
                        return "UCSC";
                    } else if (value.startsWith("NM") || value.startsWith("NR") || value.startsWith("XM") || value.startsWith("XR")) {
                        return "REFSEQ";
                    }
                    return "";
                })
                .orElse("ENSEMBL");
        model.addAttribute("transcriptDb", transcriptDb);
        model.addAttribute("variantRankComparator", new VariantEvaluation.RankBasedComparator());
        model.addAttribute("pValueFormatter", new DecimalFormat("0.0E0"));
    }

    private int numGenesPassedFilters(List<Gene> genes) {
        int numCandidateGenes = 0;
        for (Gene gene : genes) {
            if (gene.passedFilters()) {
                numCandidateGenes++;
            }
        }
        return numCandidateGenes;
    }

    //This throws 'java.nio.file.FileSystemException: The process cannot access the file because it is being used by another process.'
    // when on Windows as it seems tha Tomcat is locking the files/not setting the correct owner permissions.
    private void cleanUpSampleFiles(Path vcfPath, Path pedPath) {
        deleteSampleFile(vcfPath);
        deleteSampleFile(pedPath);
    }

    private void deleteSampleFile(Path sampleFile) {
        try {
            if (sampleFile != null) {
                logger.info("Deleting sample input file {}", sampleFile);
                Files.deleteIfExists(sampleFile);
            }
        } catch (IOException ex) {
            logger.error("Unable to delete sample file {}", sampleFile, ex);
        }
    }

    private Set<String> makeGenesToKeep(List<String> genesToFilter) {
        logger.info("Genes to filter: {}", genesToFilter);
        if (genesToFilter == null) {
            return Collections.emptySet();
        }
        return ImmutableSortedSet.copyOf(genesToFilter);
    }

    @Nullable
    private Path createVcfPathFromMultipartFile(MultipartFile multipartVcfFile) {
        if (multipartVcfFile == null || multipartVcfFile.getOriginalFilename() == null) {
            return null;
        }
        if (multipartVcfFile.getOriginalFilename().endsWith(".vcf.gz")) {
            return createPathFromMultipartFile(multipartVcfFile, ".vcf.gz");
        }
        return createPathFromMultipartFile(multipartVcfFile, ".vcf");
    }

    private Path createPedPathFromMultipartFile(MultipartFile multipartPedFile) {
        return createPathFromMultipartFile(multipartPedFile, ".ped");
    }

    private Path createPathFromMultipartFile(MultipartFile multipartFile, String suffix) {
        Path tempDirPath = Paths.get(System.getProperty("java.io.tmpdir"));
        if (!multipartFile.isEmpty()) {
            logger.info("Uploading multipart file: {}", multipartFile.getOriginalFilename());
            try {
                Path path = Files.createTempFile(tempDirPath, "exomiser-", suffix);
                multipartFile.transferTo(path.toFile());
                return path;
            } catch (IOException e) {
                logger.error("Failed to upload file {}", multipartFile.getOriginalFilename(), e);
            }
        }
        //PED files are permitted to be null so this is OK really.
        return null;
    }
}
