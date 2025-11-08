package qengine.storage;

import fr.boreal.model.logicalElements.api.Term;

import java.util.HashMap;
import java.util.Map;

public class Dictionary {

    private final Map<Term, Integer> term2id = new HashMap<>();
    private final Map<Integer, Term> id2term = new HashMap<>();
    private int nextId = 1; // on commence à 1 pour éviter 0 comme valeur "spéciale"

    /** renvoie l’ID d’un terme :
     * s’il n’existe pas, on crée un nouvel ID, on l'insère dans les deux maps, et puis on renvoie l’ID. */
    public int encode(Term t) {
        Integer id = term2id.get(t);
        if (id != null) return id;
        int nid = nextId++;
        term2id.put(t, nid);
        id2term.put(nid, t);
        return nid;
    }

    /** renvoie l’ID seulement si le terme est déjà connu, sinon -1 */
    public int encodeIfExists(Term t) {
        return term2id.getOrDefault(t, -1);
    }

    /** Retourne le Term pour un id, ou null si absent. */
    public Term decode(int id) {
        return id2term.get(id);
    }

    public int size() {
        return term2id.size();
    }

    /** public boolean contains(Term t) {
        return term2id.containsKey(t);
    }
    */

}
