/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2017 Queen Mary University of London.
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

package org.monarchinitiative.exomiser.core.genome;

import org.monarchinitiative.exomiser.core.model.Gene;
import org.monarchinitiative.exomiser.core.model.GeneIdentifier;

import java.util.List;

/**
 * Static class for providing test GeneIdentifiers and Gene instances.
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class TestGeneFactory {

    public static final GeneIdentifier FGFR2_IDENTIFIER = GeneIdentifier.builder()
            .geneId("2263")
            .geneSymbol("FGFR2")
            .hgncId("HGNC:3689")
            .hgncSymbol("FGFR2")
            .entrezId("2263")
            .ensemblId("ENSG00000066468")
            .ucscId("uc057wle.1")
            .build();

    public static final GeneIdentifier SHH_IDENTIFIER = GeneIdentifier.builder()
            .geneId("6469")
            .geneSymbol("SHH")
            .hgncId("HGNC:10848")
            .hgncSymbol("SHH")
            .entrezId("6469")
            .ensemblId("ENSG00000164690")
            .ucscId("uc003wmk.2")
            .build();

    public static final GeneIdentifier GNRHR2_IDENTIFIER = GeneIdentifier.builder()
            .geneId("114814")
            .geneSymbol("GNRHR2")
            .hgncId("HGNC:16341")
            .hgncSymbol("GNRHR2")
            .entrezId("114814")
            .ensemblId("ENSG00000211451")
            .ucscId("")
            .build();

    public static final GeneIdentifier RBM8A_IDENTIFIER = GeneIdentifier.builder()
            .geneId("9939")
            .geneSymbol("RBM8A")
            .hgncId("HGNC:9905")
            .hgncSymbol("RBM8A")
            .entrezId("9939")
            .ensemblId("ENSG00000265241")
            .ucscId("uc031uto.3")
            .build();

    private static final List<GeneIdentifier> GENE_IDENTIFIERS = List.of(FGFR2_IDENTIFIER, SHH_IDENTIFIER, GNRHR2_IDENTIFIER, RBM8A_IDENTIFIER);

    private TestGeneFactory() {}

    /**
     * Creates a list of GeneIdentifiers for the following genes: FGFR2, SHH, RBM8A and GNRHR2.
     *
     * @return an immutable list of GeneIdentifiers.
     */
    static List<GeneIdentifier> buildGeneIdentifiers() {
        return GENE_IDENTIFIERS;
    }

    /**
     * Creates a list of the following Genes: FGFR2, SHH, RBM8A and GNRHR2.
     *
     * @return a mutable list of newly instantiated Gene instances build from fully formed GeneIdentifiers.
     */
    static List<Gene> buildGenes() {
        return List.of(new Gene(FGFR2_IDENTIFIER), new Gene(SHH_IDENTIFIER), new Gene(GNRHR2_IDENTIFIER), new Gene(RBM8A_IDENTIFIER));
    }
}
