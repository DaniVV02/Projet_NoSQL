package qengine.storage;

import fr.boreal.model.logicalElements.api.Literal;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DictionaryTest {

    private final SameObjectTermFactory tf = SameObjectTermFactory.instance();

    @Test
    void encodeAssignsStableIdsAndDecodesBack() {
        Dictionary d = new Dictionary();

        Term bob = tf.createOrGetLiteral("Bob");
        Term knows = tf.createOrGetLiteral("knows");
        Term alice = tf.createOrGetLiteral("Alice");

        int idBob1 = d.encode(bob);
        int idKnows = d.encode(knows);
        int idAlice = d.encode(alice);
        int idBob2 = d.encode(bob);   // doit être le même que idBob1

        assertEquals(idBob1, idBob2, "Encode doit être stable pour le même terme");
        assertNotEquals(idBob1, idKnows);
        assertNotEquals(idBob1, idAlice);

        assertEquals(bob, d.decode(idBob1));
        assertEquals(knows, d.decode(idKnows));
        assertEquals(alice, d.decode(idAlice));

        assertEquals(3, d.size(), "Il doit y avoir 3 entrées uniques");
    }

    @Test
    void encodeIfExistsAndContains() {
        Dictionary d = new Dictionary();

        Term t1 = tf.createOrGetLiteral("t1");
        Term t2 = tf.createOrGetLiteral("t2");

        assertEquals(-1, d.encodeIfExists(t1), "Inconnu -> -1");
        assertFalse(d.contains(t1));

        int id1 = d.encode(t1);
        assertTrue(id1 > 0);
        assertEquals(id1, d.encodeIfExists(t1));
        assertTrue(d.contains(t1));

        assertEquals(-1, d.encodeIfExists(t2));
        assertFalse(d.contains(t2));
    }

    @Test
    void idsStartAtOne() {
        Dictionary d = new Dictionary();
        Literal<String> a = tf.createOrGetLiteral("a");
        assertEquals(1, d.encode(a), "Par défaut on commence à 1");
    }
}
