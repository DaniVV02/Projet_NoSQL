package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStoreIndex implements RDFStorage {

    private final Dictionary dict = new Dictionary();

    private final Set<List<Integer>> encodedTriples = new LinkedHashSet<>();

    // === Six index ===
    private final Map<Integer, Map<Integer, Set<Integer>>> spo = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> sop = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> pso = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> pos = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> osp = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<Integer>>> ops = new HashMap<>();

    private long tripleCount = 0;

    @Override
    public boolean add(RDFTriple triple) {
        int s = dict.encode(triple.getTripleSubject());
        int p = dict.encode(triple.getTriplePredicate());
        int o = dict.encode(triple.getTripleObject());

        // Triplet encodé [s, p, o] comme dans GiantTable
        List<Integer> encoded = Arrays.asList(s, p, o);

        // Si déjà présent → on ne touche pas aux index, on ne compte pas
        if (encodedTriples.contains(encoded)) {
            return false;
        }

        // Nouveau triplet → on l’ajoute dans le set encodé
        encodedTriples.add(encoded);

        // Puis on met à jour les 6 index (on sait que c'est un nouveau (s,p,o))
        insert(spo, s, p, o);  // SPO
        insert(sop, s, o, p);  // SOP
        insert(pso, p, s, o);  // PSO
        insert(pos, p, o, s);  // POS
        insert(osp, o, s, p);  // OSP
        insert(ops, o, p, s);  // OPS

        // On incrémente le compteur logique de triplets
        tripleCount++;

        return true;
    }


    private boolean insert(Map<Integer, Map<Integer, Set<Integer>>> index, int a, int b, int c) {
        return index
                .computeIfAbsent(a, k -> new HashMap<>())
                .computeIfAbsent(b, k -> new HashSet<>())
                .add(c);
    }


    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();

        Term sTerm = pattern.getTripleSubject();
        Term pTerm = pattern.getTriplePredicate();
        Term oTerm = pattern.getTripleObject();

        Integer s = (sTerm instanceof Variable) ? null : dict.encodeIfExists(sTerm);
        Integer p = (pTerm instanceof Variable) ? null : dict.encodeIfExists(pTerm);
        Integer o = (oTerm instanceof Variable) ? null : dict.encodeIfExists(oTerm);

        if (s != null && p != null && o != null) {
            // cas exact : tous connus
            if (exists(spo, s, p, o)) {
                SubstitutionImpl sub = new SubstitutionImpl();
                results.add(sub);
            }
            return results.iterator();
        }

        // on choisit l’index optimal
        if (s != null && p != null) {
            matchFromIndex(spo.getOrDefault(s, Map.of()), p, oTerm, results);
        } else if (p != null && o != null) {
            matchFromIndex(pos.getOrDefault(p, Map.of()), o, sTerm, results);
        } else if (s != null && o != null) {
            matchFromIndex(sop.getOrDefault(s, Map.of()), o, pTerm, results);
        } else {
            // sinon -> fallback : scan complet
            results.addAll(fullScan(pattern));
        }

        return results.iterator();
    }

    private boolean exists(Map<Integer, Map<Integer, Set<Integer>>> index, int a, int b, int c) {
        return index.containsKey(a)
                && index.get(a).containsKey(b)
                && index.get(a).get(b).contains(c);
    }

    private void matchFromIndex(Map<Integer, Set<Integer>> level2, Integer key,
                                Term unknownTerm, List<Substitution> results) {

        if (!level2.containsKey(key)) return;

        for (int val : level2.get(key)) {
            Map<Variable, Term> env = new HashMap<>();

            // selon quel terme est une variable
            if (unknownTerm instanceof Variable v) {
                env.put(v, dict.decode(val));
            }

            SubstitutionImpl s = new SubstitutionImpl();
            env.forEach(s::add);
            results.add(s);
        }
    }

    private List<Substitution> fullScan(RDFTriple pattern) {
        // on re utilise giant table
        GiantTable giantTable = new GiantTable();
        // on remplit la GiantTable avec tous les triplets décodés
        giantTable.addAll(this.getAtoms());
        // on réutilise sa fonction match
        Iterator<Substitution> it = giantTable.match(pattern);
        List<Substitution> results = new ArrayList<>();
        it.forEachRemaining(results::add);
        return results;
    }


    @Override
    public long howMany(RDFTriple pattern) {
        long count = 0;
        Iterator<Substitution> it = match(pattern);
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }


    @Override
    public long size() {
        return tripleCount;
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        // On reconstruit les RDFTriple à partir des IDs encodés
        List<RDFTriple> decoded = new ArrayList<>();
        for (List<Integer> ids : encodedTriples) {
            Term s = dict.decode(ids.get(0));
            Term p = dict.decode(ids.get(1));
            Term o = dict.decode(ids.get(2));
            decoded.add(new RDFTriple(s, p, o));
        }
        return Collections.unmodifiableList(decoded);
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new UnsupportedOperationException("Non demandé pour le rendu du 15 novembre.");
    }

    public void printEncodedTriples() {
        System.out.println("=== Encoded Triples (s, p, o) ===");
        for (List<Integer> triple : encodedTriples) {
            System.out.println("(" + triple.get(0) + ", " + triple.get(1) + ", " + triple.get(2) + ")");
        }
    }

}
