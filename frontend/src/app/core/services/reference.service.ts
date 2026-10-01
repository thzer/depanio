import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EnumOption } from '../models/enums';

interface ReferenceData {
  priorities: EnumOption[];
  statuses: EnumOption[];
  categories: EnumOption[];
  equipmentTypes: EnumOption[];
  establishmentTypes: EnumOption[];
  roles: EnumOption[];
}

/**
 * Listes de valeurs des formulaires, chargees depuis le serveur.
 *
 * L'appel est mis en cache par {@code shareReplay} : ces listes ne changent
 * pas pendant la session, il est donc inutile de les redemander a chaque
 * ouverture d'un formulaire.
 */
@Injectable({ providedIn: 'root' })
export class ReferenceService {
  private readonly http = inject(HttpClient);

  private readonly data$: Observable<ReferenceData> = this.http
    .get<ReferenceData>(`${environment.apiUrl}/reference/all`)
    .pipe(shareReplay({ bufferSize: 1, refCount: false }));

  all(): Observable<ReferenceData> {
    return this.data$;
  }
}
