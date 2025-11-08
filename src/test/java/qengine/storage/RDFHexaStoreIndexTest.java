package qengine.storage;

import fr.boreal.model.logicalElements.factory.api.TermFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import qengine.model.RDFTriple;

import java.util.Iterator;

/**
 * Tests unitaires pour la classe RDFHexaStore.
 * On vérifie ici l'insertion, l'encodage, le décodage et la recherche
 * à travers les 6 index du Hexastore.
 */
public class RDFHexaStoreIndexTest {

    private RDFHexaStore store;
    private TermFactory factory;

    // quelques termes RDF pour les tests
    private final String S1 = "Bob";
    private final String S2 = "Alice";
    private final String P1 = "knows";
    private final String P2 = "likes";
    private final String O1 = "Alice";
    private final String O2 = "Pizza";

    @BeforeEach
    void setUp() {
        factory = SameObjectTermFactory.instance();
        store = new RDFHexaStore();

        var bob = factory.createOrGetLiteral(S1);
        var alice = factory.createOrGetLiteral(S2);
        var knows = factory.createOrGetLiteral(P1);
        var likes = factory.createOrGetLiteral(P2);
        var pizza = factory.createOrGetLiteral(O2);

        // insertion de quelques triplets
        store.add(new RDFTriple(bob, knows, alice));   // Bob knows Alice
        store.add(new RDFTriple(alice, knows, bob));   // Alice knows Bob
        store.add(new RDFTriple(bob, likes, pizza));   // Bob likes Pizza
    }

    @Test
    void testAddAndSize() {
        assertEquals(3, store.size(), "Le store doit contenir 3 triplets.");
    }

    @Test
    void testEncodeDecodeDictionnaire() {
        // vérifie que les mêmes termes ont le même ID dans le dictionnaire interne
        var d = new Dictionary();
        var bob = factory.createOrGetLiteral("Bob");
        var id1 = d.encode(bob);
        var id2 = d.encode(bob);
        assertEquals(id1, id2, "Le même terme doit toujours avoir le même ID.");
        assertEquals(bob, d.decode(id1), "Le décodage doit retrouver le terme d'origine.");
    }

    @Test
    void testMatchExactTriplet() {
        var bob = factory.createOrGetLiteral(S1);
        var knows = factory.createOrGetLiteral(P1);
        var alice = factory.createOrGetLiteral(S2);

        RDFTriple pattern = new RDFTriple(bob, knows, alice);
        Iterator<Substitution> it = store.match(pattern);

        assertTrue(it.hasNext(), "Le triplet exact doit être trouvé.");
        assertNotNull(it.next());
        assertFalse(it.hasNext(), "Il ne doit y avoir qu'un seul match.");
    }

    @Test
    void testMatchVariableSujet() {
        var v = factory.createOrGetVariable("?x");
        var knows = factory.createOrGetLiteral(P1);
        var alice = factory.createOrGetLiteral(S2);

        RDFTriple pattern = new RDFTriple(v, knows, alice);
        Iterator<Substitution> it = store.match(pattern);

        assertTrue(it.hasNext(), "Au moins un triplet doit correspondre à ?x knows Alice.");

        var substitution = (SubstitutionImpl) it.next();
        var map = substitution.toMap();
        assertEquals(factory.createOrGetLiteral(S1), map.get(v),
                "La variable ?x doit être liée à Bob.");
    }

    @Test
    void testMatchVariableObjet() {
        var bob = factory.createOrGetLiteral(S1);
        var knows = factory.createOrGetLiteral(P1);
        var v = factory.createOrGetVariable("?y");

        RDFTriple pattern = new RDFTriple(bob, knows, v);
        Iterator<Substitution> it = store.match(pattern);

        int count = 0;
        while (it.hasNext()) {
            var substitution = (SubstitutionImpl) it.next();
            var map = substitution.toMap();
            assertTrue(map.containsKey(v), "La substitution doit contenir la variable ?y.");
            count++;
        }
        assertEquals(1, count, "Bob knows ?y doit donner 1 correspondance.");
    }

    @Test
    void testMatchParPredicat() {
        var p = factory.createOrGetLiteral("likes");
        var x = factory.createOrGetVariable("?x");
        var y = factory.createOrGetVariable("?y");

        RDFTriple pattern = new RDFTriple(x, p, y);
        Iterator<Substitution> it = store.match(pattern);

        int count = 0;
        while (it.hasNext()) {
            var sub = (SubstitutionImpl) it.next();
            assertFalse(sub.toMap().isEmpty());
            count++;
        }

        assertEquals(1, count, "likes doit apparaître une fois dans la base.");
    }

    @Test
    void testNoResult() {
        var bob = factory.createOrGetLiteral(S1);
        var livesIn = factory.createOrGetLiteral("lives_in");
        var paris = factory.createOrGetLiteral("Paris");

        RDFTriple pattern = new RDFTriple(bob, livesIn, paris);
        Iterator<Substitution> it = store.match(pattern);
        assertFalse(it.hasNext(), "Aucun triplet ne correspond à Bob lives_in Paris.");
    }

    @Test
    void testHowMany() {
        var bob = factory.createOrGetLiteral(S1);
        var knows = factory.createOrGetLiteral(P1);
        var alice = factory.createOrGetLiteral(S2);

        long count = store.howMany(new RDFTriple(bob, knows, alice));
        assertEquals(1, count, "Bob knows Alice doit être présent une seule fois.");
    }

    @Test
    void testGetAtoms() {
        var atoms = store.getAtoms();
        assertEquals(3, atoms.size(), "Le store doit restituer 3 triplets RDF.");
        assertThrows(UnsupportedOperationException.class, () -> {
            atoms.clear(); // ne doit pas être modifiable
        });
    }
}
