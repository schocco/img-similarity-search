import { TestBed } from '@angular/core/testing';

import { PaintingService } from './painting.service';

describe('PaintingService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: PaintingService = TestBed.get(PaintingService);
    expect(service).toBeTruthy();
  });
});
