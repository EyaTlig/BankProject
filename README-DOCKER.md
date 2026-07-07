# Bank Platform — Tout en Docker

Ce package ajoute la conteneurisation complète : 3 services Spring Boot,
le chatbot Python, le frontend Angular, en plus des 5 bases MySQL déjà
présentes dans votre `docker-compose.yml`.

## 1. Où placer les fichiers

Copiez chaque fichier au même chemin relatif dans votre projet :

```
bank-platform/
├── docker-compose.yml                          ← remplace le vôtre
├── .env.example                                 ← nouveau (racine)
├── bank-backend/
│   ├── auth-service/Dockerfile                  ← nouveau
│   ├── auth-service/.dockerignore               ← nouveau
│   ├── account-service/Dockerfile               ← nouveau
│   ├── account-service/.dockerignore            ← nouveau
│   ├── credit-service/Dockerfile                ← nouveau
│   ├── credit-service/.dockerignore             ← nouveau
│   ├── chatbot-service/Dockerfile                ← nouveau
│   └── chatbot-service/.dockerignore            ← nouveau
└── bank-frontend/
    ├── Dockerfile                                ← nouveau
    ├── nginx.conf                                ← nouveau
    └── .dockerignore                             ← nouveau
```

## 2. Configurer la clé Groq

À la racine du projet (à côté de `docker-compose.yml`) :

```bash
cp .env.example .env
```

Ouvrez `.env` et collez votre clé :
```
GROQ_API_KEY=gsk_votre_clé_ici
```

## 3. Lancer TOUTE la plateforme

Une seule commande, depuis la racine du projet :

```bash
docker compose up --build
```

Ça va, dans l'ordre :
1. Démarrer les 5 bases MySQL + RabbitMQ + phpMyAdmin
2. Builder (Maven) puis démarrer les 3 services Spring Boot
3. Builder (pip) puis démarrer le chatbot Python
4. Builder (npm + Angular prod) puis servir le frontend via nginx

**Premier lancement : comptez 5 à 10 minutes** (téléchargement des images de base + compilation Maven/Angular). Les lancements suivants seront bien plus rapides grâce au cache Docker.

## 4. Accéder à l'application

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| auth-service | http://localhost:8081 |
| account-service | http://localhost:8082 |
| credit-service | http://localhost:8083 |
| chatbot-service | http://localhost:8084/health |
| phpMyAdmin | http://localhost:8086 |
| RabbitMQ management | http://localhost:15672 |

## 5. Commandes utiles

```bash
# Voir les logs d'un service en particulier
docker compose logs -f chatbot-service
docker compose logs -f auth-service

# Arrêter tout
docker compose down

# Arrêter et supprimer aussi les volumes (reset complet des bases)
docker compose down -v

# Reconstruire un seul service après une modification de code
docker compose up --build auth-service

# Voir l'état de tous les conteneurs
docker compose ps
```

## 6. Points d'attention

- **Build du frontend** : le build Angular en mode production télécharge les polices Google
  Fonts pendant le build (`fonts.googleapis.com`) — nécessite une connexion internet active
  pendant `docker compose up --build`. Si ça échoue avec une erreur "Inlining of fonts failed",
  relancez simplement (souvent un problème réseau temporaire).
- **Base de données** : `spring.jpa.hibernate.ddl-auto=update` dans vos `application.properties`
  crée automatiquement les tables au premier démarrage — rien à faire manuellement.
- **Variables d'environnement** : les URLs de connexion MySQL sont surchargées automatiquement
  via `SPRING_DATASOURCE_URL` dans le docker-compose (pointent vers les conteneurs `mysql-auth`,
  `mysql-account`, `mysql-credit` au lieu de `localhost`) — vos fichiers `application.properties`
  ne sont pas modifiés, ça reste compatible avec un lancement local classique (hors Docker).
- **Le frontend appelle toujours `http://localhost:808x`** depuis le navigateur (pas les noms de
  conteneurs), ce qui fonctionne car tous les ports sont publiés sur la machine hôte.

## 7. Si un service ne démarre pas

```bash
docker compose logs <nom-du-service>
```

Cas fréquents :
- **chatbot-service en erreur 500** → vérifiez que `GROQ_API_KEY` est bien dans `.env` à la racine
- **auth/account/credit-service ne se connecte pas à MySQL** → attendez quelques secondes, MySQL
  met un peu de temps à être prêt au tout premier démarrage ; relancez avec
  `docker compose up auth-service` si besoin
