import { Platform } from 'react-native';

export interface RequestInterceptor {
  onRequest(url: string, init: RequestInit): Promise<[string, RequestInit]> | [string, RequestInit];
}

export interface ResponseInterceptor {
  onResponse(response: Response): Promise<Response> | Response;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly body?: unknown,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const DEFAULT_BASE_URL =
  Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080';

export class HttpClient {
  private readonly requestInterceptors: RequestInterceptor[] = [];
  private readonly responseInterceptors: ResponseInterceptor[] = [];

  constructor(private readonly baseUrl: string = DEFAULT_BASE_URL) {}

  addRequestInterceptor(interceptor: RequestInterceptor): void {
    this.requestInterceptors.push(interceptor);
  }

  addResponseInterceptor(interceptor: ResponseInterceptor): void {
    this.responseInterceptors.push(interceptor);
  }

  async get<T>(path: string, params?: Record<string, string | number | boolean | undefined>): Promise<T> {
    return this.request<T>('GET', this.buildPathWithQuery(path, params));
  }

  async post<T>(
    path: string,
    body?: unknown,
    params?: Record<string, string | number | boolean | undefined>,
  ): Promise<T> {
    return this.request<T>('POST', this.buildPathWithQuery(path, params), body);
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>('PUT', path, body);
  }

  async delete<T = void>(path: string): Promise<T> {
    return this.request<T>('DELETE', path);
  }

  private buildPathWithQuery(
    path: string,
    params?: Record<string, string | number | boolean | undefined>,
  ): string {
    if (!params) return path;
    const entries = Object.entries(params).filter(([, v]) => v !== undefined);
    if (entries.length === 0) return path;
    const query = entries
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&');
    return `${path}${path.includes('?') ? '&' : '?'}${query}`;
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    let url = `${this.baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
    let init: RequestInit = {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
    };

    for (const interceptor of this.requestInterceptors) {
      [url, init] = await interceptor.onRequest(url, init);
    }

    let response = await fetch(url, init);

    for (const interceptor of this.responseInterceptors) {
      response = await interceptor.onResponse(response);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const text = await response.text();
    let parsed: unknown = undefined;
    if (text.length > 0) {
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = text;
      }
    }

    if (!response.ok) {
      const errorMsg =
        typeof parsed === 'object' && parsed !== null && 'message' in parsed
          ? String((parsed as { message: unknown }).message)
          : `HTTP ${response.status}: ${response.statusText || 'Request failed'}`;
      throw new ApiError(response.status, errorMsg, parsed);
    }

    return parsed as T;
  }
}
