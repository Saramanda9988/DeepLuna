import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import type { ModelRequest, ModelResponse } from '../../api'
import { apiClient, ensureSuccess, unwrapResult } from '../lib/deepluna'
import { getErrorMessage } from '../lib/errors'

export function ModelsPage() {
  const queryClient = useQueryClient()
  const [modelForm, setModelForm] = useState<ModelRequest>({
    name: '',
    token: '',
    url: '',
  })

  const modelsQuery = useQuery({
    queryKey: ['models'],
    queryFn: async () =>
      unwrapResult(await apiClient.modelController.getAllModels(), '加载模型列表失败'),
  })

  const createModelMutation = useMutation({
    mutationFn: async (payload: ModelRequest) =>
      unwrapResult(await apiClient.modelController.createModel(payload), '创建模型失败'),
    onSuccess: () => {
      setModelForm({ name: '', token: '', url: '' })
      queryClient.invalidateQueries({ queryKey: ['models'] })
    },
  })

  const deleteModelMutation = useMutation({
    mutationFn: async (modelId: string) => {
      ensureSuccess(await apiClient.modelController.deleteModel(modelId), '删除模型失败')
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['models'] })
    },
  })

  const modelList = (modelsQuery.data ?? []) as ModelResponse[]

  return (
    <section className="grid gap-4 lg:grid-cols-3">
      <div className="card border border-base-300 bg-base-100 shadow-sm lg:col-span-1">
        <div className="card-body">
          <h2 className="card-title">创建模型</h2>
          <form
            className="space-y-3"
            onSubmit={(event) => {
              event.preventDefault()
              createModelMutation.mutate({
                name: modelForm.name?.trim(),
                url: modelForm.url?.trim(),
                token: modelForm.token?.trim(),
              })
            }}
          >
            <input
              className="input w-full"
              placeholder="模型名称"
              value={modelForm.name ?? ''}
              onChange={(event) => setModelForm((prev) => ({ ...prev, name: event.target.value }))}
              required
            />
            <input
              className="input w-full"
              placeholder="模型 URL"
              value={modelForm.url ?? ''}
              onChange={(event) => setModelForm((prev) => ({ ...prev, url: event.target.value }))}
              required
            />
            <input
              className="input w-full"
              placeholder="模型 Token"
              value={modelForm.token ?? ''}
              onChange={(event) => setModelForm((prev) => ({ ...prev, token: event.target.value }))}
            />
            <button className="btn btn-primary w-full" disabled={createModelMutation.isPending}>
              创建
            </button>
          </form>
          {createModelMutation.error ? (
            <div className="alert alert-error mt-2 text-sm">
              <span>{getErrorMessage(createModelMutation.error)}</span>
            </div>
          ) : null}
        </div>
      </div>

      <div className="card border border-base-300 bg-base-100 shadow-sm lg:col-span-2">
        <div className="card-body">
          <h2 className="card-title">模型列表</h2>
          <div className="overflow-x-auto">
            <table className="table table-zebra">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>名称</th>
                  <th>URL</th>
                  <th>创建时间</th>
                  <th className="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                {modelList.map((model) => (
                  <tr key={model.modelId || model.name}>
                    <td className="max-w-48 truncate">{model.modelId || '-'}</td>
                    <td>{model.name || '-'}</td>
                    <td className="max-w-72 truncate">{model.url || '-'}</td>
                    <td>{model.createTime || '-'}</td>
                    <td className="text-right">
                      <button
                        type="button"
                        className="btn btn-xs btn-error"
                        onClick={() => {
                          if (model.modelId) {
                            deleteModelMutation.mutate(model.modelId)
                          }
                        }}
                        disabled={!model.modelId || deleteModelMutation.isPending}
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {modelsQuery.isLoading ? <span className="loading loading-spinner loading-md" /> : null}
          {modelsQuery.error ? (
            <div className="alert alert-error mt-2 text-sm">
              <span>{getErrorMessage(modelsQuery.error)}</span>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  )
}
