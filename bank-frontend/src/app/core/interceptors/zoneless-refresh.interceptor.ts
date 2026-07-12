import { HttpInterceptorFn } from '@angular/common/http';
import { ApplicationRef, inject } from '@angular/core';
import { tap } from 'rxjs';

/**
 * En mode zoneless (provideZonelessChangeDetection dans app.config.ts),
 * Angular ne redétecte pas automatiquement les changements déclenchés
 * dans un callback .subscribe() d'une requête HTTP : une propriété ou
 * un signal mis à jour dans "next"/"error" ne rafraîchit la vue qu'au
 * prochain événement DOM (clic, focus, etc.), ce qui donnait l'effet
 * de spinner "bloqué" tant que l'utilisateur ne cliquait pas ailleurs.
 *
 * Plutôt que d'ajouter ChangeDetectorRef.detectChanges() dans chaque
 * composant qui fait un appel HTTP, cet intercepteur force un tick
 * Angular global (ApplicationRef.tick()) juste après CHAQUE réponse
 * HTTP (succès ou erreur) reçue par l'application, de façon centralisée.
 */
export const zonelessRefreshInterceptor: HttpInterceptorFn = (req, next) => {
  const appRef = inject(ApplicationRef);

  return next(req).pipe(
    tap({
      next: () => queueMicrotask(() => appRef.tick()),
      error: () => queueMicrotask(() => appRef.tick())
    })
  );
};
