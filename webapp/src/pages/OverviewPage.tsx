import { useQuery } from '@tanstack/react-query'
import { apiClient, unwrapResult } from '../lib/deepluna'
import { useAuth } from '../context/AuthContext'

export function OverviewPage() {
  const auth = useAuth()
  const userId = auth.user?.userId

  const modelsQuery = useQuery({
    queryKey: ['models'],
    queryFn: async () =>
      unwrapResult(await apiClient.modelController.getAllModels(), '加载模型列表失败'),
  })

  const sessionsQuery = useQuery({
    queryKey: ['sessions', userId],
    enabled: typeof userId === 'number',
    queryFn: async () => {
      if (typeof userId !== 'number') {
        return []
      }
      return unwrapResult(
        await apiClient.sessionController.getUserSessions(userId),
        '加载会话列表失败',
      )
    },
  })

  const embeddingHealthQuery = useQuery({
    queryKey: ['embedding-health'],
    queryFn: async () =>
      unwrapResult(await apiClient.embeddingController.healthCheck(), '加载向量库健康状态失败'),
  })

  return (
    <section className="grid gap-4 md:grid-cols-3">
      <div className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title text-base">模型数量</h2>
          <p className="text-3xl font-bold">{modelsQuery.data?.length ?? 0}</p>
        </div>
      </div>
      <div className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title text-base">会话数量</h2>
          <p className="text-3xl font-bold">{sessionsQuery.data?.length ?? 0}</p>
        </div>
      </div>
      <div className="card border border-base-300 bg-base-100 shadow-sm">
        <div className="card-body">
          <h2 className="card-title text-base">Embedding 健康</h2>
          <p className="text-sm">
            {embeddingHealthQuery.isLoading
              ? '加载中...'
              : embeddingHealthQuery.data || '无状态'}
          </p>
        </div>
      </div>
    </section>
  )
}
