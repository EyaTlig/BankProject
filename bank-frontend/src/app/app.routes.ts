import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login.component';
import {VerifyOtpComponent} from './features/auth/verify-otp/verify-otp.component';
import {LayoutComponent} from './shared/layout/layout.component';
import {authGuard} from './core/guards/auth.guard';
import {DashboardComponent} from './features/dashboard/dashboard.component';
import {TransferComponent} from './features/transfers/transfer.component';
import {RecurringTransferComponent} from './features/recurring-transfers/recurring-transfer.component';
import {AccountsComponent} from './features/accounts/accounts.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'verify-otp', component: VerifyOtpComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'recurring-transfers', component: RecurringTransferComponent },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'accounts', component: AccountsComponent },
      { path: 'transfers', component: TransferComponent }    ]}

];
