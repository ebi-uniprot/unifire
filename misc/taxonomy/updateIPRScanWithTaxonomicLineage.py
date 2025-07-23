#!/usr/bin/python3
# -*- coding: utf-8 -*-

"""
The script reads a InterProScan xml file and replaces any occurence of "OX={taxId}" in the xref name element
by the full lineage corresponding to this taxId. The output is the resolved InterProScan xml file written on the given
output path.

All the taxonomy data from NCBI are stored locally via Ete (by default in ~/.local/share/ete/taxa.sqlite)
This local storage can be updated using the following Python lines:

    from ete4 import NCBITaxa
    NCBITaxa().update_taxonomy_database()

Library dependencies (via pip):
    * ete4  (pip install https://github.com/etetoolkit/ete/archive/ete4.zip)
"""

from ete4 import NCBITaxa
from lxml import etree
import argparse
import sys, re

class TaxonomyEnricher:
    # Define regex constants
    REGEX_OX = re.compile(r'(OX=)(\d+)')

    def __init__(self, taxadb=None):
        self.ncbi = NCBITaxa(dbfile=taxadb)

        # Cache for tax_id to lineage lookups
        self.taxId_to_lineage = {}

    def get_taxonomy_full_lineage(self, tax_id):
        """
        Fetch the full lineage for a taxonomy ID using NCBI client (cache is checked first).
        """
        if tax_id in self.taxId_to_lineage:
            return self.taxId_to_lineage[tax_id]
        else:
            lineage = self.ncbi.get_lineage(tax_id)
            self.taxId_to_lineage[tax_id] = lineage
            return lineage

    """
    xml parser (lxml) based implementation that is more flexible in terms of preserving the original format.
    """
    def enrich_xref_name(self, input_file, output_file):
        """
        Enriches the `name` attribute in `<xref>` elements of an InterProScan XML file by updating the OX flag and
        writes to an output file.
        """

        # Check and update the OX= part in the name attribute value
        def resolve_taxonomy(name_attr):
            tax_id_match = self.REGEX_OX.search(name_attr)
            if tax_id_match:
                tax_id = int(tax_id_match.group(2))
                lineage = self.get_taxonomy_full_lineage(tax_id)
                if lineage:
                    replacement_OX = f'{tax_id_match.group(1)}{",".join(str(i) for i in lineage)}' # Replace OX= with lineage
                    return self.REGEX_OX.sub(replacement_OX, name_attr)
            return name_attr

        def namespace(element):
            m = re.match(r'\{(.*)\}', element.tag)
            return m.group(1) if m else ''

        tree = etree.parse(input_file)
        root = tree.getroot()
        namespace = namespace(root)

        ns = {'ns': namespace}
        # Iterate over all xref elements in the XML file
        for xref in root.findall("./ns:protein/ns:xref", ns):
            name_attr = xref.attrib.get("name")
            if name_attr:
                # Replace the OX= value with the full lineage
                updated_name = resolve_taxonomy(name_attr)
                xref.attrib["name"] = updated_name

        # Convert the ElementTree to a string
        xml_string = etree.tostring(root, encoding="UTF-8", xml_declaration=True, pretty_print=True)

        # Write the updated XML back to the output file
        with open(output_file, "wb") as file:
            file.write(xml_string)


def main(input_file, output_file, taxadb=None):
    taxonomy_enricher = TaxonomyEnricher(taxadb=taxadb)
    taxonomy_enricher.enrich_xref_name(input_file, output_file)

def parse_args():
    parser = argparse.ArgumentParser(description="""
    The script reads an input file in InterProScan xml format and will replace any occurrence of
    OX={taxId}" in the protein xref 'name' attribute by the full lineage corresponding to this taxId.
    """)
    parser.add_argument('--infile', '-i', dest="infile", required=True, help="""
    Path to the input file in interproscan xml format with one tax-id in each xref name attribute in the format OX={taxId}
    """)
    parser.add_argument('--outfile', '-o', dest="outfile", required=True, help="""
    Path to the output file in interproscan xml format with the full taxonomic lineage in each xref name attribute in the format
    OX={taxId1,taxId2,...}
    """)
    parser.add_argument('--taxa-sqlite', '-t', dest="taxadb", required=False, help="""
    Path to the sqlite DB file for taxonomy database. Default location is ~/.local/share/ete/taxa.sqlite
    """)

    return parser.parse_args()


if __name__ == "__main__":
    arguments = parse_args()
    main(arguments.infile, arguments.outfile, arguments.taxadb)