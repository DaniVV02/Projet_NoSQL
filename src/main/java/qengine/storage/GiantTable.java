package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;


public class GiantTable implements RDFStorage {

    private final Dictionary dict = new Dictionary();
    private final Set<RDFTriple> triples = new LinkedHashSet<>();

    @Override
    public boolean add(RDFTriple triple) {
        // ici on encode les 3 termes (même si on ne stocke ici que le RDFTriple, ça remplit le dictionnaire)
        dict.encode(triple.getTripleSubject());
        dict.encode(triple.getTriplePredicate());
        dict.encode(triple.getTripleObject());

        // cette partie c'ets pour eviter les doublons donc si il existe deja on le rajoute pas
        if (triples.contains(triple)) {
            return false;
        }
        triples.add(triple);
        return true;
    }

    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();

        for (RDFTriple triple : triples) {
            Map<Variable, Term> env = new HashMap<>();

            if (unifyTerm(pattern.getTripleSubject(), triple.getTripleSubject(), env)
                    && unifyTerm(pattern.getTriplePredicate(), triple.getTriplePredicate(), env)
                    && unifyTerm(pattern.getTripleObject(), triple.getTripleObject(), env)) {

                SubstitutionImpl substitution = new SubstitutionImpl();

                for (Map.Entry<Variable, Term> e : env.entrySet()) {
                    substitution.add(e.getKey(), e.getValue());
                }
                results.add(substitution);
            }
        }
        return results.iterator();
    }

    private boolean unifyTerm(Term patternTerm, Term dataTerm, Map<Variable, Term> env) {

        if (patternTerm instanceof Variable v) {
            Term already = env.get(v);

            if (already != null) {
                // si la variable est déjà liée, on vérifie la cohérence
                return already.equals(dataTerm);
            } else {
                // sinon, on lie la variable à la nouvelle valeur
                env.put(v, dataTerm);
                return true;
            }

        } else {
            // si c’est une constante, il faut que les deux soient égales
            return patternTerm.equals(dataTerm);
        }
    }


    @Override
    public Iterator<Substitution> match(StarQuery q) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long howMany(RDFTriple triple) {
        long count = 0;
        Iterator<Substitution> it = match(triple);
        while (it.hasNext()) { it.next(); count++; }
        return count;
    }

    @Override
    public long size() {
        return triples.size();
    }

    @Override
    public Collection<RDFTriple> getAtoms() {
        return Collections.unmodifiableSet(triples);
    }
}
