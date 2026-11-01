# Commandes Salon (Android)

Application Android qui consomme l'API de prise de commande du salon de thé.
Kotlin, Jetpack Compose, OkHttp, kotlinx.serialization.

## Fonctionnalités

- Saisie de la clé d'accès du personnel (stockée chiffrée sur l'appareil).
- Formulaire de commande : choix de la table (1 à 10), produits du menu regroupés par catégorie, quantités de 1 à 50.
- Liste des commandes en cours, avec le bouton de l'étape suivante (Démarrer, Marquer prête, Marquer servie, Marquer payée).
- Rafraîchissement toutes les 3 secondes.

## Installation

1. Ouvrez le dossier `commandes-android` dans **Android Studio** (version récente, Ladybug ou plus).
2. Laissez Gradle synchroniser le projet.
3. Dans `app/build.gradle.kts`, remplacez `VOTRE-PROJET.vercel.app` par l'adresse de votre API. Elle doit se terminer par `/`.
4. Lancez l'application sur un téléphone ou un émulateur.
5. Au premier lancement, saisissez la clé d'accès (la même que `API_KEY` côté serveur).

### API en local

Avec l'émulateur Android, `localhost` désigne l'émulateur lui-même. Utilisez `http://10.0.2.2:3000/` comme `API_URL`.
Le trafic HTTP en clair est refusé par défaut (`usesCleartextTraffic="false"`) : pour un test local, activez-le temporairement dans le manifeste, en mode debug uniquement.

## Structure

```
app/src/main/java/fr/salon/commandes/
├── MainActivity.kt          point d'entrée
├── AppViewModel.kt          état, rafraîchissement, actions
├── data/
│   ├── Modeles.kt           modèles (menu, commandes)
│   ├── ApiClient.kt         appels HTTP vers l'API
│   └── CleStore.kt          stockage chiffré de la clé
└── ui/
    ├── EcranCle.kt          saisie de la clé
    ├── EcranPrincipal.kt    écran principal et cartes de commande
    └── CarteFormulaire.kt   formulaire de nouvelle commande
```

## Limites

- Le projet n'a pas été compilé dans l'environnement où il a été écrit : lancez-le dans Android Studio et signalez toute erreur de compilation.
- Pas de notification ni de temps réel : l'application interroge l'API toutes les 3 secondes.
- La clé est partagée par tout le personnel, comme côté API.
- Le formulaire vide ses lignes après l'envoi, même si le serveur refuse la commande : le message d'erreur reste visible.
