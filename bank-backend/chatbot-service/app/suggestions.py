"""
Détection légère par mots-clés pour proposer 1-2 boutons de navigation rapide
en plus de la réponse texte du LLM. Volontairement simple (pas de ML) :
robuste, prévisible, et suffisant pour orienter l'utilisateur.
"""

KEYWORD_ROUTES = [
    # Groupes de mots-clés avec leurs routes correspondantes
    (["solde", "compte", "historique", "transaction"], "Voir mes comptes", "/accounts"),
    (["virement groupé", "csv", "bénéficiaires"], "Virement groupé", "/bulk-transfers"),
    (["virement permanent", "récurrent", "planifi"], "Virements permanents", "/recurring-transfers"),
    (["virement", "transfert", "transférer"], "Faire un virement", "/transfers"),
    (["simuler", "simulation", "mensualité", "échéancier"], "Simuler un crédit", "/credits/simulation"),
    (["crédit", "prêt", "emprunt"], "Espace Crédits", "/credits"),
    # Ajout de mots-clés pour les actions courantes
    (["aide", "assistance", "pb", "problème"], "Aide et contact", "/help"),
    (["connexion", "login", "2fa", "authentification"], "Sécurité et connexion", "/security"),
]

def get_suggestions(user_message: str) -> list[dict]:
    """Retourne jusqu'à 2 suggestions pertinentes basées sur les mots-clés"""
    text = user_message.lower()
    matched = []
    seen_paths = set()

    # Prioriser les correspondances exactes
    for keywords, label, path in KEYWORD_ROUTES:
        if any(kw in text for kw in keywords):
            if path not in seen_paths:
                matched.append({"label": label, "path": path})
                seen_paths.add(path)
        if len(matched) >= 2:
            break

    # Si aucune suggestion, proposer l'accueil
    if not matched:
        matched.append({"label": "Accueil", "path": "/"})

    return matched