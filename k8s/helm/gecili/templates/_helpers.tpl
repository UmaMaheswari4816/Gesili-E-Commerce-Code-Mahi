{{/* Common labels applied to every object. */}}
{{- define "gecili.labels" -}}
app.kubernetes.io/part-of: gecili-ecommerce
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/* Per-service selector labels. Call with a dict: {name: <svc>}. */}}
{{- define "gecili.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
{{- end -}}
