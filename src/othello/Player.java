package othello;

/**
 * Interface commune à tous les types de joueurs du jeu.
 *
 *
 *
 * Les classes qui implémentent cette interface sont :
 * - HumanPlayer : le joueur humain (saisit via le clavier)
 * - GUIPlayer : le joueur humain via l'interface graphique (clic souris)
 * - AIPlayer : l'intelligence artificielle (calcule le meilleur coup)
 */
public interface Player {

    /**
     * Demande au joueur de choisir un coup.
     *
     * @param state L'état actuel du jeu (plateau, joueur courant, coups valides...)
     * @return Le Point (ligne, colonne) choisi par le joueur.
     *
     *         Selon le type de joueur, cette méthode fait des choses très
     *         différentes :
     *         - HumanPlayer : attend que l'utilisateur tape une ligne et une
     *         colonne
     *         - GUIPlayer : attend que l'utilisateur clique sur le plateau
     *         - AIPlayer : lance le calcul Minimax et retourne le meilleur coup
     *         trouvé
     */
    public Point getMove(GameState state);
}