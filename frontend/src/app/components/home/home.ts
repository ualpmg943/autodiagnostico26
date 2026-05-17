import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { IntroducirVehiculo } from '../introducir-vehiculo/introducir-vehiculo';
import { SeleccionaProblema, ProblemaSeleccion } from '../selecciona-problema/selecciona-problema';
import { VehicleSearchContext } from '../../services/api.models';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [IntroducirVehiculo, SeleccionaProblema],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent {
  private readonly router = inject(Router);

  vehicleContext: VehicleSearchContext | null = null;
  seleccion: ProblemaSeleccion = { problemas: [], descripcionLibre: '' };

  get tieneProblema(): boolean {
    return this.seleccion.problemas.length > 0 || !!this.seleccion.descripcionLibre.trim();
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
}
