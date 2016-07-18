/**
 * Command line tool for syncing CSV files to VIVO using Karma.
 */
package com.github.lawlesst.karma2vivo;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import edu.isi.karma.rdf.CommandLineArgumentParser;
import edu.isi.karma.webserver.KarmaException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


public class Batch {
    private static Logger log = LoggerFactory.getLogger(Batch.class);
    private String config;
    private Boolean sync = false;
    private String BASE_DIRECTORY;
    private String baseuri = System.getenv("DATA_NAMESPACE");


    public Batch(CommandLine cl)
    {
        parseCommandLineOptions(cl);
    }

    public static void main(String[] args) throws KarmaException, IOException {

        PropertyConfigurator.configure("src/main/config/log4j.properties");
        log.info("Starting Karma2VIVO");
        Options options = createCommandLineOptions();
        CommandLine cl = CommandLineArgumentParser.parse(args, options, Batch.class.getSimpleName());
        if(cl == null)
        {
            log.error("No command line options found.");
            System.exit(1);
        }

        // Initialize Karma
        KarmaClient.setupKarmaMetadata();

        // Run the transforms and post changes to VIVO
        Batch syncer = new Batch(cl);
        syncer.doTransforms();
    }

    private void doTransforms() throws KarmaException, IOException {
        ResultSet transforms = readConfig(config);
        VIVOSPARQLClient vivoClient = new VIVOSPARQLClient();
        // loop through config file.
        while ( transforms.hasNext() ) {
            QuerySolution soln = transforms.nextSolution();
            // See if debug is set in the config.
            Boolean debug = false;
            Literal debugConfig = soln.getLiteral("debug");
            if (debugConfig != null) {
                debug = debugConfig.getBoolean();
            }
            String modelName = soln.getLiteral("name").toString();
            log.info(String.format("Processing: %s model with debug set to %s", modelName, debug));
            Literal sourceFile = soln.getLiteral("source");
            Literal kmodel = soln.getLiteral("model");
            String namedGraph = soln.getLiteral("namedGraph").toString();
            String modelPath = BASE_DIRECTORY + kmodel;
            String filename = BASE_DIRECTORY + sourceFile;
            String karmaRdf = KarmaClient.applyModel(modelPath, modelName, filename, baseuri);
            log.info("Reading Karma generated RDF into Jena model.");
            //Get Jena models for incoming RDF and existing RDF
            Model incoming = ModelUtils.getModelFromN3String(karmaRdf);

            if ( sync ) {
                log.info("Syncing triples to " + namedGraph);
                //Diff the models
                Model existing = vivoClient.getExistingNamedGraph(namedGraph);
                Model additions = incoming.difference(existing);
                Model subtractions = existing.difference(incoming);
                if (debug) {
                    debugTrips(additions, subtractions);
                } else {
                    vivoClient.syncNamedGraph(namedGraph, additions, subtractions);
                    log.info("Triples synced for: " + modelName);
                }
            } else {
                log.info("Updating triples to " + namedGraph);
                if (debug) {
                    debugTrips(incoming);
                } else {
                    vivoClient.updateNamedGraph(namedGraph, incoming);
                    log.info("Added triples to: " + modelName);
                }
            }
        }
    }

    private void debugTrips(Model additions) {
        Long numAdd = additions.size();
        log.info(numAdd + " triples to add: " + numAdd);
        additions.write(System.out, "N-TRIPLES");
    }

    private void debugTrips(Model additions, Model subtractions) {
        Long numAdd = additions.size();
        Long numSub = additions.size();
        log.info(numAdd.toString() + " triples to add:\n");
        additions.write(System.out, "N-TRIPLES");
        log.info(numSub.toString() + " triples to delete:\n");
        subtractions.write(System.out, "N-TRIPLES");
    }

    private ResultSet readConfig(String config) {
        Model model = FileManager.get().loadModel( config );
        String queryString =
                "PREFIX ingest: <http://localhost/ingest#> " +
                        "SELECT ?debug ?name ?source ?model ?namedGraph WHERE { " +
                        "    ?t a ingest:Transform ; " +
                        "       ingest:name ?name ;" +
                        "       ingest:source ?source ;" +
                        "       ingest:model ?model ; " +
                        "       ingest:namedGraph ?namedGraph ." +
                        " OPTIONAL { ?t ingest:debug ?debug }" +
                        "}";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        return qexec.execSelect();
    }

    protected void parseCommandLineOptions(CommandLine cl) {
        config = cl.getOptionValue("config");
        if (cl.hasOption("sync")) {
            log.info("Will sync triples to named graph");
            sync = true;
        }
        BASE_DIRECTORY = FilenameUtils.getFullPathNoEndSeparator(config) + File.separator;
    }


    private static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option("config", "config", true, "TTL config file with the Karma models and input files"));
        options.addOption("sync", false, "Sync triples to a named graph");
        return options;
    }
}
