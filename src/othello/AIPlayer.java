package othello;

import java.util.List;

/**
 * Représente l'IA du jeu Othello.
 * Elle utilise l'algorithme Minimax avec élagage Alpha-Bêta.
 *
 * On a implémenté 3 niveaux de difficulté, chacun avec sa propre
 * fonction d'évaluation et sa propre profondeur de recherche :
 *
 * Niveau 1 (Facile) — EV1 : compte juste les pions, profondeur 1
 * Niveau 2 (Moyen) — EV2 : poids positionnels, profondeur 3
 * Niveau 3 (Difficile)— EV3 : mobilité + coins + position, profondeur 5
 *
 * Plus la profondeur est grande, plus l'IA explore loin dans le futur,
 * mais plus le calcul est long. On a choisi ces profondeurs pour garder
 * un temps de réponse raisonnable (moins de 2 secondes par coup).
 */
public class AIPlayer implements Player {

    private int maxDepth; // Profondeur maximale de recherche dans l'arbre
    private int level; // Niveau de difficulté (1, 2 ou 3)
    private int aiColor; // Couleur de l'IA (BLACK ou WHITE), définie au début de chaque coup
    private int nodesExplored = 0; // nombre de noeuds explorés par coup
    private long timeSpent = 0; // temps de calcul en millisecondes
    /**
     * Matrice de poids positionnels pour EV2 et EV3.
     * Chaque valeur représente l'importance stratégique d'une case :
     *
     * 100 = coins (les meilleures cases, jamais retournables une fois prises)
     * -50 = cases X (diagonales des coins, très dangereuses car elles
     * donnent le coin à l'adversaire)
     * -20 = cases C (adjacentes aux coins sur les bords, aussi risquées)
     * 10 = bords stables (bonne position)
     * -1 = cases intérieures (neutres voire négatives en début de partie)
     *
     * La symétrie de la matrice reflète la symétrie du plateau d'Othello.
     */
    private static final int[][] WEIGHTS = {
            { 100, -20, 10, 5, 5, 10, -20, 100 },
            { -20, -50, -2, -2, -2, -2, -50, -20 },
            { 10, -2, -1, -1, -1, -1, -2, 10 },
            { 5, -2, -1, -1, -1, -1, -2, 5 },
            { 5, -2, -1, -1, -1, -1, -2, 5 },
            { 10, -2, -1, -1, -1, -1, -2, 10 },
            { -20, -50, -2, -2, -2, -2, -50, -20 },
            { 100, -20, 10, 5, 5, 10, -20, 100 }
    };

    // Les 4 coins du plateau : cases les plus stratégiques du jeu.
    // Un coin capturé est définitivement stable (ne peut jamais être retourné).
    private static final int[][] CORNERS = { { 0, 0 }, { 0, 7 }, { 7, 0 }, { 7, 7 } };

    /**
     * Constructeur principal : crée une IA avec un niveau de difficulté.
     * Le niveau détermine à la fois la profondeur de recherche
     * et la fonction d'évaluation utilisée.
     */
    public AIPlayer(int level) {
        this.level = level;
        if (level == 1)
            this.maxDepth = 1;
        else if (level == 2)
            this.maxDepth = 3;
        else
            this.maxDepth = 5;
    }

    /**
     * Constructeur secondaire : permet de choisir indépendamment
     * le niveau (= fonction d'évaluation) et la profondeur.
     * Utilisé pour l'analyse des performances
     * dans le rapport (comparer EV1 vs EV2 à profondeur égale).
     */
    public AIPlayer(int level, int depth) {
        this.level = level;
        this.maxDepth = depth;
    }

    /**
     * Point d'entrée de l'IA : appelé par la boucle de jeu
     * quand c'est au tour de l'IA de jouer.
     * On mémorise la couleur de l'IA puis on lance la recherche Minimax.
     */
    @Override
    public Point getMove(GameState state) {
        this.aiColor = state.getJoueurCourant();
        this.nodesExplored = 0; // remet à 0 avant chaque coup
        long start = System.currentTimeMillis();
        Point move = getBestMove(state, maxDepth);
        this.timeSpent = System.currentTimeMillis() - start;
        return move;
    }

    // ----------------------------------------------------------------
    // MINIMAX AVEC ELAGAGE ALPHA-BETA
    // ----------------------------------------------------------------

    /**
     * Lance la recherche Minimax depuis la racine de l'arbre.
     * On est toujours le joueur MAX à la racine (c'est notre tour).
     * On teste tous les coups possibles et on garde le meilleur.
     *
     * Alpha-bêta à ce niveau : alpha démarre à -infini, beta à +infini.
     * Ces bornes se resserrent au fil de l'exploration pour couper
     * les branches inutiles.
     */
    private Point getBestMove(GameState state, int depth) {
        List<Point> moves = state.getValidMoves();
        if (moves.isEmpty())
            return null;

        Point bestMove = null;
        int maxEval = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        for (Point move : moves) {
            // On simule le coup sur une copie de l'état
            GameState clone = new GameState(state);
            clone.playMove(move.getLigne(), move.getColonne());

            // On évalue ce coup avec Minimax (maintenant c'est au MIN de jouer)
            int eval = minimax(clone, depth - 1, alpha, beta, false);

            nodesExplored++;
            // On garde le coup avec la meilleure évaluation
            if (eval > maxEval) {
                maxEval = eval;
                bestMove = move;
            }
            alpha = Math.max(alpha, eval);
        }
        return bestMove;
    }

