## karma2vivo

Batch generation of RDF for VIVO using Karma models and the [VIVO SPARQL Update API](https://wiki.duraspace.org/display/VIVO/The+SPARQL+Update+API) to write the data to VIVO. *This is a work in progress.*

## Purpose/Goals

* Use [Karma](http://www.isi.edu/integration/karma/) to map existing data in a variety of formats to RDF for VIVO.

* Export [Karma's models](https://github.com/usc-isi-i2/Web-Karma/wiki/Modeling-Data) for use from outside the Karma interface. 

* Use the [Karma RDF generator API](https://github.com/usc-isi-i2/Web-Karma/wiki/Batch-Mode-for-RDF-Generation#genericrdfgenerator) to generate triples and the VIVO SPARQL Update API to write those triples to VIVO in an automated fashion.


## Installation

*This is first pass at installation instructions. This will probably require some troubleshooting and modifications to install.*

* install [Karma](https://github.com/usc-isi-i2/Web-Karma/wiki/Installation%3A-Source-Code)

* `git clone git@github.com:lawlesst/karma2vivo.git`
* `cd karma2vivo`

* run `mvn clean install`

* copy `sample-batch.sh` to `batch.sh` (or your choice) and change default values to match your environment

* run `./batch.sh sample/ingest.ttl` to run a sample batch. If all goes well you should see triples printed to your screen.

* create your own `ingest.ttl` file based on the example in `sample/ingest.ttl`.


## Usage

* An "ingest" configuration is needed. See the sample in `sample/ingest.ttl`. There can be multiple ingest:Transform blocks in one configuration.

* There is a command line option `--sync` that will compare triples in the incoming data to those that exist in VIVO and update VIVO to match the incoming data. Warning, this will remove data.

* by removing `ingest:debug "true"`, karma2vivo will connect to your VIVO store (using the environment variables defined in sample-batch.sh) and post the generated triples to VIOV.

More to come...
