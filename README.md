## karma2vivo

This is a work in progress. Feel free to open if you have questions. 

## Purpose/Goals

 * Use [Karma](http://www.isi.edu/integration/karma/) to map legacy data in a variety of formats to RDF for VIVO
 * Export [Karma's models](https://github.com/usc-isi-i2/Web-Karma/wiki/Modeling-Data) and use with the 
 * Use VIVO's SPARQL update API to automate the mapping of the legacy data to RDF 


## Installation

`This is also a work in progress. This will require some fiddling on your end to get this right.`

* install [Karma](https://github.com/usc-isi-i2/Web-Karma/wiki/Installation%3A-Source-Code)
* git clone the repository
* run `mvn install`
* copy `sample-batch.sh` to `batch.sh` (or your choice) and change default values to match your environment
* run `batch.sh --config sample/ingest.ttl` to run a sample batch. If all goes well you should see triples printed to your screen.
* sync real data by creating your own `ingest.ttl` file based on the example in `sample/ingest.ttl`. 
 * by removing `ingest:debug "true"`, karma2vivo will connect to your VIVO store (using the environment variables defined in sample-batch.sh) and sync the triples to a named graph.

## Usage

* An "ingest" configuration is needed. See the sample in `sample/ingest.ttl`. There can be multiple ingest:Transform blocks in one configuration.
* There is a command line option `--sync` that can be both powerful and dangerous. When set it will not just add triples to VIVO but it will query VIVO for
existing triples in a named graph and compare those with the incoming triples. It will then delete and/or add triples to make sure that the named graph matches
what is coming from the source data.