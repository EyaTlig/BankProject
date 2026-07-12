import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './Login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {

  form: FormGroup;
  loading = false;
  errorMessage: string | null = null;
  infoMessage: string | null = null;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  ngOnInit(): void {
    if (this.route.snapshot.queryParamMap.get('reason') === 'inactivity') {
      this.infoMessage = 'Votre session a expiré après une période d\'inactivité. Veuillez vous reconnecter.';
    }
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    this.authService.login(this.form.value).subscribe({
      next: (response) => {
        this.loading = false;
        if (response.requiresOtp) {
          this.router.navigate(['/verify-otp'], {
            queryParams: { email: this.form.value.email }
          });
        }
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Email ou mot de passe incorrect.';
      }
    });
  }

  get emailControl() {
    return this.form.get('email');
  }

  get passwordControl() {
    return this.form.get('password');
  }
}
