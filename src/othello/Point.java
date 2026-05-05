package othello;

/**
 * Représente une position sur le plateau sous forme de coordonnées (ligne,
 * colonne).
 * On utilise cette classe partout dans le jeu pour désigner un coup ou une case
 * :
 * - quand un joueur choisit où jouer
 * - quand l'IA retourne la case où elle veut jouer
 * - quand on liste les coups valides
 *
 * On aurait pu utiliser un simple tableau int[2], mais une classe dédiée
 * rend le code beaucoup plus lisible (p.getLigne() vs p[0]).
 */
public class Point {

    private int ligne;
    private int colonne;

    /**
     * Crée un point avec ses coordonnées sur le plateau.
     * 
     * @param ligne   : la ligne (0 = haut, 7 = bas)
     * @param colonne : la colonne (0 = gauche, 7 = droite)
     */
    public Point(int ligne, int colonne) {
        this.ligne = ligne;
        this.colonne = colonne;
    }

    /** Retourne la ligne de ce point. */
    public int getLigne() {
        return ligne;
    }

    /** Retourne la colonne de ce point. */
    public int getColonne() {
        return colonne;
    }

    /**
     * Affichage lisible d'un point, pour déboguer.
     * Ex: affiche "Vous etes sur la ligne 3 et la colonne 4"
     */
    /*
     * @Override
     * public String toString() {
     * return "Vous etes sur la ligne " + ligne + " et la colonne " + colonne;
     * }
     */
}