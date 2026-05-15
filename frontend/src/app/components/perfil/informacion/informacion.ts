import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthStateService } from '../../../services/auth-state.service';
import { UserApiService } from '../../../services/user-api.service';
import { AuthUserResponse } from '../../../services/api.models';

@Component({
  selector: 'app-perfil-informacion',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './informacion.html',
  styleUrl: './informacion.css'
})
export class PerfilInformacionComponent implements OnInit {
  private readonly authStateService = inject(AuthStateService);
  private readonly userApiService = inject(UserApiService);

  profile: AuthUserResponse | null = null;
  isEditing = false;
  editFullName = '';
  editEmail = '';
  isLoading = true;
  errorMessage = '';
  successMessage = '';

  ngOnInit(): void {
    const userId = this.authStateService.userId();
    if (userId === null) return;
    this.userApiService.getProfile(userId).subscribe({
      next: (user) => {
        this.profile = user;
        this.editFullName = user.fullName;
        this.editEmail = user.email;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = 'No se pudo cargar la información del perfil.';
      }
    });
  }

  get roleLabel(): string {
    if (!this.profile) return '';
    const labels: Record<string, string> = { USER: 'Usuario', TALLER: 'Mecánico', ADMIN: 'Administrador' };
    return labels[this.profile.role] || this.profile.role;
  }

  get createdAtFormatted(): string {
    if (!this.profile) return '';
    const date = new Date(this.profile.createdAt);
    return date.toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  toggleEdit(): void {
    this.isEditing = !this.isEditing;
    this.errorMessage = '';
    this.successMessage = '';
    if (!this.isEditing && this.profile) {
      this.editFullName = this.profile.fullName;
      this.editEmail = this.profile.email;
    }
  }

  saveChanges(): void {
    const userId = this.authStateService.userId();
    if (userId === null) return;

    this.errorMessage = '';
    this.successMessage = '';

    this.userApiService.updateProfile(userId, {
      fullName: this.editFullName,
      email: this.editEmail
    }).subscribe({
      next: (updated) => {
        this.profile = updated;
        this.isEditing = false;
        this.successMessage = 'Información actualizada correctamente.';
        this.authStateService.setSession({
          userName: updated.fullName,
          email: updated.email,
          userAvatar: updated.avatarUrl
        });
      },
      error: (err) => {
        if (err?.status === 409) {
          this.errorMessage = 'Ya existe una cuenta con ese correo.';
        } else {
          this.errorMessage = 'No se pudo actualizar la información.';
        }
      }
    });
  }
}
