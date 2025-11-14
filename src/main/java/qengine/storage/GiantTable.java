package qengine.storage;

import fr.boreal.model.logicalElements.api.*;
import fr.boreal.model.logicalElements.impl.ConstantImpl;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

import java.util.*;


public class GiantTable implements RDFStorage {

    private final Dictionary dict = new Dictionary();
    private final Set<List<Integer>> triples = new LinkedHashSet<>();

    @Override
    public boolean add(RDFTriple triple) {
        int s = dict.encode(triple.getTripleSubject());
        int p = dict.encode(triple.getTriplePredicate());
        int o = dict.encode(triple.getTripleObject());

        List<Integer> encoded = Arrays.asList(s, p, o);
        System.out .println(encoded);
        if (triples.contains(encoded)) {
            return false;
        }

        triples.add(encoded);
        System.out .println(triples);
        return true;
    }

    @Override
    public Iterator<Substitution> match(RDFTriple pattern) {
        List<Substitution> results = new ArrayList<>();

        // On décode le pattern si besoin
        int sPatternId = dict.encodeIfExists(pattern.getTripleSubject());
        int pPatternId = dict.encodeIfExists(pattern.getTriplePredicate());
        int oPatternId = dict.encodeIfExists(pattern.getTripleObject());

        boolean sIsVar = pattern.getTripleSubject() instanceof Variable;
        boolean pIsVar = pattern.getTriplePredicate() instanceof Variable;
        boolean oIsVar = pattern.getTripleObject() instanceof Variable;

        for (List<Integer> encodedTriple : triples) {
            int s = encodedTriple.get(0);
            int p = encodedTriple.get(1);
            int o = encodedTriple.get(2);

            Map<Variable, Term> env = new HashMap<>();
            boolean match = true;

            // Sujet
            if (sIsVar) {
                env.put((Variable) pattern.getTripleSubject(), dict.decode(s));
            } else if (sPatternId != -1 && sPatternId != s) {
                match = false;
            }

            // Prédicat
            if (pIsVar) {
                env.put((Variable) pattern.getTriplePredicate(), dict.decode(p));
            } else if (pPatternId != -1 && pPatternId != p) {
                match = false;
            }

            // Objet
            if (oIsVar) {
                env.put((Variable) pattern.getTripleObject(), dict.decode(o));
            } else if (oPatternId != -1 && oPatternId != o) {
                match = false;
            }

            if (match) {
                SubstitutionImpl substitution = new SubstitutionImpl();
                for (Map.Entry<Variable, Term> e : env.entrySet()) {
                    substitution.add(e.getKey(), e.getValue());
                }
                results.add(substitution);
            }
        }

        return results.iterator();
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
        List<RDFTriple> decodedTriples = new ArrayList<>();

        for (List<Integer> ids : triples) {
            Term s = dict.decode(ids.get(0));
            Term p = dict.decode(ids.get(1));
            Term o = dict.decode(ids.get(2));
            decodedTriples.add(new RDFTriple(s, p, o));
        }

        return Collections.unmodifiableList(decodedTriples);
    }
}
