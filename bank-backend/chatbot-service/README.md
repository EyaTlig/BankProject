# Chatbot Service — Bank Platform

Assistant conversationnel Python (FastAPI) utilisant l'API gratuite **Groq** (modèles Llama)
pour répondre aux questions des clients et les orienter dans l'application.

## Installation

```bash
cd bank-backend/chatbot-service
python -m venv venv
source venv/bin/activate      # Windows : venv\Scripts\activate
pip install -r requirements.txt
```

## Configuration

1. Crée une clé API gratuite sur https://console.groq.com/keys
2. Copie `.env.example` vers `.env` :
   ```bash
   cp .env .env
   ```
3. Colle ta clé dans `.env` :
   ```
   GROQ_API_KEY=gsk_xxxxxxxxxxxxxxxxxxxx
   ```

## Lancement

```bash
uvicorn app.main:app --reload --port 8084
```

Le service tourne sur **http://localhost:8084** (cohérent avec auth-service:8081,
account-service:8082, credit-service:8083).

Vérifier que ça fonctionne :
```bash
curl http://localhost:8084/health
```

## Endpoint

`POST /api/chatbot/chat`

```json
{
  "message": "Comment faire un virement ?",
  "history": [
    {"role": "user", "content": "Bonjour"},
    {"role": "assistant", "content": "Bonjour ! Comment puis-je t'aider ?"}
  ]
}
```

Réponse :
```json
{
  "reply": "Tu peux effectuer un virement simple depuis la section Virements (/transfers)...",
  "suggestions": [
    {"label": "Faire un virement", "path": "/transfers"}
  ]
}
```

## Notes

- Le modèle par défaut `llama-3.3-70b-versatile` est gratuit sur Groq avec des limites de
  débit généreuses (largement suffisant pour une démo/PFE).
- Le chatbot ne peut pas exécuter d'actions (virement, etc.) — il oriente uniquement
  l'utilisateur vers la bonne page (`suggestions` renvoie le chemin Angular à utiliser).
- Pour aller plus loin : ajouter un appel réel aux autres microservices (solde, statut crédit)
  en passant le JWT de l'utilisateur au chatbot-service.
