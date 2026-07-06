from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import time
from typing import Optional

from .config import settings
from .schemas import ChatRequest, ChatResponse
from .groq_client import ask_groq_async
from .suggestions import get_suggestions

app = FastAPI(
    title="Bank Platform - Chatbot Service",
    description="Assistant conversationnel pour l'application bancaire (Groq / Llama).",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Cache simple pour éviter de refaire le même appel
_response_cache = {}
CACHE_TTL = 60  # Cache de 60 secondes

@app.get("/health")
def health():
    return {"status": "ok", "model": settings.GROQ_MODEL, "cache_size": len(_response_cache)}

@app.post("/api/chatbot/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    # Vérifier si la réponse est en cache
    cache_key = f"{request.message}_{len(request.history)}"
    if cache_key in _response_cache:
        cached_time, cached_response = _response_cache[cache_key]
        if time.time() - cached_time < CACHE_TTL:
            return cached_response

    try:
        # Appel asynchrone avec timeout global
        import asyncio
        try:
            reply = await asyncio.wait_for(
                ask_groq_async(request.message, request.history),
                timeout=15.0  # Timeout global de 15 secondes
            )
        except asyncio.TimeoutError:
            # Réponse de secours en cas de timeout
            reply = "Je suis désolé, le service est momentanément saturé. Voici quelques suggestions qui pourraient vous aider :"
            suggestions = get_suggestions(request.message)
            return ChatResponse(reply=reply, suggestions=suggestions)

        suggestions = get_suggestions(request.message)
        response = ChatResponse(reply=reply, suggestions=suggestions)

        # Mettre en cache
        _response_cache[cache_key] = (time.time(), response)

        # Nettoyer le cache si trop grand
        if len(_response_cache) > 100:
            oldest_key = min(_response_cache.keys(), key=lambda k: _response_cache[k][0])
            del _response_cache[oldest_key]

        return response

    except Exception as e:
        # Réponse de secours en cas d'erreur
        return ChatResponse(
            reply="Je suis désolé, je rencontre des difficultés techniques. Voici quelques actions rapides :",
            suggestions=get_suggestions(request.message)
        )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host="0.0.0.0",
        port=settings.PORT,
        reload=False,  # Désactiver reload en production
        workers=1,     # Un seul worker pour éviter les problèmes
        timeout_keep_alive=30,
    )