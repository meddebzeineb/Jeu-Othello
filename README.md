# Othello IA — Toumi & Meddeb

Jeu d'Othello avec intelligence artificielle implémenté en Java.
Trois niveaux de difficulté basés sur l'algorithme Minimax avec élagage Alpha-Bêta.

## Prérequis

- Java JDK 11 ou supérieur
- Vérifier l'installation : `java -version`

## Structure du projet
├── src/
│   └── othello/
│       ├── Main.java                 ← point d'entrée du jeu
│       ├── ExperimentalAnalysis.java ← analyse des performances
│       ├── AIPlayer.java
│       ├── Board.java
│       ├── GameState.java
│       ├── HumanPlayer.java
│       ├── OthelloGUI.java
│       ├── Player.java
│       ├── Point.java
│       └── Tournament.java
├── Othello.jar                       ← exécutable JAR
└── README.md
## Lancer le jeu (méthode simple avec le JAR)

```bash
java -jar Othello.jar
```

## Compilation manuelle (optionnel)

Si vous souhaitez recompiler le projet depuis les sources :

```bash
javac -encoding UTF-8 -d out src/othello/*.java
java -cp out othello.Main
```

## Lancer l'analyse expérimentale

```bash
javac -encoding UTF-8 -d out src/othello/*.java
java -cp out othello.ExperimentalAnalysis
```

Affiche dans le terminal :
- Tableau des temps de calcul par fonction d'évaluation et par profondeur
- Résultats des tournois entre EV1, EV2 et EV3 à différentes profondeurs

## Modes de jeu

L'interface graphique s'ouvre avec un menu de choix :
- **Joueur vs Joueur** — deux humains s'affrontent
- **Facile** — IA niveau 1 (EV1, profondeur 1)
- **Moyen** — IA niveau 2 (EV2, profondeur 3)
- **Difficile** — IA niveau 3 (EV3, profondeur 5)
- **Tournoi** — IA vs IA avec choix des niveaux et du nombre de parties

## Niveaux de difficulté

| Niveau | Fonction d'évaluation | Profondeur |
|--------|----------------------|------------|
| Facile | EV1 — comptage de pions | 1 |
| Moyen | EV2 — poids positionnels | 3 |
| Difficile | EV3 — mobilité + coins + position | 5 |

## Auteurs

Imène TOUMI & Zeineb MEDDEB — Université Paris Cité, UE Intelligence Artificielle 2024-2025