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

        boolean added = insert(spo, s, p, o)
                | insert(sop, s, o, p)
                | insert(pso, p, s, o)
                | insert(pos, p, o, s)
                | insert(osp, o, s, p)
                | insert(ops, o, p, s);

        if (added) tripleCount++;
        return added;
    }

    private boolean insert(Map<Integer, Map<Integer, Set<Integer>>> index, int a, int b, int c) {
        return index
                .computeIfAbsent(a, k -> new HashMap<>())
                .computeIfAbsent(b, k -> new HashSet<>())
                .add(c);
    }


    private Term subjectOf(RDFTriple t) {
        return t.getTripleSubject();
    }

    private Term predicateOf(RDFTriple t) {
        return t.getTriplePredicate();
    }

    private Term objectOf(RDFTriple t) {
        return t.getTripleObject();
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
            matchFromIndex(spo.getOrDefault(s, Map.of()), p, oTerm, results, sTerm, pTerm);
        } else if (p != null && o != null) {
            matchFromIndex(pos.getOrDefault(p, Map.of()), o, sTerm, results, pTerm, oTerm);
        } else if (s != null && o != null) {
            matchFromIndex(sop.getOrDefault(s, Map.of()), o, pTerm, results, sTerm, oTerm);
        } else {
            // sinon → fallback : scan complet
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
                                Term unknownTerm, List<Substitution> results,
                                Term known1, Term known2) {

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
    public long howMany(RDFTriple triple) {
        return match(triple).hasNext() ? 1 : 0;
    }

    @Override
    public long size() {
        return tripleCount;
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        // On reconstruit les triplets à partir du dictionnaire
        List<RDFTriple> triples = new ArrayList<>();
        for (var sEntry : spo.entrySet()) {
            for (var pEntry : sEntry.getValue().entrySet()) {
                for (int o : pEntry.getValue()) {
                    triples.add(new RDFTriple(
                            dict.decode(sEntry.getKey()),
                            dict.decode(pEntry.getKey()),
                            dict.decode(o)
                    ));
                }
            }
        }
        return triples;
    }

    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new UnsupportedOperationException("Non demandé pour le rendu du 15 novembre.");
    }

}
