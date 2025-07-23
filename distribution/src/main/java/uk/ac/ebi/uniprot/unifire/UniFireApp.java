/*
 *  Copyright (c) 2018 European Molecular Biology Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package uk.ac.ebi.uniprot.unifire;

import uk.ac.ebi.uniprot.urml.core.utils.SelectorEnum;
import uk.ac.ebi.uniprot.urml.input.InputType;
import uk.ac.ebi.uniprot.urml.output.OutputFormat;

import com.google.common.base.Strings;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.function.Function;
import org.apache.commons.cli.*;
import org.drools.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.primitives.Booleans.trueFirst;
import static java.lang.System.exit;

/**
 * Entry point for the UniFIRE application.
 *
 * @author Alexandre Renaux
 */
public class UniFireApp {

    private static final Logger logger = LoggerFactory.getLogger(UniFireApp.class);

    private static final InputType DEFAULT_INPUT_TYPE = InputType.INTERPROSCAN_XML;
    private static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.ANNOTATION_TSV;
    private static final Integer DEFAULT_CHUNK_SIZE = 1000;
    private static final String USAGE_SEPARATOR = StringUtils.repeat("-", 44);

    public static void main(String[] args) throws Exception {
        disableAccessWarnings();

        Options options = new Options();

        Option ruleFileOption = Option.builder("r").longOpt("rules").hasArg().argName("RULE_URML_FILE").desc("Rule base file (path) provided by UniProt (e.g UniRule or ARBA) (format: URML).").type(File.class).required().build();
        Option inputFileOption = Option.builder("i").longOpt("input").hasArg().argName("INPUT_FILE").desc("Input file (path) containing the proteins to annotate and required data, in the format specified by the -s option.").type(File.class).required().build();
        Option outputFileOption = Option.builder("o").longOpt("output").hasArg().argName("OUTPUT_FILE").desc("Output file (path) containing predictions in the format specified in the -f option.").type(File.class).required().build();
        Option inputSourceOption = Option.builder("s").longOpt("input-source").hasArg().argName("INPUT_SOURCE").desc("Input source type. Supported input sources are:\n" + prettyPrint(InputType.values(), DEFAULT_INPUT_TYPE) + ".").type(String.class).build();
        Option outputFormatOption = Option.builder("f").longOpt("output-format").hasArg().argName("OUTPUT_FORMAT").desc("Output file format. Supported formats are:\n" + prettyPrint(OutputFormat.values(), DEFAULT_OUTPUT_FORMAT) + ".").type(String.class).build();
        Option templateFileOption = Option.builder("t").longOpt("templates").hasArg().argName("TEMPLATE_FACTS").desc("UniRule template sequence matches, provided by UniProt (format: Fact Model XML).").type(File.class).build();
        Option inputChunkSizeOption = Option.builder("n").longOpt("chunksize").hasArg().argName("INPUT_CHUNK_SIZE").desc("Chunk size (number of proteins) to be batch processed simultaneously \n(default: " + DEFAULT_CHUNK_SIZE + ").").type(Integer.class).build();
        Option helpOption = Option.builder("h").longOpt("help").desc("Print this usage.").build();

        options.addOption(ruleFileOption);
        options.addOption(inputFileOption);
        options.addOption(outputFileOption);
        options.addOption(inputSourceOption);
        options.addOption(outputFormatOption);
        options.addOption(templateFileOption);
        options.addOption(inputChunkSizeOption);
        options.addOption(helpOption);

        UniFireRunner uniFireRunner;

        if (hasHelp(helpOption, args)) {
            displayUsage(options);
            exit(0);
        }

        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            File ruleBaseFile = parseOption(cmd, ruleFileOption, FileCreatorChecker::createAndCheck, null);
            File inputFactFile = parseOption(cmd, inputFileOption, FileCreatorChecker::createAndCheck, null);
            File outputFactFile = parseOption(cmd, outputFileOption, File::new, null);
            InputType inputSource = parseOption(cmd, inputSourceOption, InputType::fromCode, DEFAULT_INPUT_TYPE);
            OutputFormat outputFormat = parseOption(cmd, outputFormatOption, OutputFormat::fromCode, DEFAULT_OUTPUT_FORMAT);
            Integer inputChunkSize = parseOption(cmd, inputChunkSizeOption, Integer::valueOf, DEFAULT_CHUNK_SIZE);
            File templateFactFile = parseOption(cmd, templateFileOption, FileCreatorChecker::createAndCheck, null);

            uniFireRunner = new UniFireRunner(ruleBaseFile, inputFactFile, outputFactFile, inputSource, outputFormat, inputChunkSize, templateFactFile);
            uniFireRunner.run();
        } catch (ParseException e) {
            logger.error(e.getMessage());
            displayUsage(options);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private static void displayUsage(Options options){
        HelpFormatter formatter = new HelpFormatter();
        formatter.setOptionComparator(Comparator.comparing(Option::isRequired, trueFirst())
                                                .thenComparing(Option::hasArg, trueFirst())
                                                .thenComparing(Option::hasLongOpt, trueFirst())
                                                .thenComparing(Option::getOpt));
        formatter.setWidth(100);
        formatter.setDescPadding(5);
        formatter.setLeftPadding(5);
        System.out.println(USAGE_SEPARATOR);
        formatter.printHelp( "unifire", USAGE_SEPARATOR, options, USAGE_SEPARATOR, true);
    }

    private static boolean hasHelp(final Option help, final String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(help);
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args, true);
        return cmd.hasOption(help.getOpt());
    }

    private static <T> T parseOption(CommandLine commandLine, Option option, Function<String, T> creator, T defaultObject)
            throws MissingArgumentException {
        if (commandLine.hasOption(option.getOpt())){
            String optionValue = commandLine.getOptionValue(option.getOpt());
            if (Strings.isNullOrEmpty(optionValue)){
                throw new MissingArgumentException(option);
            } else {
                try {
                    return creator.apply(optionValue);
                } catch (Exception e){
                    throw new IllegalArgumentException(String.format("Wrong argument for option -%s. %s",
                            option.getOpt(), e.getMessage()), e);
                }
            }
        } else {
            return defaultObject;
        }
    }

    private static class FileCreatorChecker {

        static File createAndCheck(String path) {
            File file = new File(path);
            if (file.exists()){
                return file;
            } else {
                throw new IllegalArgumentException(String.format("No such file: %s", file));
            }
        }
    }

    private static String prettyPrint(SelectorEnum[] selectorEnums, SelectorEnum defaultValue){
        StringBuilder stringBuilder = new StringBuilder();
        for (SelectorEnum selectorEnum : selectorEnums) {
            stringBuilder.append(" - ").append(selectorEnum.getCode()).append(" (").append(selectorEnum.getDescription())
                    .append(")\n");
        }
        stringBuilder.append("(default: ").append(defaultValue.getCode()).append(")");
        return stringBuilder.toString();
    }

    public static void disableAccessWarnings() {
        try {
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get((Object)null);
            Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, Long.TYPE, Object.class);
            Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Long offset = (Long)staticFieldOffset.invoke(unsafe, loggerField);
            putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
        } catch (Exception var8) {
        }

    }
}
