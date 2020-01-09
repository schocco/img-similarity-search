import {Component, Input, OnInit} from '@angular/core';
import {Painting} from "../painting.model";

@Component({
  selector: 'app-gallery-item',
  templateUrl: './gallery-item.component.html',
  styleUrls: ['./gallery-item.component.scss']
})
export class GalleryItemComponent implements OnInit {

  @Input()
  public painting: Painting;

  @Input()
  public showScore: boolean = false;

  constructor() { }

  ngOnInit() {
  }

}
