## karma2vivo

`This is a work in progress.`

## Purpose
 * leverage Karma to map CSV data to RDF for VIVO
 * use [Karma's Java API](https://github.com/usc-isi-i2/Web-Karma/wiki/Batch-Mode-for-RDF-Generation#genericrdfgenerator) to generate triples in batch mode
 * use the VIVO SPARQL update API to sync the mapped data

## Installation

* install [Karma](https://github.com/usc-isi-i2/Web-Karma/wiki/Installation%3A-Source-Code)
* git clone the repository
* run `mvn install`
* copy `sample-batch.sh` to `batch.sh` (or your choice) and change default values to match your environment
* run `batch.sh --config sample/ingest.ttl` to run a sample batch. If all goes well you should see triples printed to your screen.
* sync real data by creating your own `ingest.ttl` file based on the example in `sample/ingest.ttl`. 
 * by removing `ingest:debug "true"`, karma2vivo will connect to your VIVO store (using the environment variables defined in sample-batch.sh) and sync the triples to a named graph.
