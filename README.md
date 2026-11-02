# Commandes Salon (Android)

Application Android de prise de commande pour le salon de thé. Elle utilise l'API `api-commandes`.

Kotlin, Jetpack Compose, OkHttp, kotlinx.serialization.

## Fonctionnalités

- Saisie de la clé d'accès du personnel (la même que `API_KEY` côté serveur).
- Nouvelle commande : table de 1 à 10, produits du menu regroupés par catégorie, quantités de 1 à 50.
- Liste des commandes en cours, avec le bouton de l'étape suivante : Démarrer, Marquer prête, Marquer servie, Marquer payée.
- Rafraîchissement automatique toutes les 3 secondes.

## Obtenir l'APK (GitHub Actions)

1. Dans le dépôt : **Settings > Secrets and variables > Actions > New repository secret**.
   Nom : `API_URL`. Valeur : l'adresse de l'API, terminée par `/` (par exemple `https://mon-api.vercel.app/`).
2. Chaque envoi sur `main` lance le workflow **Générer l'APK**. On peut aussi le lancer à la main : onglet **Actions**, puis **Run workflow**.
3. Quand le run est vert, l'APK `commandes-salon-debug` se télécharge en bas de la page du run, dans **Artifacts**.
4. Décompressez l'archive téléchargée et installez `app-debug.apk` sur le téléphone (autorisez l'installation d'applications de sources inconnues).

## Compiler sur son poste

Ouvrez le dossier dans Android Studio, ou lancez :

```bash
./gradlew assembleDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.
Sans variable `API_URL`, l'application vise l'adresse factice `https://VOTRE-PROJET.vercel.app/`.

## Structure

```
app/src/main/java/fr/salon/commandes/
├── MainActivity.kt          point d'entrée
├── AppViewModel.kt          état, rafraîchissement, actions
├── data/
│   ├── Modeles.kt           modèles (menu, commandes)
│   ├── ApiClient.kt         appels HTTP vers l'API
│   └── CleStore.kt          stockage de la clé
└── ui/
    ├── EcranCle.kt          saisie de la clé
    ├── EcranPrincipal.kt    écran principal et cartes de commande
    └── CarteFormulaire.kt   formulaire de nouvelle commande
```

## Limites

- Pas de temps réel : l'application interroge l'API toutes les 3 secondes.
- La clé est partagée par tout le personnel. Elle est gardée dans le stockage privé de l'application, sans chiffrement supplémentaire.
- APK de test (debug), non signé pour le Play Store.
