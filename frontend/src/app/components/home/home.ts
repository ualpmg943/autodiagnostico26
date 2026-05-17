import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IntroducirVehiculo } from '../introducir-vehiculo/introducir-vehiculo';
import { SeleccionaProblema, ProblemaSeleccion } from '../selecciona-problema/selecciona-problema';
import { VehicleSearchContext, PersonalVehicleResponse } from '../../services/api.models';
import { AuthStateService } from '../../services/auth-state.service';
import { PersonalVehicleApiService } from '../../services/personal-vehicle-api.service';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, FormsModule, IntroducirVehiculo, SeleccionaProblema],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent {
  private readonly router = inject(Router);

  vehicleContext: VehicleSearchContext | null = null;
  seleccion: ProblemaSeleccion = { problemas: [], descripcionLibre: '' };

  personalVehicles: PersonalVehicleResponse[] = [];
  selectedPersonalVehicleId: number | null = null;
  prefillContext: VehicleSearchContext | null = null;

  get tieneProblema(): boolean {
    return this.seleccion.problemas.length > 0 || !!this.seleccion.descripcionLibre.trim();
  }

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  ngOnInit(): void {
    if (!this.isLoggedIn) {
      return;
    }
    const ownerId = this.auth.userId();
    if (ownerId === null) {
      return;
    }

    const paramId = this.route.snapshot.queryParamMap.get('personalVehicleId');
    const preselectId = paramId ? Number(paramId) : null;

    this.personalVehicleApi.listByOwner(ownerId).subscribe({
      next: (vehicles: PersonalVehicleResponse[]) => {
        this.personalVehicles = vehicles;
        if (preselectId && vehicles.some(v => v.id === preselectId)) {
          this.applyPersonalVehicle(preselectId);
        }
      },
      error: () => {
        // silencioso: el usuario puede seguir introduciendo a mano
      },
    });
  }

  onPersonalVehicleSelect(id: number | null): void {
    this.selectedPersonalVehicleId = id;
    if (id === null) {
      this.prefillContext = null;
      return;
    }
    this.applyPersonalVehicle(id);
  }

  onVehicleContextChange(ctx: VehicleSearchContext): void {
    this.vehicleContext = ctx;
  }

  onProblemaChange(seleccion: ProblemaSeleccion): void {
    this.seleccion = seleccion;
  }

  onEnviar(): void {
    this.router.navigate(['/diagnostico'], {
      state: {
        vehicle: this.vehicleContext,
        problemas: this.seleccion,
      },
    });
  }

  displayName(v: PersonalVehicleResponse): string {
    const parts = [v.brand, v.vehicleName, v.modelName].filter(Boolean);
    return parts.length > 0 ? parts.join(' ') : `Vehículo #${v.id}`;
  }

  private applyPersonalVehicle(id: number): void {
    const vehicle = this.personalVehicles.find(v => v.id === id);
    if (!vehicle) return;
    this.selectedPersonalVehicleId = id;
    this.prefillContext = {
      brand: vehicle.brand,
      modelId: null,
      modelName: vehicle.vehicleName,
      variantId: vehicle.vehicleModelId,
      variantName: vehicle.modelName,
      engineType: vehicle.engineType,
      transmission: vehicle.transmission,
      year: vehicle.year,
    };
  }
}
