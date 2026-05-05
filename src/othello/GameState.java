package othello;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'état complet du jeu à un instant donné.
 * C'est la classe la plus importante du moteur de jeu.
 *
 * Elle contient :
 * - Le plateau (Board) avec les pions
 * - Le joueur dont c'est le tour
 * - Le compteur de tours passés consécutivement
 *
 * Elle gère toute la logique des règles d'Othello :
 * - Quels coups sont valides ?
 * - Quels pions sont retournés quand on joue un coup ?
 * - Est-ce que la partie est terminée ?
 *
 * C'est aussi cette classe que l'IA copie pour simuler
 * des coups pendant le calcul Minimax, sans toucher
 * à la vraie partie en cours.
 */
public class GameState {

    // Constantes pour représenter les joueurs et les cases vides
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = -1;

    // Les 8 directions possibles autour d'une case :
    // haut-gauche, haut, haut-droite,
    // gauche, droite,
    // bas-gauche, bas, bas-droite
    // On les utilise pour chercher les pions à retourner
    private static final int[][] DIRECTIONS = {
            { -1, -1 }, { -1, 0 }, { -1, 1 },
            { 0, -1 }, { 0, 1 },
            { 1, -1 }, { 1, 0 }, { 1, 1 }
    };

    private Board board;
    private int joueurCourant;

    // Compte combien de fois de suite les joueurs ont passé leur tour.
    // Si les deux passent consécutivement (= 2), la partie est terminée
    // car aucun des deux ne peut jouer.
    private int toursPassesConsecutifs;

    /**
     * Constructeur par défaut : crée une nouvelle partie.
     * Le plateau est initialisé avec les 4 pions du centre,
     * et c'est au joueur Noir de commencer (règle standard d'Othello).
     */
    public GameState() {
        this.board = new Board();
        this.joueurCourant = BLACK;
        this.toursPassesConsecutifs = 0;
    }

    /**
     * Constructeur de copie : crée un état identique à "autre".
     * Utilisé par l'IA Minimax pour simuler des coups
     * sur une copie sans modifier la vraie partie.
     * Chaque noeud de l'arbre Minimax correspond à une copie de GameState.
     */
    public GameState(GameState autre) {
        this.board = new Board(autre.board);
        this.joueurCourant = autre.joueurCourant;
        this.toursPassesConsecutifs = autre.toursPassesConsecutifs;
    }

    /*---- Getters ----*/
    public Board getBoard() {
        return board;
    }

    public int getJoueurCourant() {
        return joueurCourant;
    }

    /**
     * Vérifie si un coup est valide pour le joueur courant.
     * Un coup est valide si :
     * 1. Les coordonnées sont dans le plateau
     * 2. La case est vide
     * 3. Le coup retourne au moins un pion adverse
     * (règle fondamentale d'Othello)
     */
    public boolean isMoveValid(int ligne, int colonne) {
        if (!board.isValid(ligne, colonne))
            return false;
        if (board.get(ligne, colonne) != EMPTY)
            return false;
        // Un coup n'est valide que s'il retourne au moins un pion
        return !getFlippedDiscs(ligne, colonne, joueurCourant).isEmpty();
    }

    /**
     * Retourne la liste de tous les coups valides pour le joueur courant.
     * Utilisé par :
     * - L'interface graphique pour afficher les coups possibles (points verts)
     * - L'IA pour explorer les coups dans Minimax
     * - La boucle de jeu pour savoir si un joueur doit passer son tour
     */
    public List<Point> getValidMoves() {
        return getValidMovesFor(joueurCourant);
    }

    /**
     * Retourne la liste de tous les coups valides pour un joueur donné.
     * Version plus générale de getValidMoves() — utilisée par l'IA
     * pour calculer la mobilité des deux joueurs dans EV3.
     * On parcourt toutes les cases vides du plateau et on vérifie
     * si chacune retournerait au moins un pion.
     */
    public List<Point> getValidMovesFor(int joueur) {
        List<Point> moves = new ArrayList<>();
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                if (board.get(i, j) == EMPTY &&
                        !getFlippedDiscs(i, j, joueur).isEmpty()) {
                    moves.add(new Point(i, j));
                }
            }
        }
        return moves;
    }

    /**
     * Calcule la liste des pions qui seraient retournés si "joueur"
     * posait un pion en (ligne, colonne).
     *
     * C'est le coeur des règles d'Othello :
     * Pour chacune des 8 directions, on avance case par case.
     * On collecte les pions adverses rencontrés.
     * Si on finit sur un pion allié, tous les pions adverses
     * collectés dans cette direction sont retournés.
     * Si on sort du plateau ou tombe sur une case vide, on annule
     * cette direction (aucun pion retourné dans ce sens).
     *
     * Cette méthode est privée car elle n'est utilisée qu'en interne
     * par isMoveValid() et playMove().
     */
    private List<Point> getFlippedDiscs(int ligne, int colonne, int joueur) {
        List<Point> toFlip = new ArrayList<>();
        int adversaire = -joueur; // l'adversaire est toujours -joueur

        for (int[] dir : DIRECTIONS) {
            int dl = dir[0], dc = dir[1];
            List<Point> candidats = new ArrayList<>();

            // On avance dans cette direction
            int r = ligne + dl, c = colonne + dc;

            // On collecte les pions adverses consécutifs
            while (board.isValid(r, c) && board.get(r, c) == adversaire) {
                candidats.add(new Point(r, c));
                r += dl;
                c += dc;
            }

            // Si on termine sur un pion allié, les candidats sont validés
            if (!candidats.isEmpty() && board.isValid(r, c)
                    && board.get(r, c) == joueur) {
                toFlip.addAll(candidats);
            }
            // Sinon on ignore cette direction (case vide ou bord du plateau)
        }
        return toFlip;
    }

    /**
     * Joue un coup : pose le pion du joueur courant et retourne
     * tous les pions adverses capturés.
     * Après le coup, on passe automatiquement au joueur suivant.
     *
     * il faut appeler isMoveValid() avant d'appeler
     * cette méthode pour s'assurer que le coup est légal.
     */
    public void playMove(int ligne, int colonne) {
        // On calcule quels pions vont être retournés
        List<Point> toFlip = getFlippedDiscs(ligne, colonne, joueurCourant);

        // On pose le pion du joueur courant
        board.set(ligne, colonne, joueurCourant);

        // On retourne tous les pions capturés
        for (Point p : toFlip) {
            board.set(p.getLigne(), p.getColonne(), joueurCourant);
        }

        // On passe au joueur suivant et on remet le compteur de passes à 0
        switchPlayer();
        toursPassesConsecutifs = 0;
    }

    /**
     * Passe le tour quand le joueur courant n'a aucun coup valide.
     * Règle d'Othello : si un joueur ne peut pas jouer, il passe.
     * Si les DEUX joueurs passent consécutivement, la partie est finie.
     */
    public void passeTour() {
        switchPlayer();
        toursPassesConsecutifs++;
    }

    /**
     * Change le joueur courant.
     * BLACK (1) devient WHITE (-1) et vice versa.
     * Fonctionne grâce au fait que WHITE = -BLACK.
     */
    public void switchPlayer() {
        joueurCourant = -joueurCourant;
    }

    /**
     * Vérifie si la partie est terminée.
     * La partie se termine dans deux cas :
     * 1. Le plateau est complètement rempli (64 pions posés)
     * 2. Les deux joueurs ont passé leur tour consécutivement
     * (aucun des deux ne peut jouer)
     */
    public boolean isGameOver() {
        if (board.isFull())
            return true;
        if (toursPassesConsecutifs >= 2)
            return true;
        return false;
    }
}