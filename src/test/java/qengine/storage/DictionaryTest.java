package qengine.storage;

import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.factory.api.TermFactory;
import fr.boreal.model.logicalElements.factory.impl.SameObjectTermFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DictionaryTest {

    private final TermFactory termFactory = SameObjectTermFactory.instance();

    @Test
    void testEncodeAndDecode() {
        Dictionary dict = new Dictionary();
        System.out.println("-- Test du dictionary --");
        // ici on creer les 3 terme de test ( BOB, Knows , ALICE) de type (s, p,o)
        Term bob = termFactory.createOrGetLiteral("Bob");
        Term knows = termFactory.createOrGetLiteral("knows");
        Term alice = termFactory.createOrGetLiteral("Alice");
        System.out.println("Creation des termes :");
        System.out.println("Bob -> " + bob);
        System.out.println("knows -> " + knows);
        System.out.println("Alice -> " + alice);

        // encodage : on test que l'encodage des term en id se passe bien
        int idBob = dict.encode(bob);
        int idKnows = dict.encode(knows);
        int idAlice = dict.encode(alice);
        System.out.println("\nEncodage :");
        System.out.println("Bob = " + idBob);
        System.out.println("knows = " + idKnows);
        System.out.println("Alice = " + idAlice);

        // on re enocde le meme terme pour voir si l'ID reste le même
        int idBob2 = dict.encode(bob);
        System.out.println("\nRé-encodage de Bob : " + idBob2 + " (doit être le même que le précédent)");

        // Décodage : on transforme les IDs en Term
        System.out.println("\nDécodage :");
        System.out.println("ID " + idBob + " -> " + dict.decode(idBob));
        System.out.println("ID " + idKnows + " -> " + dict.decode(idKnows));
        System.out.println("ID " + idAlice + " -> " + dict.decode(idAlice));

        // Verification automatique avve Junite
        assertEquals(idBob, idBob2, "Bob doit avoir le même ID à chaque encodage");
        assertEquals(idBob, dict.encode(bob));
        assertNotEquals(idBob, idKnows);
        assertNotEquals(idBob, idAlice);
        assertNotEquals(idKnows, idAlice);

        // Vérifie le décodage (id -> terme)
        assertEquals(bob, dict.decode(idBob));
        assertEquals(knows, dict.decode(idKnows));
        assertEquals(alice, dict.decode(idAlice));

        // Vérifie la taille du dictionnaire
        System.out.println("\nTaille finale du dictionnaire : " + dict.size());
        assertEquals(3, dict.size());
    }

    @Test
    void testEncodeIfExists() {
        Dictionary dict = new Dictionary();
        var termFactory = SameObjectTermFactory.instance();

        Term bob = termFactory.createOrGetLiteral("Bob");

        // Cas 1 : le terme n'existe pas encore → doit renvoyer -1
        int idUnknown = dict.encodeIfExists(bob);
        System.out.println("Avant encodage, Bob → " + idUnknown);
        assertEquals(-1, idUnknown, "encodeIfExists() doit renvoyer -1 si le terme n'existe pas");

        // On encode maintenant "Bob"
        int idBob = dict.encode(bob);
        System.out.println("Encodage de Bob, nouvel ID = " + idBob);

        // Cas 2 : le terme existe maintenant → doit renvoyer le même ID
        int idBob2 = dict.encodeIfExists(bob);
        System.out.println("Après encodage, Bob → " + idBob2);

        assertEquals(idBob, idBob2, "encodeIfExists() doit renvoyer le même ID si le terme existe déjà");

    }
}
