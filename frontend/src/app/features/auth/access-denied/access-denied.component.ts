import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-access-denied',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="access-denied">
      <div class="content">
        <span class="code">403</span>
        <h1>Access denied</h1>
        <p>You don't have permission to access this page.</p>
        <a routerLink="/">Return to bookstore</a>
      </div>
    </main>
  `,
  styles: `
    .access-denied {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 24px;
      background: #f8f6f1;
      text-align: center;
    }

    .content h1 {
      color: #292820;
      font-family: Georgia, serif;
      font-size: 32px;
    }

    .content p {
      color: #77756d;
      margin-bottom: 24px;
    }

    .code {
      color: #a17b42;
      font-size: 64px;
      font-weight: 700;
    }

    a {
      display: inline-block;
      padding: 12px 24px;
      border-radius: 6px;
      background: #292820;
      color: white;
      text-decoration: none;
    }
  `,
})
export class AccessDeniedComponent {}
