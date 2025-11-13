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

    private static final Literal<String> SUBJECT_1   = SameObjectTermFactory.instance().createOrGetLiteral("subject1");
    private static final Literal<String> PREDICATE_1 = SameObjectTermFactory.instance().createOrGetLiteral("predicate1");
    private static final Literal<String> OBJECT_1    = SameObjectTermFactory.instance().createOrGetLiteral("object1");
    private static final Literal<String> SUBJECT_2   = SameObjectTermFactory.instance().createOrGetLiteral("subject2");
    private static final Literal<String> OBJECT_2    = SameObjectTermFactory.instance().createOrGetLiteral("object2");
    private static final Literal<String> OBJECT_3    = SameObjectTermFactory.instance().createOrGetLiteral("object3");
    private static final Variable VAR_X              = SameObjectTermFactory.instance().createOrGetVariable("?x");


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
        // ajout d'un doublon -> ne change pas la taille
        boolean added = table.add(new RDFTriple(bob, knows, alice));
        assertFalse(added, "Un doublon ne doit pas être ajouté.");
        assertEquals(3, table.size(), "La taille doit rester à 3 après ajout d'un doublon.");
    }

    @Test
    public void testAddDuplicateAtom() {

        RDFTriple t1 = new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1);

        assertTrue(table.add(t1), "Premier ajout doit renvoyer true");
        assertFalse(table.add(t1), "Deuxième ajout du même triplet doit renvoyer false");
        assertEquals(4, table.size(), "la taille doit etre 4 car 3 de la table pour l'ajout de t1");

        System.out.println("Taille finale = " + table.size());
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

        Iterator<Substitution> results = table.match(pattern);
        assertTrue(results.hasNext(), "Au moins un triplet devrait correspondre à ?x knows Alice.");

        Substitution substitution = results.next();

        Substitution expected = new SubstitutionImpl();
        expected.add(v, bob);

        assertEquals(expected, substitution, "La variable ?x doit être liée à Bob.");
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
    public void testHowMany() {
        GiantTable store = new GiantTable();

        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_1));
        store.add(new RDFTriple(SUBJECT_2, PREDICATE_1, OBJECT_2));
        store.add(new RDFTriple(SUBJECT_1, PREDICATE_1, OBJECT_3));

        RDFTriple pattern = new RDFTriple(SUBJECT_1, PREDICATE_1, VAR_X);

        long count = store.howMany(pattern);
        System.out.println("howMany()= " + count);

        assertEquals(2, count, "On doit avoir 2 résultats");
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
