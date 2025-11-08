package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;
//a refaire 
public class GiantTable implements RDFStorage {

    private final Set<RDFTriple> triples = new LinkedHashSet<>();

    @Override
    public boolean add(RDFTriple triple) {
        return triples.add(triple);
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
                env.forEach(substitution::add);
                results.add(substitution);
            }
        }
        return results.iterator();
    }

    private boolean unifyTerm(Term pattern, Term data, Map<Variable, Term> env) {
        if (pattern instanceof Variable v) {
            Term bound = env.get(v);
            if (bound != null) return bound.equals(data);
            env.put(v, data);
            return true;
        } else {
            return pattern.equals(data);
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