    /**
     * Algorithme Minimax récursif avec élagage Alpha-Bêta.
     *
     * PRINCIPE DU MINIMAX :
     * On modélise le jeu comme un arbre où chaque noeud est un état du jeu.
     * - Les noeuds MAX (notre IA) cherchent à maximiser le score
     * - Les noeuds MIN (l'adversaire) cherchent à minimiser le score
     * On alterne MAX et MIN à chaque niveau de profondeur.
     *
     * PRINCIPE DE L'ELAGAGE ALPHA-BETA :
     * Alpha = meilleur score garanti pour MAX jusqu'ici
     * Beta = meilleur score garanti pour MIN jusqu'ici
     * Si beta <= alpha, on arrête d'explorer cette branche car :
     * - MAX ne choisira jamais un chemin que MIN peut forcer à être
     * inférieur à ce que MAX a déjà trouvé ailleurs
     * Cela permet de couper une grande partie de l'arbre sans changer
     * le résultat final — l'IA joue aussi bien mais calcule moins.
     *
     * CAS PARTICULIER : si un joueur n'a pas de coups valides,
     * il passe son tour (règle d'Othello) et on continue la récursion
     * avec le même joueur mais en changeant qui est MAX/MIN.
     *
     * @param state            L'état du jeu à évaluer
     * @param depth            Profondeur restante à explorer
     * @param alpha            Meilleur score pour MAX
     * @param beta             Meilleur score pour MIN
     * @param maximizingPlayer true si c'est le tour de MAX (notre IA)
     * @return Le score de cet état selon la fonction d'évaluation
     */
    private int minimax(GameState state, int depth, int alpha, int beta,
            boolean maximizingPlayer) {

        // Cas de base : profondeur atteinte ou partie terminée
        // On évalue l'état avec notre fonction d'évaluation
        if (depth == 0 || state.isGameOver())
            return evaluate(state);

        List<Point> moves = state.getValidMoves();

        // Cas particulier : le joueur courant n'a pas de coups valides
        // Règle Othello : il passe son tour, l'autre joueur rejoue
        if (moves.isEmpty()) {
            GameState clone = new GameState(state);
            clone.passeTour();
            // On inverse maximizingPlayer car c'est maintenant l'autre qui joue
            return minimax(clone, depth - 1, alpha, beta, !maximizingPlayer);
        }

        if (maximizingPlayer) {
            // Notre IA cherche à MAXIMISER le score
            int maxEval = Integer.MIN_VALUE;
            for (Point move : moves) {
                GameState clone = new GameState(state);
                clone.playMove(move.getLigne(), move.getColonne());
                int eval = minimax(clone, depth - 1, alpha, beta, false);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                // Coupure bêta : MIN ne choisira jamais cette branche
                if (beta <= alpha)
                    break;
            }
            return maxEval;

        } else {
            // L'adversaire cherche à MINIMISER le score
            int minEval = Integer.MAX_VALUE;
            for (Point move : moves) {
                GameState clone = new GameState(state);
                clone.playMove(move.getLigne(), move.getColonne());
                int eval = minimax(clone, depth - 1, alpha, beta, true);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                // Coupure alpha : MAX ne choisira jamais cette branche
                if (beta <= alpha)
                    break;
            }
            return minEval;
        }
    }

    // ----------------------------------------------------------------
    // DISPATCHER — choisit la bonne fonction d'évaluation selon le niveau
    // ----------------------------------------------------------------

    /**
     * Redirige vers la bonne fonction d'évaluation selon le niveau de l'IA.
     * C'est ici qu'on décide quelle "intelligence" utiliser pour juger
     * si un état du jeu est bon ou mauvais pour notre IA.
     */
    private int evaluate(GameState state) {
        if (level == 1)
            return evaluatePieceCount(state);
        if (level == 2)
            return evaluatePositional(state);
        return evaluateStrategic(state);
    }

    // ----------------------------------------------------------------
    // EV1 — Compte de pions (Niveau Facile)
    // ----------------------------------------------------------------

    /**
     * Fonction d'évaluation simple : compte la différence de pions.
     *
     * Score = (nombre de pions IA) - (nombre de pions adversaire)
     *
     * AVANTAGES :
     * - Très rapide à calculer
     * - Facile à comprendre
     *
     * INCONVÉNIENTS :
     * - Myope : maximiser ses pions en milieu de partie est souvent
     * une mauvaise stratégie car ces pions peuvent être retournés
     * - Ne tient pas compte de la position des pions (un coin vaut
     * autant qu'une case du centre)
     * - Efficace seulement en fin de partie quand les pions
     * ne peuvent plus être retournés
     */
    private int evaluatePieceCount(GameState state) {
        Board board = state.getBoard();
        return board.getScore(aiColor) - board.getScore(-aiColor);
    }

