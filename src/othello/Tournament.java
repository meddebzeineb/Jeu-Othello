package othello;

import java.util.List;

/**
 * Gère les tournois entre deux IA sans interface graphique.
 * Un tournoi consiste à faire jouer deux IA l'une contre l'autre
 * sur un grand nombre de parties (minimum 50 selon l'énoncé).
 *
 * Points importants :
 * - Les parties se jouent en mode "headless" (sans affichage)
 * ce qui les rend beaucoup plus rapides qu'avec l'interface graphique
 * - On alterne qui commence (Noir/Blanc) pour éviter qu'une IA
 * ait un avantage systématique lié au premier coup
 * - On collecte des statistiques : victoires, défaites, nuls,
 * et la marge moyenne de pions par partie
 */
public class Tournament {

    private final Player playerA; // Première IA
    private final Player playerB; // Deuxième IA
    private final String nameA; // Nom affiché pour l'IA A
    private final String nameB; // Nom affiché pour l'IA B

    // Statistiques collectées pendant le tournoi
    private int winsA; // Nombre de victoires de A
    private int winsB; // Nombre de victoires de B
    private int draws; // Nombre de matchs nuls
    private long totalMarginA; // Somme des marges (pions A - pions B) sur toutes les parties

    /**
     * Constructeur : prépare un tournoi entre deux joueurs.
     * 
     * @param playerA Premier joueur (IA)
     * @param nameA   Nom du premier joueur (ex: "Facile (N1)")
     * @param playerB Deuxième joueur (IA)
     * @param nameB   Nom du deuxième joueur (ex: "Difficile (N3)")
     */
    public Tournament(Player playerA, String nameA,
            Player playerB, String nameB) {
        this.playerA = playerA;
        this.nameA = nameA;
        this.playerB = playerB;
        this.nameB = nameB;
    }

    // ----------------------------------------------------------------
    // API publique
    // ----------------------------------------------------------------

    /**
     * Lance le tournoi complet avec numGames parties.
     *
     * On alterne qui joue Noir à chaque partie :
     * - Parties paires (0, 2, 4...) : A joue Noir, B joue Blanc
     * - Parties impaires (1, 3, 5...) : B joue Noir, A joue Blanc
     *
     * Cette alternance est importante car à Othello, jouer en premier
     * (Noir) peut donner un léger avantage ou désavantage selon le niveau.
     * En alternant, on neutralise cet effet et les résultats reflètent
     * vraiment la qualité des IA.
     *
     * @param numGames Nombre total de parties à jouer (minimum 50)
     */
    public void run(int numGames) {
        reset(); // Remet les compteurs à zéro avant de commencer
        for (int i = 0; i < numGames; i++) {
            boolean aIsBlack = (i % 2 == 0); // A commence la moitié des parties
            playOneGame(aIsBlack);
        }
        printSummary(numGames); // Affiche les résultats finaux
    }

    // ----------------------------------------------------------------
    // Méthodes privées
    // ----------------------------------------------------------------

    /**
     * Joue une seule partie complète entre les deux IA.
     * La partie se déroule sans aucun affichage pour aller le plus vite possible.
     *
     * @param aIsBlack true si l'IA A joue Noir dans cette partie
     */
    private void playOneGame(boolean aIsBlack) {
        GameState state = new GameState(); // Nouveau plateau vide

        // On assigne les couleurs selon qui commence
        Player blackPlayer = aIsBlack ? playerA : playerB;
        Player whitePlayer = aIsBlack ? playerB : playerA;

        int consecutivePasses = 0; // Compteur de passes consécutives

        while (!state.isGameOver()) {
            List<Point> validMoves = state.getValidMoves();

            // Si le joueur courant n'a pas de coups valides, il passe
            if (validMoves.isEmpty()) {
                state.passeTour();
                consecutivePasses++;
                // Si les deux joueurs passent consécutivement, la partie est finie
                if (consecutivePasses >= 2)
                    break;
                continue;
            }

            consecutivePasses = 0; // On remet à 0 car un coup a été joué

            // On demande à l'IA courante de choisir son coup
            Player currentPlayer = (state.getJoueurCourant() == GameState.BLACK)
                    ? blackPlayer
                    : whitePlayer;

            Point move = currentPlayer.getMove(state);

            // Sécurité : si l'IA retourne null ou un coup invalide, elle passe
            if (move == null || !state.isMoveValid(move.getLigne(), move.getColonne())) {
                state.passeTour();
            } else {
                state.playMove(move.getLigne(), move.getColonne());
            }
        }

        // La partie est terminée, on enregistre le résultat
        recordResult(state, aIsBlack);
    }

