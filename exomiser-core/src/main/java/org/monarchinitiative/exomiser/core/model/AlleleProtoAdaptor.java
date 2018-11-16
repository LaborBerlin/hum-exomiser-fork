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

package org.monarchinitiative.exomiser.core.model;

import com.google.common.collect.ImmutableMap;
import org.monarchinitiative.exomiser.core.model.frequency.Frequency;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencyData;
import org.monarchinitiative.exomiser.core.model.frequency.FrequencySource;
import org.monarchinitiative.exomiser.core.model.frequency.RsId;
import org.monarchinitiative.exomiser.core.model.pathogenicity.ClinVarData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityData;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicityScore;
import org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource;
import org.monarchinitiative.exomiser.core.proto.AlleleProto.AlleleKey;
import org.monarchinitiative.exomiser.core.proto.AlleleProto.AlleleProperties;
import org.monarchinitiative.exomiser.core.proto.AlleleProto.ClinVar;

import java.util.*;

import static org.monarchinitiative.exomiser.core.model.frequency.FrequencySource.*;
import static org.monarchinitiative.exomiser.core.model.pathogenicity.PathogenicitySource.*;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 * @since 10.1.0
 */
public class AlleleProtoAdaptor {

    // These maps are constant look-ups for keys in the AlleleProto.AlleleProperties propertiesMap which was generated by the
    // genome-data module. The keys are AlleleProperty string values.
    private static final Map<String, FrequencySource> FREQUENCY_SOURCE_MAP = new ImmutableMap.Builder<String, FrequencySource>()
            .put("KG", THOUSAND_GENOMES)
            .put("TOPMED", TOPMED)
            .put("UK10K", UK10K)

            .put("ESP_AA", ESP_AFRICAN_AMERICAN)
            .put("ESP_EA", ESP_EUROPEAN_AMERICAN)
            .put("ESP_ALL", ESP_ALL)

            .put("EXAC_AFR", EXAC_AFRICAN_INC_AFRICAN_AMERICAN)
            .put("EXAC_AMR", EXAC_AMERICAN)
            .put("EXAC_EAS", EXAC_EAST_ASIAN)
            .put("EXAC_FIN", EXAC_FINNISH)
            .put("EXAC_NFE", EXAC_NON_FINNISH_EUROPEAN)
            .put("EXAC_OTH", EXAC_OTHER)
            .put("EXAC_SAS", EXAC_SOUTH_ASIAN)

            .put("GNOMAD_E_AFR", GNOMAD_E_AFR)
            .put("GNOMAD_E_AMR", GNOMAD_E_AMR)
            .put("GNOMAD_E_ASJ", GNOMAD_E_ASJ)
            .put("GNOMAD_E_EAS", GNOMAD_E_EAS)
            .put("GNOMAD_E_FIN", GNOMAD_E_FIN)
            .put("GNOMAD_E_NFE", GNOMAD_E_NFE)
            .put("GNOMAD_E_OTH", GNOMAD_E_OTH)
            .put("GNOMAD_E_SAS", GNOMAD_E_SAS)

            .put("GNOMAD_G_AFR", GNOMAD_G_AFR)
            .put("GNOMAD_G_AMR", GNOMAD_G_AMR)
            .put("GNOMAD_G_ASJ", GNOMAD_G_ASJ)
            .put("GNOMAD_G_EAS", GNOMAD_G_EAS)
            .put("GNOMAD_G_FIN", GNOMAD_G_FIN)
            .put("GNOMAD_G_NFE", GNOMAD_G_NFE)
            .put("GNOMAD_G_OTH", GNOMAD_G_OTH)
            .put("GNOMAD_G_SAS", GNOMAD_G_SAS)
            .build();

    private static final Map<String, PathogenicitySource> PATHOGENICITY_SOURCE_MAP = new ImmutableMap.Builder<String, PathogenicitySource>()
            .put("POLYPHEN", POLYPHEN)
            .put("MUT_TASTER", MUTATION_TASTER)
            .put("SIFT", SIFT)
            .put("CADD", CADD)
            .put("REMM", REMM)
            .put("REVEL", REVEL)
            .build();

    private AlleleProtoAdaptor() {
        //un-instantiable utility class
    }

    // This would make sense to have this here rather than having similar functionality in the MvStoreUtil
    // and the VariantKeyGenerator
    public static AlleleKey toAlleleKey(Variant variant) {
        // ARGH! I didn't put the frikking genome assembly in the alleleKey!
        // adding it will probably make the data backwards-incompatible as the MVStore is essentially a TreeMap
        return AlleleKey.newBuilder()
                .setChr(variant.getChromosome())
                .setPosition(variant.getPosition())
                .setRef(variant.getRef())
                .setAlt(variant.getAlt())
                .build();
    }

    public static FrequencyData toFrequencyData(AlleleProperties alleleProperties) {
        if (alleleProperties.equals(AlleleProperties.getDefaultInstance())) {
            return FrequencyData.empty();
        }
        RsId rsId = RsId.of(alleleProperties.getRsId());
        List<Frequency> frequencies = parseFrequencyData(alleleProperties.getPropertiesMap());
        return FrequencyData.of(rsId, frequencies);
    }

