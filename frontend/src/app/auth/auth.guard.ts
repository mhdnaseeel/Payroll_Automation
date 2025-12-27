import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const userRole = authService.getRole();

    if (userRole) {
        // Check if route requires specific roles
        const expectedRoles = route.data['roles'] as Array<string>;

        if (expectedRoles && expectedRoles.length > 0) {
            if (expectedRoles.includes(userRole)) {
                return true;
            } else {
                // Unauthorized role access
                // Redirect to their own dashboard
                if (userRole === 'ADMIN') {
                    return router.parseUrl('/admin/employees');
                } else if (userRole === 'BILL') {
                    return router.parseUrl('/billing');
                } else {
                    return router.parseUrl('/user/home');
                }
            }
        }

        return true;
    }

    // Not logged in
    return router.parseUrl('/login');
};
