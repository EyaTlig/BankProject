import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink, RouterLinkActive, NavigationStart } from '@angular/router';
import {AuthService} from '../../core/services/auth.service';
import {IdleTimeoutService} from '../../core/services/idle-timeout.service';
import { ChatbotWidgetComponent } from '../chatbot/chatbot-widget.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ChatbotWidgetComponent],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit, OnDestroy {

  mobileNavOpen = signal(false);

  constructor(
    public authService: AuthService,
    public idleTimeoutService: IdleTimeoutService,
    private router: Router
  ) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationStart) {
        this.mobileNavOpen.set(false);
      }
    });
  }

  ngOnInit(): void {
    this.idleTimeoutService.start();
  }

  ngOnDestroy(): void {
    this.idleTimeoutService.stop();
  }

  toggleMobileNav(): void {
    this.mobileNavOpen.update(open => !open);
  }

  closeMobileNav(): void {
    this.mobileNavOpen.set(false);
  }

  logout(): void {
    this.idleTimeoutService.stop();
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
