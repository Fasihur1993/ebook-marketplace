import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthStateService } from '../services/auth-state.service';

export const roleGuard: CanActivateFn = (route) => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  const user = authState.currentUser();
  const allowedRoles = route.data['roles'] as string[] | undefined;

  if (!user) {
    return router.createUrlTree(['/login']);
  }

  if (!allowedRoles || allowedRoles.includes(user.role)) {
    return true;
  }

  return router.createUrlTree(['/']);
};
