package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import org.apache.commons.lang3.NotImplementedException;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;

/**
 * Implémentation d'un HexaStore pour stocker des RDFAtom.
 * Cette classe utilise six index pour optimiser les recherches.
 * Les index sont basés sur les combinaisons (Sujet, Prédicat, Objet), (Sujet, Objet, Prédicat),
 * (Prédicat, Sujet, Objet), (Prédicat, Objet, Sujet), (Objet, Sujet, Prédicat) et (Objet, Prédicat, Sujet).
 */
public class RDFHexaStore implements RDFStorage {

    private final Dictionary dict = new Dictionary();
    private final Set<RDFTriple> triples = new LinkedHashSet<>();

    @Override
    public boolean add(RDFTriple triple) {
        dict.encode(subjectOf(triple));
        dict.encode(predicateOf(triple));
        dict.encode(objectOf(triple));

        return triples.add(triple);
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
    public long size() {
        return triples.size();
    }

    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();

        for (RDFTriple triple : triples) { // on parcourt toute la base
            Map<Variable, Term> env = new HashMap<>();

            // on teste les 3 positions du triplet (sujet, prédicat, objet)
            if (unifyTerm(pattern.getTripleSubject(),   triple.getTripleSubject(),   env) &&
                    unifyTerm(pattern.getTriplePredicate(), triple.getTriplePredicate(), env) &&
                    unifyTerm(pattern.getTripleObject(),    triple.getTripleObject(),    env)) {

                // si les 3 positions correspondent → on a trouvé un match
                SubstitutionImpl s = new SubstitutionImpl();
                for (Map.Entry<Variable, Term> e : env.entrySet()) {
                    s.add(e.getKey(), e.getValue());
                }
                results.add(s);
            }
        }
        return results.iterator();
    }

    private boolean unifyTerm(Term pattern, Term data, Map<Variable, Term> env) {
        if (pattern instanceof Variable v) {
            // cas : motif = variable → on lie ou on vérifie la cohérence
            Term dejaLie = env.get(v);
            if (dejaLie != null) return dejaLie.equals(data);
            env.put(v, data);
            return true;
        } else {
            // cas: motif = constante → égalité stricte
            return pattern.equals(data);
        }
    }



    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new NotImplementedException();
    }

    @Override
    public long howMany(RDFTriple triple) {
        long c = 0;
        Iterator<Substitution> it = match(triple);
        while (it.hasNext()) { it.next(); c++; }
        return c;
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return Collections.unmodifiableCollection(triples);
    }
}
