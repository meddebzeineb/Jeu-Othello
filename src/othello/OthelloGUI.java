package othello;

import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import javax.swing.*;

/**
 * Interface graphique complète du jeu Othello.
 * Gère tout ce que l'utilisateur voit et avec quoi il interagit :
 * - La fenêtre de lancement (choix du mode de jeu)
 * - Le plateau de jeu avec les pions
 * - Les scores et le statut de la partie
 * - La fenêtre de fin de partie
 * - La configuration et l'affichage des tournois
 *
 * On utilise Java Swing pour l'interface graphique.
 * Swing est la bibliothèque graphique standard de Java,
 * elle permet de créer des fenêtres, boutons, panneaux etc.
 *
 * IMPORTANT : Swing n'est pas "thread-safe", ce qui signifie que
 * tout ce qui touche à l'affichage doit être fait dans un thread
 * spécial appelé EDT (Event Dispatch Thread).
 * C'est pourquoi on utilise SwingUtilities.invokeLater() pour
 * mettre à jour l'interface depuis la boucle de jeu.
 */
public class OthelloGUI extends JFrame {

    // -------------------------------------------------------
    // GUIPlayer — Le joueur humain via l'interface graphique
    // -------------------------------------------------------

    /**
     * Version graphique du joueur humain.
     * Au lieu de taper au clavier comme HumanPlayer,
     * le joueur clique sur le plateau.
     *
     * On utilise une SynchronousQueue pour faire communiquer
     * deux threads :
     * - Le thread graphique (EDT) qui détecte le clic
     * - Le thread de jeu (GameLoop) qui attend le coup
     *
     * Quand le joueur clique, submitMove() envoie le coup dans la queue.
     * getMove() attend qu'un coup arrive dans la queue.
     * C'est une communication thread-safe entre les deux threads.
     */
    public static class GUIPlayer implements Player {
        private final SynchronousQueue<Point> queue = new SynchronousQueue<>();

