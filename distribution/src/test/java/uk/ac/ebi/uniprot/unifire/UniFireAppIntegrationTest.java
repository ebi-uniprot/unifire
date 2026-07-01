/*
 * Copyright (c) 2018 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.uniprot.unifire;

import matchers.NodeMatcherBuilder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Vishal Joshi
 */
class UniFireAppIntegrationTest {

    @TempDir
    File outputDir;

    @Test
    void testShouldVerifyThatGeneratedTabSeparatedPredictionFileMatchesTheExpectedFile() throws Exception {
        //given
        String iprFasta = this.getClass().getResource("/unifireapp/input/input_ipr.fasta.xml").getPath();
        String ruleFile = this.getClass().getResource("/unifireapp/input/unirule-urml-latest.xml").getPath();
        String templateFile = this.getClass().getResource("/unifireapp/input/unirule-templates-latest.xml").getPath();
        String outputPath = outputDir.getPath() + "/unifire_output.tsv";
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(iprFasta);
        args.add("-r");
        args.add(ruleFile);
        args.add("-t");
        args.add(templateFile);
        args.add("-o");
        args.add(outputPath);

        //when
        String[] argsArray = args.toArray(new String[0]);
        UniFireApp.main(argsArray);

        //then
        String expectedOutputFilePath = this.getClass().getResource("/unifireapp/unifire_output.tsv").getPath();
        assertFiles(Paths.get(outputPath).toFile(), Paths.get(expectedOutputFilePath).toFile());
    }

    @Test
    void testShouldVerifyThatGeneratedXMLPredictionFileMatchesTheExpectedFile() throws Exception {
        //given
        String iprFasta = this.getClass().getResource("/unifireapp/input/input_ipr.fasta.xml").getPath();
        String ruleFile = this.getClass().getResource("/unifireapp/input/unirule-urml-latest.xml").getPath();
        String templateFile = this.getClass().getResource("/unifireapp/input/unirule-templates-latest.xml").getPath();
        String outputPath = outputDir.getPath() + "/unifire_output.xml";
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(iprFasta);
        args.add("-r");
        args.add(ruleFile);
        args.add("-t");
        args.add(templateFile);
        args.add("-o");
        args.add(outputPath);
        args.add("-f");
        args.add("XML");

        //when
        String[] argsArray = args.toArray(new String[0]);
        UniFireApp.main(argsArray);

        //then
        String expectedOutputFilePath = this.getClass().getResource("/unifireapp/unifire_output.xml").getPath();
        assertThat(new File(outputPath), CompareMatcher.isSimilarTo(new File(expectedOutputFilePath))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(NodeMatcherBuilder.unifireXMLNodeMatcher())
                .withNodeFilter(node -> !node.getNodeName().equals("#comment")));
    }

    @Test
    void test_iprscan6_ShouldVerifyThatGeneratedTabSeparatedPredictionFileMatchesTheExpectedFile() throws Exception {
        //given
        String iprFasta = this.getClass().getResource("/unifireapp/input/input_ipr6.fasta.xml").getPath();
        String ruleFile = this.getClass().getResource("/unifireapp/input/unirule-urml-latest.xml").getPath();
        String templateFile = this.getClass().getResource("/unifireapp/input/unirule-templates-latest.xml").getPath();
        String outputPath = outputDir.getPath() + "/unifire_output.tsv";
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(iprFasta);
        args.add("-r");
        args.add(ruleFile);
        args.add("-t");
        args.add(templateFile);
        args.add("-o");
        args.add(outputPath);
        args.add("-s");
        args.add("InterProScan6");

        //when
        String[] argsArray = args.toArray(new String[0]);
        UniFireApp.main(argsArray);

        //then
        String expectedOutputFilePath = this.getClass().getResource("/unifireapp/unifire_output.tsv").getPath();
        assertFiles(Paths.get(outputPath).toFile(), Paths.get(expectedOutputFilePath).toFile());
    }

    @Test
    void test_iprscan6_ShouldVerifyThatGeneratedXMLPredictionFileMatchesTheExpectedFile() throws Exception {
        //given
        String iprFasta = this.getClass().getResource("/unifireapp/input/input_ipr6.fasta.xml").getPath();
        String ruleFile = this.getClass().getResource("/unifireapp/input/unirule-urml-latest.xml").getPath();
        String templateFile = this.getClass().getResource("/unifireapp/input/unirule-templates-latest.xml").getPath();
        String outputPath = outputDir.getPath() + "/unifire_output.xml";
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(iprFasta);
        args.add("-r");
        args.add(ruleFile);
        args.add("-t");
        args.add(templateFile);
        args.add("-o");
        args.add(outputPath);
        args.add("-s");
        args.add("InterProScan6");
        args.add("-f");
        args.add("XML");

        //when
        String[] argsArray = args.toArray(new String[0]);
        UniFireApp.main(argsArray);

        //then
        String expectedOutputFilePath = this.getClass().getResource("/unifireapp/unifire_output.xml").getPath();
        assertThat(new File(outputPath), CompareMatcher.isSimilarTo(new File(expectedOutputFilePath))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(NodeMatcherBuilder.unifireXMLNodeMatcher())
                .withNodeFilter(node -> !node.getNodeName().equals("#comment")));
    }

    public static void assertFiles(File actual, File expected) throws IOException {
        List<String> actualLines = FileUtils.readLines(actual);
        List<String> expectedLines = FileUtils.readLines(expected);
        assertThat(actualLines, containsInAnyOrder(expectedLines.toArray()));
    }

}