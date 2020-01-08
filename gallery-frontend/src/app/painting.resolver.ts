import {Injectable} from "@angular/core";
import {ActivatedRouteSnapshot, Resolve, RouterStateSnapshot} from "@angular/router";
import {Painting} from "./painting.model";
import {PaintingService} from "./painting.service";
import {Observable} from "rxjs";

@Injectable({providedIn: 'root'})
export class PaintingResolver implements Resolve<Painting> {
  constructor(private service: PaintingService) {
  }

  resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Painting> {
    return this.service.getPainting(route.paramMap.get('id'));
  }
}
