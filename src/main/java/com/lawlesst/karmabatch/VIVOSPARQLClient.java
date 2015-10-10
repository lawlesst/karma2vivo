package com.lawlesst.karmabatch;

/**
 * Created by ted on 10/10/15.
 */
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;


public class VIVOSPARQLClient {
    //logger
    private static Logger log = LoggerFactory.getLogger(VIVOSPARQLClient.class);
    //VIVO API details pulled from environment variables
    private static String api = System.getenv("VIVO_API");
    private static String queryApi = api + "sparqlQuery";
    private static String updateApi = api + "sparqlUpdate";
    private static String vivoEmail = System.getenv("VIVO_EMAIL");
    private static String vivoPassword = System.getenv("VIVO_PASSWORD");
    private static String batchSize = "10000"; //System.getenv("TRIPLE_BATCH_SIZE");

    /**
     * Get all existing triples in a named graph
     *
     * @param namedGraph String with graph name.
     * @return Jena model with the existing triples.
     * @throws IOException
     */
    public Model getExistingNamedGraph(String namedGraph) throws IOException {
        String rq = "construct {?s ?p ?o} where { GRAPH <?g> {?s ?p ?o}}";
        String query = rq.replace("?g", namedGraph);
        //Accepting n-triples only.
        Header header = new BasicHeader(HttpHeaders.ACCEPT, "text/plain");

        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            log.debug("Connecting to SPARQL query API at: " + queryApi);
            log.debug("SPARQL query: " + query);
            HttpPost httpPost = new HttpPost(queryApi);
            httpPost.addHeader(header);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("query", query));
            nvps.add(new BasicNameValuePair("email", vivoEmail));
            nvps.add(new BasicNameValuePair("password", vivoPassword));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            CloseableHttpResponse response = httpclient.execute(httpPost);
            try {
                HttpEntity entity = response.getEntity();
                InputStream is = entity.getContent();
                //read response into Jena model and return
                Model model = ModelFactory.createDefaultModel();
                model.read(is, null, "N-TRIPLES");
                return model;
            } finally {
                response.close();
            }

        } finally {
            httpclient.close();
        }
    }

    /**
     * Sync the additions and subtractions to the store via the named graph.
     *
     *   http://www.w3.org/TR/sparql11-update/#deleteInsert
     *   https://wiki.duraspace.org/display/VIVO/The+SPARQL+Update+API
     *
     * @param namedGraph String with named graph.
     * @param additions Jena model with triples to add;
     * @param subtractions Jena model with triples to subtract;
     * @throws IOException
     */
    public void syncNamedGraph(String namedGraph, Model additions, Model subtractions) throws IOException {

        if (additions.isEmpty() && subtractions.isEmpty()) {
            log.info("Add and subtract graphs are empty. No changes made to <g>.".replace("<g>", namedGraph));
            return;
        }

        if (!additions.isEmpty()) {
            log.info("ADDing triples: " + additions.size());
            bulkUpdate(namedGraph, additions, "add");
        }

        if (!subtractions.isEmpty()) {
            log.info("DELETEing triples: " + subtractions.size());
            bulkUpdate(namedGraph, subtractions, "remove");
        }
    }

    /**
     * Actually POST the SPARQL update to the VIVO API.
     * @param query String with SPARQL update query and triples in ntriples format.
     * @throws IOException
     */
    public void doUpdate(String query) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            log.info("Connecting to SPARQL update API at: " + updateApi);
            HttpPost httpPost = new HttpPost(updateApi);
            List <NameValuePair> nvps = new ArrayList <NameValuePair>();
            nvps.add(new BasicNameValuePair("update", query));
            nvps.add(new BasicNameValuePair("email", vivoEmail));
            nvps.add(new BasicNameValuePair("password", vivoPassword));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
            CloseableHttpResponse response = httpclient.execute(httpPost);
            try {
                String statusLine = response.getStatusLine().toString();
                log.debug("HTTP response: " + statusLine);
                if (!statusLine.contains("200 OK")) {
                    try {
                        throw new Exception("VIVO API failed");
                    } catch (Exception e) {
                        log.error(statusLine);
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }

    /**
     * Method called to Chunk the triples into N-Sized batches and post to VIVO.
     * This is designed to work around / handle errors whne posting sets of triples
     * over 10,000 to the API.
     *
     * @param namedGraph String with named graph.
     * @param changeModel Jena model with set of changes to sync to store.
     * @param changeType Either add or remove.
     * @return Boolean true if update was successful.
     * @throws IOException
     */
    private Boolean bulkUpdate(String namedGraph, Model changeModel, String changeType) throws IOException {
        // Temporary model to hold
        Model tmpModel = ModelFactory.createDefaultModel();
        Integer bSize = Integer.parseInt(batchSize);
        // Use an integer to count triples rather than calling size on the model
        // during each loop.
        Integer size = 0;
        StmtIterator iter = changeModel.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.nextStatement();  // get next statement
            tmpModel.add(stmt);
            size++;
            if (size >= bSize) {
                // Submit
                submitBatch(tmpModel, namedGraph, changeType);
                // Reset the tmp model.
                tmpModel.removeAll();
                // Reset the counter.
                size = 0;
            }
        }
        log.info("model size:" + tmpModel.size());
        // Submit the remaining statements, if any.
        if (tmpModel.size() > 0) {
            submitBatch(tmpModel, namedGraph, changeType);
        }
        return true;
    }

    /**
     * Submit a batch of changes to the VIVO API.
     * @param changeModel A Jena model of changes.
     * @param namedGraph A String with named graph.
     * @param changeType String of change type - either "add" or "remove".
     * @throws IOException
     */
    private void submitBatch(Model changeModel, String namedGraph, String changeType) throws IOException {
        String ntriples = ModelUtils.modelToNtripleString(changeModel);
        if (changeType.equals("add")) {
            doAdd(ntriples, namedGraph);
        } else {
            doRemove(ntriples, namedGraph);
        }
    }

    /** Creates INSERT query for set of triples.
     *
     * @param ntriples String of ntriples.
     * @param namedGraph String with named graph.
     * @throws IOException
     */
    private void doAdd(String ntriples, String namedGraph) throws IOException {
        String rq  =  "INSERT DATA { GRAPH <g1> { nt } }".replace("nt", ntriples).replace("g1", namedGraph);
        doUpdate(rq);
    }

    /** Creates DELETE query for set of triples
     *
     * @param ntriples String of ntriples.
     * @param namedGraph String with named graph.
     * @throws IOException
     */
    private void doRemove(String ntriples, String namedGraph) throws IOException {
        String rq  =  "DELETE DATA { GRAPH <g1> { nt } }".replace("nt", ntriples).replace("g1", namedGraph);
        doUpdate(rq);
    }
}
