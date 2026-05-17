import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Workshop } from '../../services/api.models';
import { AuthStateService } from '../../services/auth-state.service';
import { GeolocationService } from '../../services/geolocation.service';
import { WorkshopService } from '../../services/workshop.service';
import { MapComponent } from '../map/map.component';

@Component({
  selector: 'app-taller-page',
  standalone: true,
  imports: [CommonModule, MapComponent],
  templateUrl: './taller.html',
  styleUrls: ['./taller.css']
})
export class TallerComponent implements OnInit {
  private readonly workshopService = inject(WorkshopService);
  private readonly authState = inject(AuthStateService);
  private readonly geoService = inject(GeolocationService);
  private readonly router = inject(Router);

  /** Lista cruda recibida de la BD. */
  private readonly _workshops = signal<Workshop[]>([]);

  /**
   * Lista de talleres ordenada por distancia al usuario (Haversine).
   * Si el GPS no está disponible aún, devuelve la lista sin ordenar.
   * Se re-evalúa automáticamente cuando cambia la posición o la lista.
   */
  readonly workshops = computed(() => {
    const ws = this._workshops();
    const coords = this.geoService.locationState().coords;
    if (!coords) return ws;
    return [...ws].sort(
      (a, b) => this.haversineKm(coords.lat, coords.lng, a.latitude, a.longitude)
              - this.haversineKm(coords.lat, coords.lng, b.latitude, b.longitude)
    );
  });

  readonly selectedWorkshop = signal<Workshop | null>(null);
  readonly loading = signal(true);
  readonly selecting = signal(false);
  readonly error = signal('');
  readonly userId = computed(() => this.authState.userId());

  ngOnInit(): void {
    this.loadWorkshops();
  }

  loadWorkshops(): void {
    this.loading.set(true);
    this.error.set('');

    this.workshopService.listWorkshops(this.userId()).subscribe({
      next: (workshops) => {
        this._workshops.set(workshops);
        // Preseleccionar respetando el orden por proximidad
        const sorted = this.workshops();
        this.selectedWorkshop.set(sorted.find((w) => w.selectedByClient) ?? sorted[0] ?? null);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se han podido cargar los talleres. Intentalo de nuevo en unos segundos.');
        this.loading.set(false);
      }
    });
  }

  selectPreview(workshop: Workshop): void {
    this.selectedWorkshop.set(workshop);
  }

  chooseWorkshop(workshop: Workshop): void {
    const clientId = this.userId();
    if (!clientId) {
      this.error.set('Inicia sesion como cliente para elegir un taller.');
      return;
    }

    if (workshop.selectedByClient && workshop.sessionUuid) {
      this.goToTracking(workshop.sessionUuid);
      return;
    }

    this.selecting.set(true);
    this.error.set('');

    this.workshopService.selectWorkshop(workshop.id, clientId).subscribe({
      next: (response) => {
        localStorage.setItem('trackingSessionUuid', response.tracking.sessionUuid);
        this.selecting.set(false);
        this.loadWorkshops();
        this.router.navigate(['/usuario/seguimiento/chat']);
      },
      error: (err) => {
        this.error.set(err.status === 409
          ? 'Este taller esta completo ahora mismo. Elige otro taller disponible.'
          : 'No se ha podido crear la sesion con el mecanico.');
        this.selecting.set(false);
      }
    });
  }

  goToTracking(sessionUuid: string | null): void {
    if (sessionUuid) {
      localStorage.setItem('trackingSessionUuid', sessionUuid);
    }
    this.router.navigate(['/usuario/seguimiento/chat']);
  }

  occupancyPercent(workshop: Workshop): number {
    if (workshop.vehicleLimit <= 0) {
      return 100;
    }

    return Math.min(100, Math.round((workshop.activeVehicles / workshop.vehicleLimit) * 100));
  }

  occupancyLabel(workshop: Workshop): string {
    return this.isFull(workshop) ? 'Completo' : `${this.occupancyPercent(workshop)}% ocupado`;
  }

  isFull(workshop: Workshop): boolean {
    return workshop.activeVehicles >= workshop.vehicleLimit && !workshop.selectedByClient;
  }

  /**
   * Distancia en km entre dos coordenadas usando la fórmula de Haversine.
   * Ref: https://en.wikipedia.org/wiki/Haversine_formula
   */
  private haversineKm(lat1: number, lng1: number, lat2: number, lng2: number): number {
    const R = 6371;
    const toRad = (deg: number) => deg * Math.PI / 180;
    const dLat = toRad(lat2 - lat1);
    const dLng = toRad(lng2 - lng1);
    const a = Math.sin(dLat / 2) ** 2
            + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2;
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
}
