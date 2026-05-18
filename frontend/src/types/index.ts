// ============================================
// AUTH DTOs
// ============================================
export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  access_token: string
  token_type: string
  expires_in: number
  role: 'FARMER' | 'ADMIN'
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
}

export interface UserProfile {
  id: string
  username: string
  email: string
  role: 'FARMER' | 'ADMIN'
  created_at: string
}

// ============================================
// FARM DTOs
// ============================================
export interface FarmCreateRequest {
  name: string
  location?: string
}

export interface FarmResponse {
  id: string
  owner_id: string
  name: string
  location?: string
  created_at: string
}

// ============================================
// DEVICE MODEL DTOs
// ============================================
export type DeviceType = 'SENSOR' | 'ACTUATOR'
export type MetricType = 'TEMP' | 'HUMIDITY' | 'SOIL_MOISTURE' | 'LIGHT' | 'PRESSURE' | 'CO2' | 'PUMP' | 'VALVE' | 'RELAY' | 'FAN'

export interface DeviceModelRequest {
  model_name: string
  device_type: DeviceType
  metric_type: MetricType
  manufacturer?: string
  display_unit?: string
  min_value?: number | null
  max_value?: number | null
  model_description?: string
  reference_url?: string
}

export interface DeviceModelResponse {
  id: string
  model_name: string
  device_type: DeviceType
  metric_type: MetricType
  manufacturer?: string
  display_unit?: string | null
  min_value?: number | null
  max_value?: number | null
  model_description?: string | null
  reference_url?: string | null
}

// ============================================
// DEVICE DTOs
// ============================================
export type DeviceStatus = 'PENDING' | 'ACTIVE' | 'REJECTED' | 'PENDING_REMOVAL'
export type ConnectionStatus = 'ONLINE' | 'OFFLINE'
export type OperatingMode = 'AUTO' | 'MANUAL'

export interface DeviceRequest {
  farm_id: string
  model_id: string
  name: string
}

export interface DeviceResponse {
  id: string
  farm_id: string
  model_id: string
  name: string
  status: DeviceStatus
  adafruit_feed_key: string | null
  connection_status: ConnectionStatus
  last_seen: string | null
  operating_mode: OperatingMode
  is_active: boolean
  min_value: number | null
  max_value: number | null
}

export interface AutomationConfigResponse {
  max_auto_on_minutes: number
  command_cooldown_seconds: number
}

export interface AdminStatsResponse {
  total_farmers: number
  total_farms: number
  total_devices: number
  pending_requests: number
  active_devices: number
  api_status: boolean
  mqtt_status: boolean
  db_status: boolean
}

export interface AdminFarmerResponse {
  id: string
  username: string
  email: string
  created_at: string
  farm_count: number
}

export interface AdminFarmResponse {
  id: string
  name: string
  location: string | null
  owner_name: string
  owner_email: string
  created_at: string
  device_count: number
}

export interface AdminDeviceResponse {
  id: string
  name: string
  model_name: string
  device_type: DeviceType
  status: DeviceStatus
  farm_name: string
  owner_name: string
  connection_status: ConnectionStatus
  is_active: boolean
}

// Enriched device with model info (frontend convenience)
export interface DeviceWithModel extends DeviceResponse {
  device_type?: DeviceType
  metric_type?: MetricType
  model_name?: string
  display_unit?: string | null
}

// ============================================
// RULE DTOs
// ============================================
export type RuleType = 'CONDITION' | 'SCHEDULE'
export type Operator = '>' | '<' | '=='
export type ActionCommand = 'ON' | 'OFF'

export interface RuleCreateRequest {
  farm_id: string
  rule_name: string
  rule_type: RuleType
  trigger_device_id?: string | null
  operator?: Operator | null
  threshold_value?: number | null
  cron_expression?: string | null
  action_device_id: string
  action_command: ActionCommand
}

export interface RuleResponse {
  id: string
  farm_id: string
  rule_name: string
  rule_type: RuleType
  trigger_device_id: string | null
  operator: Operator | null
  threshold_value: number | null
  cron_expression: string | null
  action_device_id: string
  action_command: ActionCommand
  is_active: boolean
}

// ============================================
// TELEMETRY DTOs
// ============================================
export interface TelemetryDataPoint {
  time: string
  value: number
}

export type AggregateInterval = '15m' | '1h' | '1d' | ''

// ============================================
// NOTIFICATION DTOs
// ============================================
export interface NotificationResponse {
  id: string
  message: string
  is_read: boolean
  created_at: string
}

// ============================================
// ERROR DTO
// ============================================
export interface ErrorResponse {
  code: number
  message: string
  details: string
}

// ============================================
// WEBSOCKET TELEMETRY MESSAGE
// ============================================
export interface TelemetryMessage {
  deviceId: string
  metricType: MetricType
  value: number
  timestamp: string
}
