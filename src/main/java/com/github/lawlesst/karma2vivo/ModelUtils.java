package com.github.lawlesst.karma2vivo;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by ted on 10/3/15.
 */
public class ModelUtils {

    //Read the Karma output as a N3 string
    public static Model getModelFromN3String(String rdf) {
        Model model = ModelFactory.createDefaultModel();
        model.read(new StringReader(rdf), null, "N3");
        return model;
    }

    public static String modelToNtripleString(Model model){
        String syntax = "N-TRIPLES";
        StringWriter out = new StringWriter();
        model.write(out, syntax);
        return out.toString();
    }
}
