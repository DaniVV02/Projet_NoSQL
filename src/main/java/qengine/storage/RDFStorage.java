package qengine.storage;


import java.util.*;
import java.util.stream.Stream;

import fr.boreal.model.logicalElements.api.Substitution;
import fr.boreal.model.logicalElements.api.Term;
import fr.boreal.model.logicalElements.api.Variable;
import fr.boreal.model.logicalElements.impl.SubstitutionImpl;
import qengine.model.RDFTriple;
import qengine.model.StarQuery;

/**
 * Contrat pour un système de stockage de données RDF
 */
public interface RDFStorage {

    /**
     * Ajoute un RDFAtom dans le store.
     *
     * @param t le triplet à ajouter
     * @return true si le RDFAtom a été ajouté avec succès, false s'il est déjà présent
     */
    boolean add(RDFTriple t);

    /**
     * @param a atom
     * @return un itérateur de substitutions correspondant aux match des atomes
     *          (i.e., sur quels termes s'envoient les variables)
     */
    Iterator<Substitution> match(RDFTriple a);


    /**
     * @param q star query
     * @return an itérateur de subsitutions décrivrant les réponses à la requete
     */
    //Iterator<Substitution> match(StarQuery q);
    default Iterator<Substitution> match(StarQuery q) {

        Variable center = q.getCentralVariable();
        List<RDFTriple> patterns = q.getRdfAtoms();

        if (patterns.isEmpty()) {
            return Collections.emptyIterator();
        }

        // 1) Solutions du premier patron
        Set<Term> candidats = new HashSet<>();

        Iterator<Substitution> it = match(patterns.get(0));
        while (it.hasNext()) {
            Substitution sub = it.next();
            Term value = sub.toMap().get(center);
            if (value != null) {
                candidats.add(value);
            }
        }

        if (candidats.isEmpty()) {
            return Collections.emptyIterator();
        }

        // 2) Filtrage successif
        for (int i = 1; i < patterns.size(); i++) {

            RDFTriple patron = patterns.get(i);
            Set<Term> ok = new HashSet<>();

            for (Term val : candidats) {

                // Instanciation manuelle SANS toucher RDFTriple
                Term s = patron.getTripleSubject();
                Term p = patron.getTriplePredicate();
                Term o = patron.getTripleObject();

                if (s.equals(center)) s = val;
                if (p.equals(center)) p = val;
                if (o.equals(center)) o = val;

                RDFTriple instancie = new RDFTriple(s, p, o);

                // Vérification via howMany (sélectivité)
                if (howMany(instancie) > 0) {
                    ok.add(val);
                }
            }

            // Intersection
            candidats.retainAll(ok);

            if (candidats.isEmpty()) {
                return Collections.emptyIterator();
            }
        }

        // 3) Création des substitutions de sortie
        List<Substitution> results = new ArrayList<>();

        for (Term val : candidats) {
            SubstitutionImpl s = new SubstitutionImpl();
            s.add(center, val);
            results.add(s);
        }

        return results.iterator();
    }


    /**
     * @param a atom
     * @return
     */
    long howMany(RDFTriple a);


    /**
     * Retourne le nombre d'atomes dans le Store.
     *
     * @return le nombre d'atomes
     */
    long size();

    /**
     * Retourne une collections contenant tous les atomes du store.
     * Utile pour les tests unitaires.
     *
     * @return une collection d'atomes
     */
    Collection<RDFTriple> getAtoms();

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Stream<RDFTriple> atoms) {
        return atoms.map(this::add).reduce(Boolean::logicalOr).orElse(false);
    }

    /**
     * Ajoute des RDFAtom dans le store.
     *
     * @param atoms les RDFAtom à ajouter
     * @return true si au moins un RDFAtom a été ajouté, false s'ils sont tous déjà présents
     */
    default boolean addAll(Collection<RDFTriple> atoms) {
        return this.addAll(atoms.stream());
    }
}
