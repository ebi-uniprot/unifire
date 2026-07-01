#!/usr/bin/env python3

############################################################################
#    Copyright (c) 2018 European Molecular Biology Laboratory
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
############################################################################

"""Infer the input XML format from an XML file's root element.

Prints one of the following values to stdout:
  - InterProScan     when the root element is "protein-matches"
  - InterProScan6    when the root element is "results"

Exits with a non-zero status on errors.
"""

import argparse
import sys
import xml.etree.ElementTree as ET


ROOT_TO_TYPE = {
    "protein-matches": "InterProScan",
    "results": "InterProScan6",
}


def infer_xml_input_type(file_path: str) -> str:
    try:
        it = ET.iterparse(file_path, events=("start",))
        _, elem = next(it)
        root = elem.tag.split("}")[-1]
    except FileNotFoundError:
        raise SystemExit(f"Error: File '{file_path}' does not exist.")
    except ET.ParseError as e:
        raise SystemExit(f"Error: Unable to parse XML file '{file_path}': {e}")

    if root not in ROOT_TO_TYPE:
        raise SystemExit(f"Error: Unknown XML root element '{root}' in file '{file_path}'.")

    return ROOT_TO_TYPE[root]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Infer the input XML format from an XML file's root element."
    )
    parser.add_argument("file", help="Path to the XML input file")
    args = parser.parse_args()

    print(infer_xml_input_type(args.file))
    return 0


if __name__ == "__main__":
    sys.exit(main())
