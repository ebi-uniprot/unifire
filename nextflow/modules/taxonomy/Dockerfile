FROM python:3.13-slim

RUN apt update \
    && apt install -y python3-dev gcc libc-dev \
    && pip3 install ete4 lxml

ADD nextflow/modules/taxonomy/update-taxonomy-cache.py /opt/code/update-taxonomy-cache.py
RUN python3 /opt/code/update-taxonomy-cache.py && ln -s ~/.local/share/ete/taxa.sqlite /opt/taxa.sqlite

ADD misc/taxonomy/updateIPRScanWithTaxonomicLineage.py /opt/code/updateIPRScanWithTaxonomicLineage.py

CMD ["/opt/code/updateIPRScanWithTaxonomicLineage.py"]