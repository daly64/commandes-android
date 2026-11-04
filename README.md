# Commandes Salon (Android)

Application Android de prise de commande pour le salon de thé. Elle utilise l'API GraphQL `api-commandes`. L'accès est libre : il n'y a plus de clé à saisir.

Kotlin, Jetpack Compose (Material 3), OkHttp, kotlinx.serialization.

## Fonctionnalités

- **Nouvelle commande** :
  - bandeau des 10 tables toujours visible en haut de l'écran ;
  - recherche de produit par nom ;
  - produits regroupés par catégorie : un appui sur la carte ajoute une unité, le compteur « − n + » apparaît ensuite ;
  - panier fixe en bas de l'écran avec la table, le nombre d'articles, le total en dinars ; le bouton indique ce qui manque (choisir une table, ajouter des produits).
- **En cours** :
  - filtres par statut en haut (Toutes, En attente, En préparation, Prêtes, Servies), avec le nombre de commandes dans chacun ;
  - une carte par commande : bande de couleur selon le statut, table, ancienneté, produits et bouton de l'étape suivante (Démarrer, Marquer prête, Marquer servie, Marquer payée) ;
  - une commande non prête depuis plus de 15 minutes passe en rouge, avec la mention « en retard ».
  Le nombre de commandes s'affiche sur l'onglet.
- **Temps réel** : abonnement SSE à `<API_URL>graphql`. Chaque commande créée ou modifiée arrive immédiatement ; une commande payée disparaît de la liste. À chaque ouverture du flux, la liste est relue ; en cas de coupure, la reconnexion se fait toutes les 3 s.

## API utilisée

L'application appelle `<API_URL>graphql`. Par défaut, `API_URL` vaut `https://api-commandes-dun.vercel.app/`.
Pour viser une autre API, définissez le secret `API_URL` du dépôt (*Settings > Secrets and variables > Actions*), terminé par `/`.

Opérations GraphQL utilisées : `menu`, `commandes`, `creerCommande`, `changerStatut`, et l'abonnement `commandeModifiee`.

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
├── AppViewModel.kt              état, panier, flux temps réel, actions
├── data/
│   ├── Modeles.kt               modèles et statuts
│   ├── ApiClient.kt             requêtes et mutations GraphQL
│   └── FluxCommandes.kt         abonnement SSE
└── ui/
    ├── theme/Theme.kt           couleurs, couleurs des statuts
    ├── Composants.kt            éléments communs (prix, ancienneté, pastille)
    ├── EcranPrincipal.kt        barre du haut, onglets, messages
    ├── EcranNouvelleCommande.kt tables, cartes produits, panier
    └── EcranCommandes.kt        cartes des commandes en cours
```

## Limites

- Le flux SSE est fermé par le serveur au bout de 5 minutes environ (limite Vercel) ; l'application se reconnecte et relit la liste.
- Le total affiché est calculé dans l'application : l'API n'enregistre pas les prix.
- L'API n'a pas d'accès protégé : toute personne qui connaît l'adresse peut lire et modifier les commandes.
- APK de test (debug), non signé pour le Play Store.