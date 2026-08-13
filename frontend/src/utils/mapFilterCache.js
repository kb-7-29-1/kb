const AMENITY_FILTER_CACHE_PREFIX = 'kb_applied_amenity_filters_';
const QUICK_FILTER_CACHE_PREFIX = 'kb_quick_filter_state_';
const LEGACY_AMENITY_FILTER_CACHE_KEY = 'kb_applied_amenity_filters';

const getUserId = (user) => user?.userId || user?.id || null;

export const getAmenityFilterCacheKey = (user) => {
  const userId = getUserId(user);
  return userId ? `${AMENITY_FILTER_CACHE_PREFIX}${userId}` : null;
};

export const getQuickFilterCacheKey = (user) => {
  const userId = getUserId(user) || 'guest';
  return `${QUICK_FILTER_CACHE_PREFIX}${userId}`;
};

export const loadAmenityFilterCache = (user) => {
  const cacheKey = getAmenityFilterCacheKey(user);
  if (!cacheKey) return [];

  try {
    const cached = localStorage.getItem(cacheKey);
    const filters = cached ? JSON.parse(cached) : [];
    return Array.isArray(filters) ? filters : [];
  } catch {
    return [];
  }
};

export const saveAmenityFilterCache = (user, filters) => {
  const cacheKey = getAmenityFilterCacheKey(user);
  if (!cacheKey) return;

  if (Array.isArray(filters) && filters.length) {
    localStorage.setItem(cacheKey, JSON.stringify(filters));
  } else {
    localStorage.removeItem(cacheKey);
  }
};

export const clearMapFilterCache = (user) => {
  saveAmenityFilterCache(user, []);

  localStorage.removeItem(getQuickFilterCacheKey(user));
  localStorage.removeItem(LEGACY_AMENITY_FILTER_CACHE_KEY);
};
