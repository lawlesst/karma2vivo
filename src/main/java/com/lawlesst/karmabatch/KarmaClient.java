package com.lawlesst.karmabatch;

import edu.isi.karma.config.ModelingConfiguration;
import edu.isi.karma.config.ModelingConfigurationRegistry;
import edu.isi.karma.controller.update.UpdateContainer;
import edu.isi.karma.er.helper.PythonRepository;
import edu.isi.karma.er.helper.PythonRepositoryRegistry;
import edu.isi.karma.kr2rml.URIFormatter;
import edu.isi.karma.kr2rml.mapping.R2RMLMappingIdentifier;
import edu.isi.karma.kr2rml.writer.N3KR2RMLRDFWriter;
import edu.isi.karma.metadata.KarmaMetadataManager;
import edu.isi.karma.metadata.PythonTransformationMetadata;
import edu.isi.karma.metadata.UserConfigMetadata;
import edu.isi.karma.metadata.UserPreferencesMetadata;
import edu.isi.karma.modeling.semantictypes.SemanticTypeUtil;
import edu.isi.karma.rdf.GenericRDFGenerator;
import edu.isi.karma.rdf.RDFGeneratorRequest;
import edu.isi.karma.webserver.ContextParametersRegistry;
import edu.isi.karma.webserver.KarmaException;
import edu.isi.karma.webserver.ServletContextParameterMap;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by ted on 10/10/15.
 */
public class KarmaClient {
    private static ServletContextParameterMap contextParameters;

    public static void main(String[] args) throws KarmaException {
        System.out.println("Hello client.");
        setupKarmaMetadata();
    }

    public static String applyModel(String modelPath, String modelName, String csvPath, String baseURI) throws KarmaException, IOException {
        /*
            This applies the Karma model to the incoming file. The base URI is used for URI creation.
         */
        GenericRDFGenerator rdfGenerator = new GenericRDFGenerator();

        R2RMLMappingIdentifier modelIdentifier = new R2RMLMappingIdentifier(
                modelName,
                new File(modelPath).toURI().toURL()
        );
        rdfGenerator.addModel(modelIdentifier);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        N3KR2RMLRDFWriter writer = new N3KR2RMLRDFWriter(new URIFormatter(), pw);
        writer.setBaseURI(baseURI);
        RDFGeneratorRequest request = new RDFGeneratorRequest(modelName, csvPath);
        request.setInputFile(new File(csvPath));
        request.setDataType(GenericRDFGenerator.InputType.CSV);
        request.addWriter(writer);
        rdfGenerator.generateRDF(request);
        //write RDF to string.
        return sw.toString();
    }


    public static void setupKarmaMetadata() throws KarmaException {

        ContextParametersRegistry contextParametersRegistry = ContextParametersRegistry.getInstance();
        contextParameters = contextParametersRegistry.registerByKarmaHome(null);

        UpdateContainer uc = new UpdateContainer();
        KarmaMetadataManager userMetadataManager = new KarmaMetadataManager(contextParameters);
        userMetadataManager.register(new UserPreferencesMetadata(contextParameters), uc);
        userMetadataManager.register(new UserConfigMetadata(contextParameters), uc);
        userMetadataManager.register(new PythonTransformationMetadata(contextParameters), uc);
        PythonRepository pythonRepository = new PythonRepository(false, contextParameters.getParameterValue(ServletContextParameterMap.ContextParameter.USER_PYTHON_SCRIPTS_DIRECTORY));
        PythonRepositoryRegistry.getInstance().register(pythonRepository);

        SemanticTypeUtil.setSemanticTypeTrainingStatus(false);
        ModelingConfiguration modelingConfiguration = ModelingConfigurationRegistry.getInstance().register(contextParameters.getId());
        modelingConfiguration.setLearnerEnabled(false); // disable automatic learning

    }
}
