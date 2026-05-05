package othello;

/**
 * Point d'entrée du programme.
 * C'est la première classe exécutée quand on lance le jeu.
 *
 * Son rôle est minimaliste : elle délègue immédiatement le lancement
 * à OthelloGUI qui gère toute l'interface graphique.
 *
 * On a fait ce choix pour séparer clairement les responsabilités :
 * - Main.java : juste lancer le programme
 * - OthelloGUI : gérer toute l'interface graphique et la boucle de jeu
 * - GameState : gérer la logique du jeu
 * - AIPlayer : gérer l'intelligence artificielle
 *
 * C'est le principe de séparation des responsabilités en POO.
 */
public class Main {
    public static void main(String[] args) {
        // Lance l'interface graphique dans le thread Swing (EDT)
        // SwingUtilities.invokeLater() dans OthelloGUI.launch() garantit
        // que l'interface est créée dans le bon thread Java
        OthelloGUI.launch();
    }
}