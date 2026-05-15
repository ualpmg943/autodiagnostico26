export type ChatRoomType = 'SEGUIMIENTO';
export type ChatSenderRole = 'MECANICO' | 'USUARIO';
export type AuthUserRole = 'USER' | 'TALLER' | 'ADMIN';

export interface AuthLoginRequest {
  email: string;
  password: string;
}

export interface AuthRegisterRequest {
  fullName: string;
  email: string;
  password: string;
  role: AuthUserRole;
}

export interface AuthUserResponse {
  id: number;
  fullName: string;
  email: string;
  role: AuthUserRole;
  avatarUrl: string;
  createdAt: string;
}

export interface ChatJoinResponse {
  roomType: ChatRoomType;
  participantId: number;
  activeUsers: number;
  maxUsers: number;
  joined: boolean;
}

export interface ChatMessageRequest {
  participantId: number;
  roomType: ChatRoomType;
  senderRole: ChatSenderRole;
  sessionUuid: string;
  commentText: string;
}

export interface ChatMessageResponse {
  id: number;
  roomType: ChatRoomType;
  participantId: number;
  sessionUuid: string;
  senderRole: ChatSenderRole;
  commentText: string;
  wordCount: number;
  readByUser: boolean;
  createdAt: string;
}

// ── Vehículos ──────────────────────────────────────────────────────────────

export type EngineType = 'PETROL' | 'DIESEL' | 'BEV' | 'HEV' | 'PHEV' | 'REEV';
export type TransmissionType = 'MT' | 'AT' | 'CVT' | 'iMT' | 'DCT' | 'eCVT' | 'DSG';

export interface VehicleModelSummary {
  id: number;
  name: string;
}

export interface VehicleVariant {
  id: number;
  modelName: string;
  transmission: TransmissionType | null;
  engineName: string | null;
  engineType: EngineType | null;
}

export interface UpdateUserRequest {
  fullName?: string;
  email?: string;
}

export interface UpdatePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface VehicleSearchContext {
  brand: string | null;
  modelId: number | null;
  modelName: string | null;
  variantId: number | null;
  variantName: string | null;
  engineType: EngineType | null;
  transmission: TransmissionType | null;
  year: number | null;
}
