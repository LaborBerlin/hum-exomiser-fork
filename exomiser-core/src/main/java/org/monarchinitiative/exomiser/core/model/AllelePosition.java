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

package org.monarchinitiative.exomiser.core.model;

import java.util.Objects;

/**
 * Class for computing and holding minimised single allele variant coordinates. Normalisation is not complete in that
 * it will not left align the variants, but it will right then left trim and adjust the position accordingly. The coordinates
 * are expected to follow the VCF spec i.e. use 1-based, inclusive positions.
 * <p>
 * It will not accept multiple allele VCF strings and it will not split MNV into SNP.
 * <p>
 * Minimisation follows the specification of Tan et al. 2015 https://dx.doi.org/10.1093/bioinformatics/btv112
 * Further details here: http://genome.sph.umich.edu/wiki/Variant_Normalization
 * and as discussed here: https://macarthurlab.org/2014/04/28/converting-genetic-variants-to-their-minimal-representation
 * <p>
 * A variant is considered minimised if:
 * 1. it has no common nucleotides on the left or right side
 * 2. each allele does not end with the same type of nucleotide, or the shortest allele has length 1
 *
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
public class AllelePosition {

    private final int start;
    private final String ref;
    private final String alt;

    private AllelePosition(int start, String ref, String alt) {
        this.start = start;
        this.ref = ref;
        this.alt = alt;
    }

    /**
     * @param start
     * @param ref
     * @param alt
     * @return an exact representation of the input coordinates.
     */
    public static AllelePosition of(int start, String ref, String alt) {
        Objects.requireNonNull(ref, "REF string cannot be null");
        Objects.requireNonNull(alt, "ALT string cannot be null");
        return new AllelePosition(start, ref, alt);
    }

    /**
     * Given a single allele from a multi-positional site, incoming variants might not be fully trimmed.
     * In cases where there is repetition, depending on the program used, the final variant allele will be different.
     * VCF:      X-118887583-TCAAAA-TCAAAACAAAA
     * Exomiser: X-118887583-T     -TCAAAA
     * CellBase: X-118887584--     - CAAAA
     * Jannovar: X-118887588-      -      CAAAA
     * Nirvana:  X-118887589-      -      CAAAA
     * <p>
     * Trimming first with Exomiser, then annotating with Jannovar, constrains the Jannovar annotation to the same
     * position as Exomiser.
     * VCF:      X-118887583-TCAAAA-TCAAAACAAAA
     * Exomiser: X-118887583-T     -TCAAAA
     * CellBase: X-118887584--     - CAAAA
     * Jannovar: X-118887583-      - CAAAA      (Jannovar is zero-based)
     * Nirvana:  X-118887584-      - CAAAA
     * <p>
     * Cellbase:
     * https://github.com/opencb/biodata/blob/develop/biodata-tools/src/main/java/org/opencb/biodata/tools/variant/VariantNormalizer.java
     * http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest/v4/hsapiens/genomic/variant/X:118887583:TCAAAA:TCAAAACAAAA/annotation?assembly=grch37&limit=-1&skip=-1&count=false&Output format=json&normalize=true
     * <p>
     * Nirvana style trimming:
     * https://github.com/Illumina/Nirvana/blob/master/VariantAnnotation/Algorithms/BiDirectionalTrimmer.cs
     * <p>
     * Jannovar:
     * https://github.com/charite/jannovar/blob/master/jannovar-core/src/main/java/de/charite/compbio/jannovar/reference/VariantDataCorrector.java
     *
     * @param start 1-based start position of the first base of the ref string
     * @param ref   reference base(s)
     * @param alt   alternate bases
     * @return a minimised representation of the input coordinates.
     */
    public static AllelePosition trim(int start, String ref, String alt) {
        Objects.requireNonNull(ref, "REF string cannot be null");
        Objects.requireNonNull(alt, "ALT string cannot be null");

//        if (cantTrim(ref, alt) || isSymbolic(ref, alt)) {
        if (cantTrim(ref, alt)) {
            return new AllelePosition(start, ref, alt);
        }

        // copy these here in order not to change input params
        int trimStart = start;
        String trimRef = ref;
        String trimAlt = alt;

        // Can't do left alignment as have no reference seq and are assuming this has happened already.
        // Therefore check the sequence is first right trimmed, then left trimmed as per the wiki link above.
        if (canRightTrim(trimRef, trimAlt)) {
            int rightIdx = trimRef.length();
            int diff = trimRef.length() - trimAlt.length();
            // scan from right to left, ensure right index > 1 so as not to fall off the left end
            while (rightIdx > 1 && rightIdx - diff > 0 && trimRef.charAt(rightIdx - 1) == trimAlt.charAt(rightIdx - 1 - diff)) {
                rightIdx--;
            }

            trimRef = trimRef.substring(0, rightIdx);
            trimAlt = trimAlt.substring(0, rightIdx - diff);
        }

        if (canLeftTrim(trimRef, trimAlt)) {
            int leftIdx = 0;
            // scan from left to right
            while (leftIdx < trimRef.length() && leftIdx < trimAlt.length() && trimRef.charAt(leftIdx) == trimAlt.charAt(leftIdx)) {
                leftIdx++;
            }
            // correct index so as not to fall off the right end
            if (leftIdx > 0 && leftIdx == trimRef.length() || leftIdx == trimAlt.length()) {
                leftIdx -= 1;
            }
            trimStart += leftIdx;
            trimRef = trimRef.substring(leftIdx);
            trimAlt = trimAlt.substring(leftIdx);
        }

        return new AllelePosition(trimStart, trimRef, trimAlt);
    }

    private static boolean cantTrim(String ref, String alt) {
        return ref.length() == 1 || alt.length() == 1;
    }

    private static boolean canRightTrim(String ref, String alt) {
        int refLength = ref.length();
        int altLength = alt.length();
        return refLength > 1 && altLength > 1 && ref.charAt(refLength - 1) == alt.charAt(altLength - 1);
    }

    private static boolean canLeftTrim(String ref, String alt) {
        return ref.length() > 1 && alt.length() > 1 && ref.charAt(0) == alt.charAt(0);
    }

    public static boolean isSnv(String ref, String alt) {
        return ref.length() == 1 && alt.length() == 1;
    }

    public static boolean isDeletion(String ref, String alt) {
        return ref.length() > alt.length();
    }

    public static boolean isInsertion(String ref, String alt) {
        return ref.length() < alt.length();
    }

    /**
     *
     * @since 12.0.0
     * @param ref the reference allele
     * @param alt the alternate allele
     * @return true if the ref or alt allele is considered symbolic
     */
    public static boolean isSymbolic(String ref, String alt) {
        // The VCF spec only mentions alt alleles as having symbolic characters, so check these first then check the ref
        // just in case.
        return isSymbolic(alt) || isSymbolic(ref);
    }

    public static boolean isSymbolic(String allele) {
        if (allele.isEmpty()) {
            return false;
        }
        return isLargeSymbolic(allele) || isSingleBreakend(allele) || isMatedBreakend(allele);
    }

    public static boolean isBreakend(String allele) {
        return isSingleBreakend(allele) || isMatedBreakend(allele);
    }

    private static boolean isLargeSymbolic(String allele) {
        return allele.length() > 1 && allele.charAt(0) == '<' || allele.charAt(allele.length() - 1) == '>';
    }

    public static boolean isSingleBreakend(String allele) {
        return allele.length() > 1 && allele.charAt(0) == '.' || allele.charAt(allele.length() - 1) == '.';
    }

    public static boolean isMatedBreakend(String allele) {
        return allele.length() > 1 && (allele.contains("[") || allele.contains("]"));
    }

    public static int length(String ref, String alt) {
        // Quote VCF 4.3 SV info
        // "LEN - For precise variants, LEN is length of REF allele, and the for imprecise variants the corresponding best estimate."
        // "SVLEN - Difference in length between REF and ALT alleles. Longer ALT alleles (e.g. insertions) have positive values,
        // shorter ALT alleles (e.g. deletions) have negative values."
        // ##INFO=<ID=LEN,Number=1,Type=Integer,Description="Length of the variant described in this record">
        // ##INFO=<ID=SVLEN,Number=.,Type=Integer,Description="Difference in length between REF and ALT alleles">
        // #CHROM POS     ID        REF              ALT          QUAL FILTER INFO                                                               FORMAT       NA00001
        // 1      2827694 rs2376870 CGTGGATGCGGGGAC  C            .    PASS   SVTYPE=DEL;LEN=15;HOMLEN=1;HOMSEQ=G;SVLEN=-14                 GT:GQ        1/1:14
        // 2       321682 .         T                <DEL>        6    PASS   SVTYPE=DEL;LEN=206;SVLEN=-205;CIPOS=-56,20;CIEND=-10,62         GT:GQ        0/1:12
        // 2     14477084 .         C                <DEL:ME:ALU> 12   PASS   SVTYPE=DEL;LEN=298;SVLEN=-297;CIPOS=-22,18;CIEND=-12,32       GT:GQ        0/1:12
        // 3      9425916 .         C                <INS:ME:L1>  23   PASS   SVTYPE=INS;LEN=1;SVLEN=6027;CIPOS=-16,22                     GT:GQ        1/1:15
        // 3     12665100 .         A                <DUP>        14   PASS   SVTYPE=DUP;LEN=21101;SVLEN=21100;CIPOS=-500,500;CIEND=-500,500  GT:GQ:CN:CNQ ./.:0:3:16.2
        // 4     18665128 .         T                <DUP:TANDEM> 11   PASS   SVTYPE=DUP;LEN=77;SVLEN=76;CIPOS=-10,10;CIEND=-10,10         GT:GQ:CN:CNQ ./.:0:5:8.3
        // TODO: should this be reflected in a refLength and varLength ?
        if (isSymbolic(ref, alt)) {
            return ref.length();
        }
        // SNV/MNV substitution case e.g. ATGC -> CGAT length = 4
        if (alt.length() == ref.length()) {
            return ref.length();
        }
        // indel case
        return alt.length() - ref.length();
    }

    /**
     * @return 1-based inclusive start position of the allele
     */
    public int getStart() {
        return start;
    }

    /**
     * VCF 4.3 spec defines 'For precise variants, END is POS + length of REF allele−1, and for imprecise variants
     * the corresponding best estimate.'
     *
     * @return 1-based closed end position of the allele
     */
    public int getEnd() {
        return start + Math.max(ref.length() - 1, 0);
    }

    public int getLength() {
        return length(ref, alt);
    }

    public String getRef() {
        return ref;
    }

    public String getAlt() {
        return alt;
    }

    public boolean isSymbolic() {
        return isSymbolic(ref, alt);
    }

    public boolean isBreakend() {
        return isBreakend(alt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllelePosition that = (AllelePosition) o;
        return start == that.start &&
                Objects.equals(ref, that.ref) &&
                Objects.equals(alt, that.alt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, ref, alt);
    }

    @Override
    public String toString() {
        return "AllelePosition{" +
                "start=" + start +
                ", ref='" + ref + '\'' +
                ", alt='" + alt + '\'' +
                '}';
    }

}
