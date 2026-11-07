import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import {
  AdminEbookApiService,
  Category,
  CreateEbookRequest,
} from '../../services/admin-ebook-api.service';

@Component({
  selector: 'app-create-ebook',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './create-ebook.component.html',
  styleUrl: './create-ebook.component.scss',
})
export class CreateEbookComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ebookApi = inject(AdminEbookApiService);
  private readonly router = inject(Router);

  readonly categories = signal<Category[]>([]);
  readonly loadingCategories = signal(false);
  readonly submitting = signal(false);
  readonly error = signal('');
  readonly categoryError = signal('');

  readonly ebookForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', Validators.required],
    price: [0, [Validators.required, Validators.min(0)]],
    categoryId: [null as number | null, Validators.required],
    pageCount: [null as number | null, Validators.min(1)],
    language: ['English', [Validators.required, Validators.maxLength(50)]],
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loadingCategories.set(true);
    this.categoryError.set('');

    this.ebookApi.getCategories().subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.categories.set(response.data.filter((category) => category.active));
        } else {
          this.categoryError.set(response.message || 'Unable to load categories.');
        }

        this.loadingCategories.set(false);
      },
      error: () => {
        this.categoryError.set('Unable to load categories. Please try again.');
        this.loadingCategories.set(false);
      },
    });
  }

  submit(): void {
    if (this.ebookForm.invalid) {
      this.ebookForm.markAllAsTouched();
      return;
    }

    const value = this.ebookForm.getRawValue();

    const request: CreateEbookRequest = {
      title: value.title!.trim(),
      description: value.description!.trim(),
      price: Number(value.price),
      categoryId: Number(value.categoryId),
      pageCount: value.pageCount,
      language: value.language!.trim(),
    };

    this.submitting.set(true);
    this.error.set('');

    this.ebookApi.createEbook(request).subscribe({
      next: (response) => {
        this.submitting.set(false);

        if (response.success) {
          this.router.navigate(['/admin/ebooks']);
        } else {
          this.error.set(response.message || 'Unable to create ebook.');
        }
      },
      error: (error) => {
        this.submitting.set(false);

        const message = error.error?.message;
        const errors = error.error?.errors;

        this.error.set(
          Array.isArray(errors) && errors.length
            ? errors.join(', ')
            : message || 'Something went wrong while creating the ebook.',
        );
      },
    });
  }
}
