import {Injectable} from '@angular/core';
import {HttpClient} from "@angular/common/http";
import {Observable, of} from "rxjs";
import {Painting} from "./painting.model";
import {catchError} from "rxjs/operators";

@Injectable({
  providedIn: 'root'
})
export class PaintingService {

  constructor(private httpClient: HttpClient) {
  }

  findPaintings(): Observable<Painting> {
    return this.httpClient.get<Painting>('/api/paintings');
  }

  findSimilarPaintings(painting: Painting, feature: string): Observable<Painting[]> {
    return this.httpClient.get<Painting[]>(`/api/paintings/${painting.id}/similar-paintings?feature=${feature}`).pipe(
      catchError(err => of([]))
    );
  }

  getPainting(paintingId: string) {
    return this.httpClient.get<Painting>(`/api/paintings/${paintingId}`);
  }
}
