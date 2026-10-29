{{/* Common labels applied to every object. */}}
{{- define "gesili.labels" -}}
app.kubernetes.io/part-of: gesili-ecommerce
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}

{{/* Per-service selector labels. Call with a dict: {name: <svc>}. */}}
{{- define "gesili.selectorLabels" -}}
app.kubernetes.io/name: {{ .name }}
{{- end -}}
