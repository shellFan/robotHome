export const PLACEHOLDER_IMAGE = '/assets/placeholder.png'

const RANDOM_IMAGE_SERVICE = /picsum\.photos|loremflickr|unsplash\.com/i

export function imageSource(url) {
  if (typeof url !== 'string') return PLACEHOLDER_IMAGE
  const value = url.trim()
  if (!value || RANDOM_IMAGE_SERVICE.test(value)) return PLACEHOLDER_IMAGE
  return value
}
