import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    # Clé API Groq (gratuite) — https://console.groq.com/keys
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")

    # Modèles gratuits disponibles chez Groq (rapides, généreux en quota) :
    # "llama-3.3-70b-versatile"  -> meilleure qualité de réponse
    # "llama-3.1-8b-instant"     -> le plus rapide
    GROQ_MODEL: str = os.getenv("GROQ_MODEL", "llama-3.1-8b-instant")

    # Port du service (cohérent avec auth-service:8081, account-service:8082, credit-service:8083)
    PORT: int = int(os.getenv("PORT", "8084"))

    # Origines autorisées en CORS (frontend Angular en dev)
    ALLOWED_ORIGINS: list[str] = os.getenv(
        "ALLOWED_ORIGINS", "http://localhost:4200"
    ).split(",")

    # URLs des autres microservices, pour que le chatbot puisse aller chercher
    # de vraies données (solde, statut crédit...) plus tard si besoin
    ACCOUNT_SERVICE_URL: str = os.getenv("ACCOUNT_SERVICE_URL", "http://localhost:8082")
    CREDIT_SERVICE_URL: str = os.getenv("CREDIT_SERVICE_URL", "http://localhost:8083")
    AUTH_SERVICE_URL: str = os.getenv("AUTH_SERVICE_URL", "http://localhost:8081")


settings = Settings()