    // ----------------------------------------------------------------
    // EV2 — Poids positionnels (Niveau Moyen)
    // ----------------------------------------------------------------

    /**
     * Fonction d'évaluation intermédiaire : utilise la matrice WEIGHTS.
     *
     * Score = somme de WEIGHTS[i][j] pour chaque pion IA
     * - somme de WEIGHTS[i][j] pour chaque pion adverse
     *
     * PRINCIPE :
     * Toutes les cases ne se valent pas à Othello :
     * - Les coins (100) sont les meilleures cases car jamais retournables
     * - Les cases X (-50) sont dangereuses car elles donnent le coin adverse
     * - Les bords (10) sont stables une fois occupés
     * - L'intérieur (-1) est risqué en début/milieu de partie
     *
     * AVANTAGES PAR RAPPORT À EV1 :
     * - Joue stratégiquement, vise les coins et les bords
     * - Évite les cases dangereuses adjacentes aux coins
     *
     * INCONVÉNIENTS :
     * - Ne tient pas compte de la mobilité (nombre de coups disponibles)
     * - Les poids sont fixes, pas adaptatifs selon la phase de jeu
     */
    private int evaluatePositional(GameState state) {
        Board board = state.getBoard();
        int score = 0;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                int cell = board.get(i, j);
                if (cell == aiColor)
                    score += WEIGHTS[i][j];
                else if (cell == -aiColor)
                    score -= WEIGHTS[i][j];
            }
        }
        return score;
    }

    // ----------------------------------------------------------------
    // EV3 — Stratégique (Niveau Difficile)
    // ----------------------------------------------------------------

    /**
     * Fonction d'évaluation avancée : combine trois critères stratégiques.
     *
     * Score final = (mobilité × poids) + (coins × poids) + (position × poids)
     *
     * Les trois critères sont :
     *
     * 1. MOBILITÉ : différence du nombre de coups légaux disponibles
     * Avoir plus de coups = plus de flexibilité, moins de risque
     * d'être forcé de jouer un mauvais coup.
     * Normalisé entre -100 et +100 pour éviter qu'il domine les autres.
     * Poids fort en début/milieu de partie (5), plus faible en fin (2)
     * car il y a naturellement moins de coups disponibles en fin de partie.
     *
     * 2. STABILITÉ DES COINS : chaque coin capturé vaut 25 points.
     * Les coins sont les seules cases totalement stables du plateau
     * (impossibles à retourner une fois prises).
     * Poids constant (4) car les coins sont précieux à toutes les phases.
     *
     * 3. POSITION : même matrice WEIGHTS qu'EV2.
     * Poids faible (1) car la mobilité capture déjà une grande
     * partie de la valeur stratégique.
     *
     * POIDS DYNAMIQUES :
     * Les poids changent selon la phase de jeu (nombre total de pions) :
     * - Début/milieu (< 40 pions) : mobilité très importante
     * - Fin de partie (>= 40 pions) : position et coins dominent
     */
    private int evaluateStrategic(GameState state) {
        Board board = state.getBoard();
        int totalDiscs = board.getScore(aiColor) + board.getScore(-aiColor);

        // --- 1. Mobilité ---
        // On compte les coups disponibles pour chaque joueur
        int myMoves = state.getValidMovesFor(aiColor).size();
        int oppMoves = state.getValidMovesFor(-aiColor).size();
        int mobility = 0;
        if (myMoves + oppMoves > 0) {
            // Normalisation à [-100, 100] pour équilibrer les trois critères
            mobility = 100 * (myMoves - oppMoves) / (myMoves + oppMoves);
        }

        // --- 2. Stabilité des coins ---
        // On compte les coins capturés par chaque joueur
        int myCorners = 0;
        int oppCorners = 0;
        for (int[] c : CORNERS) {
            int cell = board.get(c[0], c[1]);
            if (cell == aiColor)
                myCorners++;
            else if (cell == -aiColor)
                oppCorners++;
        }
        // Chaque coin vaut 25 points d'avantage
        int cornerScore = 25 * (myCorners - oppCorners);

        // --- 3. Score positionnel ---
        // Même calcul qu'EV2 avec la matrice WEIGHTS
        int positional = 0;
        for (int i = 0; i < Board.SIZE; i++) {
            for (int j = 0; j < Board.SIZE; j++) {
                int cell = board.get(i, j);
                if (cell == aiColor)
                    positional += WEIGHTS[i][j];
                else if (cell == -aiColor)
                    positional -= WEIGHTS[i][j];
            }
        }

        // --- Poids dynamiques selon la phase de jeu ---
        // En début/milieu de partie : la mobilité est cruciale
        // En fin de partie : la position et les coins sont figés
        int mobilityWeight = (totalDiscs < 40) ? 5 : 2;
        int cornerWeight = 4;
        int positionalWeight = 1;

        return mobilityWeight * mobility
                + cornerWeight * cornerScore
                + positionalWeight * positional;
    }

    public int getNodesExplored() {
        return nodesExplored;
    }

    public long getTimeSpent() {
        return timeSpent;
    }
}