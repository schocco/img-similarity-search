import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {MatCardModule} from "@angular/material/card";
import { GalleryComponent } from './gallery/gallery.component';
import { PaintingDetailComponent } from './painting-detail/painting-detail.component';
import {FlexLayoutModule} from "@angular/flex-layout";
import {MatButtonModule} from "@angular/material/button";
import { GalleryItemComponent } from './gallery-item/gallery-item.component';

@NgModule({
  declarations: [
    AppComponent,
    GalleryComponent,
    PaintingDetailComponent,
    GalleryItemComponent
  ],
  imports: [
    BrowserModule,
    FlexLayoutModule,
    AppRoutingModule,
    BrowserAnimationsModule,
    MatCardModule,
    MatButtonModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