        /**
         * Appelé par le thread graphique quand le joueur clique sur une case.
         * Envoie le coup dans la queue pour que getMove() puisse le récupérer.
         */
        void submitMove(int ligne, int colonne) {
            try {
                queue.put(new Point(ligne, colonne));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Appelé par le thread de jeu pour obtenir le coup du joueur.
         * BLOQUE jusqu'à ce que le joueur clique sur le plateau.
         * C'est ce blocage qui "pause" la partie en attendant l'humain.
         */
        @Override
        public Point getMove(GameState state) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    // -------------------------------------------------------
    // Constantes visuelles
    // -------------------------------------------------------

    /**
     * Toutes les mesures et couleurs de l'interface.
     * Les regrouper ici facilite les modifications visuelles :
     * changer CELL change la taille de toutes les cases d'un coup.
     */
    private static final int CELL = 70; // Taille d'une case en pixels
    private static final int PADDING = 20; // Marge autour du plateau
    private static final int DISC_PAD = 8; // Marge intérieure des pions

    private static final Color COLOR_BG = new Color(0, 120, 60); // Vert du plateau
    private static final Color COLOR_GRID = new Color(0, 90, 40); // Lignes de la grille
    private static final Color COLOR_BLACK = new Color(20, 20, 20); // Pions noirs
    private static final Color COLOR_WHITE = new Color(240, 240, 240);// Pions blancs
    private static final Color COLOR_SCORE_BG = new Color(22, 22, 22); // Fond du score
    private static final Color COLOR_TEXT = Color.WHITE; // Texte
    private static final Color COLOR_HIGHLIGHT = new Color(255, 220, 0, 160); // Dernier coup joué
    private static final Color COLOR_HINT = new Color(100, 220, 100, 140);// Coups possibles
    private static final Color COLOR_INVALID = new Color(220, 60, 60, 160); // Coup invalide

    // -------------------------------------------------------
    // État de l'interface
    // -------------------------------------------------------

    private final GameState gameState; // L'état du jeu en cours
    private final GUIPlayer sharedPlayer; // Le joueur humain graphique
    private final Player aiPlayer; // L'IA
    private final boolean vsAI; // true si on joue contre l'IA
    private final int currentLevel; // Niveau de l'IA (1, 2 ou 3)
    private final boolean tournamentMode; // true si mode tournoi

    private BoardPanel boardPanel; // Le panneau du plateau
    private JLabel blackScoreLabel; // Affichage score Noir
    private JLabel whiteScoreLabel; // Affichage score Blanc
    private JLabel statusLabel; // Message de statut en bas

    private Point lastMove = null; // Dernier coup joué (pour le surlignage jaune)
    private List<Point> validMoves = null; // Coups valides (pour les points verts)
    private Point flashInvalid = null; // Case cliquée invalide (pour le flash rouge)

    // -------------------------------------------------------
    // LauncherWindow — Fenêtre de lancement
    // -------------------------------------------------------

    /**
     * Fenêtre de démarrage qui s'affiche avant le jeu.
     * Permet de choisir le mode de jeu :
     * - Joueur vs Joueur
     * - Joueur vs IA (3 niveaux)
     * - Tournoi IA vs IA
     *
     * C'est une JDialog (fenêtre modale) qui bloque jusqu'à
     * ce que le joueur fasse son choix.
     */
    public static class LauncherWindow extends JDialog {

        private boolean accepted = false; // true si le joueur a cliqué "Jouer"
        private boolean vsAI = false; // true si mode Joueur vs IA
        private boolean tournamentMode = false; // true si mode Tournoi
        private int level = 1; // Niveau choisi (1, 2 ou 3)
        private int selected = 0; // Carte sélectionnée
        private ModeCard[] cards; // Les 5 cartes de choix

        public LauncherWindow() {
            super((Frame) null, "Othello", true);
            setUndecorated(true); // Pas de barre de titre Windows
            setBackground(new Color(0, 0, 0, 0)); // Fond transparent

            // Panel principal avec coins arrondis dessinés manuellement
            JPanel root = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(18, 18, 18));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                    // Ligne verte décorative en haut
                    g2.setColor(new Color(0, 200, 100));
                    g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
                }
            };
            root.setOpaque(false);
            root.setBorder(BorderFactory.createEmptyBorder(36, 40, 32, 40));

            // Titre et sous-titre
            JLabel title = new JLabel("OTHELLO");
            title.setFont(new Font("Monospaced", Font.BOLD, 38));
            title.setForeground(Color.WHITE);
            title.setAlignmentX(CENTER_ALIGNMENT);

            JLabel sub = new JLabel("Choisissez votre adversaire");
            sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
            sub.setForeground(new Color(130, 130, 130));
            sub.setAlignmentX(CENTER_ALIGNMENT);

            JPanel header = new JPanel();
            header.setOpaque(false);
            header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
            header.add(title);
            header.add(Box.createVerticalStrut(6));
            header.add(sub);

            // Les 5 cartes de choix de mode
            cards = new ModeCard[] {
                    new ModeCard("⚔", "Joueur vs Joueur", "Deux humains"),
                    new ModeCard("●", "Facile", "IA débutante"),
                    new ModeCard("◆", "Moyen", "IA équilibrée"),
                    new ModeCard("★", "Difficile", "IA redoutable"),
                    new ModeCard("⚑", "Tournoi", "IA vs IA"),
            };

            JPanel cardsPanel = new JPanel(new GridLayout(1, 5, 12, 0));
            cardsPanel.setOpaque(false);
            for (int i = 0; i < cards.length; i++) {
                final int idx = i;
                // Gestion des clics et du survol pour chaque carte
                cards[i].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        selectCard(idx);
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        cards[idx].setHovered(true);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        cards[idx].setHovered(false);
                    }
                });
                cardsPanel.add(cards[i]);
            }
            selectCard(0); // Sélection par défaut : Joueur vs Joueur

            // Bouton JOUER
            JButton playBtn = new JButton("JOUER") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    // Change de couleur selon l'état du bouton
                    if (getModel().isPressed())
                        g2.setColor(new Color(0, 160, 80));
                    else if (getModel().isRollover())
                        g2.setColor(new Color(0, 230, 115));
                    else
                        g2.setColor(new Color(0, 200, 100));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.setFont(new Font("Monospaced", Font.BOLD, 15));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(new Color(10, 10, 10));
                    g2.drawString("JOUER",
                            (getWidth() - fm.stringWidth("JOUER")) / 2,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
            };
            playBtn.setPreferredSize(new Dimension(0, 46));
            playBtn.setOpaque(false);
            playBtn.setContentAreaFilled(false);
            playBtn.setBorderPainted(false);
            playBtn.setFocusPainted(false);
            playBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Action du bouton JOUER : récupère les choix et ferme la fenêtre
            playBtn.addActionListener(e -> {
                accepted = true;
                tournamentMode = (selected == 4);
                vsAI = (selected > 0 && selected < 4);
                level = (selected > 0 && selected < 4) ? selected : 1;
                dispose(); // Ferme la fenêtre launcher
            });

            JButton quitBtn = new JButton("Quitter");
            quitBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
            quitBtn.setForeground(new Color(100, 100, 100));
            quitBtn.setOpaque(false);
            quitBtn.setContentAreaFilled(false);
            quitBtn.setBorderPainted(false);
            quitBtn.setFocusPainted(false);
            quitBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            quitBtn.addActionListener(e -> {
                accepted = false;
                dispose();
            });

            JPanel btnRow = new JPanel(new BorderLayout(12, 0));
            btnRow.setOpaque(false);
            btnRow.add(playBtn, BorderLayout.CENTER);
            btnRow.add(quitBtn, BorderLayout.EAST);

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(header);
            content.add(Box.createVerticalStrut(28));
            content.add(cardsPanel);
            content.add(Box.createVerticalStrut(28));
            content.add(btnRow);

            root.add(content, BorderLayout.CENTER);
            setContentPane(root);
            setSize(660, 330);
            setLocationRelativeTo(null); // Centre la fenêtre sur l'écran
        }

        /** Sélectionne une carte et désélectionne les autres. */
        private void selectCard(int idx) {
            selected = idx;
            for (int i = 0; i < cards.length; i++)
                cards[i].setSelected(i == idx);
        }

        public boolean isAccepted() {
            return accepted;
        }

        public boolean isVsAI() {
            return vsAI;
        }

        public boolean isTournamentMode() {
            return tournamentMode;
        }

        public int getLevel() {
            return level;
        }
    }

    // -------------------------------------------------------
    // ModeCard — Une carte de choix de mode
    // -------------------------------------------------------

    /**
     * Représente une carte cliquable dans le launcher.
     * Chaque carte correspond à un mode de jeu.
     * On dessine manuellement l'apparence de la carte avec Graphics2D
     * pour avoir un rendu personnalisé (coins arrondis, couleurs custom).
     *
     * La carte change d'apparence selon son état :
     * - Sélectionnée : bordure verte, fond légèrement vert
     * - Survolée : bordure grise claire
     * - Normale : fond sombre, bordure grise foncée
     */
    static class ModeCard extends JPanel {
        private final String icon, name, desc;
        private boolean selected = false;
        private boolean hovered = false;

        ModeCard(String icon, String name, String desc) {
            this.icon = icon;
            this.name = name;
            this.desc = desc;
            setOpaque(false);
            setPreferredSize(new Dimension(110, 120));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setSelected(boolean s) {
            selected = s;
            repaint();
        }

        void setHovered(boolean h) {
            hovered = h;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Dessin du fond et de la bordure selon l'état
            if (selected) {
                g2.setColor(new Color(0, 200, 100, 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(0, 200, 100));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(70, 70, 70));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            } else {
                g2.setColor(new Color(35, 35, 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(50, 50, 50));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 14, 14);
            }

            // Dessin de l'icône, du nom et de la description
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2.setColor(selected ? new Color(0, 200, 100) : new Color(140, 140, 140));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(icon, (getWidth() - fm.stringWidth(icon)) / 2, 40);

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(selected ? Color.WHITE : new Color(190, 190, 190));
            fm = g2.getFontMetrics();
            g2.drawString(name, (getWidth() - fm.stringWidth(name)) / 2, 62);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
            g2.setColor(new Color(90, 90, 90));
            fm = g2.getFontMetrics();
            g2.drawString(desc, (getWidth() - fm.stringWidth(desc)) / 2, 80);
        }
    }

    // -------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------

    /**
     * Constructeur principal : affiche le launcher et initialise le jeu.
     * Si le joueur ferme le launcher sans choisir, on quitte le programme.
     */
    public OthelloGUI() {
        super("Othello");
        LauncherWindow launcher = new LauncherWindow();
        launcher.setVisible(true); // Bloque jusqu'au choix du joueur

        if (!launcher.isAccepted())
            System.exit(0);

        if (launcher.isTournamentMode()) {
            this.tournamentMode = true;
            this.vsAI = false;
            this.currentLevel = 1;
        } else {
            this.tournamentMode = false;
            this.vsAI = launcher.isVsAI();
            this.currentLevel = launcher.getLevel();
        }

        this.gameState = new GameState();
        this.sharedPlayer = new GUIPlayer();
        this.aiPlayer = new AIPlayer(currentLevel);
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /**
     * Constructeur secondaire : recrée la fenêtre avec la même config.
     * Utilisé par le bouton "Rejouer" pour relancer une partie identique
     * sans repasser par le launcher.
     */
    public OthelloGUI(boolean vsAI, int level) {
        super("Othello");
        this.tournamentMode = false;
        this.vsAI = vsAI;
        this.currentLevel = level;
        this.gameState = new GameState();
        this.sharedPlayer = new GUIPlayer();
        this.aiPlayer = new AIPlayer(level);
        buildUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    // -------------------------------------------------------
    // Construction de l'interface de jeu
    // -------------------------------------------------------

    /**
     * Construit l'interface principale du jeu :
     * - En haut : les scores des deux joueurs
     * - Au centre : le plateau de jeu
     * - En bas : le message de statut (dont c'est le tour)
     */
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(COLOR_SCORE_BG);
        root.add(buildScorePanel(), BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        root.add(boardPanel, BorderLayout.CENTER);

        statusLabel = new JLabel("Tour : NOIR", SwingConstants.CENTER);
        statusLabel.setForeground(COLOR_TEXT);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        statusLabel.setBackground(COLOR_SCORE_BG);
        statusLabel.setOpaque(true);
        root.add(statusLabel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildScorePanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.setBackground(COLOR_SCORE_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        blackScoreLabel = makeScoreLabel("⚫ NOIR : 2");
        whiteScoreLabel = makeScoreLabel("⚪ BLANC : 2");
        panel.add(blackScoreLabel);
        panel.add(whiteScoreLabel);
        return panel;
    }

    private JLabel makeScoreLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setForeground(COLOR_TEXT);
        l.setFont(new Font("SansSerif", Font.BOLD, 18));
        return l;
    }

    /**
     * Met à jour tous les éléments visuels après un coup.
     * Appelé après chaque coup joué (humain ou IA).
     * Doit être appelé dans le thread EDT via SwingUtilities.invokeLater().
     */
    private void refreshUI(Point move) {
        lastMove = move;
        validMoves = gameState.isGameOver() ? null : gameState.getValidMoves();

        Board b = gameState.getBoard();
        blackScoreLabel.setText("⚫ NOIR : " + b.getScore(GameState.BLACK));
        whiteScoreLabel.setText("⚪ BLANC : " + b.getScore(GameState.WHITE));

        if (!gameState.isGameOver()) {
            String joueur = gameState.getJoueurCourant() == GameState.BLACK
                    ? "NOIR"
                    : "BLANC";
            statusLabel.setText("Tour : " + joueur);
        }

        boardPanel.repaint(); // Redessine le plateau
    }

    // -------------------------------------------------------
    // BoardPanel — Le panneau de dessin du plateau
    // -------------------------------------------------------

    /**
     * Panneau personnalisé qui dessine le plateau d'Othello.
     * Hérite de JPanel et redéfinit paintComponent() pour
     * dessiner la grille, les pions et les indicateurs visuels.
     *
     * Gère aussi les clics de souris pour que le joueur humain puisse jouer.
     */
    private class BoardPanel extends JPanel {
        BoardPanel() {
            int size = Board.SIZE * CELL + 2 * PADDING;
            setPreferredSize(new Dimension(size, size));
            setBackground(COLOR_BG);

            // Détection des clics sur le plateau
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Convertit les pixels en coordonnées de case
                    int col = (e.getX() - PADDING) / CELL;
                    int row = (e.getY() - PADDING) / CELL;

                    if (row >= 0 && row < Board.SIZE && col >= 0 && col < Board.SIZE) {
                        if (!gameState.isMoveValid(row, col)) {
                            // Flash rouge si coup invalide
                            flashInvalid = new Point(row, col);
                            repaint();
                            Timer t = new Timer(400, ev -> {
                                flashInvalid = null;
                                repaint();
                            });
                            t.setRepeats(false);
                            t.start();
                            return;
                        }
                        // Envoie le coup au GUIPlayer qui attend dans getMove()
                        sharedPlayer.submitMove(row, col);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            drawGrid(g2); // 1. Dessine la grille verte
            drawHighlights(g2); // 2. Dessine les indicateurs (jaune, vert, rouge)
            drawDiscs(g2); // 3. Dessine les pions par-dessus
        }

        /**
         * Dessine la grille du plateau avec les lignes et les 4 points
         * de référence (comme sur un vrai plateau d'Othello).
         */
        private void drawGrid(Graphics2D g2) {
            g2.setColor(COLOR_GRID);
            g2.setStroke(new BasicStroke(1.5f));
            for (int i = 0; i <= Board.SIZE; i++) {
                int x = PADDING + i * CELL;
                int y = PADDING + i * CELL;
                g2.drawLine(x, PADDING, x, PADDING + Board.SIZE * CELL);
                g2.drawLine(PADDING, y, PADDING + Board.SIZE * CELL, y);
            }
            // 4 points de référence aux intersections intérieures
            int[] dots = { 2, 6 };
            g2.setColor(COLOR_GRID.darker());
            for (int r : dots)
                for (int c : dots) {
                    int cx = PADDING + c * CELL + CELL / 2;
                    int cy = PADDING + r * CELL + CELL / 2;
                    g2.fillOval(cx - 4, cy - 4, 8, 8);
                }
        }

        /**
         * Dessine les indicateurs visuels sur le plateau :
         * - Jaune : le dernier coup joué (par l'humain ou l'IA)
         * - Vert : les coups valides pour le joueur courant
         * - Rouge : flash sur une case cliquée invalide
         */
        private void drawHighlights(Graphics2D g2) {
            // Surlignage jaune du dernier coup
            if (lastMove != null) {
                int x = PADDING + lastMove.getColonne() * CELL;
                int y = PADDING + lastMove.getLigne() * CELL;
                g2.setColor(COLOR_HIGHLIGHT);
                g2.fillRect(x + 1, y + 1, CELL - 1, CELL - 1);
            }
            // Points verts sur les coups valides
            if (validMoves != null) {
                g2.setColor(COLOR_HINT);
                int dot = 14;
                for (Point p : validMoves) {
                    int cx = PADDING + p.getColonne() * CELL + CELL / 2 - dot / 2;
                    int cy = PADDING + p.getLigne() * CELL + CELL / 2 - dot / 2;
                    g2.fillOval(cx, cy, dot, dot);
                }
            }
            // Flash rouge sur coup invalide
            if (flashInvalid != null) {
                int x = PADDING + flashInvalid.getColonne() * CELL;
                int y = PADDING + flashInvalid.getLigne() * CELL;
                g2.setColor(COLOR_INVALID);
                g2.fillRect(x + 1, y + 1, CELL - 1, CELL - 1);
            }
        }

        /**
         * Dessine tous les pions sur le plateau.
         * Chaque pion est dessiné avec un effet d'ombre et un reflet
         * pour donner un aspect 3D réaliste.
         */
        private void drawDiscs(Graphics2D g2) {
            Board board = gameState.getBoard();
            for (int i = 0; i < Board.SIZE; i++) {
                for (int j = 0; j < Board.SIZE; j++) {
                    int val = board.get(i, j);
                    if (val == Board.EMPTY)
                        continue;

                    int x = PADDING + j * CELL + DISC_PAD;
                    int y = PADDING + i * CELL + DISC_PAD;
                    int d = CELL - 2 * DISC_PAD;

                    // Ombre portée (légèrement décalée)
                    g2.setColor(new Color(0, 0, 0, 80));
                    g2.fillOval(x + 3, y + 3, d, d);

                    if (val == Board.BLACK) {
                        // Pion noir avec reflet gris
                        g2.setColor(COLOR_BLACK);
                        g2.fillOval(x, y, d, d);
                        g2.setColor(new Color(80, 80, 80, 120));
                        g2.fillOval(x + d / 4, y + d / 6, d / 3, d / 4);
                    } else {
                        // Pion blanc avec bordure et reflet blanc
                        g2.setColor(COLOR_WHITE);
                        g2.fillOval(x, y, d, d);
                        g2.setColor(new Color(180, 180, 180));
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawOval(x, y, d, d);
                        g2.setColor(new Color(255, 255, 255, 160));
                        g2.fillOval(x + d / 4, y + d / 6, d / 3, d / 4);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------
    // Boucle de jeu
    // -------------------------------------------------------

    /**
     * Lance la partie dans un thread séparé (GameLoop).
     * On utilise un thread séparé pour ne pas bloquer l'interface graphique
     * pendant que l'IA calcule ou qu'on attend le clic du joueur.
     *
     * La boucle tourne jusqu'à la fin de la partie :
     * 1. Si le joueur n'a pas de coups, il passe
     * 2. Sinon on demande son coup (humain ou IA)
     * 3. On joue le coup et on met à jour l'affichage
     */
    public void startGame() {
        setVisible(true);

        // Mode tournoi : on saute le plateau et on lance la config directement
        if (tournamentMode) {
            launchTournamentDialog();
            return;
        }

        validMoves = gameState.getValidMoves();
        refreshUI(null);

        Thread gameThread = new Thread(() -> {
            while (!gameState.isGameOver()) {
                List<Point> moves = gameState.getValidMoves();

                if (moves.isEmpty()) {
                    // Le joueur courant passe son tour
                    final String joueurPasse = gameState.getJoueurCourant() == GameState.BLACK
                            ? "NOIR"
                            : "BLANC";
                    gameState.passeTour();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText(joueurPasse + " passe son tour !");
                        refreshUI(lastMove);
                    });
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException ignored) {
                    }
                    continue;
                }

                // Détermine qui joue : humain ou IA
                Player currentPlayer;
                if (vsAI && gameState.getJoueurCourant() == GameState.WHITE) {
                    currentPlayer = aiPlayer;
                    // Petite pause pour que l'IA ne joue pas instantanément
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                } else {
                    currentPlayer = sharedPlayer; // Attend le clic du joueur
                }

                Point move = currentPlayer.getMove(gameState);
                if (move == null)
                    break;

                gameState.playMove(move.getLigne(), move.getColonne());
                final Point finalMove = move;
                // Mise à jour de l'affichage dans le thread EDT
                SwingUtilities.invokeLater(() -> refreshUI(finalMove));
            }

            // Fin de partie : affiche le dialogue de résultat
            SwingUtilities.invokeLater(() -> {
                validMoves = null;
                boardPanel.repaint();
                Timer t = new Timer(500, e -> showEndDialog());
                t.setRepeats(false);
                t.start();
            });

        }, "GameLoop");

        gameThread.setDaemon(true); // Le thread s'arrête si la fenêtre est fermée
        gameThread.start();
    }

    // -------------------------------------------------------
    // Dialogue de configuration du tournoi
    // -------------------------------------------------------

    /**
     * Affiche la fenêtre de configuration du tournoi.
     * Permet de choisir :
     * - L'IA A (niveau 1, 2 ou 3)
     * - L'IA B (niveau 1, 2 ou 3)
     * - Le nombre de parties (minimum 2, par défaut 100)
     */
    private void launchTournamentDialog() {
        String[] aiNames = { "Facile (1)", "Moyen (2)", "Difficile (3)" };

        JDialog d = new JDialog(this, "Configuration du tournoi", true);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(0, 200, 100));
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        JLabel title = new JLabel("Configuration du tournoi");
        title.setFont(new Font("Monospaced", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(20));

        // Sélecteur IA A
        JPanel rowA = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowA.setOpaque(false);
        rowA.setMaximumSize(new Dimension(300, 36));
        JLabel labelA = new JLabel("IA A :");
        labelA.setForeground(new Color(190, 190, 190));
        labelA.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JComboBox<String> comboA = new JComboBox<>(aiNames);
        comboA.setSelectedIndex(1); // Moyen par défaut
        styleCombo(comboA);
        rowA.add(labelA);
        rowA.add(comboA);
        panel.add(rowA);
        panel.add(Box.createVerticalStrut(10));

        // Sélecteur IA B
        JPanel rowB = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowB.setOpaque(false);
        rowB.setMaximumSize(new Dimension(300, 36));
        JLabel labelB = new JLabel("IA B :");
        labelB.setForeground(new Color(190, 190, 190));
        labelB.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JComboBox<String> comboB = new JComboBox<>(aiNames);
        comboB.setSelectedIndex(2); // Difficile par défaut
        styleCombo(comboB);
        rowB.add(labelB);
        rowB.add(comboB);
        panel.add(rowB);
        panel.add(Box.createVerticalStrut(10));

        // Nombre de parties
        JPanel rowN = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        rowN.setOpaque(false);
        rowN.setMaximumSize(new Dimension(300, 36));
        JLabel labelN = new JLabel("Parties :");
        labelN.setForeground(new Color(190, 190, 190));
        labelN.setFont(new Font("SansSerif", Font.PLAIN, 13));
        SpinnerNumberModel spinModel = new SpinnerNumberModel(100, 2, 10000, 2);
        JSpinner spinner = new JSpinner(spinModel);
        spinner.setPreferredSize(new Dimension(80, 28));
        rowN.add(labelN);
        rowN.add(spinner);
        panel.add(rowN);
        panel.add(Box.createVerticalStrut(24));

        JButton runBtn = makeEndButton("Lancer le tournoi",
                new Color(0, 200, 100), new Color(10, 10, 10));
        runBtn.setMaximumSize(new Dimension(300, 42));
        runBtn.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(runBtn);
        panel.add(Box.createVerticalStrut(8));

        JButton cancelBtn = makeEndButton("Annuler",
                new Color(35, 35, 35), new Color(130, 130, 130));
        cancelBtn.setMaximumSize(new Dimension(300, 40));
        cancelBtn.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(cancelBtn);

        cancelBtn.addActionListener(e -> {
            d.dispose();
            dispose();
            SwingUtilities.invokeLater(() -> new OthelloGUI().startGame());
        });

        // Lance le tournoi avec les paramètres choisis
        runBtn.addActionListener(e -> {
            int levelA = comboA.getSelectedIndex() + 1;
            int levelB = comboB.getSelectedIndex() + 1;
            int n = (int) spinner.getValue();
            if (n % 2 != 0)
                n++; // Nombre pair pour alternance équitable
            d.dispose();
            runTournament(levelA, levelB, n);
        });

        d.setContentPane(panel);
        d.setSize(380, 300);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(new Color(35, 35, 35));
        combo.setForeground(new Color(200, 200, 200));
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        combo.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60)));
    }

    // -------------------------------------------------------
    // Exécution du tournoi en arrière-plan
    // -------------------------------------------------------

    /**
     * Lance le tournoi dans un thread séparé pour ne pas bloquer l'interface.
     * Capture la sortie console du tournoi pour l'afficher dans une fenêtre
     * graphique (au lieu de juste dans le terminal).
     */
    private void runTournament(int levelA, int levelB, int n) {
        String[] names = { "", "Facile", "Moyen", "Difficile" };
        String nameA = names[levelA] + " (N" + levelA + ")";
        String nameB = names[levelB] + " (N" + levelB + ")";

        statusLabel.setText("Tournoi en cours…  " + nameA + " vs " + nameB);

        Thread t = new Thread(() -> {
            // Redirige System.out vers un buffer pour capturer les résultats
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream old = System.out;
            System.setOut(new PrintStream(baos));

            // Profondeur réduite en tournoi pour avoir des résultats rapides
            // N1=depth 1, N2=depth 3, N3=depth 4 (au lieu de 5)
            int depthA = (levelA == 3) ? 4 : (levelA == 2) ? 3 : 1;
            int depthB = (levelB == 3) ? 4 : (levelB == 2) ? 3 : 1;
            Tournament tournament = new Tournament(
                    new AIPlayer(levelA, depthA), nameA,
                    new AIPlayer(levelB, depthB), nameB);
            tournament.run(n);

            System.setOut(old); // Restaure la sortie console normale
            String results = baos.toString();
            System.out.print(results); // Affiche aussi dans le terminal

            // Affiche les résultats dans l'interface graphique
            SwingUtilities.invokeLater(() -> showTournamentResults(results, nameA, nameB));
        }, "TournamentRunner");

        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------
    // Dialogue de résultats du tournoi
    // -------------------------------------------------------

    /**
     * Affiche les résultats du tournoi dans une fenêtre dédiée.
     * Montre le texte formaté produit par Tournament.printSummary()
     * dans une zone de texte scrollable.
     */
    private void showTournamentResults(String rawResults,
            String nameA, String nameB) {
        JDialog d = new JDialog(this, "Résultats du tournoi", true);
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(0, 200, 100));
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(24, 32, 20, 32));

        JLabel title = new JLabel("Résultats du tournoi");
        title.setFont(new Font("Monospaced", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        // Zone de texte scrollable pour les résultats
        JTextArea area = new JTextArea(rawResults);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setForeground(new Color(190, 190, 190));
        area.setBackground(new Color(28, 28, 28));
        area.setEditable(false);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JScrollPane scroll = new JScrollPane(area);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
        scroll.setPreferredSize(new Dimension(360, 190));
        scroll.setMaximumSize(new Dimension(360, 190));
        scroll.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(scroll);
        panel.add(Box.createVerticalStrut(16));

        JButton againBtn = makeEndButton("Nouveau tournoi",
                new Color(0, 200, 100), new Color(10, 10, 10));
        JButton menuBtn = makeEndButton("Menu principal",
                new Color(45, 45, 45), new Color(200, 200, 200));
        JButton quitBtn2 = makeEndButton("Quitter",
                new Color(30, 30, 30), new Color(90, 90, 90));

        for (JButton b : new JButton[] { againBtn, menuBtn, quitBtn2 }) {
            b.setMaximumSize(new Dimension(360, 42));
            b.setAlignmentX(CENTER_ALIGNMENT);
        }

        againBtn.addActionListener(e -> {
            d.dispose();
            launchTournamentDialog();
        });
        menuBtn.addActionListener(e -> {
            d.dispose();
            dispose();
            SwingUtilities.invokeLater(() -> new OthelloGUI().startGame());
        });
        quitBtn2.addActionListener(e -> {
            d.dispose();
            dispose();
            System.exit(0);
        });

        panel.add(againBtn);
        panel.add(Box.createVerticalStrut(6));
        panel.add(menuBtn);
        panel.add(Box.createVerticalStrut(6));
        panel.add(quitBtn2);

        d.setContentPane(panel);
        d.setSize(440, 430);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // -------------------------------------------------------
    // Dialogue de fin de partie
    // -------------------------------------------------------

    /**
     * Affiche le résultat de la partie quand elle est terminée.
     * Montre qui a gagné, les scores finaux, et propose de :
     * - Rejouer la même configuration
     * - Lancer une nouvelle partie (retour au launcher)
     * - Quitter le programme
     */
    private void showEndDialog() {
        Board board = gameState.getBoard();
        int blackScore = board.getScore(GameState.BLACK);
        int whiteScore = board.getScore(GameState.WHITE);

        String resultLine;
        Color resultColor;
        if (blackScore > whiteScore) {
            resultLine = vsAI ? "Vous avez gagné !" : "Victoire NOIR !";
            resultColor = new Color(0, 200, 100);
        } else if (whiteScore > blackScore) {
            resultLine = vsAI ? "L'IA a gagné !" : "Victoire BLANC !";
            resultColor = vsAI ? new Color(220, 80, 80) : new Color(200, 200, 200);
        } else {
            resultLine = "Égalité !";
            resultColor = new Color(220, 180, 0);
        }

        statusLabel.setText(resultLine + "   ⚫ " + blackScore
                + "  —  ⚪ " + whiteScore);

        JDialog dialog = new JDialog(this, "Partie terminée", true);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        final Color rc = resultColor;
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(20, 20, 20));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(rc);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(28, 36, 24, 36));

        JLabel resLabel = new JLabel(resultLine);
        resLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        resLabel.setForeground(resultColor);
        resLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(resLabel);
        panel.add(Box.createVerticalStrut(20));

        JPanel scoreRow = new JPanel(new GridLayout(1, 2, 16, 0));
        scoreRow.setOpaque(false);
        scoreRow.add(makeScoreBox("⚫ NOIR", blackScore));
        scoreRow.add(makeScoreBox("⚪ BLANC", whiteScore));
        scoreRow.setAlignmentX(CENTER_ALIGNMENT);
        scoreRow.setMaximumSize(new Dimension(260, 80));
        panel.add(scoreRow);
        panel.add(Box.createVerticalStrut(8));

        // Affiche le pourcentage de cases occupées par chaque joueur
        int total = blackScore + whiteScore;
        int blackPct = (total > 0) ? Math.round(100f * blackScore / total) : 50;
        JLabel pctLabel = new JLabel(blackPct + "% NOIR  /  "
                + (100 - blackPct) + "% BLANC");
        pctLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pctLabel.setForeground(new Color(80, 80, 80));
        pctLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(pctLabel);
        panel.add(Box.createVerticalStrut(24));

        JButton replayBtn = makeEndButton("Rejouer",
                new Color(0, 200, 100), new Color(10, 10, 10));
        JButton newGameBtn = makeEndButton("Nouvelle partie",
                new Color(45, 45, 45), new Color(200, 200, 200));
        JButton quitBtn = makeEndButton("Quitter",
                new Color(30, 30, 30), new Color(90, 90, 90));

        replayBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            SwingUtilities.invokeLater(() -> new OthelloGUI(vsAI, currentLevel).startGame());
        });
        newGameBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            SwingUtilities.invokeLater(() -> new OthelloGUI().startGame());
        });
        quitBtn.addActionListener(e -> {
            dialog.dispose();
            dispose();
            System.exit(0);
        });

        JPanel btnPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        btnPanel.setOpaque(false);
        btnPanel.setAlignmentX(CENTER_ALIGNMENT);
        btnPanel.setMaximumSize(new Dimension(260, 152));
        btnPanel.add(replayBtn);
        btnPanel.add(newGameBtn);
        btnPanel.add(quitBtn);
        panel.add(btnPanel);

        dialog.setContentPane(panel);
        dialog.setSize(340, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    // Méthodes utilitaires partagées
    // -------------------------------------------------------

    /**
     * Crée une boîte affichant le score d'un joueur (utilisée dans showEndDialog).
     */
    private JPanel makeScoreBox(String label, int score) {
        JPanel box = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(35, 35, 35));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
        };
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 8));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lbl.setForeground(new Color(130, 130, 130));
        lbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel num = new JLabel(String.valueOf(score));
        num.setFont(new Font("Monospaced", Font.BOLD, 28));
        num.setForeground(Color.WHITE);
        num.setAlignmentX(CENTER_ALIGNMENT);

        box.add(lbl);
        box.add(num);
        return box;
    }

    /**
     * Crée un bouton stylisé avec fond coloré et texte personnalisé.
     * Utilisé pour tous les boutons des dialogues (Rejouer, Quitter etc.)
     * Le bouton change légèrement de couleur au survol et au clic.
     */
    private JButton makeEndButton(String text, Color bg, Color fg) {
        final String t = text;
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = getBackground();
                if (getModel().isPressed())
                    g2.setColor(base.darker());
                else if (getModel().isRollover())
                    g2.setColor(base.brighter());
                else
                    g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(t,
                        (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
            }
        };
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(0, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // -------------------------------------------------------
    // Point d'entrée statique
    // -------------------------------------------------------

    /**
     * Méthode statique appelée par Main.java pour lancer le jeu.
     * SwingUtilities.invokeLater() garantit que la création de la fenêtre
     * se fait dans le thread EDT (Event Dispatch Thread) de Swing,
     * ce qui est obligatoire pour la thread-safety de l'interface graphique.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> new OthelloGUI().startGame());
    }
}