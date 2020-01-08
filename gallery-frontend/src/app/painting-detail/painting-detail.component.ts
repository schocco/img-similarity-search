import {Component, OnDestroy, OnInit} from '@angular/core';
import {PaintingService} from "../painting.service";
import {ActivatedRoute} from "@angular/router";
import {catchError, pluck, switchMap} from "rxjs/operators";
import {BehaviorSubject, combineLatest, Observable, of, ReplaySubject, Subject, Subscription} from "rxjs";
import {Painting} from "../painting.model";
import {empty} from "rxjs/internal/Observer";

@Component({
  selector: 'app-painting-detail',
  templateUrl: './painting-detail.component.html',
  styleUrls: ['./painting-detail.component.scss']
})
export class PaintingDetailComponent implements OnInit, OnDestroy {
  private painting$: ReplaySubject<Painting> = new ReplaySubject(1);
  private similar$: Observable<Painting[]>;
  private subscriptions: Subscription[] = [];

  features: { label: string, value: string }[] = [
    {label: "similar features", value: "common512"},
    {label: "similar style", value: "style1024"},
    {label: "similar genre", value: "genre1024"}
  ];

  private selectedFeature$: BehaviorSubject<string> = new BehaviorSubject<string>(this.features[0].value);

  get selectedFeature(): string {
    return this.selectedFeature$.getValue();
  }

  set selectedFeature(feature: string) {
    this.selectedFeature$.next(feature);
  }

  constructor(private paintingService: PaintingService, private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.subscriptions.push(this.route.data
      .subscribe((data: { painting: Painting }) => {
        this.painting$.next(data.painting);
      }));
    this.similar$ = combineLatest(this.painting$, this.selectedFeature$).pipe(
      switchMap(([painting, feature]) => this.paintingService.findSimilarPaintings(painting, feature)),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(subscription => subscription.unsubscribe());
  }

}
