import { Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Durée totale d'inactivité tolérée avant déconnexion automatique.
 * Valeur classique pour une application bancaire en ligne.
 */
const IDLE_LIMIT_MS = 5 * 60 * 1000; // 5 minutes

/**
 * Durée pendant laquelle un avertissement est affiché à l'utilisateur
 * avant la déconnexion effective, avec la possibilité de rester connecté.
 */
const WARNING_DURATION_MS = 30 * 1000; // 30 secondes

/**
 * Événements considérés comme une activité utilisateur.
 */
const ACTIVITY_EVENTS: Array<keyof DocumentEventMap> = [
  'mousemove', 'mousedown', 'keydown', 'scroll', 'touchstart', 'click'
];

/**
 * Surveille l'inactivité de l'utilisateur pendant qu'il est connecté et
 * déclenche une déconnexion automatique après IDLE_LIMIT_MS sans aucune
 * interaction, avec un avertissement affiché WARNING_DURATION_MS avant.
 *
 * Démarré/arrêté explicitement par LayoutComponent (qui enrobe toutes
 * les routes authentifiées), pour ne jamais tourner sur les pages
 * publiques (login, register, accueil...).
 */
@Injectable({ providedIn: 'root' })
export class IdleTimeoutService {

  readonly showWarning = signal(false);
  readonly secondsRemaining = signal(0);

  private lastActivity = Date.now();
  private checkIntervalId: ReturnType<typeof setInterval> | null = null;
  private readonly boundRegisterActivity = () => this.registerActivity();

  constructor(private authService: AuthService, private router: Router) {}

  start(): void {
    if (this.checkIntervalId !== null) {
      return; // déjà démarré (ex: navigation entre plusieurs routes du layout)
    }

    this.lastActivity = Date.now();
    this.showWarning.set(false);

    ACTIVITY_EVENTS.forEach(eventName => {
      document.addEventListener(eventName, this.boundRegisterActivity, { passive: true });
    });

    this.checkIntervalId = setInterval(() => this.checkIdleState(), 1000);
  }

  stop(): void {
    ACTIVITY_EVENTS.forEach(eventName => {
      document.removeEventListener(eventName, this.boundRegisterActivity);
    });

    if (this.checkIntervalId !== null) {
      clearInterval(this.checkIntervalId);
      this.checkIntervalId = null;
    }

    this.showWarning.set(false);
  }

  /** Appelé quand l'utilisateur clique sur "Rester connecté" dans le modal d'avertissement. */
  stayLoggedIn(): void {
    this.registerActivity();
  }

  private registerActivity(): void {
    this.lastActivity = Date.now();
    if (this.showWarning()) {
      this.showWarning.set(false);
    }
  }

  private checkIdleState(): void {
    const idleFor = Date.now() - this.lastActivity;
    const remaining = IDLE_LIMIT_MS - idleFor;

    if (remaining <= 0) {
      this.stop();
      this.authService.logout();
      this.router.navigate(['/login'], { queryParams: { reason: 'inactivity' } });
      return;
    }

    if (remaining <= WARNING_DURATION_MS) {
      this.showWarning.set(true);
      this.secondsRemaining.set(Math.ceil(remaining / 1000));
    }
  }
}
