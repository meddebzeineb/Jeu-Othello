package othello;

/**
 * Analyse expérimentale des fonctions d'évaluation.
 * Ce fichier génère les données nécessaires pour le rapport :
 * - Temps de calcul par fonction d'évaluation et par profondeur
 * - Résultats des tournois entre EV1, EV2 et EV3 à différentes profondeurs
 *
 * Pour lancer l'analyse : exécuter la méthode main() de cette classe
 * (à la place de Main.java qui lance l'interface graphique)
 */
public class ExperimentalAnalysis {

    // Profondeurs testées
    private static final int[] DEPTHS = { 1, 3, 5 };

    // Nombre de parties par tournoi (minimum 50 selon l'énoncé)
    private static final int GAMES = 50;

    // Nombre de coups mesurés pour les stats de temps
    private static final int MOVES_SAMPLE = 20;

    public static void main(String[] args) {

        System.out.println("===========================================");
        System.out.println("   ANALYSE EXPERIMENTALE - OTHELLO IA     ");
        System.out.println("===========================================");
        System.out.println();

        // Partie 1 : temps de calcul
        analyzeTime();

        // Partie 2 : tournois entre les fonctions d'évaluation
        analyzeTournaments();
    }

    // ----------------------------------------------------------------
    // PARTIE 1 — Temps de calcul
    // ----------------------------------------------------------------

    /**
     * Mesure le temps de calcul moyen par coup pour chaque fonction
     * d'évaluation et chaque profondeur.
     *
     * On fait jouer l'IA MOVES_SAMPLE coups depuis la position initiale
     * et on calcule la moyenne du temps passé sur chaque coup.
     *
     * C'est important pour le rapport car la prof demande l'évolution
     * du temps de calcul en fonction de la profondeur de recherche.
     */
    private static void analyzeTime() {
        System.out.println("-------------------------------------------");
        System.out.println("  TABLEAU 1 : Temps de calcul (ms/coup)    ");
        System.out.println("-------------------------------------------");
        System.out.printf("%-10s | %-10s | %-10s | %-10s%n",
                "Depth", "EV1", "EV2", "EV3");
        System.out.println("-----------|------------|------------|----------");

        for (int depth : DEPTHS) {
            double timeEV1 = measureTime(1, depth);
            double timeEV2 = measureTime(2, depth);
            double timeEV3 = measureTime(3, depth);
            System.out.printf("%-10d | %-10.2f | %-10.2f | %-10.2f%n",
                    depth, timeEV1, timeEV2, timeEV3);
        }
        System.out.println();
    }

    /**
     * Mesure le temps de calcul moyen en ms pour une IA donnée.
     *
     * On fait jouer l'IA sur une vraie partie et on mesure
     * le temps de chaque appel à getMove().
     *
     * @param level le niveau de l'IA (1=EV1, 2=EV2, 3=EV3)
     * @param depth la profondeur de recherche
     * @return le temps moyen en ms par coup
     */
    private static double measureTime(int level, int depth) {
        AIPlayer ai = new AIPlayer(level, depth);
        GameState state = new GameState();
        long totalTime = 0;
        int moves = 0;

        while (moves < MOVES_SAMPLE && !state.isGameOver()) {
            if (state.getValidMoves().isEmpty()) {
                state.passeTour();
                continue;
            }
            // L'IA calcule son coup et on mesure le temps
            Point move = ai.getMove(state);
            totalTime += ai.getTimeSpent();

            // On joue le coup de l'IA pour avancer dans la partie
            if (move != null && state.isMoveValid(
                    move.getLigne(), move.getColonne())) {
                state.playMove(move.getLigne(), move.getColonne());
            }
            moves++;
        }

        return moves == 0 ? 0 : (double) totalTime / moves;
    }

    // ----------------------------------------------------------------
    // PARTIE 2 — Tournois entre fonctions d'évaluation
    // ----------------------------------------------------------------

    /**
     * Lance tous les tournois entre les 3 fonctions d'évaluation
     * à trois profondeurs différentes (1, 3, 5) pour montrer
     * l'évolution des résultats en fonction de la profondeur.
     *
     * Dans tous les cas, les deux IA ont la MÊME profondeur pour
     * comparer uniquement la qualité de la fonction d'évaluation,
     * pas l'avantage de la profondeur.
     */
    private static void analyzeTournaments() {

        // --- Tournois à depth 1 ---
        // Profondeur minimale : l'IA ne regarde qu'un seul coup en avance
        System.out.println("-------------------------------------------");
        System.out.println("  TABLEAU 2 : Tournois (depth=1, 50 parties)");
        System.out.println("-------------------------------------------");
        System.out.println();

        System.out.println(">>> EV1 vs EV2 (depth 1)");
        new Tournament(
                new AIPlayer(1, 1), "EV1 (depth 1)",
                new AIPlayer(2, 1), "EV2 (depth 1)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV1 vs EV3 (depth 1)");
        new Tournament(
                new AIPlayer(1, 1), "EV1 (depth 1)",
                new AIPlayer(3, 1), "EV3 (depth 1)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV2 vs EV3 (depth 1)");
        new Tournament(
                new AIPlayer(2, 1), "EV2 (depth 1)",
                new AIPlayer(3, 1), "EV3 (depth 1)").run(GAMES);
        System.out.println();

        // --- Tournois à depth 3 ---
        // Profondeur intermédiaire : bon compromis temps/qualité
        System.out.println("-------------------------------------------");
        System.out.println("  TABLEAU 3 : Tournois (depth=3, 50 parties)");
        System.out.println("-------------------------------------------");
        System.out.println();

        System.out.println(">>> EV1 vs EV2 (depth 3)");
        new Tournament(
                new AIPlayer(1, 3), "EV1 (depth 3)",
                new AIPlayer(2, 3), "EV2 (depth 3)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV1 vs EV3 (depth 3)");
        new Tournament(
                new AIPlayer(1, 3), "EV1 (depth 3)",
                new AIPlayer(3, 3), "EV3 (depth 3)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV2 vs EV3 (depth 3)");
        new Tournament(
                new AIPlayer(2, 3), "EV2 (depth 3)",
                new AIPlayer(3, 3), "EV3 (depth 3)").run(GAMES);
        System.out.println();

        // --- Tournois à depth 5 ---
        // Profondeur maximale : montre que les conclusions restent
        // les mêmes quelle que soit la profondeur (robustesse)
        System.out.println("-------------------------------------------");
        System.out.println("  TABLEAU 4 : Tournois (depth=5, 50 parties)");
        System.out.println("-------------------------------------------");
        System.out.println();

        System.out.println(">>> EV1 vs EV2 (depth 5)");
        new Tournament(
                new AIPlayer(1, 5), "EV1 (depth 5)",
                new AIPlayer(2, 5), "EV2 (depth 5)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV1 vs EV3 (depth 5)");
        new Tournament(
                new AIPlayer(1, 5), "EV1 (depth 5)",
                new AIPlayer(3, 5), "EV3 (depth 5)").run(GAMES);
        System.out.println();

        System.out.println(">>> EV2 vs EV3 (depth 5)");
        new Tournament(
                new AIPlayer(2, 5), "EV2 (depth 5)",
                new AIPlayer(3, 5), "EV3 (depth 5)").run(GAMES);
        System.out.println();

        System.out.println("===========================================");
        System.out.println("   ANALYSE TERMINEE                       ");
        System.out.println("===========================================");
    }
}