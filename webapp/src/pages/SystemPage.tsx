import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { apiClient, ensureSuccess, unwrapResult } from '../lib/deepluna'

export function SystemPage() {
  const queryClient = useQueryClient()
  const [providerId, setProviderId] = useState('')

  const providerQuery = useQuery({
    queryKey: ['websearch-provider'],
    queryFn: async () =>
      unwrapResult(
        await apiClient.systemConfigController.getWebSearchProvider(),
        '加载 WebSearch 配置失败',
      ),
  })

  const embeddingHealthQuery = useQuery({
    queryKey: ['embedding-health'],
    queryFn: async () =>
      unwrapResult(await apiClient.embeddingController.healthCheck(), '加载向量库健康状态失败'),
  })

  const updateProviderMutation = useMutation({
    mutationFn: async (nextProviderId: string) => {
      ensureSuccess(
        await apiClient.systemConfigController.updateWebSearchProvider({
          providerId: nextProviderId,
        }),
        '更新 Provider 失败',
      )
    },
    onSuccess: () => {
      setProviderId('')
      queryClient.invalidateQueries({ queryKey: ['websearch-provider'] })
    },
  })

  const providerEntries = Object.entries(providerQuery.data ?? {})

  return (
    <section className="grid gap-4 md:grid-cols-2">
      <div className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title">WebSearch Provider</h2>
          <div className="overflow-x-auto rounded-lg border border-base-300">
            <table className="table table-sm">
              <tbody>
                {providerEntries.length === 0 ? (
                  <tr>
                    <td className="text-sm text-base-content/60">暂无配置</td>
                  </tr>
                ) : (
                  providerEntries.map(([key, value]) => (
                    <tr key={key}>
                      <td>{key}</td>
                      <td>{value}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <form
            className="mt-3 flex gap-2"
            onSubmit={(event) => {
              event.preventDefault()
              if (!providerId.trim()) {
                return
              }
              updateProviderMutation.mutate(providerId.trim())
            }}
          >
            <input
              className="input w-full"
              placeholder="新的 providerId"
              value={providerId}
              onChange={(event) => setProviderId(event.target.value)}
              required
            />
            <button className="btn btn-primary" disabled={updateProviderMutation.isPending}>
              更新
            </button>
          </form>
        </div>
      </div>

      <div className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title">Embedding 状态</h2>
          <p className="rounded-lg bg-base-200 p-3 text-sm">
            {embeddingHealthQuery.isLoading
              ? '加载中...'
              : embeddingHealthQuery.data || '无返回'}
          </p>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => queryClient.invalidateQueries({ queryKey: ['embedding-health'] })}
          >
            刷新健康状态
          </button>
        </div>
      </div>
    </section>
  )
}
