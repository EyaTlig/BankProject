from pydantic import BaseModel, Field
from typing import Literal, Optional


class ChatMessage(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=2000)
    history: list[ChatMessage] = Field(default_factory=list)


class Suggestion(BaseModel):
    label: str
    path: str


class ChatResponse(BaseModel):
    reply: str
    suggestions: list[Suggestion] = Field(default_factory=list)
