package othello;

/**
 * Représente le plateau de jeu d'Othello.
 * C'est la structure de base du jeu : une grille 8x8 où on place les pions.
 * Cette classe ne contient PAS la logique des règles (c'est GameState qui s'en
 * charge),
 * elle gère uniquement le stockage et l'accès aux cases du plateau.
 */
public class Board {

    // Taille standard d'un plateau d'Othello : 8x8
    public static final int SIZE = 8;

    // On représente les pions par des entiers simples :
    // 0 = case vide, 1 = pion Noir, -1 = pion Blanc
    // Utiliser -1 pour Blanc est très pratique : pour avoir l'adversaire
    // d'un joueur, on fait juste "-joueur" (ex: -BLACK = WHITE)
    public static final int EMPTY = 0;
    public static final int BLACK = 1;
    public static final int WHITE = -1;

    // La grille elle-même : un tableau 2D d'entiers
    // grille[ligne][colonne] contient EMPTY, BLACK ou WHITE
    private int[][] grille;

    /**
     * Constructeur par défaut : crée un plateau vide avec la
     * configuration de départ standard d'Othello.
     * Au début, 4 pions sont placés au centre en diagonale.
     */
    public Board() {
        this.grille = new int[SIZE][SIZE];
        // Configuration initiale standard d'Othello :
        // Les 4 pions du centre, en diagonale alternée
        grille[3][3] = WHITE;
        grille[3][4] = BLACK;
        grille[4][3] = BLACK;
        grille[4][4] = WHITE;
    }

    /**
     * Constructeur de copie : crée un nouveau plateau identique à "autre".
     * On utilise ça pour simuler des coups sans modifier
     * le vrai plateau. C'est crucial pour le Minimax !
     */
    public Board(Board autre) {
        this.grille = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            // System.arraycopy est plus rapide qu'une boucle manuelle
            // pour copier une ligne entière du tableau
            System.arraycopy(autre.grille[i], 0, this.grille[i], 0, SIZE);
        }
    }

    /**
     * Retourne la valeur d'une case (EMPTY, BLACK ou WHITE).
     * Utilisé partout dans le jeu pour lire l'état du plateau.
     */
    public int get(int ligne, int colonne) {
        return grille[ligne][colonne];
    }

    /**
     * Modifie le contenu d'une case.
     * Utilisé quand on pose un pion ou qu'on retourne des pions adverses.
     */
    public void set(int ligne, int colonne, int player) {
        grille[ligne][colonne] = player;
    }

    /**
     * Vérifie que des coordonnées sont bien dans les limites du plateau.
     * Indispensable pour ne pas avoir d'ArrayIndexOutOfBoundsException
     * quand on parcourt les directions autour d'une case.
     */
    public boolean isValid(int ligne, int colonne) {
        return ligne >= 0 && ligne < SIZE && colonne >= 0 && colonne < SIZE;
    }

    /**
     * Compte le nombre de pions d'un joueur sur tout le plateau.
     * C'est le "score" affiché en haut de l'interface graphique.
     * Aussi utilisé par la fonction d'évaluation de l'IA.
     */
    public int getScore(int player) {
        int score = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grille[i][j] == player)
                    score++;
            }
        }
        return score;
    }

    /**
     * Vérifie si le plateau est complètement rempli.
     * C'est l'une des deux conditions de fin de partie
     * (l'autre étant que les deux joueurs passent leur tour).
     */
    public boolean isFull() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (grille[i][j] == EMPTY)
                    return false;
            }
        }
        return true;
    }

    /**
     * Affichage textuel du plateau dans le terminal.
     * N = pion Noir, B = pion Blanc, . = case vide.
     * Utile pour déboguer sans interface graphique.
     */
    /*
     * @Override
     * public String toString() {
     * StringBuilder sb = new StringBuilder();
     * sb.append("   0 1 2 3 4 5 6 7\n");
     * sb.append("  -----------------\n");
     * for (int i = 0; i < SIZE; i++) {
     * sb.append(i + "| ");
     * for (int j = 0; j < SIZE; j++) {
     * int pion = grille[i][j];
     * if (pion == 1)
     * sb.append("N ");
     * else if (pion == -1)
     * sb.append("B ");
     * else
     * sb.append(". ");
     * }
     * sb.append("|\n");
     * }
     * sb.append("  -----------------");
     * return sb.toString();
     * }
     */
}