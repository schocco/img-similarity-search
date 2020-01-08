import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {GalleryComponent} from "./gallery/gallery.component";
import {PaintingDetailComponent} from "./painting-detail/painting-detail.component";
import {PaintingResolver} from "./painting.resolver";


const routes: Routes = [
  {path: '', component: GalleryComponent},
  {
    path: ':id',
    component: PaintingDetailComponent,
    resolve: {
      painting: PaintingResolver
    }
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
