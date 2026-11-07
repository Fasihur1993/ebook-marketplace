import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  AdminCategoryApiService,
  SaveCategoryRequest,
} from '../services/admin-category-api.service';
import { Category } from '../../../core/models/category.model';

@Component({
  selector: 'app-category-management',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly categoryApi = inject(AdminCategoryApiService);
  private readonly fb = inject(FormBuilder);

  readonly categories = signal<Category[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly saving = signal(false);
  readonly modalOpen = signal(false);
  readonly editingCategory = signal<Category | null>(null);
  readonly saveError = signal('');

  readonly categoryToToggle = signal<Category | null>(null);
  readonly changingCategoryId = signal<number | null>(null);
  readonly statusError = signal('');

  readonly categoryForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', Validators.maxLength(500)],
  });

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.error.set('');

    this.categoryApi.getCategories().subscribe({
      next: (response) => {
        this.categories.set(response.data ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Unable to load categories. Please try again.');
        this.loading.set(false);
      },
    });
  }

  openCreateModal(): void {
    this.editingCategory.set(null);
    this.saveError.set('');

    this.categoryForm.reset({
      name: '',
      description: '',
    });

    this.modalOpen.set(true);
  }

  openEditModal(category: Category): void {
    this.editingCategory.set(category);
    this.saveError.set('');

    this.categoryForm.reset({
      name: category.name,
      description: category.description ?? '',
    });

    this.modalOpen.set(true);
  }

  closeModal(): void {
    if (this.saving()) {
      return;
    }

    this.modalOpen.set(false);
    this.editingCategory.set(null);
  }

  saveCategory(): void {
    if (this.categoryForm.invalid) {
      this.categoryForm.markAllAsTouched();
      return;
    }

    const request: SaveCategoryRequest = {
      name: this.categoryForm.controls.name.value.trim(),
      description: this.categoryForm.controls.description.value.trim(),
    };

    if (!request.name) {
      this.categoryForm.controls.name.setErrors({ required: true });
      return;
    }

    this.saving.set(true);
    this.saveError.set('');

    const category = this.editingCategory();

    const operation = category
      ? this.categoryApi.updateCategory(category.id, request)
      : this.categoryApi.createCategory(request);

    operation.subscribe({
      next: () => {
        this.saving.set(false);
        this.modalOpen.set(false);
        this.editingCategory.set(null);
        this.loadCategories();
      },
      error: (error) => {
        this.saving.set(false);

        this.saveError.set(
          error.status === 409
            ? 'A category with this name already exists.'
            : (error.error?.message ?? 'Unable to save category. Please try again.'),
        );
      },
    });
  }

  confirmStatusChange(category: Category): void {
    this.statusError.set('');
    this.categoryToToggle.set(category);
  }

  cancelStatusChange(): void {
    if (this.changingCategoryId() !== null) {
      return;
    }

    this.categoryToToggle.set(null);
    this.statusError.set('');
  }

  updateCategoryStatus(): void {
    const category = this.categoryToToggle();

    if (!category) {
      return;
    }

    const newStatus = !category.active;

    this.changingCategoryId.set(category.id);
    this.statusError.set('');

    this.categoryApi.updateCategoryStatus(category.id, newStatus).subscribe({
      next: (response) => {
        if (response.data) {
          this.categories.update((categories) =>
            categories.map((item) => (item.id === category.id ? response.data! : item)),
          );
        } else {
          this.loadCategories();
        }

        this.changingCategoryId.set(null);
        this.categoryToToggle.set(null);
      },
      error: (error) => {
        this.changingCategoryId.set(null);

        this.statusError.set(
          error.error?.message ?? 'Unable to update category status. Please try again.',
        );
      },
    });
  }
}
