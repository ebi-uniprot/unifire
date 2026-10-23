#!/usr/bin/python3

#  Copyright (c) 2018 European Molecular Biology Laboratory
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

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

import argparse
import os
from pathlib import Path

from ete4 import NCBITaxa


def main():
    parser = argparse.ArgumentParser(description="Update the NCBI taxonomy cache database.")
    parser.add_argument(
        "--sqlite-path",
        "-s",
        type=str,
        default=None,
        help="Path to the NCBI taxonomy SQLite database file.",
    )
    args = parser.parse_args()

    # ete4 does not create its data directory on first use and fails to
    # download the taxdump into it, so make sure it exists.
    ete_data_dir = Path(os.environ.get("XDG_DATA_HOME", Path.home() / ".local" / "share")) / "ete"
    ete_data_dir.mkdir(parents=True, exist_ok=True)

    ncbi = NCBITaxa(dbfile=args.sqlite_path)
    ncbi.update_taxonomy_database()


if __name__ == "__main__":
    main()
