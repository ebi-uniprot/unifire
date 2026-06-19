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

import uk.ac.ebi.uniprot.unifire.UniFireApp;
import uk.ac.ebi.uniprot.urml.core.utils.SelectorEnum;
import uk.ac.ebi.uniprot.urml.input.InputType;

import com.google.common.base.Strings;
import java.io.File;
import java.util.Comparator;
import java.util.function.Function;
import org.apache.commons.cli.*;
import org.drools.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.primitives.Booleans.trueFirst;
import static java.lang.System.exit;

/**
 * Entry point for the PIRSR application
 * 
 * @author Chuming Chen
 *
 */
public class PIRSRApp {

	private static final Logger logger = LoggerFactory.getLogger(PIRSRApp.class);
	private static final InputType DEFAULT_INPUT_TYPE = InputType.INTERPROSCAN_XML;
	private static final String USAGE_SEPARATOR = StringUtils.repeat("-", 100);

	public static void main(String[] args) throws Exception {
		UniFireApp.disableAccessWarnings();
		
		Options options = new Options();
		Option inputFileOption = Option.builder("i").longOpt("input_file").hasArg().argName("INPUT_FILE")
				.desc("Input file (path) containing the proteins to annotate and required data in " + DEFAULT_INPUT_TYPE.getDescription() + " format.")
				.type(File.class).required().build();
		Option inputTypeOption = Option.builder("t").longOpt("input_type").hasArg().argName("INPUT_TYPE")
				.desc(String.format("Type of the input file provided by -i option. Supported Input types are " +
								"\n%s\n%s %s", prettyPrint(InputType.INTERPROSCAN_XML),
						prettyPrint(InputType.FACT_XML), printDefault(InputType.INTERPROSCAN_XML)))
				.type(InputType.class).optionalArg(true).build();
		Option pirsrDataDirOption = Option.builder("d").longOpt("pirsr_data_dir").hasArg().argName("PIRSR_DATA_DIR").desc("Directory for PIRSR data.")
				.type(File.class).required().build();
		Option outputDirOption = Option.builder("o").longOpt("output_dir").hasArg().argName("OUTPUT_DIR")
				.desc("Directory for SRHMM hmmalign result and enhanced IPRScan Facts XML file.").type(File.class).required().build();
		Option hmmalignOption = Option.builder("a").longOpt("hmmalign").hasArg().argName("HMMALIGN").desc("Path to hmmalign command.").type(File.class)
				.required().build();
		Option helpOption = Option.builder("h").longOpt("help").desc("Print this usage.").build();

		options.addOption(pirsrDataDirOption);
		options.addOption(inputFileOption);
		options.addOption(inputTypeOption);
		options.addOption(hmmalignOption);
		options.addOption(outputDirOption);
		options.addOption(helpOption);

		if (hasHelp(helpOption, args)) {
			displayUsage(options);
			exit(0);
		}

		PIRSRRunner pirsrRunner;
		try {
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			File outputDirectory = parseOption(cmd, outputDirOption, File::new, null);
			File pirsrDataDirectory = parseOption(cmd, pirsrDataDirOption, FileCreatorChecker::createAndCheck, null);
			File inputFactFile = parseOption(cmd, inputFileOption, FileCreatorChecker::createAndCheck, null);
			InputType inputType = parseOption(cmd, inputTypeOption, InputTypeChecker::check,
					InputType.INTERPROSCAN_XML);
			File hmmalignCommand = parseOption(cmd, hmmalignOption, FileCreatorChecker::createAndCheck, null);

			pirsrRunner = new PIRSRRunner(pirsrDataDirectory, inputFactFile, inputType, outputDirectory, hmmalignCommand);
			pirsrRunner.run();
		} catch (ParseException e) {
			logger.error(e.getMessage());
			displayUsage(options);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static void displayUsage(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(Comparator.comparing(Option::isRequired, trueFirst()).thenComparing(Option::hasArg, trueFirst())
				.thenComparing(Option::hasLongOpt, trueFirst()).thenComparing(Option::getOpt));
		formatter.setWidth(100);
		formatter.setDescPadding(5);
		formatter.setLeftPadding(5);
		formatter.printHelp("pirsr", USAGE_SEPARATOR, options, USAGE_SEPARATOR, true);
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
                throw new IllegalArgumentException(String.format("No such file or directory: %s", file));
            }
        }
    }

    private static class InputTypeChecker {
		static InputType check(String type) {
			if ( type == null || type.equals("InterProScan") ) {
				return InputType.INTERPROSCAN_XML;
			}
			else if (type.equals("InterProScan6") ) {
				return InputType.INTERPROSCAN6_XML;
			}
			else if (type.equals("XML")) {
				return InputType.FACT_XML;
			}
			else {
				throw new IllegalArgumentException(
						String.format("Invalid input type %s. Must be InterProScan or XML", type));
			}
		}
	}

	private static String prettyPrint(SelectorEnum selectorEnum){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(" - ").append(selectorEnum.getCode()).append(" (").append(selectorEnum.getDescription())
					.append(")\n");
		return stringBuilder.toString();
	}

	private static String printDefault(SelectorEnum defaultValue) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("(default: ").append(defaultValue.getCode()).append(")");
		return stringBuilder.toString();
	}

}
