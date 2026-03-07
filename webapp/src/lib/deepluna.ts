import { DeepLuna } from '../../api'

export const TOKEN_STORAGE_KEY = 'deepluna.token'
export const USER_STORAGE_KEY = 'deepluna.user'
export const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL?.trim() || 'http://localhost:8090'

type ApiEnvelope<T> = {
  success: boolean
  errCode?: number
  errMsg?: string
  data?: T
}

export const apiClient = new DeepLuna({
  BASE: API_BASE_URL,
  TOKEN: async () => localStorage.getItem(TOKEN_STORAGE_KEY) ?? '',
})

export function unwrapResult<T>(result: ApiEnvelope<T>, fallbackMessage: string): T {
  if (!result.success) {
    throw new Error(result.errMsg || `${fallbackMessage}（errCode=${result.errCode ?? -1}）`)
  }
  if (result.data === undefined || result.data === null) {
    throw new Error(`${fallbackMessage}（data 为空）`)
  }
  return result.data
}

export function ensureSuccess(
  result: ApiEnvelope<unknown>,
  fallbackMessage: string,
): void {
  if (!result.success) {
    throw new Error(result.errMsg || `${fallbackMessage}（errCode=${result.errCode ?? -1}）`)
  }
}
