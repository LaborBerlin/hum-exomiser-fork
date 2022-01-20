/*
 * The Exomiser - A tool to annotate and prioritize genomic variants
 *
 * Copyright (c) 2016-2022 Queen Mary University of London.
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

package org.monarchinitiative.exomiser.data.genome;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.monarchinitiative.exomiser.data.genome.model.AlleleResource;
import org.monarchinitiative.exomiser.data.genome.model.resource.TabixAlleleResource;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jules Jacobsen <j.jacobsen@qmul.ac.uk>
 */
class ResourceDownloaderTest {

    @Test
    void downloadWithTabixIndex(@TempDir Path tempDir) throws Exception {
        URL url = Paths.get("src/test/resources/test_empty.vcf.gz").toUri().toURL();
        Path alleleGzipFile = tempDir.resolve("test_empty.vcf.gz");
        AlleleResource testResource = new TabixAlleleResource("test", url, alleleGzipFile, line -> null);
        ResourceDownloader.download(testResource);
        assertThat(alleleGzipFile.toFile().exists(), is(true));
        assertThat(tempDir.resolve("test_empty.vcf.gz.tbi").toFile().exists(), is(true));
    }

    @Test
    void downloadNonExistentFileThrowsException(@TempDir Path tempDir) throws Exception {
        URL url = Paths.get("src/test/resources/no_file_here.vcf.gz").toUri().toURL();
        Path alleleGzipFile = tempDir.resolve("no_file_here.vcf.gz");
        AlleleResource testResource = new TabixAlleleResource("test", url, alleleGzipFile, line -> null);
        assertThrows(ResourceDownloadException.class, () -> ResourceDownloader.download(testResource));
    }

    @Test
    void downloadWithoutTabixIndex(@TempDir Path tempDir) throws Exception {
        URL url = Paths.get("src/test/resources/test_first_ten_dbsnp.vcf.gz").toUri().toURL();
        Path alleleGzipFile = tempDir.resolve("test_first_ten_dbsnp.vcf.gz");
        AlleleResource testResource = new TabixAlleleResource("test", url, alleleGzipFile, line -> null);
        ResourceDownloader.download(testResource);
        assertThat(alleleGzipFile.toFile().exists(), is(true));
        assertThat(tempDir.resolve("test_first_ten_dbsnp.vcf.gz.tbi").toFile().exists(), is(false));
    }
}