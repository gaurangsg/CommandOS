import { z } from 'zod';

export const ApiResponseSchema = <T extends z.ZodType>(dataSchema: T) =>
  z.object({
    success: z.literal(true),
    data: dataSchema,
    timestamp: z.string().datetime({ offset: true }),
  });

const HealthSchema = z.object({
  status: z.literal('UP'),
  service: z.string(),
});

export type ApiHealth = z.infer<typeof HealthSchema>;

export const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1').replace(/\/$/, '');

export async function fetchHealth(signal?: AbortSignal): Promise<ApiHealth> {
  const response = await fetch(`${apiBaseUrl}/health`, {
    headers: { Accept: 'application/json' },
    signal,
  });

  if (!response.ok) {
    throw new Error(`API health check failed (${response.status})`);
  }

  const body: unknown = await response.json();
  return ApiResponseSchema(HealthSchema).parse(body).data;
}
