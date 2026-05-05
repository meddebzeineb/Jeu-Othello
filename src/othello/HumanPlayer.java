package othello;

import java.util.Scanner;

/**
 * Représente un joueur humain qui joue via le terminal (clavier).
 * Cette classe est la version console du joueur humain.
 *
 * Dans notre interface graphique, c'est GUIPlayer qui est utilisée
 * à la place (le joueur clique sur le plateau plutôt que de taper au clavier).
 * HumanPlayer reste utile pour tester le jeu sans interface graphique,
 * et pour déboguer le moteur de jeu rapidement.
 */
public class HumanPlayer implements Player {

    // Scanner : l'outil Java pour lire ce que l'utilisateur tape au clavier
    private Scanner scanner;

    /**
     * Constructeur : initialise le scanner sur l'entrée standard (le clavier).
     * System.in correspond à ce que l'utilisateur tape dans le terminal.
     */
    public HumanPlayer() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Demande au joueur humain d'entrer les coordonnées de son coup.
     *
     * La méthode tourne en boucle jusqu'à ce que le joueur entre
     * des coordonnées valides (entre 0 et 7).
     * Cette méthode vérifie seulement que les coordonnées
     * sont dans les limites du plateau, pas que le coup est légal selon
     * les règles d'Othello. C'est GameState.isMoveValid() qui fait ça.
     *
     * @param state L'état actuel du jeu (non utilisé ici car le joueur
     *              décide lui-même où jouer, mais requis par l'interface Player)
     * @return Le Point (ligne, colonne) choisi par le joueur
     */
    @Override
    public Point getMove(GameState state) {
        int ligne = -1;
        int colonne = -1;

        // On répète la demande tant que les coordonnées ne sont pas valides
        while (true) {
            System.out.print("Entrez la ligne (0-7) : ");
            if (scanner.hasNextInt()) {
                ligne = scanner.nextInt();
            }

            System.out.print("Entrez la colonne (0-7) : ");
            if (scanner.hasNextInt()) {
                colonne = scanner.nextInt();
            }

            // Vérifie que les coordonnées sont bien dans le plateau (0 à 7)
            if (ligne >= 0 && ligne < Board.SIZE && colonne >= 0 && colonne < Board.SIZE) {
                return new Point(ligne, colonne);
            }

            // Si on arrive ici, les coordonnées étaient hors limites
            System.out.println("Coordonnées invalides. Réessayez.");
        }
    }
}