# Commandes Salon (Android)

Application Android de prise de commande pour le salon de thé. Elle utilise l'API GraphQL `api-commandes`.

Kotlin, Jetpack Compose (Material 3), OkHttp, kotlinx.serialization.

## Fonctionnalités

- **Accès** : saisie de la clé du personnel (la même que `API_KEY` côté serveur), sur une carte centrée.
- **Nouvelle commande** :
  - grille de 10 tables en grandes cases ;
  - produits du menu en cartes, regroupés par catégorie : un appui ajoute une unité, « − » en retire une ;
  - panier fixe en bas de l'écran avec la table, le nombre d'articles, le total en dinars et le bouton d'envoi.
- **En cours** : une carte par commande, avec une bande de couleur selon le statut, le numéro de table, l'ancienneté, les produits et le bouton de l'étape suivante (Démarrer, Marquer prête, Marquer servie, Marquer payée). Le nombre de commandes s'affiche sur l'onglet.
- Rafraîchissement automatique toutes les 3 secondes.

## API utilisée

L'application appelle `<API_URL>graphql`. Par défaut, `API_URL` vaut `https://api-commandes-dun.vercel.app/`.
Pour viser une autre API, définissez le secret `API_URL` du dépôt (*Settings > Secrets and variables > Actions*), terminé par `/`.

Opérations GraphQL utilisées : `menu`, `commandes`, `creerCommande`, `changerStatut`.

## Obtenir l'APK

Chaque envoi sur `main` lance le workflow **Générer l'APK** (onglet **Actions**). L'APK est publié :
- dans la release `apk-latest`, téléchargeable sans connexion :
  https://github.com/daly64/commandes-android/releases/download/apk-latest/app-debug.apk
- en artefact `commandes-salon-debug` sur la page du run.

En cas d'échec, le journal complet est publié dans la release `build-log`.

## Compiler sur son poste

```bash
./gradlew assembleDebug
```

L'APK est produit dans `app/build/outputs/apk/debug/app-debug.apk`.

## Structure

```
app/src/main/java/fr/salon/commandes/
├── MainActivity.kt              point d'entrée
├── AppViewModel.kt              état, panier, rafraîchissement, actions
├── data/
│   ├── Modeles.kt               modèles et statuts
│   ├── ApiClient.kt             client GraphQL
│   └── CleStore.kt              stockage de la clé
└── ui/
    ├── theme/Theme.kt           couleurs, couleurs des statuts
    ├── Composants.kt            éléments communs (prix, ancienneté, pastille)
    ├── EcranCle.kt              carte d'accès
    ├── EcranPrincipal.kt        barre du haut, onglets, messages
    ├── EcranNouvelleCommande.kt tables, cartes produits, panier
    └── EcranCommandes.kt        cartes des commandes en cours
```

## Limites

- Pas de temps réel : l'API sur Vercel n'accepte pas les WebSockets, l'application interroge donc l'API toutes les 3 secondes.
- La clé est partagée par tout le personnel et gardée dans le stockage privé de l'application.
- Le total affiché est calculé dans l'application : l'API n'enregistre pas les prix.
- APK de test (debug), non signé pour le Play Store.
