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

package org.proteininformationresource.pirsr;

import matchers.NodeMatcherBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.matchers.CompareMatcher;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author Vishal Joshi
 */
@Disabled("Unfortunately, this test is platform dependent due to hmmalign dynamically linking to" +
        " underlying gcc lib. It might fail for some OS which does not satisfy the hmmalign's lib dependencies, " +
        "thus making it brittle to add to build")
class PIRSRAppIntegrationTest {

    @TempDir
    File outputDir;

    static String hmmAlignPath;

    @BeforeAll
    public static void setUpWholeClass() throws IOException, InterruptedException {
        String inputDirPath = PIRSRAppIntegrationTest.class.getResource("/pirsrapp/input/").getPath();
        String hmmmerSourcePath = inputDirPath + "hmmer-3.3.2/";
        hmmAlignPath = hmmmerSourcePath + "/bin/hmmalign";

        if (!new File(hmmAlignPath).exists()) {

            ProcessBuilder builder = new ProcessBuilder();
            builder.command("tar", "zxf", "hmmer.tar.gz");
            builder.directory(new File(inputDirPath));
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            assertThat("hmmer tar didn't untar successfully", exitCode, is(equalTo(0)));

            builder.command("./configure", "--prefix", hmmmerSourcePath);
            builder.directory(new File(hmmmerSourcePath));
            process = builder.start();
            streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            exitCode = process.waitFor();
            assertThat("hmmer configure didn't go successfully", exitCode, is(equalTo(0)));

            builder = new ProcessBuilder();
            builder.command("make", "install");
            builder.directory(new File(hmmmerSourcePath));
            process = builder.start();
            streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            exitCode = process.waitFor();
            assertThat("hmmer make install didn't go successfully", exitCode, is(equalTo(0)));

        }
    }

    @Test
    void testShouldVerifyThatDefaultInterproScanOutputXmlMatchesTheExpectedFile() throws Exception {
        //given
        String inputIprScanFile = this.getClass().getResource("/pirsrapp/input/pirsr_data/PIRSR-input-iprscan.xml").getPath();
        String pirsrDataDir = this.getClass().getResource("/pirsrapp/input/pirsr_data/").getPath();
        String outputPath = outputDir.getPath();
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(inputIprScanFile);
        args.add("-d");
        args.add(pirsrDataDir);
        args.add("-a");
        args.add(hmmAlignPath);
        args.add("-o");
        args.add(outputPath);

        //when
        String[] argsArray = args.toArray(new String[0]);
        PIRSRApp.main(argsArray);

        //then
        File expectedOutputFile = new File(this.getClass().getResource("/pirsrapp/expectedoutput/PIRSR-input-iprscan-urml.xml").getPath());
        File actualOutputFile = new File(outputPath + "/PIRSR-input-iprscan-urml.xml");
        assertThat(actualOutputFile, CompareMatcher.isSimilarTo(expectedOutputFile)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(NodeMatcherBuilder.factXMLNodeMatcher())
                .withNodeFilter(node -> !node.getNodeName().equals("#comment")));
    }

    @Test
    void testShouldVerifyThatDefaultInterproScan6OutputXmlMatchesTheExpectedFile() throws Exception {
        //given
        String inputIprScanFile = this.getClass().getResource("/pirsrapp/input/pirsr_data/PIRSR-input-iprscan6.xml").getPath();
        String pirsrDataDir = this.getClass().getResource("/pirsrapp/input/pirsr_data/").getPath();
        String outputPath = outputDir.getPath();
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(inputIprScanFile);
        args.add("-d");
        args.add(pirsrDataDir);
        args.add("-a");
        args.add(hmmAlignPath);
        args.add("-t");
        args.add("InterProScan6");
        args.add("-o");
        args.add(outputPath);

        //when
        String[] argsArray = args.toArray(new String[0]);
        PIRSRApp.main(argsArray);

        //then
        // NOTE: Expected output file should be identical to InterProScan 5
        File expectedOutputFile = new File(this.getClass().getResource("/pirsrapp/expectedoutput/PIRSR-input-iprscan-urml.xml").getPath());
        File actualOutputFile = new File(outputPath + "/PIRSR-input-iprscan-urml.xml");
        assertThat(actualOutputFile, CompareMatcher.isSimilarTo(expectedOutputFile)
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(NodeMatcherBuilder.factXMLNodeMatcher())
                .withNodeFilter(node -> !node.getNodeName().equals("#comment")));
    }

    @Test
    void testShouldVerifyThatFactXmlMatchesTheExpectedFile() throws Exception {
        //given
        String iprFasta = this.getClass().getResource("/pirsrapp/input/pirsr_data/PIRSR-input-fact.xml").getPath();
        String pirsrDataDir = this.getClass().getResource("/pirsrapp/input/pirsr_data/").getPath();
        String outputPath = outputDir.getPath();
        List<String> args = new ArrayList<>();
        args.add("-i");
        args.add(iprFasta);
        args.add("-d");
        args.add(pirsrDataDir);
        args.add("-a");
        args.add(hmmAlignPath);
        args.add("-o");
        args.add(outputPath);
        args.add("-t");
        args.add("XML");
        String expectedOutputFile = this.getClass().getResource("/pirsrapp/expectedoutput/PIRSR-input-fact-urml.xml").getPath();

        //when
        String[] argsArray = args.toArray(new String[0]);
        PIRSRApp.main(argsArray);

        //then
        String actualOutputFile = outputPath + "/PIRSR-input-fact-urml.xml";
        assertThat(new File(actualOutputFile), CompareMatcher.isSimilarTo(new File(expectedOutputFile))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .withNodeMatcher(NodeMatcherBuilder.factXMLNodeMatcher())
                .withNodeFilter(node -> !node.getNodeName().equals("#comment")));
    }

    private static class StreamGobbler implements Runnable {
        private final InputStream inputStream;
        private final Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
        }
    }
}