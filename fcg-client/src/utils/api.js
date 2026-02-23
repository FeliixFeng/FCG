import http from './http'

export const fetchFamilyInfo = () => http.get('/api/family/info')
export const createFamily = (familyName) => http.post(`/api/family/create?familyName=${encodeURIComponent(familyName)}`)
export const joinFamily = (inviteCode) => http.post(`/api/family/join?inviteCode=${encodeURIComponent(inviteCode)}`)
export const fetchFamilyMembers = () => http.get('/api/family/members')
