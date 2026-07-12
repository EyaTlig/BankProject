import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  AdminUserResponse,
  CreateClientRequest,
  CreateEmployeeRequest,
  UserRole,
  AdminPermission,
  PERMISSION_MODULES
} from '../../../core/models/admin-user.model';

type CreateMode = 'client' | 'employee';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  showCreateForm = signal(false);
  createMode = signal<CreateMode>('client');
  creating = signal(false);
  createError = signal<string | null>(null);
  createSuccess = signal<string | null>(null);
  newClient: CreateClientRequest = { firstName: '', lastName: '', email: '' };
  newEmployee: { firstName: string; lastName: string; email: string } = { firstName: '', lastName: '', email: '' };
  newEmployeePermissions = signal<Set<AdminPermission>>(new Set());

  permissionModules = PERMISSION_MODULES;
  permissionsPanelId = signal<number | null>(null);
  editingPermissions = signal<Set<AdminPermission>>(new Set());
  savingPermissions = signal(false);
  permissionsError = signal<string | null>(null);

  constructor(
    private adminUserService: AdminUserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  openCreateForm(): void {
    this.newClient = { firstName: '', lastName: '', email: '' };
    this.newEmployee = { firstName: '', lastName: '', email: '' };
    this.newEmployeePermissions.set(new Set());
    this.createMode.set('client');
    this.createError.set(null);
    this.createSuccess.set(null);
    this.showCreateForm.set(true);
  }

  setCreateMode(mode: CreateMode): void {
    this.createMode.set(mode);
    this.createError.set(null);
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.createError.set(null);
  }

  isNewEmployeePermissionChecked(permission: AdminPermission): boolean {
    return this.newEmployeePermissions().has(permission);
  }

  toggleNewEmployeePermission(permission: AdminPermission): void {
    const current = new Set(this.newEmployeePermissions());
    if (current.has(permission)) {
      current.delete(permission);
    } else {
      current.add(permission);
    }
    this.newEmployeePermissions.set(current);
  }

  submitCreateClient(): void {
    const { firstName, lastName, email } = this.newClient;
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      this.createError.set('Tous les champs sont obligatoires.');
      return;
    }

    this.creating.set(true);
    this.createError.set(null);
    this.adminUserService.createClient(this.newClient).subscribe({
      next: (created) => {
        this.users.update(list => [created, ...list]);
        this.creating.set(false);
        this.showCreateForm.set(false);
        this.createSuccess.set(`Client ${created.firstName} ${created.lastName} créé. Ses identifiants ont été envoyés par email à ${created.email}.`);
        setTimeout(() => this.createSuccess.set(null), 6000);
      },
      error: (err) => {
        this.createError.set(err?.error?.message || "Impossible de créer ce client.");
        this.creating.set(false);
      }
    });
  }

  submitCreateEmployee(): void {
    const { firstName, lastName, email } = this.newEmployee;
    if (!firstName.trim() || !lastName.trim() || !email.trim()) {
      this.createError.set('Tous les champs sont obligatoires.');
      return;
    }

    const request: CreateEmployeeRequest = {
      firstName,
      lastName,
      email,
      permissions: Array.from(this.newEmployeePermissions())
    };

    this.creating.set(true);
    this.createError.set(null);
    this.adminUserService.createEmployee(request).subscribe({
      next: (created) => {
        this.users.update(list => [created, ...list]);
        this.creating.set(false);
        this.showCreateForm.set(false);
        this.createSuccess.set(`Employé ${created.firstName} ${created.lastName} créé. Ses identifiants ont été envoyés par email à ${created.email}.`);
        setTimeout(() => this.createSuccess.set(null), 6000);
      },
      error: (err) => {
        this.createError.set(err?.error?.message || "Impossible de créer cet employé.");
        this.creating.set(false);
      }
    });
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

  openPermissionsPanel(user: AdminUserResponse): void {
    this.editingPermissions.set(new Set(user.permissions));
    this.permissionsError.set(null);
    this.permissionsPanelId.set(user.id);
  }

  closePermissionsPanel(): void {
    this.permissionsPanelId.set(null);
    this.permissionsError.set(null);
  }

  isPermissionChecked(permission: AdminPermission): boolean {
    return this.editingPermissions().has(permission);
  }

  togglePermission(permission: AdminPermission): void {
    const current = new Set(this.editingPermissions());
    if (current.has(permission)) {
      current.delete(permission);
    } else {
      current.add(permission);
    }
    this.editingPermissions.set(current);
  }

  savePermissions(user: AdminUserResponse): void {
    this.savingPermissions.set(true);
    this.permissionsError.set(null);
    const permissions = Array.from(this.editingPermissions());

    this.adminUserService.updatePermissions(user.id, { permissions }).subscribe({
      next: (updated) => {
        this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
        this.savingPermissions.set(false);
        this.permissionsPanelId.set(null);
      },
      error: (err) => {
        this.permissionsError.set(err?.error?.message || "Impossible de mettre à jour les permissions.");
        this.savingPermissions.set(false);
      }
    });
  }
}
