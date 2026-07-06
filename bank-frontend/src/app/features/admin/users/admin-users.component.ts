import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AdminUserResponse, UserRole } from '../../../core/models/admin-user.model';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.css'
})
export class AdminUsersComponent implements OnInit {

  users = signal<AdminUserResponse[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  actionPendingId = signal<number | null>(null);
  resetPasswordSuccessFor = signal<number | null>(null);
  confirmDeleteId = signal<number | null>(null);

  constructor(private adminUserService: AdminUserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);
    this.adminUserService.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger la liste des utilisateurs.");
        this.loading.set(false);
      }
    });
  }

  toggleStatus(user: AdminUserResponse): void {
    this.actionPendingId.set(user.id);
    this.adminUserService.updateStatus(user.id, { enabled: !user.enabled }).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.actionPendingId.set(null);
      },
      error: () => {
        this.error.set("Impossible de mettre à jour le statut de cet utilisateur.");
        this.actionPendingId.set(null);
      }
    });
  }

  changeRole(user: AdminUserResponse, role: string): void {
    const newRole = role as UserRole;
    if (newRole === user.role) return;

    this.actionPendingId.set(user.id);
    this.adminUserService.updateRole(user.id, { role: newRole }).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.actionPendingId.set(null);
      },
      error: () => {
        this.error.set("Impossible de mettre à jour le rôle de cet utilisateur.");
        this.actionPendingId.set(null);
      }
    });
  }

  onRoleSelect(user: AdminUserResponse, event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.changeRole(user, value);
  }

  resetPassword(user: AdminUserResponse): void {
    this.actionPendingId.set(user.id);
    this.adminUserService.resetPassword(user.id).subscribe({
      next: () => {
        this.resetPasswordSuccessFor.set(user.id);
        this.actionPendingId.set(null);
        setTimeout(() => {
          if (this.resetPasswordSuccessFor() === user.id) {
            this.resetPasswordSuccessFor.set(null);
          }
        }, 5000);
      },
      error: () => {
        this.error.set("Impossible de réinitialiser le mot de passe.");
        this.actionPendingId.set(null);
      }
    });
  }

  closeResetPasswordSuccess(): void {
    this.resetPasswordSuccessFor.set(null);
  }

  askDeleteConfirmation(userId: number): void {
    this.confirmDeleteId.set(userId);
  }

  cancelDelete(): void {
    this.confirmDeleteId.set(null);
  }

  confirmDelete(userId: number): void {
    this.actionPendingId.set(userId);
    this.adminUserService.deleteUser(userId).subscribe({
      next: () => {
        this.users.update(list => list.filter(u => u.id !== userId));
        this.actionPendingId.set(null);
        this.confirmDeleteId.set(null);
      },
      error: () => {
        this.error.set("Impossible de supprimer cet utilisateur.");
        this.actionPendingId.set(null);
        this.confirmDeleteId.set(null);
      }
    });
  }
}
