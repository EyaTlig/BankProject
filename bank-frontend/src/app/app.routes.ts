import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import {VerifyOtpComponent} from './features/auth/verify-otp/verify-otp.component';
import {LayoutComponent} from './shared/layout/layout.component';
import {authGuard} from './core/guards/auth.guard';
import {DashboardComponent} from './features/dashboard/dashboard.component';
import {TransferComponent} from './features/transfers/transfer.component';
import {RecurringTransferComponent} from './features/recurring-transfers/recurring-transfer.component';
import {AccountsComponent} from './features/accounts/accounts.component';
import {HomeComponent} from './features/public/home/home.component';
import {CreditsComponent} from './features/credits/credits.component';
import {adminGuard} from './core/guards/admin.guard';
import {clientOnlyGuard} from './core/guards/client-only.guard';
import {AdminDashboardComponent} from './features/admin/dashboard/admin-dashboard.component';
import {AdminUsersComponent} from './features/admin/users/admin-users.component';
import {AdminCreditsComponent} from './features/admin/credits/admin-credits.component';
import {BulkTransferComponent} from './features/bulk-transfers/bulk-transfer.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'verify-otp', component: VerifyOtpComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent, canActivate: [clientOnlyGuard] },
      { path: 'accounts', component: AccountsComponent, canActivate: [clientOnlyGuard] },
      { path: 'transfers', component: TransferComponent, canActivate: [clientOnlyGuard] },
      { path: 'transfers/bulk', component: BulkTransferComponent, canActivate: [clientOnlyGuard] },
      { path: 'recurring-transfers', component: RecurringTransferComponent, canActivate: [clientOnlyGuard] },
      { path: 'credits', component: CreditsComponent, canActivate: [clientOnlyGuard] },
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: AdminDashboardComponent },
          { path: 'users', component: AdminUsersComponent },
          { path: 'credits', component: AdminCreditsComponent }
        ]
      }
    ]}

];
