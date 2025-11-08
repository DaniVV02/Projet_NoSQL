package qengine.storage;

import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import fr.boreal.model.logicalElements.api.*;

import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;

import java.util.Iterator;
import java.util.*;


public class GiantTableTest {

    private GiantTable table;
    private TermFactory termFactory;

    private Term bob, alice, knows, likes, pizza;

    @BeforeEach
    void setUp() {
        table = new GiantTable();
        termFactory = SameObjectTermFactory.instance();

        // Création de quelques termes RDF
        bob = termFactory.createOrGetLiteral("Bob");
        alice = termFactory.createOrGetLiteral("Alice");
        knows = termFactory.createOrGetLiteral("knows");
        likes = termFactory.createOrGetLiteral("likes");
        pizza = termFactory.createOrGetLiteral("Pizza");

        // Ajout de triplets à la table
        table.add(new RDFTriple(bob, knows, alice));
        table.add(new RDFTriple(alice, knows, bob));
        table.add(new RDFTriple(bob, likes, pizza));
    }

    @Test
    void testAddAndSize() {
        assertEquals(3, table.size(), "Il devrait y avoir 3 triplets dans la table.");
        // ajout d'un doublon → ne change pas la taille
        boolean added = table.add(new RDFTriple(bob, knows, alice));
        assertFalse(added, "Un doublon ne doit pas être ajouté.");
        assertEquals(3, table.size(), "La taille doit rester à 3 après ajout d'un doublon.");
    }

    @Test
    void testMatchExact() {
        // pattern exactement identique à un triplet
        RDFTriple pattern = new RDFTriple(bob, knows, alice);
        Iterator<?> results = table.match(pattern);
        assertTrue(results.hasNext(), "Le triplet exact doit être trouvé.");
        results.next();
        assertFalse(results.hasNext(), "Il ne doit y avoir qu’un seul résultat.");
    }

    @Test
    void testMatchWithVariables() {
        // pattern avec une variable ?x
        var v = termFactory.createOrGetVariable("?x");
        RDFTriple pattern = new RDFTriple(v, knows, alice);

        Iterator<?> results = table.match(pattern);
        assertTrue(results.hasNext(), "Au moins un triplet devrait correspondre à ?x knows Alice.");

        var substitution = (SubstitutionImpl) results.next();

        var map = substitution.toMap();

        assertTrue(map.containsKey(v), "La substitution doit contenir la variable ?x.");
        assertEquals(bob, map.get(v), "La variable ?x doit être liée à Bob.");

    }


    @Test
    void testMatchMultipleResults() {
        // pattern avec 2 variables : ?x knows ?y
        var x = termFactory.createOrGetVariable("?x");
        var y = termFactory.createOrGetVariable("?y");

        RDFTriple pattern = new RDFTriple(x, knows, y);
        Iterator<?> results = table.match(pattern);

        int count = 0;
        while (results.hasNext()) {
            results.next();
            count++;
        }

        assertEquals(2, count, "Deux triplets devraient correspondre à ?x knows ?y.");
    }

    @Test
    void testMatchNoResult() {
        // pattern sans correspondance
        RDFTriple pattern = new RDFTriple(alice, likes, pizza);
        Iterator<?> results = table.match(pattern);
        assertFalse(results.hasNext(), "Aucun triplet ne correspond à Alice likes Pizza.");
    }

    @Test
    void testHowMany() {
        long count = table.howMany(new RDFTriple(bob, knows, alice));
        assertEquals(1, count, "Bob knows Alice doit apparaître une fois.");
    }

    @Test
    void testGetAtoms() {
        var atoms = table.getAtoms();
        assertEquals(3, atoms.size(), "La collection retournée doit contenir 3 triplets.");
        assertThrows(UnsupportedOperationException.class, () -> {
            atoms.clear(); // ne doit pas être modifiable
        });
    }
}
