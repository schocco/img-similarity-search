import {NgModule} from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {GalleryComponent} from "./gallery/gallery.component";
import {PaintingDetailComponent} from "./painting-detail/painting-detail.component";


const routes: Routes = [
  {path: '', component: GalleryComponent},
  {path: ':id', component: PaintingDetailComponent}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