    /**
     * Enregistre le résultat d'une partie terminée.
     * On calcule qui a gagné selon le score final (nombre de pions).
     * On met à jour les compteurs de victoires/défaites/nuls
     * et on ajoute la marge de cette partie au total.
     *
     * @param state    L'état final du jeu après la partie
     * @param aIsBlack true si A jouait Noir dans cette partie
     */
    private void recordResult(GameState state, boolean aIsBlack) {
        Board board = state.getBoard();

        int blackScore = board.getScore(GameState.BLACK);
        int whiteScore = board.getScore(GameState.WHITE);

        // On convertit les scores Noir/Blanc en scores A/B
        // selon qui jouait quelle couleur dans cette partie
        int scoreA = aIsBlack ? blackScore : whiteScore;
        int scoreB = aIsBlack ? whiteScore : blackScore;

        // On ajoute la marge de cette partie (peut être négative si A a perdu)
        totalMarginA += (scoreA - scoreB);

        // On met à jour le bon compteur
        if (scoreA > scoreB)
            winsA++;
        else if (scoreB > scoreA)
            winsB++;
        else
            draws++;
    }

    /**
     * Affiche un résumé formaté des résultats du tournoi.
     * Montre les victoires, défaites, nuls en nombre et en pourcentage,
     * ainsi que la marge moyenne de pions par partie pour l'IA A.
     *
     * Une marge positive signifie que A gagne en moyenne X pions de plus que B.
     * Une marge négative signifie que B domine.
     *
     * @param numGames Nombre total de parties jouées
     */
    private void printSummary(int numGames) {
        double avgMargin = (double) totalMarginA / numGames;

        // On utilise des caractères ASCII simples (+, -, |)
        // car les caractères UTF-8 (╔══╗) s'affichent mal sur Windows
        System.out.println("+==========================================+");
        System.out.printf("|  Tournament: %-28s|%n", nameA + " vs " + nameB);
        System.out.printf("|  Games played: %-26d|%n", numGames);
        System.out.println("+==========================================+");
        System.out.printf("|  %-12s wins : %4d  (%5.1f %%)      |%n", nameA, winsA, pct(winsA, numGames));
        System.out.printf("|  %-12s wins : %4d  (%5.1f %%)      |%n", nameB, winsB, pct(winsB, numGames));
        System.out.printf("|  Draws            : %4d  (%5.1f %%)      |%n", draws, pct(draws, numGames));
        System.out.println("+==========================================+");
        System.out.printf("|  Avg margin for %-24s|%n", nameA + ":");
        System.out.printf("|    %+.2f discs per game                  |%n", avgMargin);
        System.out.println("+==========================================+");
    }

    /**
     * Remet tous les compteurs à zéro avant un nouveau tournoi.
     * Appelé au début de run() pour éviter de mélanger les résultats
     * si on réutilise le même objet Tournament.
     */
    private void reset() {
        winsA = 0;
        winsB = 0;
        draws = 0;
        totalMarginA = 0;
    }

    /**
     * Calcule un pourcentage simple.
     * 
     * @param part  La valeur partielle (ex: nombre de victoires)
     * @param total Le total (ex: nombre de parties)
     * @return Le pourcentage entre 0.0 et 100.0
     */
    private double pct(int part, int total) {
        return total == 0 ? 0.0 : 100.0 * part / total;
    }
}