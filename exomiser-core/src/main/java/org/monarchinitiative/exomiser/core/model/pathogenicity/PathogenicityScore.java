/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2019 Queen Mary University of London.
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

package org.monarchinitiative.exomiser.core.model.pathogenicity;

/**
 *
 * @author Jules Jacobsen <jules.jacobsen@sanger.ac.uk>
 */
public interface PathogenicityScore extends Comparable<PathogenicityScore> {

    public static PathogenicityScore of(PathogenicitySource source, float score) {
        switch (source) {
            case POLYPHEN:
                return PolyPhenScore.of(score);
            case MUTATION_TASTER:
                return MutationTasterScore.of(score);
            case SIFT:
                return SiftScore.of(score);
            case CADD:
            case REMM:
            case REVEL:
                // TODO: Add MVP, ClinPred, PrimateAi, M-CAP,
            default:
                return new BasePathogenicityScore(source, score);
        }
    }

    public float getScore();

    public PathogenicitySource getSource();
}
