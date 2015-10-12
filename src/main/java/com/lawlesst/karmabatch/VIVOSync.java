/**
 * Created by ted on 10/10/15.
 */
package com.lawlesst.karmabatch;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


public class VIVOSync {
    private static Logger log = LoggerFactory.getLogger(VIVOSync.class);
    private String config;
    private String BASE_DIRECTORY;
    private String baseuri;


    public VIVOSync(CommandLine cl)
    {
        parseCommandLineOptions(cl);
    }

    public static void main(String[] args) throws KarmaException, IOException {

        Options options = createCommandLineOptions();
        CommandLine cl = CommandLineArgumentParser.parse(args, options, VIVOSync.class.getSimpleName());
        if(cl == null)
        {
            log.error("No command line options found.");
            System.exit(1);
        }

        // Initialize Karma
        KarmaClient.setupKarmaMetadata();

        // Run the transforms and post changes to VIVO
        VIVOSync syncer = new VIVOSync(cl);
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
            log.info(String.format("Processing: %s with debug set to %s", modelName, debug));
            Literal sourceFile = soln.getLiteral("source");
            Literal kmodel = soln.getLiteral("model");
            String namedGraph = soln.getLiteral("namedGraph").toString();
            String modelPath = BASE_DIRECTORY + kmodel;
            String filename = BASE_DIRECTORY + sourceFile;
            String karmaRdf = KarmaClient.applyModel(modelPath, modelName, filename, baseuri);
            log.info("Reading Karma generated RDF into Jena model.");
            //Get Jena models for incoming RDF and existing RDF
            Model incoming = ModelUtils.getModelFromN3String(karmaRdf);
            Model existing = vivoClient.getExistingNamedGraph(namedGraph);

            //Diff the models
            Model additions = incoming.difference(existing);
            Model subtractions = existing.difference(incoming);
            if (debug) {
                log.info("Triples to add:\n");
                additions.write(System.out, "N-TRIPLES");
                log.info("Triples to delete:\n");
                subtractions.write(System.out, "N-TRIPLES");
            } else {
                vivoClient.syncNamedGraph(namedGraph, additions, subtractions);
                log.info("Triples synced for: " + modelName);
            }
        }
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
        config = cl.getOptionValue("baseuri");
        BASE_DIRECTORY = FilenameUtils.getFullPathNoEndSeparator(config) + File.separator;
    }


    private static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption(new Option("config", "config", true, "TTL config file with the Karma models and input files"));
        options.addOption(new Option("baseuri", "baseuri", true, "BASE URI for new statements"));
        return options;
    }
}
