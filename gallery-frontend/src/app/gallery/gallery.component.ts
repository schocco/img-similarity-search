import { Component, OnInit } from '@angular/core';
import {PaintingService} from "../painting.service";
import {Observable} from "rxjs";
import {Painting} from "../painting.model";

@Component({
  selector: 'app-gallery',
  templateUrl: './gallery.component.html',
  styleUrls: ['./gallery.component.scss']
})
export class GalleryComponent implements OnInit {

  private paintings: Observable<Painting>;

  constructor(private paintingService: PaintingService) {
  }

  ngOnInit() {
    this.paintings = this.paintingService.findPaintings();
  }

}