    private static List<Frequency> parseFrequencyData(Map<String, Float> values) {
        List<Frequency> frequencies = new ArrayList<>(values.size());
        for (Map.Entry<String, Float> field : values.entrySet()) {
            String key = field.getKey();
            if (FREQUENCY_SOURCE_MAP.containsKey(key)) {
                FrequencySource source = FREQUENCY_SOURCE_MAP.get(key);
                float value = field.getValue();
                frequencies.add(Frequency.of(source, value));
            }
        }
        return frequencies;
    }

    public static PathogenicityData toPathogenicityData(AlleleProperties alleleProperties) {
        if (alleleProperties.equals(AlleleProperties.getDefaultInstance())) {
            return PathogenicityData.empty();
        }
        List<PathogenicityScore> pathogenicityScores = parsePathogenicityData(alleleProperties.getPropertiesMap());
        ClinVarData clinVarData = parseClinVarData(alleleProperties.getClinVar());
        return PathogenicityData.of(clinVarData, pathogenicityScores);
    }

    private static List<PathogenicityScore> parsePathogenicityData(Map<String, Float> values) {
        List<PathogenicityScore> pathogenicityScores = new ArrayList<>();
        for (Map.Entry<String, Float> field : values.entrySet()) {
            String key = field.getKey();
            if(PATHOGENICITY_SOURCE_MAP.containsKey(key)) {
                PathogenicitySource source = PATHOGENICITY_SOURCE_MAP.get(key);
                float score = field.getValue();
                pathogenicityScores.add(PathogenicityScore.of(source, score));
            }
        }
        return pathogenicityScores;
    }

    private static ClinVarData parseClinVarData(ClinVar clinVar) {
        if (clinVar.equals(clinVar.getDefaultInstanceForType())) {
            return ClinVarData.empty();
        }
        ClinVarData.Builder builder = ClinVarData.builder();
        builder.alleleId(clinVar.getAlleleId());
        builder.primaryInterpretation(toClinSig(clinVar.getPrimaryInterpretation()));
        builder.secondaryInterpretations(toClinSigSet(clinVar.getSecondaryInterpretationsList()));
        builder.includedAlleles(getToIncludedAlleles(clinVar.getIncludedAllelesMap()));
        builder.reviewStatus(clinVar.getReviewStatus());
        return builder.build();
    }

    private static Map<String, ClinVarData.ClinSig> getToIncludedAlleles(Map<String, ClinVar.ClinSig> includedAllelesMap) {
        if (includedAllelesMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, ClinVarData.ClinSig> converted = new HashMap<>(includedAllelesMap.size());

        for (Map.Entry<String, ClinVar.ClinSig> included : includedAllelesMap.entrySet()) {
            converted.put(included.getKey(), toClinSig(included.getValue()));
        }

        return converted;
    }

    private static Set<ClinVarData.ClinSig> toClinSigSet(List<ClinVar.ClinSig> protoClinSigs) {
        if (protoClinSigs.isEmpty()) {
            return Collections.emptySet();
        }
        Set<ClinVarData.ClinSig> converted = new HashSet<>(protoClinSigs.size());
        for (ClinVar.ClinSig protoClinSig : protoClinSigs) {
            converted.add(toClinSig(protoClinSig));
        }
        return converted;
    }

    private static ClinVarData.ClinSig toClinSig(ClinVar.ClinSig protoClinSig) {
        switch (protoClinSig) {
            case BENIGN:
                return ClinVarData.ClinSig.BENIGN;
            case BENIGN_OR_LIKELY_BENIGN:
                return ClinVarData.ClinSig.BENIGN_OR_LIKELY_BENIGN;
            case LIKELY_BENIGN:
                return ClinVarData.ClinSig.LIKELY_BENIGN;
            case UNCERTAIN_SIGNIFICANCE:
                return ClinVarData.ClinSig.UNCERTAIN_SIGNIFICANCE;
            case LIKELY_PATHOGENIC:
                return ClinVarData.ClinSig.LIKELY_PATHOGENIC;
            case PATHOGENIC_OR_LIKELY_PATHOGENIC:
                return ClinVarData.ClinSig.PATHOGENIC_OR_LIKELY_PATHOGENIC;
            case PATHOGENIC:
                return ClinVarData.ClinSig.PATHOGENIC;
            case CONFLICTING_PATHOGENICITY_INTERPRETATIONS:
                return ClinVarData.ClinSig.CONFLICTING_PATHOGENICITY_INTERPRETATIONS;
            case AFFECTS:
                return ClinVarData.ClinSig.AFFECTS;
            case ASSOCIATION:
                return ClinVarData.ClinSig.ASSOCIATION;
            case DRUG_RESPONSE:
                return ClinVarData.ClinSig.DRUG_RESPONSE;
            case OTHER:
                return ClinVarData.ClinSig.OTHER;
            case PROTECTIVE:
                return ClinVarData.ClinSig.PROTECTIVE;
            case RISK_FACTOR:
                return ClinVarData.ClinSig.RISK_FACTOR;
            case NOT_PROVIDED:
            case UNRECOGNIZED:
            default:
                return ClinVarData.ClinSig.NOT_PROVIDED;
        }
    }
}
