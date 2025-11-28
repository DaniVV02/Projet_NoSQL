package qengine.program;

import fr.boreal.model.formula.api.FOFormula;
import fr.boreal.model.formula.api.FOFormulaConjunction;
import fr.boreal.model.kb.api.FactBase;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.query.api.FOQuery;
import fr.boreal.model.queryEvaluation.api.FOQueryEvaluator;
import fr.boreal.query_evaluation.generic.GenericFOQueryEvaluator;
import fr.boreal.storage.natives.SimpleInMemoryGraphStore;
import org.eclipse.rdf4j.rio.RDFFormat;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;
import qengine.parser.RDFTriplesParser;
import qengine.parser.StarQuerySparQLParser;
import qengine.storage.RDFHexaStore;
import qengine.storage.RDFStorage;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CompareWithInteGraal {
    private static final String WORKING_DIR = "data/";
    private static final String SAMPLE_DATA_FILE = WORKING_DIR + "sample_data.nt";
    private static final String SAMPLE_QUERY_FILE = WORKING_DIR + "sample_query.queryset";

    public static void main(String[] args) throws IOException {
        // Charger les triplets
        List<RDFTriple> rdfAtoms = parseRDFData();

        // Charger les requêtes
        List<StarQuery> starQueries = parseSparQLQueries(SAMPLE_QUERY_FILE);

        // Préparer les deux systèmes
        FactBase factBase = new SimpleInMemoryGraphStore();
        RDFStorage myStore = new RDFHexaStore(); // ou new GiantTable() aussi

        for (RDFTriple triple : rdfAtoms) {
            factBase.add(triple);
            myStore.add(triple);
        }

        // comparaison des résultats pour chaque requête
        for (StarQuery starQuery : starQueries) {
            FOQuery<FOFormulaConjunction> foQuery = starQuery.asFOQuery();
            FOQueryEvaluator<FOFormula> evaluator = GenericFOQueryEvaluator.defaultInstance();
            Iterator<Substitution> integraalResults = evaluator.evaluate(foQuery, factBase);
            Iterator<Substitution> myResults = myStore.match(starQuery);

            compareResults(starQuery, integraalResults, myResults);
        }
    }

    private static List<RDFTriple> parseRDFData() throws IOException {
        FileReader rdfFile = new FileReader(CompareWithInteGraal.SAMPLE_DATA_FILE);
        List<RDFTriple> rdfAtoms = new ArrayList<>();
        try (RDFTriplesParser rdfParser = new RDFTriplesParser(rdfFile, RDFFormat.NTRIPLES)) {
            while (rdfParser.hasNext()) {
                rdfAtoms.add(rdfParser.next());
            }
        }
        return rdfAtoms;
    }

    private static List<StarQuery> parseSparQLQueries(String queryFilePath) throws IOException {
        List<StarQuery> starQueries = new ArrayList<>();
        try (StarQuerySparQLParser queryParser = new StarQuerySparQLParser(queryFilePath)) {
            while (queryParser.hasNext()) {
                starQueries.add((StarQuery) queryParser.next());
            }
        }
        return starQueries;
    }

    private static void compareResults(StarQuery starQuery,
                                       Iterator<Substitution> integraalResults,
                                       Iterator<Substitution> myResults) {
        Set<String> integraalSet = new HashSet<>();
        integraalResults.forEachRemaining(sub -> integraalSet.add(sub.toString()));

        Set<String> mySet = new HashSet<>();
        myResults.forEachRemaining(sub -> mySet.add(sub.toString()));

        System.out.println("=== Query: " + starQuery.getLabel() + " ===");
        System.out.println("Réponses de InteGraal: " + integraalSet);
        System.out.println("Réponses de notre système: " + mySet);

        if (integraalSet.equals(mySet)) {
            System.out.println("Les résultats sont identiques !\n");
        } else {
            Set<String> missing = new HashSet<>(integraalSet);
            missing.removeAll(mySet);
            Set<String> extra = new HashSet<>(mySet);
            extra.removeAll(integraalSet);

            if (!missing.isEmpty()) System.out.println("Missing: " + missing);
            if (!extra.isEmpty()) System.out.println("Extra: " + extra);
            System.out.println();
        }
    }
}
