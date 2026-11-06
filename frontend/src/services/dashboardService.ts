import { apiRequest } from './api';
import { authService } from './authService';
import type { DashboardResumo } from '../types/dashboard';

export const dashboardService = {
  consultar: (pagina: number, signal?: AbortSignal) => apiRequest<DashboardResumo>(`/api/dashboard?pagina=${pagina}`, { token: authService.tokenAtual(), signal }),
};
