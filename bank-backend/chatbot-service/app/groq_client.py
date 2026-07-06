from groq import AsyncGroq
from fastapi import HTTPException

from .config import settings
from .system_prompt import SYSTEM_PROMPT
from .schemas import ChatMessage  # <-- import manquant

_client: AsyncGroq | None = None

def get_client() -> AsyncGroq:
    global _client
    if not settings.GROQ_API_KEY:
        raise HTTPException(
            status_code=500,
            detail="GROQ_API_KEY manquante. Définis-la dans le fichier .env "
                    "(clé gratuite sur https://console.groq.com/keys).",
        )
    if _client is None:
        _client = AsyncGroq(api_key=settings.GROQ_API_KEY)
    return _client

async def ask_groq_async(message: str, history: list[ChatMessage]) -> str:
    client = get_client()

    messages = [{"role": "system", "content": SYSTEM_PROMPT}]
    for h in history[-5:]:
        messages.append({"role": h.role, "content": h.content})
    messages.append({"role": "user", "content": message})

    try:
        completion = await client.chat.completions.create(
            model=settings.GROQ_MODEL,
            messages=messages,
            temperature=0.3,
            max_tokens=250,
        )
    except Exception as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Erreur lors de l'appel à l'API Groq : {exc}",
        )

    return completion.choices[0].message.content.strip()